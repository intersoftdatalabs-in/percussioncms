# Erlang review — issue #5498 (locale description)

Persona: erlang 0.1.1
Pack: percussion
Gate: advisory (strict: in-diff bugs, missing behavioral tests, non-portable paths)
Base: origin/main
Head: 56baa800d1329e58e783e52e0f6a60833bdf2930

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:320 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=15 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/localeDescription.ts:69 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `formatFingerprint` cognitive=16 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent / companions

Slice 105 is a description-only save on the existing locale PUT. Language
string, label, enabled status, and format stay. Blank description clears.
Cancel and HTTP 400/403/409 are not success.

Companions in this diff:

- Helper + Vitest (`localeDescription.ts` / `.test.ts`)
- Panel chrome + panel tests (`LocaleDetailPanel`)
- Catalog description attribute (`LocalesPanel`)
- H2 Playwright `developer-locale-description.spec.js` (peer of `developer-locale-editor.spec.js`)
- Product-docs note on `product-docs/8.2/admin/developer-locales.md` (not an index)

No Java API shape change. No new path/file I/O. No banned thrash paths.
`handleSave` complexity is the existing locale save plus descriptionShown
reset. `formatFingerprint` is a field join used to refuse a PUT response that
changed the stored format. Neither is a behavioral bug.

Playwright on H2 QA: 1 passed. Console-clean and server.log-clean for the
exercised path.

## Verdict

Gate: PASS. Recommendation: approve. May commit/push: yes.
