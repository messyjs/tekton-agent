---
name: update-models
description: REFRESH OLLAMA MODEL LISTS. When user says "update the model list", "update models", "refresh models", "sync models", or "update ollama models". Queries all 3 Ollama instances (laptop, workstation, mjlaptop) for local models via /api/tags, combines with known cloud models, and updates ALL 3 config files (.pi/agent/models.json, .tekton/models.json, npm/tekton-agent/configs/models.json). Also updates rules files and config.yaml if new accounts were added. Reports what changed.
---

# Skill: update-models

**Trigger phrases:** "update the model list", "update models", "refresh models", "sync models", "update ollama models"

**What it does:** Queries all 3 Ollama instances for their current local models, combines with known cloud models, and updates all 3 config files plus rules files.

## Procedure

### Step 1: Query all Ollama instances

```bash
# Laptop (local)
curl -s http://127.0.0.1:11434/api/tags

# Workstation (LAN)
curl -s http://192.168.68.70:11434/api/tags

# MJ Laptop (LAN) -- static IP .60 (was .57/.62 DHCP, no longer valid)
curl -s http://192.168.68.60:11434/api/tags
# MJ Laptop fallback (Cloudflare tunnel, works from any network)
curl -s https://ollama-mjlaptop.messy-jesse.com/api/tags
```

If any instance is unreachable, keep its existing model list and note it as offline.

### Step 2: Build model lists per provider

For each Ollama instance, combine:
- **Local models** (from `/api/tags` response) -- use actual `context_length` from the API response
- **Cloud models** (Ollama cloud models with `:cloud` suffix) -- these are always available on any Ollama instance

Known cloud models (add new ones if discovered; ctx verified 2026-09-01):
- `deepseek-v4-pro:cloud` (1048576 ctx)
- `deepseek-v4-flash:cloud` (1048576 ctx)
- `glm-5.1:cloud` (202752 ctx)
- `glm-5.2:cloud` (1000000 ctx)
- `glm-5.3:cloud` (1048576 ctx)
- `glm-5.3-flash:cloud` (1048576 ctx, vision)
- `kimi-k2.5:cloud` (262144 ctx, vision)
- `kimi-k2.7-code:cloud` (131072 ctx)
- `minimax-m3:cloud` (131072 ctx)
- `nemotron-3-super:cloud` (131072 ctx)

### Step 3: Preserve Cloudflare Workers AI providers

The config files contain 3 Cloudflare Workers AI providers that must NOT be removed or overwritten:
- `cloudflare-workers-ai` (account 6e8007f49209393c14aec0053bce1d04)
- `cloudflare-workers-ai-2` (account fbe832b57f8960ec252851b8d1fa497a)
- `cloudflare-workers-ai-3` (account 276074798f35779569aab9c24c6cf27a)

Each has 26 text generation models fetched from the Cloudflare API. When rebuilding config files, preserve these providers as-is. They sort after local Ollama accounts.

IMPORTANT: Do NOT update ~/.openclaw/ files. That is a separate system (OpenClaw), not Tekton.

### Step 4: Update ALL THREE config files

These are the 3 files that MUST all be updated (TEKTON files, NOT OpenClaw):

1. **`C:\Users\Massi\.pi\agent\models.json`** (runtime, powers / command)
   - Format: detailed objects with `id`, `name`, `reasoning`, `input`, `contextWindow`, `maxTokens`, `cost`
   - Local models get "(Laptop)", "(Workstation)", or "(MJ Laptop)" suffix on name
   - Cloud models get "(Laptop)", "(Workstation)", or "(MJ Laptop)" suffix on name
   - Also maintain: `ollama-workstation-tunnel`, `ollama-laptop-tunnel`, `ollama-mjlaptop-tunnel`, `openrouter`, `zhipu`

2. **`D:\AI Drive\.tekton\models.json`** (dashboard + fusion engine)
   - Same format as above
   - Tunnel providers use tunnel URLs and "(...Tunnel)" suffix on names

3. **`C:\Users\Massi\AppData\Roaming\npm\node_modules\tekton-agent\configs\models.json`** (NPM fallback)
   - Different format: simple string arrays for models, different provider structure
   - Only local + cloud models as strings, no detailed metadata

### Step 5: Update rules files (if new accounts or models were added)

Check these files and update the Ollama accounts rule if needed:
- `.pi/agent/AGENTS.md` (Rule 6)
- `.pi/agent/SYSTEM.md` (Rule 6)
- `.tekton/SOUL.md` (Rule 6)
- `.claude/CLAUDE.md` (Rule 7)
- `D:\AI Drive\CLAUDE.md` (Rule 7)

### Step 6: Update config.yaml fusion engine

If new models are added that should be in the fusion pool, update:
- `D:\AI Drive\.tekton\config.yaml` -- add to `models:` or `mjLaptopModels:` section

### Step 7: Report

After updating, report:
- Which instances were queried (online/offline)
- How many local models found per instance
- How many cloud models included
- Total models per provider per config file
- Any new models discovered that weren't in the config before