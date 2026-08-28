# Erlang review — issue #3959 REST CD-18 locale create/update/delete

**Date:** 2026-08-28  
**Branch:** `fix/issue-3959-locale-crud`  
**Scope:** uncommitted CD-18 locale write (rest + sitemanage + product-docs 8.2)  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + Spring stub + sitemanage impl + adaptor tests); Admin 403 / lock 409 typed exceptions (shared-fields peer); no path I/O in this slice

## Summary

Admin REST POST/PUT/DELETE `/services/locales` over existing `IPSContentDesignWs` locale methods (create/load/save/delete with a held lock released on save). GET catalog remains. 400/403/404/409 mapped like shared-fields / keyword write peers. Auto-translation editor and SPA are out of scope.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover adaptor success, Admin 403, unknown 404, duplicate/lock/dependency 409, and resource HTTP mapping. Spring `TestLocalesAdaptor` implements the new interface methods. Standalone `mvnw clean install` green for `rest` and `projects/sitemanage`. No new filesystem path joins.

## Issues

None (hard-gate).

### Notes (non-blocking)

- Create/update hold the design lock for the request and release on save (keyword-write pattern), not a separate lock/unlock REST pair (content-type pattern). Documented in product-docs.
- Format-profile write and auto-translation remain in `designGaps`.
- Cross-platform path checklist: N/A (no new file I/O).
