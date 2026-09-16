#!/usr/bin/env bash
# ⚔ Tekton Agent installer — macOS / Linux / Termux
# Usage: ./install.sh [--minimal|--recommended|--full] [--os-app] [--mobile-chat]
#                      [--model NAME] [--provider URL] [--ov-server URL] [--user NAME]
set -euo pipefail

PROFILE="recommended"; OS_APP=0; MOBILE=0; OV_SERVER=""; MODEL=""; PROVIDER_URL=""; TEKTON_USER=""; SKIP_RUNTIME=0; ENGINES=""; ALL_ENGINES=0; ASSUME_YES=0; OPT_INSTALL=0; TOOLCHAINS=""
while [[ $# -gt 0 ]]; do case "$1" in
  --minimal) PROFILE="minimal";; --recommended) PROFILE="recommended";; --full) PROFILE="full";;
  --os-app) OS_APP=1;; --mobile-chat) MOBILE=1;; --ov-server) OV_SERVER="$2"; shift;;
  --model) MODEL="$2"; shift;; --provider) PROVIDER_URL="$2"; shift;;
  --user) TEKTON_USER="$2"; shift;; --skip-runtime) SKIP_RUNTIME=1;;
  --engines) ENGINES="$2"; shift;; --all-engines) ALL_ENGINES=1;; --yes) ASSUME_YES=1;; --toolchains) TOOLCHAINS="$2"; shift;;
  -h|--help) sed -n '2,5p' "$0"; exit 0;; *) echo "unknown flag: $1"; exit 1;; esac; shift; done

BOLD="\033[1m"; CYAN="\033[36m"; GOLD="\033[33m"; DIM="\033[2m"; RST="\033[0m"
say() { printf "%b\n" "$1"; }
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEKTON_HOME="${TEKTON_HOME:-$HOME/.tekton}"
TEKTON_SRC="${TEKTON_SRC:-$HOME/.tekton-src/tekton}"   # CLI monorepo build location
MODEL="${MODEL:-glm-5.1:cloud}"
PROVIDER_URL="${PROVIDER_URL:-http://localhost:11434}"

cat "$REPO_DIR/assets/splash.txt" 2>/dev/null || true

