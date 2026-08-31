# Erlang review — issue #4036 CD-04 SPA include system/shared field picker

**Branch:** `feat/issue-4036-spa-include-system-shared-fields`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** WebUI screen requires Playwright companion; product-docs companion; WRAP_ROOT POST body; behavioral tests for 409/404 and origin stay; i18n English-after-@ keys

## Summary

Developer Content Type detail gains an include picker that POSTs existing REST `…/fields/include` under a held design-session lock. Origin stays `system`/`shared`. Duplicate include 409 keeps the lock; lock-conflict 409 clears local lock; unknown catalog field 400/404 keeps the lock. Local field create/delete is not added. REST is not re-implemented.

## Scope

Uncommitted WebUI SPA + Vitest, perc-qa-automation Playwright spec, product-docs 8.2 admin/REST. C5 Playwright required (UI). Cross-platform path I/O: none (REST URLs use `/`).

## Issues

None that block.

## Companions

| Companion | Status |
|-----------|--------|
| `includeContentTypeField` WRAP_ROOT client | yes |
| Content Type detail picker chrome | yes |
| Vitest API wrap + panel 409/404/origin | yes |
| Playwright H2 surface spec | yes |
| product-docs 8.2 admin + REST | yes |
| i18n `DEV_MSG` keys | yes |
| REST re-implementation | out of scope |

## C2

No Java public type `final`/`sealed` or signature change. `downstream_checked=none`.

## Build

- `WebUI`: standalone `..\mvnw.cmd clean install` BUILD SUCCESS; Vitest Tests 3264 passed
- `modules/perc-qa-automation`: standalone `..\..\mvnw.cmd clean install` BUILD SUCCESS (no Java tests)

## Cross-platform path checklist

- No new filesystem path joins
- REST/URL paths correctly use `/`
- Playwright uses `path` + `URLSearchParams`, not OS file joins
- N/A for installers/packaging
