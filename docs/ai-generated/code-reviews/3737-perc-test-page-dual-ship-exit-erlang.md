# Erlang review: #3737 perc.Test page dual-ship templateDef exit

| Field | Value |
|-------|--------|
| **Date** | 2026-08-22 |
| **Branch** | `fix/issue-3737-page-templatedef-exit` |
| **Scope** | uncommitted vs `HEAD` / `origin/main` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |

## Summary

`perc.Test` never authored page `pages/` or `*.templateDef`. This slice empties the dual-ship page templateDef waive list and the shared Page/Gadget G4 ship-path waive list so CI fails closed if dummy dual-ship `pages/` or `rxconfig/Pages|Gadgets` XML reappears, including under `perc.Test`. Widget G4 waiver is left for sibling #3736.

Memory patterns hit: behavioral tests for changed inventory policy; portable `Path`/`Files` only; change-class companions (Page + Gadget tests because they share `WAIVED_PACKAGE_DIRS`); no invented dummy product pages.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Path logic uses `Path.resolve` / `Files`
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp trees use JUnit `@TempDir`

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Nits (non-blocking)

- Emptying Gadget G4 waive is slightly broader than the issue title; required because `PSPageDefinitionXmlInventory.WAIVED_PACKAGE_DIRS` aliases the shared scanner list. Product tree has zero Gadget XML.
- Dual-ship **code path** (`PSPageXmlDualShip.materializeInstallTemplateDefs`) remains; out of scope (parent retirement checklist residual, not this slice).

## Evidence

`cd modules/perc-packages && ../../mvnw.cmd clean install` → **BUILD SUCCESS**; Tests run: 209, Failures: 0. Package-build logged `Building package: perc.Test.ppkg` with **no** `dual-ship page templateDefs` line.
