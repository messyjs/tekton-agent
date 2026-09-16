---
name: house-style
description: >-
  The House Style — Tekton's design language, distilled from BountyFull.
  Use BEFORE writing any UI (new apps, pages, components, dashboards, Agent OS
  screens, landing pages, emails-as-html). Copy assets/house-theme.css into the
  new project first, then extend from it. Plain, elegant, brilliant — from the
  first line of every build.
---

# House Style — plain, elegant, brilliant

Every interface Tekton builds speaks this dialect. No exceptions, no "just this once".

## Start here

1. Copy `assets/house-theme.css` into the project as the base stylesheet (or emit its token block as the first CSS).
2. Build ONLY from the recipes below unless the user explicitly asks otherwise.
3. Dark-first. If a light theme is required, invert alphas — never the philosophy.

## The tokens (non-negotiable)

| Token | Value | Role |
|---|---|---|
| `--bg` | `#0b0d10` | near-black canvas |
| `--panel` / `--panel2` | `rgba(255,255,255,.04)` / `.025` | surfaces are white alpha, never solid gray |
| `--border` / `--border-strong` | `rgba(255,255,255,.11)` / `.22` | hairlines only |
| `--text / --dim / --faint` | `#f4f5f7` / `#c7ccd4` / `#a3abb6` | three-step text hierarchy |
| `--green / --amber / --red` | `#8fc7a0` / `#d3ba7e` / `#d98d8d` | ok / warn / error — desaturated pastels |
| `--gold` | `#d6cfbf` | warm highlight (stars, brand, active) |
| `--grad` | `linear-gradient(180deg,#f4f5f7,#e4e6ea)` | subtle text/hero accent only |

## Hard rules

1. **Alpha over solid** — surfaces/borders are white-alpha layers on `--bg`; never solid `#222` panels.
2. **Hairlines** — 1px borders; depth comes from layering, never heavy shadows.
3. **No neon** — accents are muted pastels. If it glows, it's wrong.
4. **Radius scale** — 4 (chips/badges) · 6 (small btn) · 8 (btn/input/nav) · 10 (code blocks) · 12 (panels).
5. **Type** — `"Segoe UI Variable Text", "Segoe UI", system-ui`; base 14px; micro-labels 11–12px, 600 weight, uppercase, letter-spacing .4–.8px; values can go big (26px/700) inside tiles.
6. **Mono** — Consolas for code/textarea/terminal-style output, 12.5px, line-height 1.5.
7. **Spacing** — 4 · 8 · 12 · 14 · 18 · 22 · 28 · 40. Sidebar 232px. Panels 18px padding, main 22/28.
8. **Motion** — `transition: all .15s ease` on interactive elements; nothing else moves.
9. **Active nav** — `box-shadow: inset 2px 0 0 var(--text)` — the inset bar, not a colored pill.
10. **Status dots** — 8px circles: green ok, amber warn, red err, `#4a525e` off.

## Component vocabulary

- **panel** — the unit of content: `background:var(--panel); border:1px solid var(--border); border-radius:12px; padding:18px`
- **tile** — stat: uppercase faint label → 26px value → faint sub
- **chip / badge** — 11px uppercase, hairline border, colored text for state (green cash / amber key / red spam)
- **btn** — transparent + hairline; `.primary` inverts to white fill, dark text
- **card grid** — `repeat(auto-fill, minmax(360px,1fr))`, gap 14, hover lifts border to `--border-strong` + bg to `.055`
- **bar** — 6px progress track `rgba(255,255,255,.08)`, fill `var(--dim)`
- **sidebar layout** — `grid-template-columns: 232px 1fr`, logo block, nav items, spacer, faint foot

## Review checklist before shipping any UI

- [ ] tokens block present, no hardcoded colors outside it
- [ ] three-step text hierarchy respected (no mid-gray soup)
- [ ] panels are alpha layers; borders are hairlines
- [ ] micro-labels uppercase + tracked; values bold
- [ ] status communicated by pastel dot/badge, not neon
- [ ] hover states exist on everything interactive (.15s ease)
