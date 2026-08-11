# Erlang review — issue #2901 UI-SRC-02 binding expression show-more

|     Field      |                                                     Value                                                     |
|----------------|---------------------------------------------------------------------------------------------------------------|
| Branch         | `fix/issue-2901-binding-expression-show-more`                                                                |
| Scope          | `WebUI` developer TemplateDetailPanel + pure helpers; Playwright companion under `modules/perc-qa-automation` |
| Recommendation | **approve**                                                                                                   |
| Gate           | **May commit/push: yes**                                                                                      |
| Date           | 2026-08-11                                                                                                    |

## Summary

Adds long binding **expression** expand/collapse on Template detail bindings (UI-SRC-02): maxWidth clamp (320px) retained, collapsed maxHeight for long JEXL, Show more / Show less control with `aria-expanded`. Pure helpers in `bindingExpressionPreview.ts`; Vitest for helpers + panel toggle; Playwright surface companion for live CMS.

## Scope

- New: `WebUI/src/main/ts/developer/bindingExpressionPreview.ts`
- New: `WebUI/src/test/ts/developer/bindingExpressionPreview.test.ts`
- New: `modules/perc-qa-automation/frontend/tests/developer-template-binding-expression.spec.js`
- Modified: `TemplateDetailPanel.tsx`, `messages.ts`, `TemplateDetailPanel.test.tsx`, tech-debt row UI-SRC-02

Cross-platform path review: no filesystem path joins; Playwright reuses existing `BASE_URL` / catalog helpers.

Change-class companions (peer UI-SRC-01): pure helpers, Vitest, Playwright, messages keys, tech-debt Done status — all present.

## Issues

None at **bug** severity.

### suggestion

1. **TMX**: New `DEV_MSG` keys use English-after-`@` fallback only; optional follow-up to seed `DeveloperUi.tmx` for non-en locales (same as UI-SRC-01).
2. **Design SPA**: `TemplateSourceEditor` still uses plain expression inputs without show-more; out of scope for #2901 / TemplateDetailPanel slice.

### nit

1. Expanded-row state is index-based; reordering bindings via order inputs does not renumber expand state (acceptable; users expand after order edits as needed).

## Tests

- Vitest: `bindingExpressionPreview.test.ts` (10), `TemplateDetailPanel.test.tsx` (8) — green.
- Playwright: `developer-template-binding-expression.spec.js` — code landed; live run when QA CMS available (`perc-devctl qa-up` + surface filter).

## Product documentation

N/A — developer-module polish (same class as UI-SRC-01); no new operator procedure beyond existing Developer template detail docs.
