# Erlang review — PSExtensionMethod returntype attribute case

|          Field          |                                                                                Value                                                                                 |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Date**                | 2026-07-18                                                                                                                                                           |
| **Branch**              | `989-react-cui-widget-builder`                                                                                                                                       |
| **Scope**               | Uncommitted local changes vs `HEAD`                                                                                                                                  |
| **Intent**              | Fix perc.Baseline package install: pageutils extension fails with `returnType cannot be null or empty` because packages use `returntype` and code reads `returnType` |
| **Recommendation**      | **approve**                                                                                                                                                          |
| **Gate**                | **May commit/push: yes**                                                                                                                                             |
| **Memory patterns hit** | Case-sensitive XML attributes vs legacy package spelling; missing behavioral tests (avoided)                                                                         |

## Summary

Startup log (05:46–05:48) shows only **3** package failures after prior deployer fixes (down from ~32). Root of `perc.Baseline`:

- `pageutils(Extension)` → `PSExtensionMethod.fromXML` → `returnType cannot be null or empty`
- Shipped XML: `returntype="java.util.List"` (all lowercase)
- Code: `getAttribute("returnType")` (camelCase) → empty → strict setter throws

DOM attribute names are case-sensitive. Fix accepts both names; `toXML` continues to write canonical `returnType`. Six unit tests cover legacy/canonical/prefer-canonical/missing/round-trip.

`perc.nav` and `perc.openGraphWidget` fail with `Transaction silently rolled back` without a clearer nested cause in this cycle; may clear after Baseline installs or need a follow-up pass.

## Files

|                  Path                   |             Role              |
|-----------------------------------------|-------------------------------|
| `system/.../PSExtensionMethod.java`     | read both attribute spellings |
| `system/.../PSExtensionMethodTest.java` | new unit tests                |

## Verification

- `PSExtensionMethodTest`: 6 run, 0 failures

## Issues

### Bugs

_None._

### Suggestions

1. Optionally normalize package source files to `returnType` over time; not required once deserializer accepts both.
2. Re-tally `perc.nav` / `openGraphWidget` after Baseline succeeds.

### Nits

_None._

## Gate

| | |
|--|--|
| **Recommendation** | approve |
| **May commit/push** | **yes** |

## Handoff

Safe to commit/push. Rebuild system module into install and re-run package install.
