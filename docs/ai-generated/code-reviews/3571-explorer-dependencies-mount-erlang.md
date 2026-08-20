# Erlang review — #3571 Explorer Dependencies mount

**Branch:** `fix/issue-3571-explorer-dependencies-mount`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-08-19  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** missing behavioral tests (covered); incomplete change-class closure (WebUI screen → Playwright + product-docs companions present); CMS `/` URL paths are not OS file I/O (false-positive guard)

## Summary

Human QA on #2776 failed because View → Dependencies stayed on `explorer-dependencies-hint` after selecting a content row. `hasDependencyItem` treated any non-empty `selection.item.id` as eligible while Relationships already required `parseExplorerContentId` (numeric or GUID last-segment). Playwright accepted hint **or** viewer, so H2 QA could green without mounting.

This slice:

- Aligns `hasDependencyItem` with `hasRelationshipItem` (`parseExplorerContentId`)
- Passes the parsed content id into `DependencyViewer` (and parses GUID inside the viewer)
- Adds Vitest for GUID row, omitted-id path lookup (`theme.css`), and folder hint
- Rewrites Playwright so a Sites content row **must** mount `explorer-dependencies-panel` / `dependency-viewer`
- Updates `product-docs/8.2/admin/content-explorer.md` operator steps

No Java API shape changes. No non-portable filesystem path joins.

## Issues

None blocking.

### nit — Playwright console-clean listeners attach after `beforeEach` navigation

`attachConsoleCleanGate` in the hint test runs after login/goto. Uncaught errors during the initial Explorer load are not asserted. The mount test navigates again after attach. Acceptable for this slice; pageerror on the mount path is still gated.

### nit — `String(parseExplorerContentId(...) ?? selection.item!.id)` fallback is dead

The panel only mounts when `hasDependencyItem` is true, so parse already succeeded. Harmless and matches the Relationships panel shape.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Shell eligibility + parsed id | `ContentExplorerShell.tsx` |
| Viewer GUID parse | `DependencyViewer.tsx` + unit test |
| Vitest selected-row mount | GUID, omitted id / path lookup, folder hint |
| Playwright surface | `explorer-dependencies.spec.js` — no hint-or-panel skip when a content row exists |
| Product docs | `product-docs/8.2/admin/content-explorer.md` |
| QA README | `modules/perc-qa-automation/README.md` |

## Cross-platform path checklist

- No new `"/" +` / `"\\" +` filesystem construction
- CMS Explorer paths and REST URLs correctly use `/`
- Playwright `paginatedFolder` URL is HTTP, not OS I/O
- N/A for installers / temp files

## Tests / build (pre-PR)

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Vitest 2896 passed; Surefire Java 63 (17+7+15+11+6+7)
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS (no Java tests)

C5 Playwright (2026-08-19):

- `perc-devctl qa-up --skip-image-build` → cell `perc-matrix-cms-h2` HEALTH:healthy HTTP:200; `qa-health` RESULT:FAIL `DETAIL:server_log_errors` FastForward `PSDbStorageService Could not import item` (BUG:#3592, not `Failed startup of context`)
- Hot-copy `WebUI/target/generated-webui/cm/modern/assets/` → `/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/`
- `npm run test:surface -- --path tests/explorer-dependencies.spec.js` — 2 passed
- `npm run test:golden` — 2 passed
- console-clean=yes (pageerror empty; network `Failed to load resource` filtered)
- server.log-clean=yes for this feature (BUG:#3592 FastForward import + search-index at cell start; thin H2 relationship 403/auth viewer is in-scope)
