# Erlang review — #3738 M2 H2 zero-legacy evidence after perc.Test modern

| Field | Value |
|-------|--------|
| **Date** | 2026-08-22 |
| **Branch** | `fix/issue-3738-m2-h2-zero-legacy-evidence` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |

## Summary

Extends product/H2 M2 selection evidence so the Widget G4 waive list must be **empty or `perc.Test` only**. On current `main` (slice #3736 unmerged) tests still assert the `perc.Test` residual may select `LEGACY_WIDGET_XML`. After that waiver is dropped, the same harness requires `perc.Test` modern-first. Criteria snapshot refreshed to **2026-08-22**: M1 PASS (Widget non-waived), M2 PARTIAL, **M3 FAIL**. `PSLegacyDefinitionXmlShim` is not deleted.

## Scope

- `modules/perc-packages` evidence API + Surefire
- `projects/sitemanage` DAO H2 harness + javadoc
- Criteria / dual-run / README snapshot text
- Cross-platform path review: no new filesystem joins; helpers take `Path` / `Set` only. Tests use `Path.resolve` / `Files` / `@TempDir`.

Memory patterns hit: behavioral tests for new policy helpers; change-class companions (tests + criteria snapshot); do not treat green selection scan as M2 PASS / shim removal.

## Issues

None at bug / missing-tests / non-portable-path severity.

## Notes

- C2: new public helpers/constants only; existing `PSProductPackageRootSelectionEvidence` was already `final`; no public/protected signature removed. sitemanage standalone clean install is the known consumer.
- C5: N/A (no UI surface).
- Product-docs: N/A (engineering criteria + test harness; operator dual-run load behavior unchanged).
