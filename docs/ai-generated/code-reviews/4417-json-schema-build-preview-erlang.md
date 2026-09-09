# Erlang review — #4417 json-schema Build and Preview

**Scope:** `feat/issue-4417-json-schema-build-preview` vs `origin/main`  
**Reviewer:** Erlang (Grok Build night-issue-prs self-review)  
**Date:** 2026-09-09

## Summary

Vertical increment for Virtual Site `sourceKind=json-schema`: REST Build/Preview, Developer Sites Build + Preview chrome (Publish stays hidden), adaptor tests, Playwright C5, product-docs 8.2. Persist PUT/GET from #4416/#4423 is consumed, not reimplemented. SPI assemble already exists on main.

## Scope

- `rest` OpenAPI + resource tests + `ISiteAdaptor` / DTO comments
- `projects/sitemanage` adaptor comments + `SitesAdaptorTest` Build/Preview (no Publish)
- `WebUI` chrome helpers, hints, Vitest
- `modules/perc-qa-automation` fixture + live Build/Preview specs
- `product-docs/8.2` REST / admin Sites / developer virtual-sites / site-config
- Cross-platform path review: fixture deploy uses in-container POSIX `/opt/Percussion/tmp/...` (Linux QA cell only). Java tests use NIO `Path`/`Files`. Playwright `C:/json-schema-docs` is a portable-safe *logical* persist path (same as persist slice), not a host filesystem join.

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover `pagesWritten > 0`, leftover remoteUrl/credentials/cloud rootPath/`jsonschema.url`/remote `$ref` 400, missing assemble fail-closed (`available=false` HTTP 200 / file 404). Publish chrome remains false.

## Issues

None.

## Prior report / Memory patterns

Peer graphql-sdl Build/Preview (#4402/#4406): copy companion set, keep Publish out of this slice.
