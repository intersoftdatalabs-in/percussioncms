# Erlang review — #4416 json-schema persist + Developer Sites source chrome

**Reviewer:** Erlang (independent of implementer)
**Date:** 2026-09-09
**Branch:** `fix/issue-4416-json-schema-persist-chrome`
**Base:** `origin/main`
**Gate:** approve
**May commit/push:** yes

## Summary

Vertical persist+chrome increment for Virtual Site `sourceKind=json-schema`. REST GET/PUT round-trips a portable-safe local `rootPath`; leftover `virtual.remoteUrl`, credentials, cloud URL `rootPath`, and `jsonschema.url` fail closed (400). Developer Sites adds the source-kind option and save/GET-roundtrip chrome. Build/Preview/Publish chrome stays hidden (sibling #4417). Unknown kinds remain 400. Existing kinds unchanged.

## Scope

Uncommitted + branch vs `origin/main`. Peers: graphql-sdl persist #4405, asyncapi-yaml persist #4397. Helper validation for `json-schema` (local-only root, credential reject, `jsonschema.url` reject) already landed with SPI/CLI #4419.

Prior report / Memory patterns: virtual-site persist slices (envelope wrap, local-root only, no secrets on PUT body, hide later-slice chrome).

Cross-platform path review: persist tests use portable drive-letter roots (`C:/json-schema-docs`) matching graphql-sdl peers; no new `"/" +` filesystem joins; helper continues to use NIO `Path`.

## Recommendation

Approve. Companions match the persist change class: rest resource tests + OpenAPI guard, sitemanage adaptor tests, WebUI i18n/Vitest, Playwright C5 specs, product-docs 8.2 REST/admin/developer/reference.

## Issues

None (no bugs, no missing behavioral tests for new persist logic, no non-portable path I/O).

## Notes

- `shouldShowVirtualBuildChrome` / Preview / Publish stay false for `json-schema` by design.
- sitemanage must be built after perc-system SNAPSHOT is installed (allow-list comes from `VirtualSiteSourceType`).
