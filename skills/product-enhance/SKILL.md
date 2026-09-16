---
name: product-enhance
description: >
  AUTOMATIC PRODUCT ENHANCEMENT. Use /enhance to analyze a project, identify missing
  features, and suggest improvements. Levels: /enhance (3), /enhance 10, /enhance 20, /enhance 50.
  Also: /score to see current score, /suggest to get recommendations without building.
version: 1.0.0
metadata:
  tekton:
    tags: [product, enhancement, features, improve, quality, score]
    category: product
    confidence: 0.85
---

# Product Enhance -- Analyze and Suggest

When the user types `/enhance`, `/score`, or `/suggest`, execute this process:

## /enhance [level]

Default level is 3. Levels: 3 (quick), 10 (standard), 20 (deep), 50 (maximum).

1. Identify the project in the current working directory
2. Scan all files to understand what exists
3. Score the project across relevant dimensions (0-10 each)
4. Identify the top N missing features based on project type
5. Present suggestions grouped by priority
6. Ask the user which features to build, then implement them

## /score

Show the current project score without suggesting improvements:

```
PROJECT SCORE: [name] [[type]]
================================
error_handling:     3/10  [-] needs try/catch, error boundaries
input_validation:   7/10  [+] good coverage
testing:            2/10  [--] almost no tests
logging:            5/10  [~] basic logging present
...
TOTAL: 45/90 (50%)
```

## /suggest

Like /enhance but only shows suggestions without offering to build them.

## Scoring Dimensions

Score each dimension 0-10 based on what actually exists in the code:

- **error_handling**: try/catch blocks, error boundaries, fallback UI
- **loading_states**: spinners, skeletons, progress indicators
- **input_validation**: form validation, type checking, sanitization
- **auth**: login/signup, session management, role-based access
- **persistence**: database, localStorage, file storage
- **testing**: unit tests, integration tests, test coverage
- **documentation**: README, API docs, inline comments, changelog
- **accessibility**: ARIA labels, keyboard nav, screen reader support
- **responsive**: mobile layout, breakpoints, fluid grids
- **logging**: structured logging, log levels, log rotation
- **config_management**: env vars, config files, secrets management
- **feedback**: toast notifications, success/error messages, progress bars

Not all dimensions apply to all projects. Skip irrelevant ones.

## Project Type Detection

- **Web App**: package.json + public/ or src/ with HTML/CSS/JS
- **API**: Express/FastAPI/Flask server with routes
- **CLI**: main entry point with argparse/click/commander
- **Library**: setup.py/pyproject.toml with lib/ or src/ for export
- **Mobile**: android/, ios/, or pubspec.yaml

Skip dimensions that don't apply (e.g., loading_states for APIs, accessibility for CLIs).
