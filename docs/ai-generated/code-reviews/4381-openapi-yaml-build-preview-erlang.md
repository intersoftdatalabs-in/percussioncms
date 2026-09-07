# Erlang review — issue #4381 openapi-yaml Build and Preview

Date: 2026-09-07
Branch: feat/issue-4381-openapi-yaml-build-preview
Scope: uncommitted vs origin/main (REST + sitemanage + WebUI + perc-qa-automation + product-docs/8.2)

## Summary

Vertical slice enables REST `POST …/virtual/build` and `GET …/virtual/preview` plus Developer Sites Build/Preview chrome for `sourceKind=openapi-yaml` from a local OpenAPI 3 YAML fixture. Publish chrome stays hidden (sibling #4382). Persist PUT/GET is consumed from #4380 / PR #4386 (not re-implemented). Peer pattern: llms-txt #4374 / PR #4378.

Production build already used `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds including `OPENAPI_YAML`. This slice allow-lists chrome, documents OpenAPI, and proves local-fixture Build (`pagesWritten > 0`) plus last-build Preview (`available=true`; missing build `available=false` HTTP 200). Leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` stay 400.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

- New adaptor tests use `Path` / `Files` and do not assert Unix-only `Path.toString()` shapes.
- QA fixture helper uses `path.join` on the host; in-container POSIX root `/opt/Percussion/tmp/openapi-yaml-virtual-qa` is Linux-cell only (same as llms-txt peer).
- No new `"/" +` filesystem joins in Java production code.

## Issues

None blocking.

## Memory patterns hit

- Change-class companions: REST OpenAPI + resource tests, sitemanage adaptor tests, Spring `SitesTestAdaptor` comment, WebUI chrome + Vitest, Playwright + fixture helper, product-docs 8.2.
- Leftover `virtual.remoteUrl` / credentials / cloud `rootPath` fail closed (400).
- Missing last-build Preview is `available=false` HTTP 200, not 500.
- Sole HTML home fallback for kinds that do not emit `index.html` (`8.2/listPets-1.html`).
- Publish chrome remains hidden (sibling #4382).

## Tests

- rest `SitesResourceTest` 126 tests, module Tests run: 1266 Failures: 0
- sitemanage `SitesAdaptorTest` 243 tests, module Tests run: 2457 Failures: 0 (125 skipped baseline)
- WebUI Surefire Tests run: 69 Failures: 0; Vitest included in `mvnw clean install` (exit 0)
