# Erlang self-review — issue #2331 perc-ant javac batch 2

**Date:** 2026-08-07  
**Branch:** `fix/issue-2331-perc-ant-javac-batch2`  
**Module:** `modules/perc-ant`  
**Verdict:** **Approve** (commit-ready)

## Scope

Residual batch 2 of parent #2023 / tracker #2200 (after merged batch 1 PR #2330). Clear remaining **~40** project-`-Xlint` main-source diagnostics:

|         Area          |                                                                 Fix                                                                  |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `PSJunitFileSelector` | `Class<?>` / `Iterator<String>` / typed `loadClass` / `ms_testAnnotation`                                                            |
| `gui/PSRxBuildInput`  | `List<String>` / `Set<String>` / `Enumeration<? extends ZipEntry>`                                                                   |
| Install this-escape   | `final class` on `PSModifyProviders`, `PSRenameDeprecatedApps`, `PSTableAction`, `PSUpdateWebApps` (field inits call `getRootDir()`) |

Did **not** re-touch batch 1 files from PR #2330 except the intentional residual this-escape files named in #2331 (`PSRenameDeprecatedApps`, `PSTableAction`).

## Checklist

|                  Gate                   |                                              Result                                              |
|-----------------------------------------|--------------------------------------------------------------------------------------------------|
| Bugs in typed refactors                 | None — collections already held strings / ZipEntry elements; only declarations and casts updated |
| this-escape                             | Real fix via `final class` (peer pattern #2286 / #2016); no class-level `@SuppressWarnings`      |
| Portable paths / file I/O               | N/A — no path-string or I/O behavior changes                                                     |
| Behavioral unit tests                   | No new non-trivial logic; existing module suite green (**43** tests)                             |
| Prefer real fix over suppress           | Yes — no new `@SuppressWarnings`                                                                 |
| Standalone `mvnw clean install`         | BUILD SUCCESS                                                                                    |
| Scope confined to perc-ant + review doc | Yes                                                                                              |

## Verification

- `cd modules/perc-ant && ../../mvnw clean install` → BUILD SUCCESS
- Main-source project-`-Xlint` javac warnings: **0** (was ~40 residual after batch 1)
- Tests: 43 run, 0 fail, 0 skip

## Residual

Main-source project Xlint for `perc-ant` is zero after this batch. Optional later work (not filed unless needed): javadoc "no main description" noise on unrelated install tasks; test-source Xlint if ever enabled module-wide.
