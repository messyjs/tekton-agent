---
name: dreams
description: Run a Dreams pattern analysis on accumulated agent data. Scans Obsidian vault, session logs, decisions, and sends batches to glm-5.1:cloud for pattern recognition. Use when you want to find non-obvious patterns, repeated mistakes, or behavioral drift across sessions. Triggers - "run a dream", "dream analysis", "find patterns", "what patterns exist in", "dreams".
---

# Dreams

Dreams is your pattern recognition engine. It reads through accumulated session data, decisions, and logs, then uses glm-5.1:cloud to surface non-obvious patterns, correlations, and behavioral drift.

## Commands

All commands run from `D:/AI Drive/dreams/`:

### Run a dream analysis
```bash
cd "D:/AI Drive/dreams" && python3 dreams.py run <source> "<pattern_category>"
```

Sources: `daily_logs`, `decisions`, `cavemem`, `project_state`, `user_profile`, `workspace`, `all`

Examples:
- `python3 dreams.py run all "patterns in repeated mistakes"`
- `python3 dreams.py run decisions "decision drift over time"`
- `python3 dreams.py run daily_logs "what agents get wrong most often"`

### List available data sources
```bash
cd "D:/AI Drive/dreams" && python3 dreams.py sources
```

### View past dream runs
```bash
cd "D:/AI Drive/dreams" && python3 dreams.py history
```

### Define a new use case (interactive)
```bash
cd "D:/AI Drive/dreams" && python3 dreams.py define
```

## Output

Every dream run produces:
1. **Obsidian note** at `D:/AI Drive/Obsidian/dreams/dream-<timestamp>.md` -- visible to all agents
2. **JSON archive** at `D:/AI Drive/dreams/output/dream-<timestamp>.json` -- machine-readable
3. **Index update** at `D:/AI Drive/Obsidian/dreams/index.md` -- running log of all dreams

## Model routing

Uses glm-5.1:cloud on ollama-workstation by default. Bounces to ollama-laptop if throttled. No Claude fallback -- all analysis runs on Ollama.

## When to use

- After a week of sessions, run `all` to find behavioral patterns
- When an agent repeats the same mistake, run `daily_logs` to see the pattern
- Before making a system change, run `decisions` to see if this decision has been made before
- When coordination feels off, run `cavemem` to see session-level drift
