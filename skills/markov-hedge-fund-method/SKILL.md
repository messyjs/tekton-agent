---
name: markov-hedge-fund-method
description: Observable Markov regime model for any ticker. Builds the transition matrix from a rolling-return regime label (Bull / Bear / Sideways), forecasts n-step ahead via matrix power, solves the stationary distribution, and runs a walk-forward backtest. Optional Hidden Markov Model upgrade via hmmlearn. Android app available with multi-source data support.
---

# markov-hedge-fund-method

Install location: `~/.claude/skills/markov-hedge-fund-method/`
Android app: `~/.claude/skills/markov-hedge-fund-method/android/`
Framework: Roan (@RohOnChain). Installed as a skill by Lewis Jackson.

## CLI Usage

```
cd ~/.claude/skills/markov-hedge-fund-method
uv run scripts/markov_regime.py --ticker <SYMBOL> [--years 10] [--window 20] [--threshold 0.05] [--no-hmm] [--json]
```

Or with your own CSV: `--csv path/to/file.csv`

## Android App

Located at `android/`. Installed on connected Samsung Galaxy Note 10.

Data sources:
- **Yahoo Finance** — free, no key needed (default)
- **Alpha Vantage** — free tier, stocks/forex (API key required)
- **CoinGecko** — free, crypto-focused (optional Pro key)
- **Polygon.io** — free tier, stocks/crypto/forex (API key required)

Users can add API keys in the Settings screen (gear icon).

## Build & Deploy

```
cd ~/.claude/skills/markov-hedge-fund-method/android
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/Massi/Android/Sdk"
./gradlew assembleDebug
adb -s RF8M8106Q9Z install -r app/build/outputs/apk/debug/app-debug.apk
adb -s RF8M8106Q9Z shell am start -n com.markovhedgefund.app/.MainActivity
```