# ── 1a. preflight — detect everything, prompt for optionals ────────
CORE_MISS=(); OPT_MISS=()
command -v node >/dev/null 2>&1 && [[ $(node -v | cut -c2- | cut -d. -f1) -ge 20 ]] || CORE_MISS+=("node20+")
command -v git  >/dev/null 2>&1 || CORE_MISS+=("git")
command -v npm  >/dev/null 2>&1 || CORE_MISS+=("npm")
[[ $OS_APP -eq 1 ]] && ! command -v cargo >/dev/null 2>&1 && OPT_MISS+=("rust/cargo  [recommended - builds the Agent OS App]")
[[ $MOBILE -eq 1 ]] && ! command -v cloudflared >/dev/null 2>&1 && OPT_MISS+=("cloudflared (remote mobile chat tunnel)")
echo -e "${BOLD}▸ preflight${RST}"
for m in "${CORE_MISS[@]:-}"; do  [[ -n "$m" ]] && echo -e "  ${GOLD}● missing (required)  $m  → auto-install${RST}"; done
for m in "${OPT_MISS[@]:-}"; do   [[ -n "$m" ]] && echo -e "  ${DIM}○ missing (optional)  $m${RST}"; done
if [[ ${#OPT_MISS[@]:-0} -gt 0 ]]; then
  if [[ $ASSUME_YES -eq 1 ]]; then OPT_INSTALL=1; echo -e "${DIM}  --yes: installing optional packages automatically${RST}"
  elif [[ -t 0 ]]; then
    read -r -p "  Install the missing optional packages now? [Y/n] " ans
    [[ ! "$ans" =~ ^[Nn] ]] && OPT_INSTALL=1 || OPT_INSTALL=0
  else
    echo -e "${DIM}  (non-interactive: skipping optional installs — re-run with --yes to auto-install)${RST}"
  fi
fi
install_pkg() {  # best-effort package install for a named tool
  if command -v brew >/dev/null 2>&1; then brew install "$1"
  elif command -v apt-get >/dev/null 2>&1; then sudo apt-get install -y "$2" || apt-get install -y "$2"
  elif command -v dnf >/dev/null 2>&1; then sudo dnf install -y "$2"
  else return 1; fi
}
install_cloudflared() {
  if command -v brew >/dev/null 2>&1; then brew install cloudflared
  else
    curl -fsSL -o /tmp/cloudflared https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64       && (sudo mv /tmp/cloudflared /usr/local/bin/cloudflared 2>/dev/null || { mkdir -p "$HOME/.local/bin"; mv /tmp/cloudflared "$HOME/.local/bin/cloudflared"; echo "add $HOME/.local/bin to PATH"; chmod +x "$HOME/.local/bin/cloudflared" 2>/dev/null; })
  fi
}

# ── 1. dependencies ──────────────────────────────────────────────────
need_node=1; command -v node >/dev/null 2>&1 && [[ $(node -v | cut -c2- | cut -d. -f1) -ge 20 ]] && need_node=0
if [[ $need_node -eq 1 ]]; then
  echo -e "${CYAN}▸ installing Node.js 20+${RST}"
  if [[ -n "${TERMUX_VERSION:-}" ]]; then pkg install -y nodejs-lts git
  elif command -v brew >/dev/null 2>&1; then brew install node git
  elif command -v apt-get >/dev/null 2>&1; then (apt-get update -y && apt-get install -y curl git) ; curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - ; sudo apt-get install -y nodejs
  elif command -v dnf >/dev/null 2>&1; then sudo dnf install -y nodejs git
  else echo "Install Node.js 20+ manually: https://nodejs.org"; exit 1; fi
fi
if ! command -v git >/dev/null 2>&1; then
  echo -e "${CYAN}▸ installing git${RST}"; install_pkg git || { echo "git required — install manually"; exit 1; }
fi
command -v npm >/dev/null 2>&1 || { echo "npm required"; exit 1; }

# ── 2. runtime + CLI ────────────────────────────────────────────────
if [[ $SKIP_RUNTIME -eq 0 ]]; then
  echo -e "${CYAN}▸ installing pi runtime (@earendil-works/pi-coding-agent)${RST}"
  npm install -g @earendil-works/pi-coding-agent || echo -e "${DIM}  (pi already installed? continuing)${RST}"
fi
if ! command -v tekton >/dev/null 2>&1; then
  echo -e "${CYAN}▸ building Tekton CLI from messyjs/tekton${RST}"
  mkdir -p "$HOME/.tekton-src"
  [[ -d "$HOME/.tekton-src/tekton" ]] || git clone https://github.com/messyjs/tekton "$HOME/.tekton-src/tekton"
  ( cd "$HOME/.tekton-src/tekton" && npm install && npm run build -w @tekton/cli && npm link ) \
    || { echo -e "${DIM}  build failed — ask for repo access or npm publish @tekton/cli${RST}"; }
else
  echo -e "${DIM}▸ tekton CLI already installed: $(command -v tekton)${RST}"
fi

# ── 3. ~/.tekton home ───────────────────────────────────────────────
echo -e "${CYAN}▸ wiring $TEKTON_HOME (context home, tekton.md)${RST}"
mkdir -p "$TEKTON_HOME"/{skills,extensions,sessions,checkpoints}
[[ -f "$TEKTON_HOME/tekton.md" ]] || sed "s/{{USER_NAME}}/${TEKTON_USER:-Knight}/g; s/{{USER_STYLE}}/direct, hands-on builder/g" "$REPO_DIR/config/tekton.md" > "$TEKTON_HOME/tekton.md"
[[ -f "$TEKTON_HOME/config.yaml" ]] || sed "s/{{MODEL_FAST}}/$MODEL/g; s/{{MODEL_DEEP}}/$MODEL/g; s|{{PROVIDER_URL}}|$PROVIDER_URL|g" "$REPO_DIR/config/config.example.yaml" > "$TEKTON_HOME/config.yaml"
[[ -f "$TEKTON_HOME/models.json" ]] || cp "$REPO_DIR/config/models.example.json" "$TEKTON_HOME/models.json"
[[ -f "$TEKTON_HOME/settings.json" ]] || cp "$REPO_DIR/config/settings.example.json" "$TEKTON_HOME/settings.json"
# reuse existing pi auth if present
[[ -f "$HOME/.pi/agent/auth.json" && ! -f "$TEKTON_HOME/auth.json" ]] && cp "$HOME/.pi/agent/auth.json" "$TEKTON_HOME/auth.json" || true

# ── 4. skills per profile ───────────────────────────────────────────
SK_SESSION="checkpoint compress startup health"
SK_ROUTING="sorting-hat update-models add-account"
SK_PROJECT="project-registry project-status treasure-map dictionary find-skills"
SK_ENGINES="engine-forge subagent-forge provider-add"
SK_CAVEMAN="caveman caveman-commit caveman-compress caveman-help caveman-review"
SK_TRADING="hermes-trading markov-hedge-fund-method tv-fast-ops orca-cli"
SK_POWER="orchestration skillception computer-use skeleton-key dreams hunger-games quantum-consciousness"
SK_MEDIA="pause play product-enhance 1up"
install_skills() { for s in "$@"; do [[ -d "$REPO_DIR/skills/$s" ]] && cp -r "$REPO_DIR/skills/$s" "$TEKTON_HOME/skills/"; done; }
echo -e "${CYAN}▸ installing skills ($PROFILE)${RST}"
install_skills house-style
if [[ "$PROFILE" == "recommended" || "$PROFILE" == "full" ]]; then
  install_skills $SK_CAVEMAN $SK_SESSION $SK_ROUTING $SK_PROJECT $SK_ENGINES
  command -v cavemem >/dev/null 2>&1 || npm install -g cavemem || echo -e "${DIM}  cavemem not on npm — install from source later${RST}"
fi
if [[ "$PROFILE" == "full" ]]; then install_skills $SK_TRADING $SK_POWER $SK_MEDIA; fi

# ── 5. OpenViking recall ────────────────────────────────────────────
if [[ "$PROFILE" != "minimal" ]]; then
  echo -e "${CYAN}▸ installing openviking recall extension${RST}"
  [[ -d "$TEKTON_HOME/extensions/openviking" ]] || cp -r "$REPO_DIR/extensions/openviking" "$TEKTON_HOME/extensions/"
  if [[ -n "$OV_SERVER" ]]; then
    grep -q "^openviking:" "$TEKTON_HOME/config.yaml" 2>/dev/null || printf "\nopenviking:\n  enabled: true\n  server: %s\n" "$OV_SERVER" >> "$TEKTON_HOME/config.yaml"
  fi
fi

# ── 5b. engines (capability servers for sub-agents) ─────────────────
if [[ $ALL_ENGINES -eq 1 || -n "$ENGINES" ]]; then
  echo -e "${CYAN}▸ registering engines${RST}"
  cp "$REPO_DIR/config/engines.yaml" "$TEKTON_HOME/engines.yaml"
  if [[ $ALL_ENGINES -eq 1 ]]; then
    ( cd "$HOME/.tekton-src/tekton" 2>/dev/null && npm run build -w @tekton/forge -w @tekton/voice -w @tekton/docling-service -w @tekton/browser-use-service -w @tekton/ml-ops ) || echo -e "${DIM}  engine build skipped (tekton-src missing)${RST}"
  fi
  echo -e "${DIM}  registry: $TEKTON_HOME/engines.yaml — start engines listed there;${RST}"
  echo -e "${DIM}  sub-agents (orchestration skill) call engines via gateway/MCP adapters.${RST}"
fi

# ── 5c. app toolchains (--toolchains "web,windows,macos,linux,android,ios,vst,games") ──
ensure_rust() {
  command -v cargo >/dev/null 2>&1 && return 0
  [[ -f "$HOME/.cargo/env" ]] && . "$HOME/.cargo/env" && return 0
  echo -e "${CYAN}  installing rustup${RST}"
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
  . "$HOME/.cargo/env" 2>/dev/null
}
if [[ -n "$TOOLCHAINS" ]]; then
  echo -e "${CYAN}▸ toolchains: $TOOLCHAINS${RST}"
  for t in ${TOOLCHAINS//,/ }; do
    case "$t" in
      web)      echo "  web/html: node (installed with core) - nothing extra";;
      windows)  ensure_rust; echo "  windows apps: + MSVC Build Tools (winget install Microsoft.VisualStudio.2022.BuildTools --override '--add Microsoft.VisualStudio.Workload.VCTools --passive') + WebView2";;
      macos)    ensure_rust; xcode-select -p >/dev/null 2>&1 || echo "  macos apps: run 'xcode-select --install' (Xcode CLT)";;
      linux)    ensure_rust; echo "  linux apps: + build-essential libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev libssl-dev patchelf (apt)";;
      android)  ensure_rust; rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android 2>/dev/null; echo "  android apps: + Android Studio (SDK+NDK), set ANDROID_HOME + NDK_HOME - then 'tauri android build'";;
      ios)      ensure_rust; rustup target add aarch64-apple-ios aarch64-apple-ios-sim 2>/dev/null; echo "  ios apps: needs full Xcode from the App Store (macOS only) - then 'tauri ios init'";;
      vst)      echo "  vst/audio plugins: C++ toolchain + CMake + JUCE - Windows: MSVC Build Tools; mac: xcode-select --install; linux: build-essential cmake";;
      games)    echo "  games: Unity Hub / Unreal / Godot per engine; html5 games need only node";;
      *)        echo "  unknown toolchain: $t";;
    esac
  done
