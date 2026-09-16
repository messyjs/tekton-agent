#!/usr/bin/env bash
# ⚔ Tekton Agent uninstaller — removes installed components, keeps your data by default.
# Usage: ./uninstall.sh [--yes] [--purge]   (--purge backs up ~/.tekton then removes it)
set -uo pipefail
ASSUME_YES=0; PURGE=0
for a in "$@"; do case "$a" in --yes) ASSUME_YES=1;; --purge) PURGE=1;; esac; done
TEKTON_HOME="${TEKTON_HOME:-$HOME/.tekton}"
ask() { [[ $ASSUME_YES -eq 1 ]] && return 0; read -r -p "$1 [y/N] " a; [[ "$a" =~ ^[Yy] ]]; }
GOLD="\033[33m"; CYAN="\033[36m"; DIM="\033[2m"; RST="\033[0m"

echo -e "${GOLD}\u2694 Tekton Agent uninstaller${RST}"
[[ -d "$TEKTON_HOME" ]] || echo -e "${DIM}  ($TEKTON_HOME not found \u2014 components only)${RST}"

SKILLS="caveman caveman-commit caveman-compress caveman-help caveman-review checkpoint compress startup health sorting-hat update-models add-account project-registry project-status treasure-map dictionary find-skills engine-forge subagent-forge provider-add house-style hermes-trading markov-hedge-fund-method tv-fast-ops orca-cli orchestration skillception computer-use skeleton-key dreams hunger-games quantum-consciousness pause play product-enhance 1up"
echo -e "${CYAN}\u25b8 removing skills from $TEKTON_HOME/skills${RST}"
for s in $SKILLS; do rm -rf "$TEKTON_HOME/skills/$s" 2>/dev/null; done
[[ -d "$TEKTON_HOME/extensions/openviking" ]] && { rm -rf "$TEKTON_HOME/extensions/openviking"; echo -e "  removed openviking extension"; }
[[ -f "$TEKTON_HOME/engines.yaml" ]] && rm -f "$TEKTON_HOME/engines.yaml"
OLD_CMD=$(grep -A1 "^cli:" "$TEKTON_HOME/config.yaml" 2>/dev/null | grep "command:" | sed "s/.*command: *//" | tr -d " ")
if [[ -n "$OLD_CMD" && "$OLD_CMD" != "tekton" ]]; then
  rm -f "$HOME/.local/bin/$OLD_CMD" 2>/dev/null
  for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do [[ -f "$rc" ]] && sed -i.bak "/alias $OLD_CMD=/d" "$rc" 2>/dev/null; done
  echo -e "  removed launch command shim: $OLD_CMD"
fi
[[ -d "$TEKTON_HOME/subagents" ]] && rm -rf "$TEKTON_HOME/subagents"
[[ -d "$TEKTON_HOME/subagents" ]] && rm -rf "$TEKTON_HOME/subagents"

echo -e "${CYAN}\u25b8 removing npm components${RST}"
command -v cavemem >/dev/null 2>&1 && { npm uninstall -g cavemem 2>/dev/null; echo -e "  removed cavemem"; }
if command -v tekton >/dev/null 2>&1 && [[ -d "$HOME/.tekton-src/tekton" ]]; then
  ( cd "$HOME/.tekton-src/tekton" && npm unlink -g 2>/dev/null ); echo -e "  unlinked tekton CLI"
fi
command -v pi >/dev/null 2>&1 && { npm uninstall -g @earendil-works/pi-coding-agent 2>/dev/null; echo -e "  removed pi runtime"; }
[[ -d "$HOME/.tekton-src" ]] && rm -rf "$HOME/.tekton-src"

if [[ $PURGE -eq 1 && -d "$TEKTON_HOME" ]]; then
  BK="$TEKTON_HOME-backup-$(date +%Y%m%d-%H%M%S)"
  ( cd "$(dirname "$TEKTON_HOME")" && tar czf "$(basename "$BK").tar.gz" "$(basename "$TEKTON_HOME")" ) \
    && echo -e "  backed up \u2192 $BK.tar.gz"
  ask "  DELETE $TEKTON_HOME entirely (sessions, memory, projects)?" && rm -rf "$TEKTON_HOME" && echo -e "  removed (backup kept)"
else
  echo -e "${DIM}\u25b8 kept (your data): $TEKTON_HOME \u2014 sessions, memory, projects, models, config${RST}"
  echo -e "${DIM}  remove later with: $0 --purge${RST}"
fi
echo -e "${DIM}\u25b8 untouched: OpenViking server data (~/.openviking) \u2014 manage separately${RST}"
echo -e "${GOLD}Done. Only the worthy may draw again.${RST}"
