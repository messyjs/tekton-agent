---
name: add-account
description: ADD NEW OLLAMA INSTANCE. When user says "add ollama account", "add new machine", "new ollama server", or "add ollama instance". Asks for name, IP, tunnel URL, and description. Queries the new instance for local models, combines with cloud models, and updates ALL 3 models.json files (runtime, dashboard, npm fallback), ALL 5 rules files (AGENTS.md, SYSTEM.md, SOUL.md, both CLAUDE.md), config.yaml fusion engine, and update-tekton-models.py script. Verifies all changes.
---

# Skill: add-account

**Trigger phrases:** "add ollama account", "add new machine", "add ollama instance", "new ollama server"

**Purpose:** Add a new Ollama instance to ALL config files and rules files in one command. Prevents the 30-minute manual process we had before.

## Required Information

Before starting, ask the user for:
1. **Account name** (e.g., "ollama-workstation2", "ollama-homeserver")
2. **LAN IP:port** (e.g., "192.168.68.75:11434")
3. **Tunnel URL** (e.g., "https://ollama-homeserver.messy-jesse.com" or "none" if no tunnel)
4. **Description** (e.g., "Home server - RTX 4090 GPU")

If the user gives an IP but no tunnel, offer to set up the Cloudflare tunnel DNS and route.

## Procedure

### Step 1: Query the new Ollama instance

```bash
curl -s --connect-timeout 5 http://<IP>:11434/api/tags
```

If reachable, parse the model list. If unreachable, ask user to verify the IP.

### Step 2: Determine cloud models

Ask: "Should this instance also serve cloud models (deepseek-v4-pro:cloud, glm-5.1:cloud, etc.)?"
If yes, include all known cloud models with the instance suffix.

### Step 3: Update all 3 models.json files

**File 1: `~/.pi/agent/models.json`** (runtime, powers / command)
- Add provider with detailed model objects (id, name, reasoning, input, contextWindow, maxTokens, cost)
- If tunnel URL provided, also add a `-tunnel` provider variant
- Model names get "(AccountName)" or "(AccountName Tunnel)" suffix

**File 2: `~/.tekton/models.json`** (dashboard + fusion engine)
- Same format as File 1
- Also add tunnel variant if tunnel URL provided

**File 3: `~/AppData/Roaming/npm/node_modules/tekton-agent/configs/models.json`** (NPM fallback)
- Different format: simple string arrays for models, different provider structure
- Add provider with `id`, `name`, `baseUrl`, `priority: 0`, `models: [...]`, `apiMode: "chat_completions"`, `local: true`

### Step 4: Update all 5 rules files

Update the Ollama accounts rule in ALL of these:

1. `~/.pi/agent/AGENTS.md` -- Rule 6
2. `~/.pi/agent/SYSTEM.md` -- Rule 6
3. `~/.tekton/SOUL.md` -- Rule 6
4. `~/.claude/CLAUDE.md` -- Rule 7
5. `D:\AI Drive\CLAUDE.md` -- Rule 7

Change "all X accounts" to "all X+1 accounts" and add the new account line.

### Step 5: Update config.yaml fusion engine

Add a new section to `~/.tekton/config.yaml`:
- Add `serverUrl<AccountName>` with the tunnel URL under `fusion:`
- Add `<accountName>Models:` section listing models for fusion pool

### Step 6: Update update-tekton-models.py

Add the new account to `~/.pi/agent/bin/update-tekton-models.py`:
- Add constants for IP and URL
- Add `--<accountname>` flag
- Add update blocks for all 3 config files

### Step 7: Verify all changes

Run a verification check:
```bash
python3 -c "
import json
for path, label in [
    (r'C:\Users\Massi\.pi\agent\models.json', '.pi/agent'),
    (r'D:\AI Drive\.tekton\models.json', '.tekton'),
    (r'C:\Users\Massi\AppData\Roaming\npm\node_modules\tekton-agent\configs\models.json', 'npm'),
]:
    with open(path) as f:
        d = json.load(f)
    if '<account_name>' in d.get('providers', {}):
        models = d['providers']['<account_name>']['models']
        print(f'{label}: <account_name> OK ({len(models)} models)')
    else:
        print(f'{label}: <account_name> MISSING')
"
```

### Step 8: Report

Summarize what was added:
- Account name and IPs
- Number of models (local + cloud)
- Which files were updated
- Suggest restarting any active terminal sessions