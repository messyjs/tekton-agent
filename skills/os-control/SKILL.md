---
name: os-control
description: >-
  Control ANY software on ANY OS. Use when the user asks to operate, automate,
  click, script, configure, or drive any application: desktop apps, system
  settings, media players, IDEs, browsers, games, or OS features. Picks the
  right backend per platform and escalates through the layers below.
---

# OS Control — the universal software-control playbook

Tekton controls software in layers. Always try the highest layer first; drop
down only when needed.

## Layer 1 — native CLI / config (PREFERRED)

Most software is scriptable and we just need to find the interface:

| OS | Tool | Examples |
|---|---|---|
| Windows | `powershell` (incl. UIAutomation via .NET), `reg`, `winget`, `nircmd` | `(Get-Process).CloseMainWindow()`, registry tweaks, winget installs |
| macOS | `osascript` (AppleScript + System Events), `defaults`, `brew` | `osascript -e 'tell app "Spotify" to play'`, defaults write |
| Linux | `systemctl`, `gsettings`, `xdotool`, `wmctrl`, `dbus-send` | window control, settings, services |
| Android | `termux-api` (sms, call, clipboard, notify), `adb`, `am`/`pm` intents | send SMS, toggle wifi, install apps |
| Anything | the app's own CLI, config files, env vars, REST API | check `--help`, docs, dotfiles |

## Layer 2 — MCP (apps that expose it)

- Registry: `~/.tekton/engines.yaml` (transport: mcp/http). An MCP server for an
  app = a permanent capability. Build missing ones with `skeleton-key`.
- Current pi build does NOT load `mcp.json` natively — MCP reaches Tekton through
  the gateway adapters or engines. Re-check after `pi` updates.

## Layer 3 — GUI automation (LAST RESORT, when no API exists)

| OS | Backend | Skill |
|---|---|---|
| Any browser | Playwright/CDP via browser-use engine | `browser-use-service` engine |
| Windows | UIAutomation, SendKeys, mouse via PowerShell | `computer-use` |
| macOS | System Events keystrokes/clicks via osascript | `computer-use` |
| Android | `input tap/text/swipe` via adb | `computer-use` |

## Escalation rule

1. CLI/config/API exists → use it (fast, reliable).
2. MCP server exists (or `skeleton-key` can build one) → register in engines.yaml, use it.
3. GUI-only app → screenshot + computer-use (look, click, verify — always verify after acting).
4. Report what you did + how to undo it.

## Safety

- Never automate destructive actions without confirmation.
- Prefer reversible actions; note the undo path before acting.
- On Android, ask before SMS/calls/installations.
