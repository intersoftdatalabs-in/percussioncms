# #6655 — Spike: can nav hoverIntent be replaced by an npm-supported lib or custom app code

**Date**: 2026-11-19
**Type**: Research spike (recommendation only — no payload change landed)
**Issue**: [#6655](https://github.com/intersoftdatalabs-in/percussioncms/issues/6655)
**Parent**: #6522 (remainder slice of #6502 under #6403, parent audit #5627)
**Follow-on**: [#6689](https://github.com/intersoftdatalabs-in/percussioncms/issues/6689)
**Labels**: p3

## TL;DR (recommendation)

**Replace with custom app code (Option B), not npm.** There is no
maintained jQuery-compatible `hoverIntent` on npm — `hoverintent@2.2.1`
is the incompatible vanilla-JS rewrite and `jquery.hoverintent` does not
exist (registry 404, verified during this spike). Wiring the vanilla lib
would still require a hand-written `$.fn` adapter, i.e. custom code PLUS
an extra third-party dependency. The recommended path is a small
first-party `$.fn.hoverIntent`-compatible shim (~40 lines,
interval/sensitivity/timeout contract) that lets superfish 1.7.10 and
`navbar.js` stay untouched, so the two vendored files
(`hoverIntent.js` 4.2 KB + `hoverIntent.min.js` 1.1 KB) can be deleted.
Effort: **S (half-day)**. Follow-on: #6689. Until it lands, **keep the
pair vendored** — deleting without a replacement silently changes menu
timing (see B1 trade below).

---

## 1. Current usage (inventory)

| Item | Value |
|------|-------|
| Vendored files | `perc.widgets.nav/.../navBar/js/hoverIntent.js` (classic briancherne jQuery plugin, `$.fn.hoverIntent`, interval/sensitivity/timeout) + `.min.js` twin |
| Consumer | `navBar/js/superfish.js` (1.7.10, already npm-tracked via `superfish: ^1.7.10` + byte-identity parity test) — `applyHandlers`: `if ($.fn.hoverIntent && !o.disableHI) { $menu.hoverIntent(over, out, targets); } else { mouseenter/mouseleave fallback }` |
| Init | `navBar/js/navbar.js` — `$("ul.sf-menu").superfish({ pathClass: "current", autoArrows: false, delay: 400, pathLevels: 0 })` (no `disableHI`, so hoverIntent is live today) |
| Payload wiring | `percNavBar.xml` preview (`hoverIntent.js`) + publish (`hoverIntent.min.js`) file ids with `percSystem.jquery` dep; `psx_archiveInfo.xml` + `psx_archiveManifest.xml` entries |
| Snippet | `percNavBar/templates/percNavBarSnippet.vm` (skip-link + `<nav aria-label>` + `ul.sf-menu`) |

Slice-7 already dropped the dead `supersubs` pair (zero callers); hoverIntent
is the remaining live 3rd-party script in the nav payload.

## 2. Options assessed

### Option A — npm-supported hover-intent equivalent: NOT VIABLE

- `hoverintent@2.2.1` ("Fire mouse events when the user intends it",
  single `index.js`, zero dependencies) is the vanilla rewrite: API is
  `hoverintent(element, onOver, onOut)`. It registers no `$.fn.hoverIntent`,
  so superfish's `$.fn.hoverIntent && !o.disableHI` check ignores it and
  the menu silently degrades to plain mouseenter/leave — the very timing
  change this spike must avoid.
- No jQuery-compatible maintained package exists (`jquery.hoverintent`
  → registry 404). The upstream jQuery plugin repo is archived.
- An adapter shim (`$.fn.hoverIntent` backed by the vanilla lib) is custom
  code anyway, plus a new third-party dependency to track — worst of both
  worlds. Rejected.

### Option B — first-party replacement: RECOMMENDED

Write `perc-hover-intent.js` (+ minified publish twin) implementing
`$.fn.hoverIntent(handlerIn, handlerOut, selector)` with the same
interval (100 ms) / sensitivity (7 px) / timeout contract superfish calls
today. Superfish and `navbar.js` stay byte-identical; only the payload
references (`percNavBar.xml` preview + publish ids, archive info/manifest)
are renamed to the shim, and the two vendored files are deleted.

Why this preserves behavior exactly:

- `over` (open) still fires only after the pointer slows below the
  sensitivity threshold; fast diagonal pass-through still does not open
  submenus. `out` (close) still honors superfish's own `delay: 400`.
- Keyboard (`focusin`/`focusout`), touch (`touchHandler`), and the
  skip-link in the snippet are bound independently of hoverIntent in
  superfish — untouched by construction.
- Failure mode is safe: if the shim ever fails to load, superfish's
  `$.fn.hoverIntent &&` guard falls back to plain mouseenter/leave
  (the path the existing jsdom parity test already validates with
  `disableHI: true`).

### Fallback B1 — `disableHI: true`, drop with no replacement

One-line `navbar.js` change + delete the pair. Cost: submenus open
immediately on `mouseenter` (close delay retained). Acceptable but a real
traversal-flicker regression for mouse users; keep as the fallback if the
shim's parity proof fails (record the call on #6655). Keyboard/touch
unaffected either way.

## 3. UX / a11y lens (what must be preserved)

hoverIntent exists for mouse-traversal ergonomics, NOT for keyboard access —
that distinction bounds the risk:

1. **Keyboard/focus parity** — `focusin`/`focusout` bindings are separate
   from the hoverIntent branch; any replacement must keep Tab/Shift-Tab
   open/close identical (parity test asserts this).
2. **Touch parity** — `touchHandler` (tap-to-open, `MSPointerDown`/
   `pointerdown`) is separate; unchanged.
3. **Mouse timing parity** — slow-hover opens, fast pass-through does not,
   close after 400 ms idle. This is the only behavior the shim must
   reproduce (jsdom synthetic-mousemove comparison old-vs-new).
4. **No-JS / no-shim safety** — the `$.fn.hoverIntent &&` guard means a
   missing shim degrades to immediate open, never to a dead menu.

## 4. Effort estimate (follow-on #6689)

| Step | Cost |
|------|------|
| Shim (~40 lines) + min twin | 1–2 h |
| jsdom old-vs-new timing parity test (extends `widgetPayloadNpmParity.test.js` nav block) | 1–2 h |
| Payload ref swaps + file deletes + guard-test header update | < 1 h |
| Published-widget smoke (preview/published page, zero console errors) | < 1 h |
| **Total** | **Half-day (S)** |

No Maven module changes (payload resources only); no QA-cell run required
for this decision record — the follow-on owns the jsdom proof, which runs
in the frontend Vitest gate, not the cell.

## 5. Decision

- **npm: no.** Incompatible rewrite only; adapter negates the benefit.
- **Custom shim: yes** — #6689 tracks implementation.
- **Until #6689 lands:** keep the pair vendored; do NOT set `disableHI`
  as a drive-by (timing regression without a recorded call).
