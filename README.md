<div align="center">

```text
                  ⚔
                  █
                  █
                 ╔╩╗
    ▄▄██▄▄      ▐█ █▌
   ▟██████▙    ▐█ █▌
   █ ◉  ◉ █   ▐█ █▌
   ▜██████▛    ╲█╱
    ▐█▄█▌     ▄▄█▄▄▄▄▄
    ▐████▌   ▄████████▄
    ▐████▌  ▄██████████▄
     ████  ▄████████████▄▄▄▄▄▄▄▄▄▄▄▄▄▄
  ▄▄████████████████████████████████████▄▄▄
 ▐█████████  T H E   S T O N E  ████████████▌
  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
   Only the worthy may draw the blade.
```

# ⚔ Tekton Agent

**The self-improving coding agent** — a synthesis of three lineages:
**Pi** (minimal, self-modifying core) · **Hermes** (multi-platform orchestration) · **OpenMythos** (recurrent-depth model routing)

Windows · macOS · Linux · Termux/Android · Agent OS App (Windows/macOS/Android) · Mobile chat from anywhere

</div>

---

## What is Tekton?

Tekton fuses **Pi Coder** (the minimal self-modifying coding agent), **Hermes Agent**
(multi-platform orchestration: trading, voice, dashboard, devices) and **OpenMythos**
(recurrent-depth reasoning architecture powering the ModelRouter) into one CLI + one Agent OS App.

It holds your **chat sessions**, your **software & skills**, your **projects**, your **models** —
and remembers everything via **Caveman** memory and **OpenViking** long-term recall.

## Install

| Platform | Command |
|---|---|
| **Windows** (PowerShell 7+) | `iex "& { $(irm https://raw.githubusercontent.com/messyjs/tekton-agent/main/install.ps1) } -Profile recommended"` |
| **macOS / Linux** | `curl -fsSL https://raw.githubusercontent.com/messyjs/tekton-agent/main/install.sh \| bash -s -- --recommended` |
| **Android (Termux)** | `pkg install git nodejs && curl -fsSL .../install.sh \| bash -s -- --minimal` |

> Repo is private — clone first if raw URLs 404: `git clone https://github.com/messyjs/tekton-agent && ./install.sh`

### Profiles

| Profile | Flag | Contents |
|---|---|---|
| **Minimal** | `--minimal` | Runtime + `tekton.md` context + config. Bare blade. |
| **Recommended** ⭐ | `--recommended` | + **Caveman suite**, **OpenViking recall**, session kit (checkpoint/compress/startup/health), routing kit (sorting-hat/update-models/add-account), project kit (project-registry/project-status/treasure-map/dictionary/find-skills) |
| **Full** | `--full` | Everything: + trading kit (hermes-trading, markov-hedge-fund-method, tv-fast-ops, orca-cli), power kit (orchestration, skillception, computer-use, skeleton-key, dreams, hunger-games, quantum-consciousness), media kit (pause, play, product-enhance, 1up) |

### Add-ons

| Add-on | Flag | What you get |
|---|---|---|
| **Agent OS App** | `--os-app` | Desktop/mobile app (Tauri 2): your chat sessions, projects, installed software, models, health — Windows, macOS **and Android** from one codebase. |
| **Mobile remote chat** | `--mobile-chat` | Tekton gateway + Cloudflare tunnel: chat with Tekton **from anywhere**. Browser PWA on the phone, or the Agent OS app pointed at your tunnel URL. |
| **OpenViking server** | `--ov-server URL` | Wire long-term recall to your self-hosted OpenViking (port 1933). |
| **Engines** | `--engines "forge,voice"` / `--all-engines` | Capability servers (HTTP/MCP) — forge (self-improvement), trading sidecar, voice, docling, browser-use, ml-ops, gann. Main agent **and Orca sub-agents** attach through the gateway. Registry: `~/.tekton/engines.yaml`. |

```bash
# everything, the full kingdom
./install.sh --full --os-app --mobile-chat --ov-server http://your-server:1933
```

## Recommended install (our picks)

1. **caveman** suite + `cavemem` — persistent memory, rituals, compression of self
2. **openviking** extension — long-term recall brain (self-hosted)
3. **checkpoint** — save/resume any session state
4. **compress** — context compression so long sessions stay sharp
5. **startup** — session init ritual, context load
6. **health** — self-diagnostics of services/models
7. **sorting-hat** — route tasks across Ollama accounts, credits never dry
8. **update-models** + **add-account** — keep the model catalog fresh
9. **project-registry** / **project-status** — Tekton knows what you're building
10. **treasure-map** — spoken architecture → precise file/flow spec
11. **find-skills** + **skillception** — self-extension (the self-improving part)
12. **dictionary** — never guesses at your vague words

## Repo layout

```
tekton-agent/
├── install.sh / install.ps1     # cross-platform installer (profiles + add-ons)
├── config/
│   ├── tekton.md                # THE context file (replaces AGENTS/CLAUDE.md)
│   ├── config.example.yaml      # models, fusion, gateway
│   ├── models.example.json      # pi-format provider catalog
│   └── settings.example.json
├── skills/                      # 33 curated skills incl. house-style (the design DNA)
├── extensions/openviking/       # OpenViking recall extension
├── app/                         # ⚔ Agent OS App (Tauri 2: Win/mac/Android)
├── assets/splash.txt            # the knight & the stone
└── docs/AGENT-OS.md             # Agent OS + mobile remote chat architecture
```

## The CLI

The Tekton CLI source lives in [`messyjs/tekton`](https://github.com/messyjs/tekton)
(Pi + `@tekton/cli` monorepo). The installer clones, builds and links it, then wires
`~/.tekton` as the context home with `tekton.md` at the center.

## License

MIT — see [LICENSE](LICENSE).
