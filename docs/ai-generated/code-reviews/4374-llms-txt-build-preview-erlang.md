# Erlang review — issue #4374 llms-txt Build and Preview

Date: 2026-09-06
Branch: feat/issue-4374-llms-txt-build-preview
Scope: uncommitted vs origin/main (REST + sitemanage + WebUI + perc-qa-automation + product-docs/8.2)

## Summary

Vertical slice enables REST `POST …/virtual/build` and `GET …/virtual/preview` plus Developer Sites Build/Preview chrome for `sourceKind=llms-txt` from a local `llms.txt` fixture. Publish chrome stays hidden (sibling #4375). Persist PUT/GET is consumed from #4373 (not re-implemented). Peer pattern: robots-txt #4361 / PR #4372.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

- New adaptor tests and QA fixture helper use `Path` / `Files` (Java) and `path.join` (Node host).
- In-container fixture root is a POSIX path (`/opt/Percussion/tmp/llms-txt-virtual-qa`) because the QA cell is Linux; host copies use `path.join`.
- Tests do not assert Unix-only OS path strings from `Path.toString()`.
- No new `"/" +` filesystem joins in Java production code.

## Issues

None blocking.

## Memory patterns hit

- Change-class companions: REST OpenAPI + resource tests, sitemanage adaptor tests, Spring `SitesTestAdaptor` comment, WebUI chrome + Vitest, Playwright + fixture helper, product-docs 8.2.
- Leftover `virtual.remoteUrl` / credentials / cloud `rootPath` fail closed (400).
- Missing last-build Preview is `available=false` HTTP 200, not 500.
- Sole HTML home fallback for kinds that do not emit `index.html`.

## Tests

- rest `SitesResourceTest` 109 tests, module Tests run: 1237 Failures: 0
- sitemanage `SitesAdaptorTest` 219 tests, module Tests run: 2501 Failures: 0 (125 skipped baseline)
- WebUI Vitest Test Files 441 / Tests 4109 passed
