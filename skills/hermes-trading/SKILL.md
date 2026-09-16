---
name: hermes-trading
description: Self-improving trading agent that reflects on outcomes and evolves its strategy one variable at a time. Runs on workstation with glm-5.1:cloud as the reflection brain. Triggers - "run hermes", "trading reflection", "trading strategy", "hermes-trading".
---

# Hermes Trading Agent

A self-improving trading agent that pulls market data, takes paper trades, scores them against a hard goal, and reflects on outcomes using glm-5.1:cloud -- changing exactly ONE variable per cycle.

## Commands

All commands run from `D:/AI Drive/tekton-trading/`:

### Force a reflection cycle
```bash
cd "D:/AI Drive/tekton-trading" && python3 -m hermes_trading.run --reflect
```
Uses glm-5.1:cloud by default. Add `--reflect-mode fallback` for deterministic rules.

### Run one trading cycle
```bash
cd "D:/AI Drive/tekton-trading" && python3 -m hermes_trading.run --once
```

### Run continuously
```bash
cd "D:/AI Drive/tekton-trading" && python3 -m hermes_trading.run
```

### Check current strategy
```bash
cat "D:/AI Drive/tekton-trading/state/strategy.yaml"
```

### Check strategy history
```bash
ls "D:/AI Drive/tekton-trading/state/history/"
```

### Check hypotheses log
```bash
cat "D:/AI Drive/tekton-trading/state/hypotheses.jsonl"
```

### Check trades
```bash
cat "D:/AI Drive/tekton-trading/state/trades.jsonl"
```

### Check heartbeat
```bash
cat "D:/AI Drive/tekton-trading/state/heartbeat.json"
```

## Key Files
- `state/goal.yaml` -- success/failure criteria (target return, max drawdown, min Sharpe)
- `state/strategy.yaml` -- current strategy (evolves with each reflection)
- `state/trades.jsonl` -- every paper trade logged
- `state/hypotheses.jsonl` -- every strategy change with reasoning
- `state/history/` -- every prior strategy version preserved

## Edit goal.yaml before going live
Change `trading_mode: paper` to `live` and `accept_risk: true` ONLY when ready. This is a hard safety rail.

## Model routing
Reflection brain uses glm-5.1:cloud on ollama-workstation (primary) and ollama-laptop (backup). Falls back to deterministic rules if both fail.
