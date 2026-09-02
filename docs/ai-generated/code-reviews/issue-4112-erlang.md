# Erlang review — issue #4112 SPA UI-02 action menu create/delete

**Branch:** `feat/issue-4112-spa-ui02-create-delete`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no new filesystem path I/O; REST/URL paths use `/`)

## Summary

Finish Developer Action Menus SPA create (POST) and delete (DELETE) on the existing REST surface. Stacks open #4189 / #4171 JAXB bind (`skip.default.json.provider.registration` on `rest-jax-rs`, explicit finder DTO root name). Does not re-implement REST write or steal UI-03 PUT/tabs (cluster #4194). Playwright now asserts catalog GET lists the created name and GET after DELETE is 404. Vitest covers invalid 400, duplicate 409, missing 404, non-Admin 403, system 409, and catalog list after create+back.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Notes

- SPA chrome already existed on `main` (cluster #4151); remaining acceptance was live POST bind + catalog include/omit.
- UI-03 usage/command/visibility and UI-04 children are not claimed.
- Memory patterns hit: dual-ship rest+sitemanage+perc-system into H2 QA WAR; do not docker-restart the cell.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
