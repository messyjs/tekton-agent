---
name: project-registry
description: >
  Project tracking and fast session resumption. Use /projects to list registered
  projects, /projects [name] to load a project's context card.
version: 1.0.0
metadata:
  tekton:
    tags: [projects, registry, context, session, resume]
    category: product
    confidence: 0.85
---

# Project Registry -- Never Search For A Project Again

When the user types `/projects`, `/projects [name]`, or mentions working on a specific project:

## /projects

List all registered projects. Read the registry file at `D:\AI Drive\.tekton\project_registry.json`.
If it doesn't exist, scan `D:\AI Drive` for project directories and create it.

Display:
```
REGISTERED PROJECTS
===================
1. tekton-agent  [API]     Python/FastAPI   Score: 62/90   Last: 2025-06-11
2. tekton-app    [MOBILE]  Flutter/Dart      Score: 45/90   Last: 2025-06-10
3. apex          [WEB]     React/TypeScript  Score: 71/90   Last: 2025-06-09
```

## /projects [name]

Load a project's context card. This gives you everything you need to resume work:

1. Read the project's entry from the registry
2. Read key files listed in the context card
3. Summarize the project state for the user

Display:
```
LOADING: tekton-agent
=====================
Type: API (Python/FastAPI)
Score: 62/90
Key files: server.py, agent.py, tools/auto_enhance.py
Last session: Added product-enhance and 1up skills
Recent changes: Rewrote skill files, fixed notepad rules
```

## Auto-Registration

Every time you work on a project:
1. Check if it's in the registry
2. If not, add it with type, tech stack, and key files
3. If yes, update the last-worked date and recent changes
4. Save the registry to `D:\AI Drive\.tekton\project_registry.json`

## Registry Format

```json
{
  "projects": {
    "project-name": {
      "type": "web_app|api|cli|library|mobile",
      "tech_stack": ["Python", "FastAPI", "React"],
      "score": 62,
      "max_score": 90,
      "key_files": ["path/to/main.py", "path/to/config.yaml"],
      "last_worked": "2025-06-11",
      "recent_changes": "Description of last session's work",
      "directory": "D:\AI Drive\project-name"
    }
  }
}
```
