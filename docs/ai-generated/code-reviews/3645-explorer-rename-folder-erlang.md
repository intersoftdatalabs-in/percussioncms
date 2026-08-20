# Erlang review — #3645 Explorer rename selected folder (product route)

**Branch:** `fix/issue-3645-explorer-rename-folder`  
**Base:** `origin/main`  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent of implementer)

## Summary

Product-route Rename on `spa.jsp?entry=explorer` with `rxFolderMutations` **off** posted a bare `{path, newName}` body. Sitemanage `PSRenameFolderItem` is `@XmlRootElement(name = "RenameFolderItem")` with field `name`. The SPA now wraps `{ RenameFolderItem: { path, name } }` (trailing slash on path, matching other pathmanagement envelopes). After a successful rename, `ContentExplorerShell` bumps `listEpoch` so the detail list remounts and `ExplorerTree` reloads loaded children.

Playwright on H2 QA (`perc-devctl qa-up` → `qa-health` → surface spec) **2 passed, 0 skipped**. Golden smoke **2 passed**. Gap-matrix not flipped to Present. Cluster #3644 (Create Folder wrap/tree epoch) is overlapped on `pathApi.renameFolder` and `ExplorerTree.childrenEpoch` by design — same wrap, additive `onRename` refresh.

Memory patterns hit: JAXB WRAP_ROOT_VALUE envelopes; Playwright no-skip when H2 demo parent exists; WebUI screen companion; no Unix-only filesystem joins (URL `/` only).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (hard-gate).

Cross-platform path checklist: N/A for filesystem I/O. URL and REST path helpers use `/`. Unique folder names are ASCII. Playwright BASE_URL join does not hardcode OS separators.

## Tests / evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS. Surefire Tests run: 63, Failures: 0. Vitest Tests 2898 passed (383 files).
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 0 (Playwright is npm).
- `npm run test:unit` helper suite includes `explorer-rename-folder.test.js` — 337 passed.
- C5: `qa-up` TEST_CMS_URL=`http://127.0.0.1:9993`; hot-copy `WebUI/target/generated-webui/cm/modern` into `perc-matrix-cms-h2` WAR; Jetty Stop/Start inside cell; `qa-health` RESULT:OK HEALTH:healthy.
- `npm run test:surface -- --path tests/explorer-rename-folder.spec.js` — 2 passed, 0 skipped. console-clean=yes. server.log-clean=yes (no ERROR/FATAL).
- `npm run test:golden` — 2 passed.

downstream_checked: none (no Java `final`/`sealed` or public API signature change).
