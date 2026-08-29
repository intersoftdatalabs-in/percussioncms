# Erlang review — issue #3981 REST CD-18 auto-translation set GET/PUT

**Date:** 2026-08-29  
**Branch:** `feat/issue-3981-auto-translation-set`  
**Scope:** uncommitted CD-18 auto-translation set (rest + sitemanage + product-docs 8.2 + gap map)  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + Spring stub + sitemanage impl + adaptor tests); Admin 403 / lock 409 typed exceptions (locale/system-def peers); rest `MainTest` Spring stub for new adaptor interface; no path I/O in this slice

## Summary

Admin REST GET/PUT `/services/locales/auto-translations` over existing `IPSContentDesignWs.loadTranslationSettings` / `saveTranslationSettings` (held design lock released on save). Empty list clears the singleton set. Unknown locale or content type is 400. Non-Admin is 403. Lock conflict is 409. No SPA chrome; format-profile editor remains SOAP.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover resource HTTP mapping (200/400/403/409/500/503) and adaptor success, empty-list clear, unknown locale/type, duplicate row, Admin 403, missing session 403, and lock 409 on load/save. Spring `TestAutoTranslationsAdaptor` implements the new interface. Standalone `mvnw clean install` green for `rest` (Tests run: 770) and `projects/sitemanage` (Tests run: 1799). No new filesystem path joins.

## Issues

None (hard-gate).

### Notes (non-blocking)

- PUT validates locale/content-type/workflow/community against catalogs **before** taking the design lock, then copies Hibernate `@Version` from the locked current set onto matching keys so SOAP save does not null versions.
- Class-level `@Path("/locales/auto-translations")` is more specific than `LocalesResource` `@Path("/{idOrLang}")` (fewer template variables). JAX-RS matching should prefer the literal path.
- Format-profile write remains in locale `designGaps`. SPA locale/auto-translation editor is the remaining CD-18 slice.
- Cross-platform path checklist: N/A (no new file I/O).
