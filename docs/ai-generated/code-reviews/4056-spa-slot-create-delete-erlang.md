# Erlang review — #4056 AS-01 SPA slot create/delete

- Branch: `feat/issue-4056-spa-slot-create-delete`
- Date: 2026-08-31
- Reviewer: Erlang (independent of implementer)
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: change-class closure (WebUI + Playwright + product-docs); behavioral tests for 400/403/409; no file I/O / path work

## Scope

Uncommitted WebUI Developer Slots catalog create/delete, assemblyApi POST/DELETE clients, perc-qa-automation Playwright surface spec, product-docs 8.2 Developer Slots. REST POST/DELETE already on main (#4006). No Java REST/sitemanage changes.

## Summary

SPA catalog matches Locales peer chrome: New slot → create form (name required, no spaces/wildcards; optional label/description/`REGULAR|INLINE`) → POST `/services/slots`; detail Delete → DELETE `/services/slots/{idOrName}` with confirm. System-slot 409 is surfaced. Finder/relationship write is not added.

## Cross-platform path checklist

N/A — no filesystem path construction. REST URLs use `/` (correct for URIs). Playwright unique names are alphanumeric.

## Issues

None (hard gate).

## Tests

- Vitest: invalid name (client disable + API 400), invalid slotType 400, duplicate 409, non-Admin 403, system-slot delete 409, successful create/delete, double-submit guard.
- Playwright: `developer-slot-editor.spec.js` (create+delete, duplicate 409, system 409).
- Product-docs: `product-docs/8.2/admin/developer-slots.md` + REST/index links.

## Module builds

- `WebUI`: `mvnw clean install` BUILD SUCCESS, Tests run: 3272, Failures: 0
- `modules/perc-qa-automation`: `mvnw clean install` BUILD SUCCESS; `npm run test:unit` 482 passed
