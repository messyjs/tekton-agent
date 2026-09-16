# ⚔ Tekton Agent OS & Remote Mobile Chat

## Vision

One agent, every device. Tekton holds your **chat sessions**, **software & skills**,
**projects**, **models** — and reaches you anywhere.

```
                    ┌──────────────────────────┐
                    │   TEKTON AGENT OS (Tauri) │
                    │  Chat · Sessions · Projects│
                    │  Models · Health · Engines │
                    └──────────┬───────────────┘
                               │  http / tunnel
                    ┌──────────┴───────────────┐
                    │   TEKTON GATEWAY (:8080)  │
                    └──┬───────────┬───────────┘
                       │           │
              ┌────────┴──┐   ┌────┴──────────┐
              │ WORKSTATION│   │ ANY DEVICE    │
              │ tekton CLI │   │ phone browser │
              │ engines    │   │ Agent OS App  │
              │ ollama     │   │ (Android/iOS) │
              └────────────┘   └───────────────┘
```

## Agent OS App (Tauri 2)

One codebase → **Windows, macOS, Android** (iOS later via `tauri ios`).

- **Chat** — talk to Tekton through the gateway (local or tunnel URL).
- **Sessions** — every chat stored at `~/.tekton/sessions/*.json`; searchable history.
- **Projects** — the project registry (`~/.tekton/projects.json`): what you're building, where it lives.
- **Models** — provider catalog (`~/.tekton/models.json`).
- **Health** — gateway + engine status probes.

### Build

```bash
cd app
npm install
npx tauri build              # Windows → .msi/.exe, macOS → .app/.dmg
npx tauri android init       # once; needs Android SDK/NDK
npx tauri android build      # → .apk / .aab
```

The Rust core reads `$TEKTON_HOME` (default `~/.tekton`) with a path guard — the app
can never read outside the Tekton home. Chat relays to the gateway URL you set in ⚙.

## Chat with Tekton from anywhere (mobile)

1. **Start the gateway** on the machine running Tekton:
   `tekton --gateway`            → serves chat/API on `:8080`
2. **Expose it** with a Cloudflare tunnel (already your standard pattern):
   `cloudflared tunnel --url http://localhost:8080`
3. **From any phone, any network**:
   - Browser: open the tunnel URL — lightweight chat PWA experience, nothing to install, or
   - Agent OS App (Android): set the tunnel URL in ⚙ settings — full sessions/projects/models UI.

Security notes: front the tunnel with your Cloudflare access policies as with your
existing tunnels (`*.messy-jesse.com`). The gateway binds localhost by default.

## Engines & sub-agents

Engines are **capability servers** (HTTP and/or MCP). The main agent and every **Orca
sub-agent** attach through the gateway adapters or direct MCP.

| Engine | Package | Transport | Purpose |
|---|---|---|---|
| **forge** ⭐ | `@tekton/forge` | MCP | builds/refines skills, personas, SOUL — the self-improvement loop |
| **pi-agent-service** | `@tekton/pi-agent-service` | HTTP+MCP | trading intelligence sidecar (Gann, Fibonacci, TradingView control) |
| **voice** | `@tekton/voice` | HTTP | speak/listen |
| **docling** | `@tekton/docling-service` | HTTP | PDF/audio/video → text → memory |
| **browser-use** | `@tekton/browser-use-service` | HTTP | browser automation for web-worker sub-agents |
| **ml-ops** | `@tekton/ml-ops` | HTTP | training/eval loop — measures self-improvement |
| **gann** | `@tekton/gann` | library | Gann trading engine |

Install: `./install.sh --engines "forge,voice"` or `--all-engines`.
Registry lands at `~/.tekton/engines.yaml` — every engine declares a **`subagent_role`**:
the contract telling Orca sub-agents what the engine can do for them.

### Sub-agent pattern

- Coordinator (you + Tekton) decomposes work via the **orchestration** skill (Orca:
  threaded messages, task DAGs, decision gates, worker_done waits).
- **orca-cli** handles full ownership handoffs (worktrees, terminals).
- Sub-agents are just agents: they load `tekton.md`, see the engine registry, and call
  engines the same way the main agent does — one protocol, any number of workers.

## What "hold" means

| The app holds | Source of truth |
|---|---|
| Chat sessions | `~/.tekton/sessions/` (JSON, portable) |
| Software | `~/.tekton/skills/` + `extensions/` + engines registry |
| Projects | `~/.tekton/projects.json` |
| Models | `~/.tekton/models.json` + `config.yaml` |
| Memory | `MEMORY.md`, cavemem db, OpenViking server |
