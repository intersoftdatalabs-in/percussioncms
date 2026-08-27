# Erlang review — issue #3896 Developer CT field-rule expressions chrome (CD-05-07)

**Branch:** `feat/issue-3896-ct-field-rule-expressions`  
**Date:** 2026-08-27  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (WebUI chrome + Vitest + Playwright + product-docs); live CXF consume needs MessageBodyReader ahead of jacksonProvider (ViewExecuteRequest / item-exits peers); dedicated section file to reduce merge conflict with open PRs on ContentTypeDetailPanel.

## Summary

Developer Content Type detail chrome for field-rule **expression text** (validation / visibility / input / output translation). Consumes existing GET/PUT `.../fields/{fieldName}/ruleExpressions` (#3784). Lock-gated editors; main **Save content type** PUTs dirty fields; 409 does not steal the lock. Live CXF JAXB rejected a flat `fieldName` body; a JsonReader binds wrap + flat (same pattern as ViewExecuteRequestJsonReader).

## Scope

- Base: `origin/main`
- Files: WebUI field-rule API/helpers + `ContentTypeFieldRulesSection` + minimal panel/messages wiring; rest JsonReader + test; sitemanage jaxrs provider ref; Playwright surface spec; product-docs 8.2 admin + REST
- Peer: item-exits #3901 / control properties #3903 (do not steal); REST PUT #3784
- Prior report: `issue-3784-erlang.md` (REST only; chrome was out of scope)
- Memory patterns hit: CXF consume wrap/flat; Playwright C5 H2 QA; dedicated section vs mega-panel

## Recommendation

approve

## Gate

- Blocking bugs: 0
- Missing behavioral tests: no
- Non-portable path/file I/O: no
- May commit/push: yes

## Issues

None blocking.

PUT persist uses held design lock and does not unlock. Unlocked textareas are disabled. 409 on field-rule PUT clears the held lock via `onLockLost`. Expression text is one line per rule (`variable operator value`, `ext:FQN`, `ref:name`); not a Workbench visual builder (documented).

Change-class closure: WebUI API + section + panel hook + Vitest (unwrap/parse/PUT wrap + lock/save/409) + Playwright surface + product-docs. JsonReader + beans.xml so live CXF binds the existing PUT (not a re-implementation of save semantics).

`ContentTypeDetailPanel` changes are small (ref + dirty + one save call + mount) so peer PRs #3901/#3903 can rebase.

## Cross-platform path checklist

Applied. No filesystem path construction. REST `/services/contenttypes/.../ruleExpressions` strings are URI paths (correct `/`). Playwright uses `path` from Node only in existing helpers.

## Tests

- WebUI Vitest: field-rule unwrap/parse/PUT wrap; panel unlocked/save/409; DeveloperShell catalog/detail
- rest: JsonReader wrap + flat + camelCase root
- Playwright: `npm run test:surface -- --path tests/developer-content-type-field-rules.spec.js` — 2 passed on H2 QA
- Module suites: `rest` Tests run: 648, Failures: 0; `sitemanage` Tests run: 1628, Failures: 0, Skipped: 125; `WebUI` BUILD SUCCESS (Vitest 3154 passed)

## Non-blocking notes

- Full Workbench rule-builder parity remains out of scope (expression text only).
- Control property values chrome remains a sibling (#3903).
- C5: console-clean=yes; server.log-clean=yes (no ERROR/FATAL in test window).
