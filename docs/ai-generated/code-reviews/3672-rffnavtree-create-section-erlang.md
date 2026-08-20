# Erlang review — #3672 H2 sample create-section HTTP 200 under rffNavTree

**Branch:** `fix/issue-3672-rffnavtree-create-section`  
**Base:** `origin/main`  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + Playwright); FastForward rffNav* vs percNav* dual ids; ItemDef catalog drop of 313–315; do not seed a second NavTree

## Summary

`GET /section/tree/{site}` already returns HTTP 200 for FastForward sample sites (`rffNavTree` type **315**) via the JCR perc/rff alias (#3611). `POST /section/create` still failed on skip-image-build H2 cells because Managed Nav queries and ItemDef only knew `percNav*` **1015–1017**. `findChildNavonLocator` filtered folder children by registered perc ids, so the parent `rffNavTree` was missed; checkout/edit of type 315 then threw (`content type 315 is not registered` / empty `PSNavException`).

Fix: expand `PSNavConfig.getNav*TypeIds()` with well-known siblings (`1017→315`, `1016→314`, `1015→313`) **after** registered ids so `get(0)` still creates with percNavon. ItemDef lookups resolve 313–315 to the registered perc sibling. Skip sample-workflow navon check-in (same as NavTree #3364 — `stateId must be > 0` marks the TX rollback-only). Playwright create-section requires POST 200 + treeitem when tree GET is 200.

C5 on skip-image-build H2: tree GET 200 and Escape-to-close pass; POST `/section/create` still HTTP 500 after percNavon save (`PSContentWs.checkinItems` NPE / landing attach). Residual for landing checkout. Does not seed a second NavTree. Landing-field dialog work stays in #3661 / PR #3671.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover alias expansion, registered-id resolution, well-known role helpers, installer sample rows staying on type 315, and Playwright helper matching for `POST /section/create`.

Cross-platform path checklist: **clean** — installer test uses `Path.of` / `Files`; no new OS filesystem separators. Playwright URLs keep `/`.

## Issues

None (blocking).

## Change-class closure

| Companion | Status |
|-----------|--------|
| Query type-id expansion (313–315 ↔ 1015–1017) | Done (`PSNavConfig.getNav*TypeIds`) |
| ItemDef alias so checkout/edit of type 315 uses perc.nav editors | Done (`PSItemDefManager`) |
| `isNavonItem` / `isNavonTreeItem` / `isManagedNavType` | Done (type ids + well-known roles) |
| `PSNavNameAliases` expand/resolve helpers + tests | Done |
| Installer wiring: sample NavTrees stay on type 315; rff editors 313–315 still copied | Done (`InstallSampleSitesWiringTest`) |
| Playwright POST 200 + treeitem; 0 skip when tree GET is 200 | Done |
| `product-docs/8.2/admin/architecture-navigation.md` | Done |
| Do not seed second NavTree / do not allowlist 500 | Honored |
| Landing name/title/template dialog | Out of scope (#3661 / PR #3671) |

## Tests / builds observed

Recorded in the PR body after standalone `mvnw clean install` on `system`, `modules/perc-distribution-tree`, and `modules/perc-qa-automation`.
