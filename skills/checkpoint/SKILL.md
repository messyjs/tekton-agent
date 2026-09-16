---
name: checkpoint
description: SAVE SESSION STATE. When user says "checkpoint", "save state", "save progress", or "remember this". Appends a dated section to MEMORY.md with accomplishments, TODOs for next session, key decisions, and files modified. Updates projects.json with session info. Confirms what was saved.
---

# Skill: checkpoint

**Trigger phrases:** "checkpoint", "save state", "save progress", "checkpoint save", "remember this"

**Purpose:** Save a structured snapshot of current session state to MEMORY.md and projects.json so the next session can pick up exactly where you left off.

## Procedure

### Step 1: Read current state

Read these files to understand current state:
- `D:\AI Drive\.tekton\MEMORY.md` -- append, don't overwrite
- `D:\AI Drive\.tekton\projects.json` -- update project entries
- `D:\AI Drive\.tekton\USER.md` -- check for corrections

### Step 2: Ask what to save (if not obvious)

If the user just says "checkpoint" without context, ask:
1. "What were you working on?" (one-line summary)
2. "What's the current status?" (in progress, blocked, complete, etc.)
3. "Any blockers or TODOs for next time?"

If the user says "checkpoint" in the middle of work, infer the context from recent conversation.

### Step 3: Write to MEMORY.md

Append a dated section to MEMORY.md:

```markdown

## [YYYY-MM-DD] Checkpoint

### What was being worked on:
[one-line summary]

### Current status:
[in progress / blocked / complete / etc.]

### What was accomplished:
- [accomplishment 1]
- [accomplishment 2]
- ...

### Blockers / TODOs for next session:
- [ ] [todo 1]
- [ ] [todo 2]
- ...

### Key decisions made:
- [decision 1]
- [decision 2]

### Files modified:
- [path/to/file1] -- [what changed]
- [path/to/file2] -- [what changed]
```

### Step 4: Update projects.json

If this relates to a registered project, update its entry:
- `last_session`: current timestamp
- `recent_changes`: add what changed
- `active_tasks`: add or update tasks
- `notes`: add relevant notes

If no project entry exists, offer to create one.

### Step 5: Confirm

Show the user what was saved:
```
CHECKPOINT SAVED
===============
Working on: [summary]
Status: [status]
TODOs for next time:
  - [ ] todo 1
  - [ ] todo 2

Saved to: MEMORY.md, projects.json
```

### Auto-checkpoint

If the user says "pause" or "save" or is about to end a session, automatically offer to checkpoint if one hasn't been made in the last 30 minutes.