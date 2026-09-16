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

# MJ Laptop (LAN)
curl -s http://192.168.68.62:11434/api/tags
```

If any instance is unreachable, keep its existing model list and note it as offline.

### Step 2: Build model lists per provider

For each Ollama instance, combine:
- **Local models** (from `/api/tags` response) -- use actual `context_length` from the API response
- **Cloud models** (Ollama cloud models with `:cloud` suffix) -- these are always available on any Ollama instance

Known cloud models (add new ones if discovered):
- `deepseek-v4-pro:cloud` (131072 ctx)
- `deepseek-v4-flash:cloud` (131072 ctx)
- `glm-5.1:cloud` (202752 ctx)
- `glm-5.2:cloud` (202752 ctx)
- `kimi-k2.5:cloud` (262144 ctx)
- `kimi-k2.7-code:cloud` (131072 ctx)
- `minimax-m3:cloud` (131072 ctx)
- `nemotron-3-super:cloud` (131072 ctx)

### Step 3: Update ALL THREE config files

These are the 3 files that MUST all be updated:

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

### Step 4: Update rules files (if new accounts or models were added)

Check these files and update the Ollama accounts rule if needed:
- `.pi/agent/AGENTS.md` (Rule 6)
- `.pi/agent/SYSTEM.md` (Rule 6)
- `.tekton/SOUL.md` (Rule 6)
- `.claude/CLAUDE.md` (Rule 7)
- `D:\AI Drive\CLAUDE.md` (Rule 7)

### Step 5: Update config.yaml fusion engine

If new models are added that should be in the fusion pool, update:
- `D:\AI Drive\.tekton\config.yaml` -- add to `models:` or `mjLaptopModels:` section

### Step 6: Report

After updating, report:
- Which instances were queried (online/offline)
- How many local models found per instance
- How many cloud models included
- Total models per provider per config file
- Any new models discovered that weren't in the config before