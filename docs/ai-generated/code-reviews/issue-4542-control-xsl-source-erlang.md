# Erlang review — #4542 Developer Controls user-control XSL source (slice 16)

**Branch:** `fix/issue-4542-control-xsl-source`  
**Parent:** #1690  
**Scope:** rest + sitemanage GET `xslSource` round-trip, SPA source editor, Playwright, product-docs 8.2  
**Date:** 2026-09-17

## Summary

Admin GET/PUT of **user** CE control XSL from **Developer → Controls**.
`GET /services/cecontrols/{name}` attaches persisted user-control stylesheet
text (`Files.readString` on the contained user-dir file). List rows omit
`xslSource`. PUT already wrote `xslSource`; invalid source remains 400.
System packaged controls stay read-only (409, no file write). SPA drops the
CONTROL_DESIGN_GAPS XSL-editor string and maps 403/404/400/409. Coerce
one-element JAXB `designGaps` strings before `Array.filter` (dropping the
XSL gap left a single honesty string). Playwright extends the existing CE
controls surface spec (H2 3/3).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Companions

| Kind | Status |
|------|--------|
| rest `ControlDef` / `ControlsResource` GET docs + resource test | yes |
| sitemanage `ControlAdaptor.attachUserXslSource` + write tests | yes |
| SPA `ControlDetailPanel` editor + 400 map + gap drop | yes |
| Vitest (API, detail GET/PUT XSL, 400) | yes |
| Playwright `developer-control-update-delete.spec.js` | yes |
| product-docs 8.2 admin CE Controls + REST | yes |
| Dual-ship `WebUI/war` | N/A (SPA is `src/main/ts` → generated `cm/modern`) |

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Read/write uses `Path` / `Files.readString` / `Files.writeString` (existing user-dir containment via `Path.startsWith` + `normalize`)
- [x] Tests use `@TempDir` and `Path.resolve` — no Unix-only absolute shapes
- [x] XSL payload is XML text, not an OS path

Memory patterns hit: change-class closure (rest + sitemanage + WebUI + Playwright + product-docs); behavioral tests for GET round-trip / system no-write; portable NIO I/O.

> Co-Authored by Grok Build 1.0.34 using grok-4.6 with agent night-issue-prs.
