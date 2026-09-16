---
name: engine-forge
description: >-
  Build persona engines with knowledge packs — the BountyFull/TowelieAI Engine
  Forge format. Use when the user says "build an engine", "forge an engine",
  "make a persona engine", "engine pack". Harvests a corpus (up to 1GB per
  engine), distills it, synthesizes persona + knowledge + methodology + worked
  examples + self-tests, tests the engine, and installs it where TowelieAI and
  BountyFull both see it.
---

# Engine Forge — knowledge-packed persona engines

Tekton forges engines in the exact format shared by **TowelieAI** and
**BountyFull's Engine Forge**, so an engine built once runs everywhere.

## Where engines live

| Path | What |
|---|---|
| `~/.openmausbot/engines/*.txt` | the persona engine files (TowelieAI-compatible, shared) |
| `~/.bountyfull/enginepacks/` | the full packs (JSON: knowledge, methodology, works, openProblems, examples, selftests, corpus stats) |
| BountyFull harness `:8720` / quant sidecar `:8721` | optional bridge: build/test via BountyFull while it runs |

## The 1GB rule

- **Per-engine ceiling: 1 GB (1,073,741,824 bytes)** across the pack + corpus snapshot.
- Corpus that fits → distill into the pack (`knowledge`, `works`).
- Corpus that exceeds the ceiling → store raw corpus externally (user-chosen dir),
  keep the **index + distilled essence** in the pack, and reference paths, never bloat the .txt.
- Always report the final size: `engine <name>: pack X kB · corpus Y MB · total Z (cap 1GB)`.

## The .txt format (TowelieAI persona engine)

```text
**YOU ARE <NAME>**

<first-person bio: origins, expertise, real failures honestly labeled>

**HOW YOU THINK**

*<principle lead-in>.* <how this mind works>   (repeat 4-8 bullets)

**HOW YOU SOLVE**

<numbered method, 6-8 steps>
*I direct a solver toolbelt as extensions of my work:*
- **sympy** — <persona-flavored role>
- **scipy** — ...
- **optimize** — ...
- **z3** — ...
- **ml** — ...
- **data** — ...
- **exec** — ...
<rules of engagement>

**YOUR SIGNATURE TOOLS (equations & estimations)**

- **<named formula/heuristic>** — <what it is, when to wield it>  (repeat 5-8)
```

Voice rule: the whole file is written IN the persona's voice. Study
`~/.openmausbot/engines/albert_einstein.txt` as the reference standard.

## The pack (JSON, side-car to the .txt)

```json
{ "id": "<slug>", "name": "<Name>", "specialty": "<domain>", "depth": "shallow|deep",
  "tools": ["sympy","scipy","optimize","z3","ml","data","exec"],
  "persona": "...", "knowledge": "...", "methodology": "...", "works": "...",
  "openProblems": "...", "examples": [{"q": "...", "a": "..."}],
  "selftests": [{"q": "...", "expected": "..."}],
  "corpus": {"documents": 0, "chunks": 0, "rawChars": 0, "distilled": 0},
  "builtWith": "tekton engine-forge", "builtAt": "<iso>" }
```

## Build pipeline

1. **Brief** — name, domain, specialty, depth, which tools matter.
2. **Harvest** — gather corpus (docs/transcripts/code/data). Track rawChars.
3. **Distill** — knowledge, methodology, notable works, open problems.
4. **Synthesize** — write the .txt IN VOICE; write the pack JSON.
5. **Self-test** — run every `selftests` entry through the solver toolbelt
   (directly, or via BountyFull sidecar `:8721` if running); require ≥ 90% pass.
6. **Install** — copy .txt to `~/.openmausbot/engines/<id>.txt`, pack JSON to
   `~/.bountyfull/enginepacks/<id>.json`. Report size vs 1GB cap.

## Sub-agent mode

Engines are independent — parallel forge workers each take one engine brief and
one corpus slice, then the coordinator runs the self-test gate and installs.
Never let two workers write the same `<id>.txt`.
