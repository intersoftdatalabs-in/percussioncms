# Erlang review — #3676 H2 create-section landing attach without check-in

**Branch:** `fix/issue-3676-create-section-landing-checkin`  
**Base:** `origin/main` (includes `fix/issue-3672-rffnavtree-create-section` / PR #3677)  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + Playwright); sample-workflow check-in NPE marks TX rollback-only (#3364); do not weaken fail-closed Playwright

## Summary

After #3672 aliases, percNavon save succeeds on H2 sample `rffNavTree` (type 315). Attaching the landing page then 500s because `PSContentWs.checkinItems` / `prepareForEdit` NPEs when CONTENTSTATEID is 0 or `sys_contentstateid` is missing on the percNavon item def (same class as NavTree save-without-check-in #3364).

This change:

- Skips `checkinItems` on create-section landing attach (`PSSiteSectionService.attachLandingPageWithoutForcedCheckin`), matching `PSSiteContentDao` homepage attach.
- `PSManagedNavService.addLandingPageToNavnode` skips sample-workflow `prepareForEdit` and still adds the AA relation (fresh percNavon is already checked out after save).
- Expands sample-workflow detectors for landing-attach NPE / missing state field without treating those as skippable on the generic navon check-in helper.
- Playwright spec stays fail-closed: POST 200 + treeitem when tree GET is 200 (from #3677); 0 skipped.

Does not seed a second NavTree. Does not annotate HTTP 500.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover skip-checkin on create, attach after prepareForEdit NPE, rethrow of non-sample failures, and detector split (check-in vs attach). Cross-platform path checklist: **clean** — no new filesystem path construction.

## Issues

None (blocking).

## Change-class closure

| Companion | Status |
|-----------|--------|
| Skip landing/navon `checkinItems` on create section | Done (`PSSiteSectionService`) |
| No-checkout attach when sample workflow NPE | Done (`PSManagedNavService.prepareForEditIgnoringSampleWorkflow`) |
| Detector tests (NPE / sys_contentstateid / non-sample) | Done |
| Playwright POST 200 + treeitem, 0 skip | Done (fail-closed spec from #3677 kept) |
| `product-docs/8.2/admin/architecture-navigation.md` | Done |
| Do not seed second NavTree / do not allowlist 500 | Honored |

## Tests / builds observed

Standalone `mvnw clean install`: `system` BUILD SUCCESS (Tests run: 2295, Failures: 0); `projects/sitemanage` BUILD SUCCESS (Tests run: 1354, Failures: 0).
