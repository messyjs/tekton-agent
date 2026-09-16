---
name: health
description: SYSTEM HEALTH CHECK. When user says "health check", "system status", "check services", "is everything running", or "status". Pings all 3 Ollama instances (laptop, workstation, mjlaptop), checks 3 Cloudflare tunnels, tests dashboard (7700) and feed server (8787), checks disk space. Returns a clean UP/DOWN status table with fix suggestions for anything offline.
---

# Skill: health

**Trigger phrases:** "health check", "system status", "check services", "is everything running", "status", "ping all"

**Purpose:** Quick health check of all services, Ollama instances, tunnels, and dashboard. Returns a status table.

## Procedure

### Step 1: Ping all Ollama instances

Run each with 5-second timeout. Report model count if up, or OFFLINE if down.

```bash
echo "=== OLLAMA INSTANCES ==="
curl -s --connect-timeout 5 http://127.0.0.1:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  Laptop (127.0.0.1):      UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  Laptop (127.0.0.1):      OFFLINE"

curl -s --connect-timeout 5 http://192.168.68.70:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  Workstation (192.168.68.70): UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  Workstation (192.168.68.70): OFFLINE"

curl -s --connect-timeout 5 http://192.168.68.62:11434/api/tags | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  MJ Laptop (192.168.68.62):  UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  MJ Laptop (192.168.68.62):  OFFLINE"
```

### Step 2: Check Cloudflare tunnels

```bash
echo ""
echo "=== CLOUDFLARE TUNNELS ==="
curl -s --connect-timeout 8 https://ollama.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  ollama.messy-jesse.com:      UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  ollama.messy-jesse.com:      DOWN"

curl -s --connect-timeout 8 https://tekton-fused.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  tekton-fused.messy-jesse.com: UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  tekton-fused.messy-jesse.com: DOWN"

curl -s --connect-timeout 8 https://ollama-mjlaptop.messy-jesse.com/api/tags 2>/dev/null | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'  ollama-mjlaptop.messy-jesse.com: UP - {len(d.get(\"models\",[]))} models')" 2>/dev/null || echo "  ollama-mjlaptop.messy-jesse.com: DOWN"
```

### Step 3: Check other services

```bash
echo ""
echo "=== SERVICES ==="
curl -s --connect-timeout 3 http://192.168.68.70:7700 >/dev/null 2>&1 && echo "  Dashboard (7700):         UP" || echo "  Dashboard (7700):         DOWN"
curl -s --connect-timeout 3 http://192.168.68.70:8787 >/dev/null 2>&1 && echo "  Feed Server (8787):       UP" || echo "  Feed Server (8787):       DOWN"

# Check if dashboard is accessible via tunnel
curl -s --connect-timeout 5 https://tekton.messy-jesse.com >/dev/null 2>&1 && echo "  Dashboard tunnel:         UP" || echo "  Dashboard tunnel:         DOWN"

# Check speak tunnel (IndexTTS)
curl -s --connect-timeout 5 https://speak.messy-jesse.com >/dev/null 2>&1 && echo "  IndexTTS tunnel (7860):    UP" || echo "  IndexTTS tunnel (7860):    DOWN"
```

### Step 4: Check disk space

```bash
echo ""
echo "=== DISK ==="
df -h /d 2>/dev/null | tail -1 || echo "  D: drive info unavailable"
```

### Step 5: Present results as a clean table

Format all results in a dark-mode status table:

```
SYSTEM HEALTH CHECK
===================

OLLLAMA INSTANCES:
  Laptop (127.0.0.1):          UP   25 models
  Workstation (192.168.68.70): UP   14 models
  MJ Laptop (192.168.68.62):   UP    7 models

TUNNELS:
  ollama.messy-jesse.com:      UP
  tekton-fused.messy-jesse.com: UP
  ollama-mjlaptop.messy-jesse.com: UP

SERVICES:
  Dashboard (7700):            UP
  Feed Server (8787):          UP
  Dashboard tunnel:            UP
  IndexTTS tunnel:             UP

DISK:
  D: drive: 2.1 TB free / 4 TB total
```

If anything is DOWN, flag it clearly and suggest fixes:
- Ollama offline: "Start with `ollama serve` or check if the machine is on"
- Tunnel down: "Check `cloudflared` service or restart the tunnel"
- Dashboard down: "SSH to workstation and run the dashboard start command"