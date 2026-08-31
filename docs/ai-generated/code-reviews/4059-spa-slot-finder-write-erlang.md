# Erlang review: #4059 AS-01 SPA slot finder/relationship write

- **Branch:** `fix/issue-4059-spa-slot-finder-write`
- **Base:** `origin/main`
- **Date:** 2026-08-31
- **Reviewer:** Erlang (pre-commit, independent of implementer)
- **Recommendation:** approve
- **May commit/push:** yes
- **Gate:** approve
- **Memory patterns hit:** change-class closure (WebUI Vitest + Playwright + product-docs); behavioral tests for omit/clear/409/400; no path I/O

## Summary

Developer Slot detail can lock, then edit `finderName` / `relationshipName` / `finderArguments`. Unchanged finder fields are omitted on PUT so a properties-only save does not wipe catalog finder values. Empty relationship sends `""` (clear). Invalid finder / unlocked finder write surface 400 / 409. REST is not reimplemented.

## Scope

Uncommitted WebUI SPA + Vitest, perc-qa-automation Playwright, product-docs 8.2 admin Developer Slots and REST note.

## Cross-platform path checklist

N/A — no new filesystem path construction. Playwright uses URL paths with `/`.

## Issues

None blocking.

### Suggestion (non-blocking)

H2 GET after finder-arguments PUT may still omit custom argument maps (Jackson/JAXB map bind). SPA still sends args on the wire (Playwright asserts PUT body). REST bind is out of this slice.

## Tests / evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Vitest Tests 3522 passed; Surefire Failures 0
- Playwright `npm run test:surface -- --path tests/developer-slot-finder-editor.spec.js` — 1 passed; console-clean=yes; server.log-clean=yes (login/audit INFO only)
