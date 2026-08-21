# Erlang review: #3675 dual-ship page templateDef CI inventory

| Field | Value |
|-------|--------|
| **Date** | 2026-08-20 |
| **Branch** | `fix/issue-3675-dual-ship-page-templatedef-ci-gate` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: **yes** |

## Summary

Adds a fail-closed Surefire inventory so product package-build cannot silently re-introduce dual-ship page `*.templateDef` materialization (parent #2630). Peer of `PSPageDefinitionXmlInventory` / `PSWidgetDefinitionXmlInventory`. Scan uses committed `package-install.properties` only (JVM native override cannot hide a missing opt-in). Waiver is `perc.Test` only; #3674 leftover widget binaries are cited as **not** dual-ship-retained (empty retain list). `PSPackageBuilder` fails closed on non-waived dual-ship writes. Retirement checklist CI row marked done.

## Scope

- `modules/perc-packages` inventory API + Surefire + package-build fail-closed
- Retirement checklist CI row + module/scripts README companions
- Memory patterns: portable Path/Files; product-tree + TempDir fail-fast (G4 inventory peer)

**Cross-platform path review:** package walking uses `DirectoryStream` + `Path.resolve` / `toAbsolutePath().normalize()`. Tests locate Packages via `Path.of("src", "main", "resources", "Packages")` and assert absolute finding paths / `startsWith` under TempDir. Log scan uses `Files.readAllLines` (UTF-8) and includes a `\r\n` fixture. No hardcoded `/` or `\` filesystem joins, no Unix-only roots, no `:`-only path lists.

## Issues

None (bug / missing tests / non-portable I/O).

Behavioral tests cover: product tree clean + native page packages; leftover authored root templateDefs without `pages/` ignored; native modern pages clean; waived `perc.Test` dual-ship clean; dummy non-waived dual-ship fail; explicit `dual-ship` property fail; JVM native override ignored; relative packages root; mixed waived/non-waived; log format/parse; log waived vs fail; log file CRLF; materialization allowed helper; non-directory / missing log rejection.

Change-class companions: inventory + tests + package-build hook + README + scripts README + retirement CI row. Product-docs N/A (not operator-facing). Playwright N/A.

## Gate

approve — no bug, missing behavioral tests, or non-portable path I/O.
