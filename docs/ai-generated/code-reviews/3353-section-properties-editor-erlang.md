# Erlang review — #3353 section properties editor

**Branch:** `feat/issue-3353-section-properties-editor`  
**Scope:** uncommitted Navigation SPA section-properties dialog vs `origin/main`  
**Date:** 2026-08-14  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** reuse existing REST envelopes; cancel must not mutate; validation errors must not become 500; companion tests + product-docs + Playwright for WebUI screens.

## Summary

Adds a CM1-parity **Section properties** dialog on Navigation for regular (and root) sections: title, folder name, target window, CSS classes, login/groups. Load uses `GET /section/properties/{id}`; save uses `POST /section/update` with the existing `SiteSectionProperties` envelope. Folder ACL is passed through. Root folder name and inherited login stay locked. Vitest covers form validation/save/cancel and shell enablement/load/save/cancel. Playwright surface spec asserts cancel does not POST. Product-docs 8.2 Navigation page updated.

## Cross-platform path checklist

N/A — no new filesystem path I/O, installers, or OS path assertions.

## Issues

None blocking.

### Notes (not gates)

- Folder ACL write-principals list is not edited in this dialog (out of issue field list; documented as later).
- HTML `required` plus client validators keep empty title/folder from posting.
- Load generation increment on cancel avoids stale GET applying after close.

## Tests

- `WebUI/src/test/ts/architecture/SectionPropertiesDialog.test.tsx`
- `WebUI/src/test/ts/architecture/ArchitectureShell.test.tsx` (#3353 cases)
- `WebUI/src/test/ts/api/architecture/sectionMutations.test.ts` (`applySectionPropertiesForm`)
- `modules/perc-qa-automation/frontend/tests/architecture-nav-section-properties.spec.js`

Standalone `WebUI` `mvnw clean install`: **BUILD SUCCESS**, Tests run: 2334, Failures: 0.
