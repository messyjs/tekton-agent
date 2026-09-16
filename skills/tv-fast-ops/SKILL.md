---
name: tv-fast-ops
description: >-
  Fast operational playbook for driving TradingView Desktop from the trading
  chat app (CDP :9222, tradingview MCP tools). Use at the start of every
  trading task: fastest data paths, engine coordination rules, war-room
  layout specifics, order flow pointers. Triggers: any chart check, trade
  placement, price/quote request, engine status, war-room, paper account.
---

# TV Fast Ops (trading chat app)

## Speed rules (read first)

1. NEVER screenshot or read the full page for simple data. Use MCP data tools:
   - Symbol/quote/price: `quote_get` (blank = current chart symbol)
   - Bars: `data_get_ohlcv` with `summary:true` first; full bars only if needed
   - Ticks: `data_get_ticks`
   - Order book: `depth_get`
   - Equity/account: `data_get_equity`
   - Pine indicator values: `data_get_study_values`, `data_get_indicator`
   - Pine labels/tables from trade modules: `data_get_pine_labels`, `data_get_pine_tables`, `data_get_pine_boxes`, `data_get_pine_lines`
   - Strategy performance: `data_get_strategy_results`, `data_get_trades_analysis`
2. Keep answers short. Report numbers, not prose. No preamble.
3. One tool round when possible; batch independent reads mentally, not with many turns.
4. UI clicks (ui_click, ui_mouse_click) are the SLOW path - only for order tickets and panel
   operations that have no data-tool equivalent.

## Environment facts

- TV Desktop is at CDP port 9222. War-room tab = FIRST chart page (tab index 0),
  layout "ai" (id 202465827), symbol BINANCE:BTCUSDT.P, 1m timeframe.
- Bottom panel = TradingView Paper Trading, account "tekton" (USD, starts $100,000).
  Kill-switches in engine: halt if equity <= 97,000 or >= 106,000.
- Engine files: D:/AI Drive/hft/ (state.json = live state, engine.js), logs at
  D:/AI Drive/tmp/hft_log/ (engine.log = every tick + every engine action,
  tradelog.csv = every fill/bracket, STOP file in that dir halts the engine).
- A helper exists for raw CDP eval: node D:/AI Drive/tmp/cdp_eval.js 0 "<js>" 15000

## Engine coordination (critical)

- The engine (node engine.js, 60s loop) trades the same paper account autonomously:
  1m pivot breakout entries, 0.5 BTC clips, bracket orders, BE-lock + trailing.
- Check engine.log + state.json BEFORE proposing trades. If the engine is
  in_position, do NOT open another position - manage or wait.
- If the user asks to "stop trading": write the STOP file
  (D:/AI Drive/tmp/hft_log/STOP) and say so. Removing STOP + restarting the
  engine process re-arms it (see app Engine menu or do it via powershell).
- Never run a second copy of engine.js.

## Order placement flow

- Paper orders go through the chart right-click ticket or the bottom panel DOM.
  The engine's proven flow lives in D:/AI Drive/hft/engine.js (assertion-gated:
  side, qty, price, type verified in the ticket summary before submit).
- Read back the position from the bottom panel after any order ("Avg fill price"
  row) and report fill price + SL/TP to the user.
- NEVER place orders on the "MJ Strategy 250K start" account - confirm the panel
  shows "tekton" first if the account selector state is uncertain.

## Sessions

- If a turn stalls (no output >45s), the app watchdog auto-aborts and retries once.
  If asked to switch models, use set_model (cloud glm/deepseek models are reliable;
  avoid flash-class models for multi-step orchestration).
## Engine spawning rules (critical - learned 2026-09-07)

- NEVER create or launch a new autonomous trading engine/loop process (engine scripts,
  schedulers, watchers) without the user EXPLICITLY approving the design first.
- If the user asks for a strategy: discuss it, propose the implementation, WAIT for
  confirmation before spawning anything persistent. A strategy prompt is NOT permission
  to run an unsupervised engine on the account.
- One engine at a time. If asked to run a strategy while another engine trades, say so
  and ask which should stop.
- Any engine you do build must: register in D:/AI Drive/trading-chat/engines.json,
  log its fills/exits, respect a STOP file, and be reported to the user before it
  starts trading.

## Manual trading by the user (critical)

- The paper account tekton is account-level, NOT per-chart. If engines are trading on it,
  the user MUST halt them first before trading manually on ANY chart tab - otherwise the
  engine will see the user's position as its own and try to manage it (moving SLs).
- If the user wants to trade concurrently, offer the second paper account
  (MJ Strategy 250K start) on a separate chart tab's panel instead.
- HALT (app button) kills all hft engines + stops the agent. It does NOT flatten
  open positions - attached SL/TP brackets keep working server-side after a halt.
