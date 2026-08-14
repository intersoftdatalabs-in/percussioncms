# Erlang review — #3357 nav type aliases

**Branch:** `fix/3357-nav-type-aliases`  
**Base:** `origin/main` (`a58c18bb18`)  
**Date:** 2026-08-13  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs companion); wrong-type fakes (N/A — no static Field.set)

## Summary

`percNav*` and `rffNav*` are the same Managed Nav roles (shared `RXS_CT_NAVON` / `RXS_CT_NAVTREE`). Catalog, `Navigation.properties`, CE app names (`psx_cepercNavTree` vs `../psx_cerffNavTree/rffNavTree.html`), `isNavTree`, and the architecture DAO each used a single literal or fail-closed on the first missing alias. That left Navigation empty on FastForward-named trees and warned on expected CE dual names.

This change introduces `PSNavNameAliases`, fail-soft type/slot load in `PSNavConfig`, list/alias checks in `PSManagedNavService.isNavTree` and `PSSiteArchitectureDao`, and suppresses the expected perc/rff CE editor mismatch. Product-docs Navigation page states rff* is the same type, not an empty tree.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover the new helper and the DAO recognition path (including rff item when config lists only perc). Standalone `clean install` on `system` and `sitemanage` both BUILD SUCCESS after the `splitConfiguredNames` constructor cleanup.

Cross-platform path checklist: **clean** — helper treats `/` as URL/editor-path separators (correct); no new filesystem joins.

## Issues

None (blocking).

### Suggestions (non-blocking)

1. **`PSNavConfig` constructor still talks to `IPSContentMgr` directly** — a missing-second-alias “does not drop tree GUIDs” test would need an injectable finder. The DAO + `PSNavNameAliases` tests cover the empty-Navigation recognition path. Acceptable given the locator/singleton shape.
2. **`PSSiteArchitectureDao.isNavTreeType` null-checks `navService`** after a required constructor arg. Harmless dead branch.
3. **Image-slot list write** (`navImageSlotTypes` instead of `menuSlotTypes`) is a real related bugfix without a dedicated test. Left as follow-up; it is not the empty-tree defect.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Helper + call sites (`system`, `sitemanage`) | Present |
| Behavioral unit tests (`PSNavNameAliasesTest`, `PSSiteArchitectureDaoNavTreeAliasTest`) | Present |
| `product-docs/8.2/admin/architecture-navigation.md` | Present |
| Playwright / WebUI screen | N/A (server recognition, no SPA change) |
| Seed-on-GET | Correctly **not** in this PR (#3352 / #3355) |

## Tests / builds observed

- `system`: standalone `clean install` BUILD SUCCESS — Tests run: 2093, Failures: 0, Errors: 0, Skipped: 238 (`PSNavNameAliasesTest` 4/4)
- `sitemanage`: standalone `clean install` BUILD SUCCESS — Tests run: 1125, Failures: 0, Errors: 0, Skipped: 125
- `PSSiteArchitectureDaoNavTreeAliasTest` 4/4 after Optional unwrap + only-perc-config alias case
