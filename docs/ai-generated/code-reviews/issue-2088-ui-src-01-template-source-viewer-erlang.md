# Erlang review — issue #2088 UI-SRC-01 template source viewer

|     Field      |                                                     Value                                                     |
|----------------|---------------------------------------------------------------------------------------------------------------|
| Branch         | `fix/issue-2088-template-source-viewer`                                                                       |
| Scope          | `WebUI` developer TemplateDetailPanel + pure helpers; Playwright companion under `modules/perc-qa-automation` |
| Recommendation | **approve**                                                                                                   |
| Gate           | **May commit/push: yes**                                                                                      |
| Date           | 2026-08-05                                                                                                    |

## Summary

Adds a lightweight template source viewer (line-number gutter, copy-to-clipboard with feedback, pure-TS token highlight + edit/preview toggle) without Monaco/CodeMirror/Prism. Behavioral Vitest coverage for helpers and panel chrome; Playwright spec landed for live CMS (run deferred / blocked on #2065).

## Scope

- New: `WebUI/src/main/ts/developer/templateSourceViewer.ts`
- New: `WebUI/src/test/ts/developer/templateSourceViewer.test.ts`
- New: `modules/perc-qa-automation/frontend/tests/developer-template-source-viewer.spec.js`
- Modified: `TemplateDetailPanel.tsx`, `messages.ts`, `TemplateDetailPanel.test.tsx`, tech-debt row UI-SRC-01

Cross-platform path review: no filesystem path joins in this change; Playwright uses existing `BASE_URL` helpers.

Memory patterns: WebUI product screen requires Vitest + Playwright companion — both present.

## Issues

None at **bug** severity.

### suggestion

1. **TMX**: New `DEV_MSG` keys use English-after-`@` fallback only; optional follow-up to add `DeveloperUi.tmx` segs for non-en locales (same pattern as other thin WebUI strings PRs).

### nit

1. Line-height sync between gutter and textarea is CSS-best-effort; acceptable for this slice.

## Tests

- Vitest: `templateSourceViewer.test.ts` (14), `TemplateDetailPanel.test.tsx` (3) — green locally.
- Playwright: `developer-template-source-viewer.spec.js` — code only; live blocked on CMS/H2 qa-up.

