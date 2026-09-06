# Erlang review — #4365 Pipelines Slice C HTTP datasource execute

**Date:** 2026-09-06  
**Branch:** `fix/issue-4365-pipelines-http-execute`  
**Reviewer:** Erlang (self-review, implementer independent checklist)

## Summary

Vertical Slice C increment: native IR persist of HTTP backend tank (loopback/local fixture URL), execute mapped JSON rows, Developer chrome save + Test invoke, H2 Playwright, product-docs 8.2.

## Scope

- `system` HTTP adapter + URL guard + runtime branch + locator default
- `rest` PUT `/pipelines/{app}/resources/{resource}/backendTank`
- `projects/sitemanage` `PipelinesAdaptor.putHttpBackendTank`
- `WebUI` Developer Pipelines HTTP fields
- `modules/perc-qa-automation` surface spec
- `product-docs/8.2` REST + admin Developer Pipelines

Cross-platform path review: native IR persist uses `Path` / `Files` in existing `PSPipelineIrFileStore`. HTTP URLs use `/` as URI paths (correct). Bundled fixture is classpath, not OS temp. Tests use `127.0.0.1` loopback `HttpServer` with ephemeral ports. No Unix-only `/tmp` or `\` joins added.

Memory patterns: SSRF fail-closed (loopback + no userinfo + no redirects) peers virtual-site HTTP JSON.

## Recommendation

approve

## Gate

**May commit/push: yes**

No remaining bugs after mapper fallback when classic XML mappings miss HTTP JSON keys. Behavioral tests cover persist, cloud 400, loopback fetch, bundled fixture, redirect refuse, adaptor Admin/cloud/save.

## Issues

(none)
