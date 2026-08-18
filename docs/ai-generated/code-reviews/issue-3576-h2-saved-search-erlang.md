# Erlang review — #3576 H2 saved-search picker/run

**Scope:** uncommitted vs `HEAD` on `fix/issue-3576-h2-saved-search-picker-run`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral unit tests; Playwright companion for WebUI; no path I/O

## Summary

Slice proves Explorer SearchPanel picker + Run on H2 QA and stops Playwright from
soft-skipping when a catalog search exists. Nested Jackson `SearchDefList.SearchDef`
unwrap is shared by WebUI `searchesApi` and the Playwright helper. Custom-URL
rows stay non-executable (Run disabled). SearchPanel chrome was not rebuilt.

## Issues

None (hard gate).

### Nits

- `unwrapSearchDefList` / `unwrapSearchDefs` are duplicated in TS and JS. Acceptable:
  Playwright helpers cannot import the Vite TS client. Keep both tested.

## Cross-platform path checklist

N/A — no filesystem path/file I/O. URLs correctly use `/`.

## Evidence

- WebUI `mvnw clean install`: BUILD SUCCESS; Surefire Tests run: 61; Vitest 2872 passed
- perc-qa-automation `mvnw clean install`: BUILD SUCCESS
- Helper unit: 14 passed
- Playwright `test:surface --path tests/explorer-saved-search.spec.js`: 4 passed, 0 skipped
  on `TEST_CMS_URL=http://127.0.0.1:9993` (H2 QA cell healthy)
- console-clean=yes (pageerror/console error collector)
- server.log-clean=no BUG:#3592 (FastForward import + search-index modifier noise;
  not saved-search execute)
