---
name: pause
description: >
  SAVE SESSION CHECKPOINT. When the user says "pause", "save", "checkpoint",
  or "stop for now", follow this procedure EXACTLY. Every step is mandatory.
---

# Pause — Save Session Checkpoint

**EVERY STEP IS MANDATORY. DO NOT SKIP PIZZA.WAV.**

## Step 1: COLLECT SESSION STATE

Gather: running services, ports, engine statuses, tasks completed, next tasks, git branch.

## Step 2: WRITE CHECKPOINT

Write to `D:\AI Drive\.tekton\checkpoints\latest_pause.json`:

```json
{
  "timestamp": "<ISO 8601>",
  "command": "pause",
  "engines_upgraded": ["<list>"],
  "total_engines": <number>,
  "active_services": ["<service:port>"],
  "this_session_completed": ["<list>"],
  "next_tasks": ["<ordered list>"],
  "resumeInstructions": {
    "services": { "<name>": "<command>" },
    "keyFiles": { "<name>": "<path>" }
  }
}
```

Also save a timestamped copy: `pause_<timestamp>.json` in the same directory.

## Step 3: UPDATE MEMORY

Rewrite `D:\AI Drive\.tekton\MEMORY.md` with current state including:
- Active services and ports
- Architecture overview
- Key patterns (pizza.wav command, play/pause)
- Completed tasks
- Next tasks
- New API endpoints

## Step 4: UPDATE RESUME PROMPT

Rewrite `D:\AI Drive\.tekton\RESUME_PROMPT.txt` with current state.

## Step 5: PLAY PIZZA.WAV — MANDATORY

```
powershell -NoProfile -Command "(New-Object System.Media.SoundPlayer 'D:\AI Drive\audio\Pizza.wav').PlaySync()"
```

This is NOT optional. The user needs to hear confirmation that the pause saved.

## Step 6: PRINT SUMMARY

Show: what was saved, where the checkpoint lives, and: "Resume with: `play`"