# Erlang review: #1689 license-maven-plugin THIRD-PARTY inventory

|       Field        |              Value               |
|--------------------|----------------------------------|
| **Date**           | 2026-08-02                       |
| **Branch**         | `feat/1689-license-maven-plugin` |
| **Scope**          | Uncommitted work for issue #1689 |
| **Recommendation** | approve                          |
| **Gate**           | May commit/push: **yes**         |
| **Blocking bugs**  | 0                                |

## Summary

Adopts `org.codehaus.mojo:license-maven-plugin` on the reactor root to generate a versioned
`THIRD-PARTY.txt` inventory from the dependency set. Hand-curated component lists and version pins
are removed from `NOTICE.txt` and `thirdPartyCopyright`; both become stable pointers only. The
installer module copies `LICENSE.txt`, `NOTICE.txt`, and the generated inventory into the assembly
root. Behavioral tests cover the blurb policy and packaging when the inventory is present.

## Scope

- `pom.xml` — plugin version property + root-only aggregate execution
- `NOTICE.txt` — stable product notice + pointer
- `system/.../PSStringResources.properties` — thin `thirdPartyCopyright` / copyright year
- `system/.../PSThirdPartyCopyrightTest.java` — new
- `modules/perc-distribution-tree/pom.xml` — copy license artifacts into assembly
- `modules/perc-distribution-tree/.../ThirdPartyInventoryPackagingTest.java` — new
- `src/license/*` — missing-license map + README
- Out of scope discarded: `modules/perc-i18n/scripts/cache/i18n_translate.json` (unrelated drift)

**Memory patterns hit:** non-portable path joins (checked clean — uses `Path`/`Files`); missing
behavioral tests (present for blurb + packaging); incomplete change-class (packaging companion
included).

**Cross-platform path review:** clean. Tests resolve repo root via `Path` walk/`resolve`; no
hardcoded `/` or `\` filesystem joins; no Unix-only absolute roots; no line-ending fragile multi-line
file equality assertions.

## Issues

### suggestion — packaging soft-skips when inventory missing

- **File:** `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/ThirdPartyInventoryPackagingTest.java`
- **Note:** `assumeTrue` means standalone Surefire without a prior root aggregate pass does not fail.
  Acceptable for this monorepo (AC targets full reactor). Documented in pom comment + `src/license/README.md`.
  Full-reactor / process-resources path was verified locally (LICENSE + NOTICE + THIRD-PARTY in assembly).

### nit — copyright year pin in test

- **File:** `system/.../PSThirdPartyCopyrightTest.java` (`1999-2026`)
- **Note:** Will need a yearly bump; matches product prose. Acceptable.

## Gate

No bugs. No missing behavioral tests for the changed policy. Path I/O portable.

**May commit/push: yes**
