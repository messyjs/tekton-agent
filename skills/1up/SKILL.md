---
name: 1up
description: >
  PROACTIVE AUTO-BUILD. The agent observes project patterns and one-ups the user
  by building features before they ask. Use /1up (3 features), /1up 10, /1up 20, /1up 50.
version: 1.0.0
metadata:
  tekton:
    tags: [1up, auto-build, proactive, enhance, feature]
    category: product
    confidence: 0.85
---

# 1up -- Proactive Auto-Build

When the user types `/1up`, `/1up 3`, `/1up 10`, `/1up 20`, or `/1up 50`, execute this process:

## Step 1: IDENTIFY PROJECT

Look at the current working directory. Identify the project type:
- Web app (has package.json, src/, public/)
- API (has routes/, api/, or server entry point)
- CLI (has main.py, cli.py, or bin/)
- Library (has lib/, setup.py, or package.json without main)
- Mobile app (has android/, ios/, or pubspec.yaml)

## Step 2: SCAN PROJECT

Use bash and read tools to:
1. List all files in the project directory
2. Read key files (package.json, main entry point, config files)
3. Identify what the project currently has and what it's missing

Check these dimensions based on project type:

| Dimension | Web App | API | CLI | Library |
|-----------|---------|-----|-----|---------|
| error_handling | YES | YES | YES | YES |
| loading_states | YES | NO | NO | NO |
| input_validation | YES | YES | YES | YES |
| auth | YES | YES | NO | NO |
| persistence | YES | YES | NO | NO |
| testing | YES | YES | YES | YES |
| documentation | YES | YES | YES | YES |
| accessibility | YES | NO | NO | NO |
| responsive | YES | NO | NO | NO |
| logging | YES | YES | YES | YES |
| config_management | YES | YES | YES | YES |
| feedback | YES | NO | NO | NO |

## Step 3: LEARN FROM PROFILE

Read the enhancement profile at `D:\AI Drive\.tekton\enhance_profile.json` if it exists.
Check what features the user typically adds, skips, or rejects.
Boost dimensions the user prioritizes. Deprioritize dimensions they skip.

If the profile doesn't exist yet, start with balanced priorities.

## Step 4: PLAN AND BUILD

For each feature to build (based on level: 3, 10, 20, or 50):

1. Read existing code to understand patterns and style
2. Create the feature following the project's conventions
3. Integrate it into the project
4. Verify nothing broke (run tests or lint if available)
5. Record what was built

## Step 5: REPORT

Show the user what was built:

```
1UP COMPLETE
============
Level: Quick (3 features built)
Project: [name] [[type]]

Built:
  1. [dimension] Feature description
     -> file/path/created.tsx (created)
     -> file/path/modified.tsx (modified: what changed)
  2. ...
  3. ...

Skipped (from your profile):
  - Feature name (reason)

Profile updated at: D:\AI Drive\.tekton\enhance_profile.json
```

## Sub-commands

- `/1up learn` -- Show what the profile has learned about the user
- `/1up off` -- Disable auto-enhance mode
- `/1up reset` -- Clear the enhancement profile and start fresh