fi

# ── 6. add-ons ──────────────────────────────────────────────────────
if [[ $MOBILE -eq 1 ]]; then
  echo -e "${GOLD}▸ mobile remote chat:${RST}"
  echo "    1) run:      tekton --gateway        (serves chat on :8080)"
  echo "    2) expose:   cloudflared tunnel --url http://localhost:8080"
  echo "    3) phone:    open the tunnel URL in any browser, or point the"
  echo "                 Agent OS App at it. Chat with Tekton from anywhere."
  if ! command -v cloudflared >/dev/null 2>&1; then
    if [[ $OPT_INSTALL -eq 1 ]]; then echo -e "${CYAN}  installing cloudflared${RST}"; install_cloudflared || echo -e "${DIM}  auto-install failed — see docs/AGENT-OS.md${RST}"
    else echo -e "${DIM}    (install cloudflared: brew install cloudflared | apt install cloudflared | re-run with --yes)${RST}"; fi
  fi
fi
if [[ $OS_APP -eq 1 ]]; then
  echo -e "${CYAN}▸ Agent OS App (Tauri 2: Windows/macOS/Android)${RST}"
  if command -v cargo >/dev/null 2>&1; then
    ( cd "$REPO_DIR/app" && npm install && npx tauri build ) \
      && echo -e "${GOLD}  built → app/src-tauri/target/release/bundle/${RST}" \
      || echo -e "${DIM}  build failed — see docs/AGENT-OS.md (Android needs Android SDK)${RST}"
  elif [[ $OPT_INSTALL -eq 1 ]]; then
    echo -e "${CYAN}  installing rustup (user-level)${RST}"
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    . "$HOME/.cargo/env" 2>/dev/null
    ( cd "$REPO_DIR/app" && npm install && npx tauri build ) || echo -e "${DIM}  build failed — see docs/AGENT-OS.md${RST}"
  else
    echo "  Rust not found. Re-run with --yes to auto-install rustup, or: https://rustup.rs"
  fi
