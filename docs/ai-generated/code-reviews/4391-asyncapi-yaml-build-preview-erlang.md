# Erlang review — issue #4391 asyncapi-yaml Build and Preview

Date: 2026-09-07
Branch: feat/issue-4391-asyncapi-yaml-build-preview
Scope: uncommitted vs origin/main (REST + sitemanage + WebUI + perc-qa-automation + product-docs/8.2)

## Summary

Vertical slice enables REST `POST …/virtual/build` and `GET …/virtual/preview` plus Developer Sites Build/Preview chrome for `sourceKind=asyncapi-yaml` from a local AsyncAPI 2/3 YAML fixture. Publish chrome stays hidden (later Publish slice). Persist PUT/GET is consumed from #4390 / PR #4397 (not re-implemented). Peer pattern: openapi-yaml #4381 / PR #4387.

Production build already used `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds including `ASYNCAPI_YAML`. This slice documents REST Build/Preview, shows chrome, and proves local-fixture Build (`pagesWritten > 0`) plus last-build Preview (`available=true`; missing build `available=false` HTTP 200). Leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` stay 400.

## Recommendation

approve

## Gate

May commit/push: yes (after C1 module clean install + C5 Playwright)

## Cross-platform path checklist

- New adaptor tests use `Path` / `Files` and do not assert Unix-only `Path.toString()` shapes.
- QA fixture helper uses `path.join` on the host; in-container POSIX root `/opt/Percussion/tmp/asyncapi-yaml-virtual-qa` is Linux-cell only (same as openapi-yaml peer).
- No new `"/" +` filesystem joins in Java production code.

## Issues

None blocking.

## Memory patterns hit

- Change-class companions: REST OpenAPI + resource tests, sitemanage adaptor tests, Spring `SitesTestAdaptor` comment, WebUI chrome + Vitest, Playwright + fixture helper, product-docs 8.2.
- Leftover `virtual.remoteUrl` / credentials / cloud `rootPath` fail closed (400).
- Missing last-build Preview is `available=false` HTTP 200, not 500.
- Sole HTML home fallback for kinds that do not emit `index.html` (`8.2/onLightMeasured-1.html`).
- Publish chrome remains hidden (later sibling).

## Tests

- rest `SitesResourceTest` 143 tests, module Tests run: 1286 Failures: 0
- sitemanage `SitesAdaptorTest` 267 tests, module Tests run: 2556 Failures: 0 (125 skipped baseline)
- WebUI Surefire Tests run: 69 Failures: 0; Vitest Test Files 441 passed, Tests 4135 passed
- perc-qa-automation `mvnw clean install` BUILD SUCCESS (no Java tests)
- Playwright C5: `npm run test:surface -- --path tests/developer-site-virtual-source.spec.js --grep asyncapi-yaml` 4 passed; console-clean=yes; server.log-clean=yes
