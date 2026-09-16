---
name: sorting-hat
description: Route each task to the right Ollama account so credits never run dry. Bounces between ollama-workstation and ollama-laptop (both running glm-5.1:cloud). Default to workstation, bounce to laptop when throttled. Triggers - "which model", "route this", "sorting hat", "which account".
---

# The Sorting Hat

All inference runs on glm-5.1:cloud. No Claude. The only decision is which account to hit so you never run out of credits.

## Account Routing

Two Ollama accounts, both running glm-5.1:cloud, both nearly unlimited:

- **ollama-workstation**: http://192.168.68.70:11434 (always on, no battery concerns, preferred)
- **ollama-laptop**: http://127.0.0.1:11434 (portable, backup)

### Bounce Rules

1. Default to ollama-workstation for every request.
2. If ollama-workstation returns a credit/rate-limit error, bounce immediately to ollama-laptop.
3. If ollama-laptop also throttles, wait 60 seconds and retry ollama-workstation.
4. Track which account was last throttled and pre-emptively route to the other one until that cooldown clears.

### Model Reference

- Primary model: `glm-5.1:cloud` on both accounts
- If glm-5.1:cloud is unavailable, check what is running on each box:
  - ollama-workstation: http://192.168.68.70:11434/api/tags
  - ollama-laptop: http://127.0.0.1:11434/api/tags
  - Fallback order: glm-5.1:cloud > any other available model

## How to route

1. Default: send to ollama-workstation.
2. If throttled: bounce to ollama-laptop.
3. If both throttled: wait, retry workstation.

No fallback to Claude. Everything runs on Ollama.
