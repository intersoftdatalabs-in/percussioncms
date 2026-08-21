# Erlang review — #3673 perc.Baseline native page install

| Field | Value |
|-------|--------|
| **Branch** | `fix/issue-3673-baseline-native-page-install` |
| **Scope** | uncommitted vs `HEAD` / `origin/main` |
| **Recommendation** | approve |
| **Gate** | pass |
| **May commit/push** | yes |
| **Reviewed** | 2026-08-20 |

## Summary

Opt `perc.Baseline` into `page.installMode=native` (peer of `perc.baseTemplates` / `perc.responsiveTemplates`). Package-build stages archive `TemplateDef-N/` from modern `pages/` and does not dual-ship root `*.templateDef`. Mapping + ACL side-cars unchanged. Goldens extended in `PSPageXmlNativeInstallTest`. Dual-ship retirement checklist (and related inventory/ADR/implementer-guide) updated.

Package-build log evidence: `native-install page TemplateDefs for perc.Baseline: 7 written`.

## Issues

None (no bugs, no missing behavioral tests for this change class, no non-portable path I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Tests use `Path` / `Files` (`product.resolve(stem + ".templateDef.aclDef")` — `+` is filename suffix, not a path separator)
- [x] Line-ending assertions use `normalizeNewlines` for dual-ship vs native XML parity
- [x] No Unix-only absolute path shapes

## Change-class companions

| Kind | Present |
|------|---------|
| `package-install.properties` `page.installMode=native` | yes (peer copy) |
| Mapping + ACL side-cars kept | yes (test asserts ACL files; mapping GUIDs) |
| Native golden (`PSPageXmlNativeInstallTest`) | yes (7 stems, GUIDs, assemblers, no root dual-ship, perc.page XML parity) |
| Dual-ship checklist row | yes |
| Module `mvnw clean install` | BUILD SUCCESS, Tests run: 180, Failures: 0 |
| Public API / `final` blast radius | N/A (properties + tests + docs) |
| Product-docs | N/A (package-build internal; operator `.ppkg` wire format still TemplateDef XML) |
| Playwright / WebUI | N/A |

## Memory patterns hit

- Incomplete change-class closure — checked peers (`perc.baseTemplates` native opt-in + `PSPageXmlNativeInstallTest`)
- Non-portable path joins — none in this diff
- Tests that only grep source strings — native test stages archive and parses XML

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
