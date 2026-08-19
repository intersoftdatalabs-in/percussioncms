# Erlang review: #3581 G4 Pages/Gadgets ship-path inventory

| Field | Value |
|-------|--------|
| **Date** | 2026-08-18 |
| **Branch** | `fix/issue-3581-g4-pages-gadgets-inventory` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: **yes** |

## Summary

Adds a Surefire/CI inventory gate for product Page and Gadget definition XML under Packages ship paths, peer of `PSWidgetDefinitionXmlInventory` (#3026). Shared scanner covers staging `sys__UserDependency--rxconfig/{Pages,Gadgets}` and shim-recognized `rxconfig/{Pages,Gadgets}`. Waiver is `perc.Test` only. `PSLegacyDefinitionXmlShim` is untouched.

## Scope

- `modules/perc-packages` inventory API + facades + Surefire
- Criteria G4 row + module/scripts README
- Memory patterns: portable Path/Files; product-tree + TempDir fail-fast (Widget G4 peer)

**Cross-platform path review:** all ship-path construction uses `Path.resolve(String)` / `Files.newDirectoryStream` / `toAbsolutePath().normalize()`. Finding paths are anchored on the scanned ship dir + file name (same CWD-relative regression as Widget #3026). Tests assert absolute paths and `startsWith` under the ship dir. No hardcoded `/` or `\` filesystem joins, no Unix-only roots, no `:`-only path lists.

## Issues

None (bug / missing tests / non-portable I/O).

Behavioral tests cover: product tree clean; waived-only clean; dummy non-waived fail (staging + rxconfig); mixed waived/non-waived; modern `pages/` and catalog JSON ignored; relative packages root; portable resolve; combined PAGE+GADGET scan.

## Gate

approve — no bug, missing behavioral tests, or non-portable path I/O.
