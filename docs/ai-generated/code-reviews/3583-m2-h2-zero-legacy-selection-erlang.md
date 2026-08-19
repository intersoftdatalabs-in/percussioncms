# Erlang review — #3583 M2 H2 runtime zero-legacy-selection evidence

| Field | Value |
|-------|--------|
| **Date** | 2026-08-18 |
| **Branch** | `fix/issue-3583-m2-h2-zero-legacy-selection` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |

## Summary

Adds CI-assertable product/H2 dual-run selection evidence: non-waived widget package roots must select modern-first (`wouldUseLegacyShim == false` / `MODERN_COMPONENT_PACKAGE`). Unexpected `LEGACY_*` fails Surefire. Waived `perc.Test` and customer-only XML remain legacy. Shim is not deleted. Criteria M2 snapshot refreshed to 2026-08-18 and still **PARTIAL / FAIL overall** (M3 FAIL).

## Scope

- New `PSProductPackageRootSelectionEvidence` + tests in `modules/perc-packages`
- New `PSWidgetDaoProductH2ZeroLegacySelectionTest` + javadoc in `projects/sitemanage`
- Criteria / dual-run / README evidence docs
- Cross-platform path review: new code uses `Path` / `Files` / `Path.of(segments)` / `DirectoryStream` filename globs. No hardcoded filesystem separators, no Unix-only roots, no `C:\` assertions.

## Issues

None at bug / missing-tests / non-portable-path severity.

## Notes

- `DefinitionFinding.isUnexpectedLegacy` treats `kind == null` as clean; product tests also require all 47 known stems via `containsAll`.
- Waiver list is reused from `PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` (`perc.Test` only).
- C2: new public type only; no existing API made `final`/`sealed` and no signature change.

## Re-review (PR #3600 kilo-code-bot)

**Date:** 2026-08-18
**Recommendation:** approve
**Gate:** pass
**May commit/push:** yes

Removed unreachable `return;` after `System.exit(2)` in
`PSProductPackageRootSelectionEvidence.main`. CLI usage still exits 2 on
missing `packagesRoot`. No path/I/O change. `cd modules/perc-packages &&
../../mvnw.cmd clean install` BUILD SUCCESS, Tests run: 142, Failures: 0.
