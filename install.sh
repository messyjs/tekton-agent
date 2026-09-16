#!/usr/bin/env bash
# ⚔ Tekton Agent installer — macOS / Linux / Termux
# Usage: ./install.sh [--minimal|--recommended|--full] [--os-app] [--mobile-chat]
#                      [--model NAME] [--provider URL] [--ov-server URL] [--user NAME]
set -euo pipefail

PROFILE="recommended"; OS_APP=0; MOBILE=0; OV_SERVER=""; MODEL=""; PROVIDER_URL=""; TEKTON_USER=""; SKIP_RUNTIME=0; ENGINES=""; ALL_ENGINES=0
while [[ $# -gt 0 ]]; do case "$1" in
  --minimal) PROFILE="minimal";; --recommended) PROFILE="recommended";; --full) PROFILE="full";;
  --os-app) OS_APP=1;; --mobile-chat) MOBILE=1;; --ov-server) OV_SERVER="$2"; shift;;
  --model) MODEL="$2"; shift;; --provider) PROVIDER_URL="$2"; shift;;
  --user) TEKTON_USER="$2"; shift;; --skip-runtime) SKIP_RUNTIME=1;;
  --engines) ENGINES="$2"; shift;; --all-engines) ALL_ENGINES=1;;
  -h|--help) sed -n '2,5p' "$0"; exit 0;; *) echo "unknown flag: $1"; exit 1;; esac; shift; done

BOLD="\033[1m"; CYAN="\033[36m"; GOLD="\033[33m"; DIM="\033[2m"; RST="\033[0m"
say() { printf "%b\n" "$1"; }
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEKTON_HOME="${TEKTON_HOME:-$HOME/.tekton}"
TEKTON_SRC="${TEKTON_SRC:-$HOME/.tekton-src/tekton}"   # CLI monorepo build location
MODEL="${MODEL:-glm-5.1:cloud}"
PROVIDER_URL="${PROVIDER_URL:-http://localhost:11434}"

cat "$REPO_DIR/assets/splash.txt" 2>/dev/null || true

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
command -v git >/dev/null 2>&1 || { echo "git required"; exit 1; }
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
SK_CAVEMAN="caveman caveman-commit caveman-compress caveman-help caveman-review"
SK_TRADING="hermes-trading markov-hedge-fund-method tv-fast-ops orca-cli"
SK_POWER="orchestration skillception computer-use skeleton-key dreams hunger-games quantum-consciousness"
SK_MEDIA="pause play product-enhance 1up"
install_skills() { for s in "$@"; do [[ -d "$REPO_DIR/skills/$s" ]] && cp -r "$REPO_DIR/skills/$s" "$TEKTON_HOME/skills/"; done; }
echo -e "${CYAN}▸ installing skills ($PROFILE)${RST}"
if [[ "$PROFILE" == "recommended" || "$PROFILE" == "full" ]]; then
  install_skills $SK_CAVEMAN $SK_SESSION $SK_ROUTING $SK_PROJECT
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

# ── 6. add-ons ──────────────────────────────────────────────────────
if [[ $MOBILE -eq 1 ]]; then
  echo -e "${GOLD}▸ mobile remote chat:${RST}"
  echo "    1) run:      tekton --gateway        (serves chat on :8080)"
  echo "    2) expose:   cloudflared tunnel --url http://localhost:8080"
  echo "    3) phone:    open the tunnel URL in any browser, or point the"
  echo "                 Agent OS App at it. Chat with Tekton from anywhere."
  command -v cloudflared >/dev/null 2>&1 || echo -e "${DIM}    (install cloudflared: brew install cloudflared | apt install cloudflared)${RST}"
fi
if [[ $OS_APP -eq 1 ]]; then
  echo -e "${CYAN}▸ Agent OS App (Tauri 2: Windows/macOS/Android)${RST}"
  if command -v cargo >/dev/null 2>&1; then
    ( cd "$REPO_DIR/app" && npm install && npx tauri build ) \
      && echo -e "${GOLD}  built → app/src-tauri/target/release/bundle/${RST}" \
      || echo -e "${DIM}  build failed — see docs/AGENT-OS.md (Android needs Android SDK)${RST}"
  else
    echo "  Rust not found. Install rustup first: https://rustup.rs then re-run --os-app"
  fi
fi

# ── 7. done ─────────────────────────────────────────────────────────
echo; echo -e "${GOLD}⚔ Tekton Agent installed ($PROFILE profile)${RST}"
echo -e "  home      : $TEKTON_HOME        context: $TEKTON_HOME/tekton.md"
echo -e "  skills    : $(ls "$TEKTON_HOME/skills" 2>/dev/null | wc -l) installed"
echo -e "  next      : run ${BOLD}tekton${RST} — pull the sword from the stone."
