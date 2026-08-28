# Erlang review — #3948 H2 QA virtual-source SPA kinds

**Scope:** uncommitted vs `HEAD` on `fix/issue-3948-qa-webui-virtual-source-kinds` (vs `origin/main`).
**Reviewer:** Erlang (independent of implementer).
**Date:** 2026-08-28

## Summary

Cycle Verify #3948 failed Playwright `tests/developer-site-virtual-source.spec.js` because a skip-image-build H2 cell received only rest/sitemanage JARs. The live SPA `developer-site-virtual-source-kind` select was `[repository, git-filesystem, csv-filesystem, sql-database, http-json]` (no `object-storage` / `rss-atom`). origin/main SPA source already lists both kinds (`VirtualSiteSourcePanel` / `SOURCE_KIND_SELECT_VALUES`).

This change hardens `hot-deploy-webui-modern.py` to require quoted `object-storage` **and** `rss-atom` in the live entry’s developer chunk, accepts Vite 8/rolldown template-literal backticks (production minification), and wires `qa-rebuild-chain --then-qa-up` plus `--then-qa-deploy-webui` so H2 QA gets `WebUI/target/generated-webui/cm/modern` after jar/rebuild paths.

Memory patterns hit: missing behavioral tests; non-portable path joins (not present — host paths use `Path`, container dest is POSIX); incomplete change-class (deploy script + perc-devctl + tests + operator docs).

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New path logic uses `Path` / `relative_to().as_posix()` / `container_dest_file`
- [x] Container dest remains absolute POSIX (`/opt/Percussion/...`)
- [x] Tests use `tempfile` + `Path`; no Unix-only absolute assertions
- [x] `subprocess.run(..., shell=False)`

## Issues

None that block.

Nit: `--skip-object-storage-check` remains the dest/flag name for skipping **all** kind markers (including rss-atom). Alias `--skip-kind-marker-check` is documented; keep the old flag so existing scripts do not break.

## Tests

- `python docker/scripts/test_hot_deploy_webui_modern.py` — 21 tests, OK
- `python docker/scripts/test_perc_devctl.py` — 72 tests, OK
- C5: `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js` — 34 passed against `TEST_CMS_URL=http://127.0.0.1:9993` after full `qa-deploy-webui` + in-cell Jetty restart (and rest/sitemanage/perc-system SNAPSHOT copies for live save/build)

## Product documentation

N/A — operator H2 QA deploy toolchain (`perc-devctl` / docker scripts), not a CMS product-docs surface. Engineering notes: `docker/README.md`, `docs/developer-module/workbench-rest-and-qa-modes.md`, `modules/perc-qa-automation/README.md`.
