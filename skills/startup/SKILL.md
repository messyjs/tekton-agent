---
name: startup
description: SESSION INITIALIZATION. Automatically run at the start of every new session, or when user says "startup", "initialize", "boot up", or "what's the status". Reads MEMORY.md, USER.md, IDENTITY.md, projects.json, pings all Ollama instances and tunnels, checks dashboard and services, scans recent file modifications, and presents a clean briefing table. Asks if user wants to continue previous work.
---

# Skill: startup

**Trigger:** Every new session (automatic). Also triggered by "startup", "start up", "initialize", "boot up", "what's the status".

**Purpose:** Load all context, check what's running, give the user a briefing so the session starts at full efficiency instead of spending 10 minutes rediscovering basics.

## Procedure

### Step 1: Read context files (in this exact order)

Read these files silently (do not dump their contents into chat, just absorb them):

1. `D:\AI Drive\.tekton\MEMORY.md` -- long-term memory, recent notes
2. `D:\AI Drive\.tekton\USER.md` -- user profile and corrections
3. `D:\AI Drive\.tekton\IDENTITY.md` -- who I am, where my files are
4. `D:\AI Drive\.tekton\projects.json` -- registered projects and status

If any file is missing or empty, note it but continue.

### Step 2: Check running services

Run these checks quickly (each with 3-second timeout):

```bash
# Check Ollama instances
curl -s --connect-timeout 3 http://127.0.0.1:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'Laptop: {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "Laptop Ollama: OFFLINE"

curl -s --connect-timeout 3 http://192.168.68.70:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'Workstation: {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "Workstation Ollama: OFFLINE"

curl -s --connect-timeout 3 http://192.168.68.62:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'MJ Laptop: {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "MJ Laptop Ollama: OFFLINE"

# Check Cloudflare tunnels
curl -s --connect-timeout 5 https://ollama.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print('Laptop tunnel: UP')" 2>/dev/null || echo "Laptop tunnel: DOWN"
curl -s --connect-timeout 5 https://tekton-fused.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print('WS tunnel: UP')" 2>/dev/null || echo "WS tunnel: DOWN"
curl -s --connect-timeout 5 https://ollama-mjlaptop.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print('MJ Laptop tunnel: UP')" 2>/dev/null || echo "MJ Laptop tunnel: DOWN"

# Check dashboard
curl -s --connect-timeout 3 http://192.168.68.70:7700 >/dev/null 2>&1 && echo "Dashboard (7700): UP" || echo "Dashboard (7700): DOWN"

# Check feed server
curl -s --connect-timeout 3 http://192.168.68.70:8787 >/dev/null 2>&1 && echo "Feed Server (8787): UP" || echo "Feed Server (8787): DOWN"
```

### Step 3: Check recent activity

```bash
# Most recently modified files in AI Drive (last 10)
find "D:\AI Drive" -maxdepth 2 -type f -mmin -1440 -name "*.py" -o -name "*.js" -o -name "*.ts" -o -name "*.json" -o -name "*.md" 2>/dev/null | head -15

# Check git status of active projects
ls -lt "D:\AI Drive" --time=modify 2>/dev/null | head -10
```

### Step 4: Give briefing

Present a clean briefing table like this:

```
TEKTON STARTUP BRIEFING
=======================

STATUS:
  Laptop Ollama:     UP (25 models)
  Workstation Ollama: UP (14 models)
  MJ Laptop Ollama:  UP (7 models)
  Laptop tunnel:      UP
  WS tunnel:          UP
  MJ Laptop tunnel:   UP
  Dashboard (7700):   UP
  Feed Server (8787): UP

RECENT ACTIVITY:
  - [last modified project/file]
  - [second most recent]
  - ...

MEMORY NOTES:
  - [key items from MEMORY.md]

PROJECTS:
  - [project name]: [status from projects.json]

DEFAULT MODEL: [from settings.json]
```

### Step 5: Ask if user wants to continue previous work

If there are active tasks in MEMORY.md or projects.json, ask:
"Last session you were working on [X]. Want to continue, or start something new?"

Do NOT auto-resume. Just ask.