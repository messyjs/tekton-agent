---
name: play
description: >
  RESUME FROM CHECKPOINT. When the user says "play", "resume", "continue", "start up",
  or "restart", execute this procedure EXACTLY. This is NOT a game request.
  This is ALWAYS available — even on a brand new session with zero other context.
---

# Play — Resume From Checkpoint

**THIS IS A MANDATORY PROCEDURE. DO NOT SKIP STEPS.**

## Step 1: PLAY PIZZA.WAV — THIS IS NOT OPTIONAL

IMMEDIATELY run this bash command as your VERY FIRST action:

```
powershell -NoProfile -Command "(New-Object System.Media.SoundPlayer 'D:\AI Drive\audio\Pizza.wav').PlaySync()"
```

The user needs to HEAR that the session has started. This is an accessibility feature.
If you skip this, you have broken a core requirement.

## Step 2: LOAD CHECKPOINT DATA

Read these two files IN ORDER:

1. `D:\AI Drive\.tekton\checkpoints\latest_pause.json`
2. `D:\AI Drive\.tekton\MEMORY.md`

If latest_pause.json doesn't exist at that path, try:
- `/d/AI Drive/.tekton/checkpoints/latest_pause.json`
- Search for any `pause_*.json` or `latest_pause.json` in `D:\AI Drive\.tekton\checkpoints\`
- Use the most recent one

## Step 3: RESTART SERVICES

Start all services listed in the checkpoint's `resumeInstructions.services`. Core services:

```bash
cd "/d/AI Drive/pi-agent/tekton" && node dashboard-enhanced.mjs --port 7700 --host 0.0.0.0 &
cd "/d/AI Drive/pi-agent/tekton/packages/gann-app" && python gann_app.py --port 7799 &
```

Wait 3 seconds, then verify:
```bash
curl -s http://localhost:7700/ | head -3
curl -s http://localhost:7799/api/engines | python3 -c "import json,sys; d=json.load(sys.stdin); print(f'Engines: {len(d[\"engines\"])}')"
```

If a service fails, retry once. If still failing, report but continue.

## Step 4: PRINT RESUME SUMMARY

Show a table like:
```
[RESUME] CHECKPOINT RESUMED
| Component | Status |
|-----------|--------|
| Dashboard (7700) | [OK] LIVE |
| Command Center (7799) | [OK] LIVE |
| Engines | 13 live |

Next tasks: [list from checkpoint]
```

## Step 5: BEGIN WORKING

Immediately start on the FIRST item in `next_tasks` from the checkpoint.
DO NOT say "What would you like me to do?" or "How can I help?" — JUST START.

## MANDATORY PIZZA RULE

Every time you complete a task or give a summary, you MUST play pizza.wav again:
```
powershell -NoProfile -Command "(New-Object System.Media.SoundPlayer 'D:\AI Drive\audio\Pizza.wav').PlaySync()"
```

This is not optional. It's an audible notification system the user depends on.