# Erlang review: #4068 REST UI-05 display format write

**Branch:** `feat/issue-4068-display-format-write`  
**Date:** 2026-08-31  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** rest adaptor Spring stub on test classpath (exact `IXxxAdaptor` type); Admin write via existing IPS*DesignWs (no new SOAP); duplicate 409 / invalid 400 / missing 404 / non-Admin 403; no lock steal (`overrideLock=false`).

## Summary

Admin POST/PUT/DELETE on `/services/displayformats` persist through existing `IDisplayFormatAdaptor` + `IPSUiDesignWs` (create then save / load-with-lock then save / delete). Resource maps HTTP status; adaptor owns Admin/session, unique name, and lock policy. Spring stub `DisplayFormatTestAdaptor` still implements exact `IDisplayFormatAdaptor`. Product-docs 8.2 REST notes updated. No SPA.

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Cross-platform path checklist

N/A — no file I/O or path construction.

## Tests

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 914, Failures: 0; `DisplayFormatResourceTest` 21
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; `DisplayFormatAdaptorWriteTest` 19, `DisplayFormatAdaptorSafeKeyTest` 7
