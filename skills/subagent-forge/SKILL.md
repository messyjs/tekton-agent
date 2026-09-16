---
name: subagent-forge
description: >-
  Build sub-agents bound to engines so they match a character. Use when the
  user says "build a sub agent", "forge a sub agent", "make an agent like
  <persona>", "spawn builder". Pairs with engine-forge: the sub-agent inherits
  the engine's persona file, so Einstein-engine sub-agents think like Einstein.
---

# SubAgent Forge — characters that run the engines

A **sub-agent** = engine persona + role + toolbelt + spawn prompt. The forge
derives it FROM an engine, so the character always matches.

## Locations

| Path | Purpose |
|---|---|
| `~/.tekton/subagents/*.yaml` | sub-agent definitions (created here) |
| `~/.openmausbot/engines/*.txt` | persona source (from engine-forge) |
| `~/.tekton/engines.yaml` | capability servers the sub-agent may call |
| orchestration skill (Orca) | how sub-agents are spawned and coordinated |

## Definition format

```yaml
name: einstein-quant
character: einstein            # engine id -> ~/.openmausbot/engines/einstein.txt
engine: pi-agent-service       # engines.yaml binding (tools/service)
role: worker                   # worker | coordinator | specialist
tools: [sympy, scipy, z3, exec]
model: glm-5.1:cloud           # optional; omit -> sorting-hat routes
spawn: orca                    # transport: orca | gateway | pi-print
mission: >-
  Market regime puzzles with principle-first reasoning.
spawn_prompt: |
  You are Albert Einstein. <derived from the engine persona, verbatim voice.>
  YOUR MISSION: <mission>. Use tools: <toolbelt>. Report as <name>.
```

## Forge pipeline

1. **Pick or forge the engine** — existing engine, or run `engine-forge` first
   (sub-agents built on a nonexistent engine are refused: character needs a soul).
2. **Derive the definition** — copy the persona's voice rules into `spawn_prompt`
   (first-person, exact), set role + mission + toolbelt from the engine's tool list.
3. **Consistency check** — name, persona, voice, tools must all trace to the engine.
   A sub-agent that contradicts its character fails the forge.
4. **Write** `~/.tekton/subagents/<name>.yaml`.
5. **Smoke test** — one-shot `spawn_prompt` run; verify it answers in character.
6. **Register** — list it in `~/.tekton/engines.yaml` under `subagents:` so
   orchestration/gateway can discover it.

## Spawning

- **Orca** (orchestration skill): dispatch workers with the `spawn_prompt` as
  their opening context; the definition file is the worker's identity card.
- **Gateway**: `POST /chat` with `agent: <name>` header/route.
- Sub-agents inherit the House Style and memory protocol from `tekton.md`.

## Parallel forging

Multiple sub-agents = one forge worker per brief. The coordinator runs the
smoke tests and registration; workers never write the same file.
