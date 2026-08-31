# Erlang review — #4030 CD-16 SPA system-def field save/add/delete

| Field | Value |
|-------|--------|
| **Date** | 2026-08-31 |
| **Branch** | `feat/issue-4030-spa-system-def-write` |
| **Scope** | Uncommitted vs `HEAD` / `origin/main` |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |
| **Memory patterns hit** | Change-class completeness (WebUI client + panel + Vitest + Playwright + product-docs); WRAP_ROOT_VALUE wire wrap; request lock released on save (no content-type Lock chrome); agent-safe H2 Playwright surface |

## Summary

Developer **System definition** chrome writes on existing REST: `PUT /services/systemdef`, `POST /services/systemdef/fields`, `DELETE /services/systemdef/fields/{fieldName}` (Admin; request lock released on save). `SystemDefPanel` adds save/add/delete; Vitest covers invalid name 400, duplicate 409, lock 409. Playwright surface-filtered H2 spec for add/save/delete and duplicate 409. product-docs 8.2 admin Developer System Def. Out of scope: control/stylesheet/flow, shared-field write, auto-translation.

## Issues

None that block.

## Notes (non-blocking)

- POST wrap uses JAXB `@XmlRootElement` `SystemDefField` (live H2: `SystemDefFieldSummary` is 400). GET/PUT wrap `SystemDefDetail`.
- Live H2 PUT after first save and add/delete of persistable fields 500 (NPE / missing CONTENTSTATUS column). Residual #4037. SPA still calls PUT/POST/DELETE; Playwright proves chrome + duplicate 409 on a fresh cell.
- Duplicate vs lock 409: add uses lock-message sniff only when the 409 body contains `lock`; save treats all 409 as lock. Matches REST (add 409 is duplicate or lock; PUT 409 is lock).
- Client name regex is ASCII-stricter than `Character.isLetter` on the server; Add stays disabled until the name is REST-safe. 400 is still surfaced if the server rejects a client-valid name.
- Cross-platform path checklist: N/A (no filesystem I/O). Field names are URL-encoded on DELETE.

## Tests / companions

- Vitest `systemDefApi.test.ts` (validation, wrap/unwrap, GET/PUT/POST/DELETE)
- Vitest `SystemDefPanel.test.tsx` (catalog, add disable, 400, duplicate 409, lock 409, save/add/delete)
- Playwright `developer-system-def-editor.spec.js` (add/save/delete + duplicate 409, console guards)
- product-docs `8.2/admin/developer-system-def.md` + index / REST / content-types links

## Builds

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 3266, Failures: 0
- Downstream: C2 N/A (no Java public API / `final` / signature change)

## Cross-platform path checklist

- [x] No new filesystem path construction
- [x] DELETE uses `encodeURIComponent` (URL path, not OS path)
- [x] N/A temp files / line endings / scripts