fi

# ── 6b. verify ──────────────────────────────────────────────────────
echo -e "${BOLD}▸ verifying install${RST}"
ok() { echo -e "  ${GOLD}✓ $1${RST}"; }; bad() { echo -e "  ${GOLD}✗ $1${RST}"; }
command -v node  >/dev/null 2>&1 && ok "node $(node -v)"          || bad "node missing"
command -v git   >/dev/null 2>&1 && ok "git"                       || bad "git missing"
command -v tekton >/dev/null 2>&1 && ok "tekton CLI"               || bad "tekton CLI not linked"
[[ -f "$TEKTON_HOME/tekton.md" ]] && ok "context: $TEKTON_HOME/tekton.md" || bad "tekton.md missing"
command -v cavemem >/dev/null 2>&1 && ok "cavemem"                 || true
[[ $OS_APP -eq 1 && ! -d "$REPO_DIR/app/src-tauri/target" ]] && bad "Agent OS app not built (see above)" || true
[[ $MOBILE -eq 1 ]] && command -v cloudflared >/dev/null 2>&1 && ok "cloudflared" || true

# ── 7. done ─────────────────────────────────────────────────────────
echo; echo -e "${GOLD}⚔ Tekton Agent installed ($PROFILE profile)${RST}"
echo -e "  home      : $TEKTON_HOME        context: $TEKTON_HOME/tekton.md"
echo -e "  skills    : $(ls "$TEKTON_HOME/skills" 2>/dev/null | wc -l) installed"
echo -e "  next      : run ${BOLD}tekton${RST} — pull the sword from the stone."
