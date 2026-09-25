<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang Code Review — GH-4837 PublishingShell runtime message catalog

## Summary

Uncommitted work on `fix/issue-4837-runtime-message-catalog` wires PublishingShell **Runtime** chrome through the publishing message catalog. Labels that were raw English (Site, Publish server, Refresh, Idle, Start, Demand publish, content-id help, Queue demand, Advanced cleanup, confirm/purge prompts, last-result prefixes) now resolve via `message()` / `runtimeMessage()`, with matching **en-us** TMX units in `CmsUi.tmx`.

Machine CLI (`scratch/issue-4837-mkd-review.md`) reported **0** findings / **0** bugs on **5** files. Independent review of the **6-file** working-tree diff vs `origin/main` agrees: **no blocking bugs**. The CLI file count is short by one (product-docs markdown); that page was read and is accurate.

## Scope

- Base: `origin/main` (`68d86eeeff`)
- Head: working tree (uncommitted; `HEAD` == `origin/main`)
- Branch: `fix/issue-4837-runtime-message-catalog`
- Issue: [#4837](https://github.com/intersoftdatalabs-in/percussioncms/issues/4837) — parent #4531 slice 32
- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Pattern memory: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` and `~/.local/share/mkd/agents/erlang/PATTERNS.md`
- Files in diff (6):

| Path | Role |
|------|------|
| `WebUI/src/main/ts/i18n/message.ts` | `MSG.PUBLISH.SECTIONS.RUNTIME` keys |
| `WebUI/src/main/ts/publishing/sections/RuntimeSection.tsx` | catalog wiring + `{0}` helper |
| `WebUI/src/test/ts/publishing/runtimeEditions.test.tsx` | Vitest: substitution + catalog heading |
| `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` | en-us TMX units |
| `modules/perc-qa-automation/frontend/tests/publishing/runtimeEditions.spec.js` | Playwright: live `I18N.message` + heading |
| `product-docs/8.2/admin/publishing.md` | operator note (catalog / en-us fallback) |

## Change class

**WebUI product-screen i18n (Publishing Runtime chrome).** Issue acceptance: catalog strings; unit or Playwright assertion against a key when the catalog is loaded; **en-us only** (do not backfill pl/sv/tr); product-docs if operator-visible.

### Closure (peers + module AGENTS)

| Companion | Present |
|-----------|---------|
| SPA chrome via `message(MSG…)` (WebUI i18n rule) | yes — `RuntimeSection.tsx` |
| Key constants in `message.ts` | yes — `MSG.PUBLISH.SECTIONS.RUNTIME` |
| Canonical TMX `CmsUi.tmx` en-us `<tu>` | yes — 26 new units; Site reuses existing `perc.ui.publish.title@Site` |
| Locale matrix backfill (pl/sv/tr/…) | **intentionally omitted** — issue AC; other locales fall back to en-us (`regional → base → en-us`) |
| Vitest behavioral (catalog, not hardcoded node) | yes — stubbed `window.I18N` + `runtime-demand-heading` |
| Playwright live CMS (WebUI HARD GATE) | yes — `runtimeEditions.spec.js` evaluates `I18N.message(key)` and asserts the heading |
| `product-docs/8.2/` operator note | yes — Runtime subsection; `id: admin-publishing` unchanged |
| Dual-ship `WebUI/war/**` lockstep | N/A — TS source is canonical; bundle is generated |
| Agent rule files | none in diff |

`runtimeMessage` calls `message(key)` then `split("{0}").join(arg)`. That is the correct workaround: `tmx.jsp` `___psxReplaceMsgTokens` only interpolates when `args` is a non-empty array, and `resolveMessage` does **not** apply args on the `@`-suffix fallback. ArchitectureShell uses the same split/join pattern.

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: **yes**
- Findings to address before merge: 0
- Agent rule files in diff: none

## Machine pass verification

`scratch/issue-4837-mkd-review.md`:

- Reported: 0 findings, 0 bugs, recommend approve, may commit/push yes
- Analyzed files: **5** (CLI)
- Working-tree files vs `origin/main`: **6**

The missing file is `product-docs/8.2/admin/publishing.md` (markdown often skipped by the pack). Independent read: frontmatter `id: admin-publishing` stable; added sentence matches shipped behavior (en-us pack, other locales fall back to English). No product-docs defect.

CLI “0 bugs / may commit yes” is accepted after this pass.

## Cross-platform path / file I/O checklist

- No new filesystem path joins, OS roots, or path-string assertions
- TMX / URL / classpath paths continue to use `/` (correct)
- Line-ending sensitive assertions: none added
- **Outcome**: clean

## Issues

_No blocking issues._

### Issue 1 — Severity: nit

- File: `WebUI/src/main/ts/publishing/sections/RuntimeSection.tsx:41`
- Description: `import { buttonStyle, … }` sits **after** `const RT` and `export function runtimeMessage`. ESM hoists imports, so this compiles; it is harder to scan than the peer (`LogsSection` keeps all imports first).
- Suggestion: move the styles import up with the other imports; keep `RT` / `runtimeMessage` after the import block.
- Status: open (non-blocking)

### Issue 2 — Severity: nit

- File: `WebUI/src/test/ts/publishing/runtimeEditions.test.tsx:21-25`
- Description: Local `fallback()` is inserted between import groups and duplicates `fallbackLabelFromKey` in `message.ts`.
- Suggestion: import `fallbackLabelFromKey` from `@/i18n/message`; keep imports contiguous.
- Status: open (non-blocking)

### Issue 3 — Severity: suggestion

- File: `WebUI/src/main/ts/publishing/sections/RuntimeSection.tsx:197` and `:217`
- Description: Client-set `lastResult.status` values `"site cleared"` and `"log purged"` remain English inside the catalogued `Last result: {0}` wrapper. Slice AC listed chrome labels; job-detail chrome is out of scope. Operators on a non-en-us locale still see those two tokens in English.
- Suggestion: follow-up keys if a later slice catalogs result tokens.
- Status: accepted as-is for #4837

### Issue 4 — Severity: nit

- File: `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx:27550`
- Description: Empty-editions copy dropped the trailing period (`No editions for this site.` → `No editions for this site`). Fallback `@` text matches the new TMX.
- Suggestion: restore the period in key + `<seg>` if the previous sentence punctuation was intentional.
- Status: open (non-blocking)

## Behavioral tests

| Behaviour | Coverage |
|-----------|----------|
| `{0}` substitution from catalog key (no I18N) | `runtimeEditions.test.tsx` `runtimeMessage` |
| Demand heading from `window.I18N`, not hardcoded English | same file, `runtime-demand-heading` |
| Live en-us catalog resolves demand key (not key echo) | Playwright `runtimeEditions.spec.js` |
| Heading text equals resolved catalog string | same spec |
| Existing start/stop status still asserted | Vitest + Playwright (unchanged paths) |

Acceptance (“unit test **or** Playwright checks a key rather than a hardcoded English-only node when the catalog is loaded”) is met on **both** layers.

Playwright still uses en-us role names (`Queue demand`, `Refresh`) for buttons. That matches the en-us TMX segments and the existing start/stop test’s Refresh click. Not a gate failure for this slice.

## Security / data-loss

- No secrets, tokens, or credentials
- `window.confirm` copy moved to catalog; confirm still gates clear-site and purge
- `runtimeMessage` interpolates job id / edition id into **text** (React children / `confirm`), not HTML
- Clear-site still requires confirm; purge still requires a job id

## Intent / missing-context

- **en-us only** is required by the issue, not a missing locale-matrix companion. Module `perc-i18n/AGENTS.md` would normally back-fill via `i18n_translate.py`; parent slice 32 overrides that. Product-docs states the fallback.
- `MSG.PUBLISH_STOP` / `PUBLISH_SECTION_RUNTIME` / loading / error were already catalogued; this slice does not regress them.
- `ed.jobStatus` remains server data (out of scope: job-detail chrome, slice 29).
- Pre-existing duplicate TMX tuid `perc.ui.profile.modern@Saving…` is **not** in this diff. New #4837 tuids are unique.

## Pre-commit evidence (author)

This review does not run Maven. Author still owes standalone `cd WebUI && ../mvnw clean install` and `cd modules/perc-i18n && ../../mvnw clean install` before PR (perc-qa-automation spec-only; product-docs smoke optional). Erlang gate for **correctness of this diff** is green.

## Handoff

- Artifact: `docs/ai-generated/code-reviews/issue-4837-erlang.md`
- Product code: **not edited** (review-only)
- May commit/push: **yes**
