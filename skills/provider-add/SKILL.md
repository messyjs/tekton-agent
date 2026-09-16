---
name: provider-add
description: >-
  Connect a new model platform (Ollama, OpenAI, Anthropic, OpenRouter, Groq,
  Gemini, LM Studio, llama.cpp, any OpenAI-compatible) to Tekton. Use when the
  user says "add provider", "add openai", "add anthropic", "add openrouter",
  "use groq", "connect my api".
---

# Provider Add — connect any model platform in two minutes

Tekton speaks two dialects: **OpenAI-compatible** (almost everyone) and
**native Anthropic** (via pi's auth). Add providers to `~/.tekton/models.json`
(pi format), wire routing in `config.yaml`, ping-test, done.

## Known platforms

| Platform | baseUrl | api | Notes |
|---|---|---|---|
| Ollama (any host) | `http://HOST:11434/v1` | openai-completions | apiKey: `ollama` |
| OpenAI | `https://api.openai.com/v1` | openai-completions | key in auth or provider |
| Anthropic | (native) | anthropic | via `pi` login / auth.json |
| OpenRouter | `https://openrouter.ai/api/v1` | openai-completions | 400+ models |
| Groq | `https://api.groq.com/openai/v1` | openai-completions | fast inference |
| Gemini | `https://generativelanguage.googleapis.com/v1beta` | openai-completions | OpenAI-compatible endpoint |
| LM Studio | `http://localhost:1234/v1` | openai-completions | local |
| llama.cpp server | `http://localhost:8080/v1` | openai-completions | local |
| Azure OpenAI | `https://<res>.openai.azure.com/openai/v1` | openai-completions | deployment ids as model ids |
| Custom | any `/v1` | openai-completions | works with qmd/proxies |

## Steps

1. **Collect**: provider id (short, e.g. `ollama-workstation`), baseUrl, apiKey,
   model ids to expose.
2. **Write** a `providers.<id>` block into `~/.tekton/models.json`
   (same shape as `models.example.json` — copy it).
3. **Route**: set `models.fast` / `models.deep` in `config.yaml` (or leave to
   sorting-hat for multi-account bouncing).
4. **Ping**: one-shot completion through the new provider; require a sane reply.
5. **Refresh**: run the `update-models` skill so the catalog and the Agent OS
   Models tab stay current.
6. **Secrets**: API keys live in `auth.json` / provider blocks — never in
   `tekton.md`, never in chat transcripts, never committed.

## Multi-account pattern

Same platform, several hosts (workstation, laptop, phone)? Give each a distinct
provider id (`ollama-workstation`, `ollama-laptop`) — `sorting-hat` then routes
tasks across them and `add-account` manages the fleet.
