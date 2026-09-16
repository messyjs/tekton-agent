---
name: project-status
description: SHOW ACTIVE WORK. When user says "what am I working on", "project status", "what's active", or "show projects". Reads projects.json for registered projects, MEMORY.md for recent activity, scans recently modified files, checks git status of active projects. Presents a status table with suggestions for next steps.
---

# Skill: project-status

**Trigger phrases:** "what am I working on", "project status", "what's the status", "what's active", "show projects"

**Purpose:** Give a quick briefing on what projects are active, what's in progress, and what's been recently modified. Reads from project registry, recent files, and MEMORY.md.

## Procedure

### Step 1: Read project registry

```bash
cat "D:\AI Drive\.tekton\projects.json"
```

### Step 2: Read recent memory

```bash
cat "D:\AI Drive\.tekton\MEMORY.md"
```

### Step 3: Check recently modified files

```bash
# Top 20 most recently modified files in AI Drive (excluding node_modules, .git, caches)
find "D:\AI Drive" -maxdepth 3 -type f \( -name "*.py" -o -name "*.js" -o -name "*.ts" -o -name "*.tsx" -o -name "*.json" -o -name "*.md" -o -name "*.yaml" -o -name "*.yml" -o -name "*.toml" -o -name "*.pine" \) -not -path "*/node_modules/*" -not -path "*/.git/*" -not -path "*/__pycache__/*" -not -path "*/.next/*" -mmin -1440 2>/dev/null | while read f; do stat -c "%Y %n" "$f" 2>/dev/null || echo "0 $f"; done | sort -rn | head -20
```

### Step 4: Check git status of active projects

For each project in projects.json, check if it has uncommitted changes:
```bash
for dir in [project directories]; do
  if [ -d "$dir/.git" ]; then
    cd "$dir" && git status --short 2>/dev/null | head -5
  fi
done
```

### Step 5: Present status table

```
PROJECT STATUS
=============

REGISTERED PROJECTS:
  tekton-agent    - web_api    - last session: 2026-06-11  - 2 sessions

RECENTLY MODIFIED (last 24h):
  [file1] - [time ago]
  [file2] - [time ago]
  ...

ACTIVE TASKS (from MEMORY.md):
  - [task 1]
  - [task 2]

RECENT DECISIONS:
  - [decision 1]
  - [decision 2]

SUGGESTED NEXT STEPS:
  - [based on TODOs in memory]
  - [based on incomplete tasks]
```

### Step 6: Offer to continue

Based on what's active and in-progress, offer options:
- "Last session was working on [X]. Want to continue?"
- "Project [Y] hasn't been touched in [Z days]. Want to check on it?"
- "No active tasks. What would you like to work on?"