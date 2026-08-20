# Erlang review — #3654 Explorer rxFolderMutations flag-on folder ops no-skip

**Branch:** `fix/issue-3654-explorer-rx-folder-mutations-noskip`  
**Date:** 2026-08-20  
**Scope:** uncommitted vs `HEAD` (issue #3654 / parent #3102)  
**Memory patterns hit:** change-class closure (WebUI + Playwright + product-docs); Jackson WRAP_ROOT_VALUE envelopes; Playwright no-skip when façade HTTP is 200; dual-run flag default off; cross-platform path checklist (URL `/` only)

## Summary

Diagnostic Explorer dual-run (`spa.jsp?entry=explorer&rxFolderMutations=1`) still dropped the query on spa.jsp → path rewrite, so Create/Rename/Delete stayed on pathmanagement. The handoff now persists `rxFolderMutations` on the client path and in sessionStorage. RX GET unwraps `RxFolder` WRAP_ROOT_VALUE; PUT by-id wraps `{ RxFolder: { name } }` so JAXB no longer 400s. Playwright `explorer-rx-folder-mutations.spec.js` hard-fails on 404/503 and on pathmanagement posts. Product default stays flag **off**. Tree-node eviction after RX delete is left to #3652/#3653.

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path I/O in this diff.

## Issues

None.

## Cross-platform path checklist

- No new filesystem path joins
- REST/SPA URLs use `/` — allowed
- Playwright helpers encode RX path segments without OS separators
- No Unix-only roots or Windows-only drive letters

## Companions (change class: WebUI product screen + diagnostic flag)

| Companion | Status |
|-----------|--------|
| Flag survives spa.jsp rewrite (`App.applyEntryQueryToPath` + sessionStorage) | done |
| RxFolder unwrap + PUT wrap | done |
| Vitest (`folderMutations`, `rxFolderApi`, `rxFolderMutationsFlag`, `parseEntryQuery`) | done |
| Playwright `explorer-rx-folder-mutations.spec.js` no-skip | done |
| Helper unit tests + `package.json` `test:unit` | done |
| `product-docs/8.2/admin/content-explorer.md` + `developer/rest.md` diagnostic note | done |

## Test evidence

- `WebUI` `../mvnw.cmd clean install` — BUILD SUCCESS (Vitest 2952 passed)
- `modules/perc-qa-automation` `../../mvnw.cmd clean install` — BUILD SUCCESS
- `node --test tests/unit/explorer-rx-folder-mutations.test.js` — 8 passed
- C5: `perc-devctl qa-up --skip-image-build` TEST_CMS_URL=http://127.0.0.1:9993; hot-copy `cm/modern/assets`; Playwright `explorer-rx-folder-mutations.spec.js` 4 passed 0 skipped; golden 2 passed; console-clean=yes; server.log-clean=yes
