# Erlang review — #4045 CD-03 SPA local field create/delete

**Branch:** `feat/issue-4045-spa-local-field-create-delete`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI + Playwright + product-docs); behavioral tests; incomplete REST persist is residual not a SPA bug

## Summary

Developer Content Type detail adds/deletes **local** fields via existing REST `POST/DELETE .../fields` under a held lock. Origin is always `local`. Vitest covers unlocked no-op, duplicate 409 without dropping lock, lock-lost 409, and catalog add/delete. Playwright proves unlocked chrome + wrapped POST + duplicate 409 (field writes intercepted so H2 `saveContentTypes` cannot ALTER tables). Product-docs 8.2 admin Developer Content Types updated.

## Issues

None blocking.

### Residual (not this SPA gate)

Live H2 `POST .../fields` persists the field mapping then re-inits the CE application before the backend column exists (`no such column RXCD03…` / `PSTableMetaData`). That is REST/schema (#3960 follow-up), not the SPA client. Do not re-run live POST against sample types until schema ALTER is fixed.

## Cross-platform path checklist

N/A — no filesystem path construction.

## Tests / build

- `cd WebUI && ../mvnw.cmd clean install` BUILD SUCCESS; Tests 3260 passed
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS (error-detail on `PSErrorsException`)
- Playwright `test:surface -- --path tests/developer-content-type-local-fields.spec.js` 2 passed
