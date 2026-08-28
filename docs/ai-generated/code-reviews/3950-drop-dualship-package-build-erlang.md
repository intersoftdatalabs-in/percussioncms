# Erlang review: #3950 drop DualShip materialize from package-build

| Field | Value |
|-------|--------|
| **Date** | 2026-08-28 |
| **Branch** | `fix/issue-3950-drop-dualship-package-build` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: **yes** |

## Summary

Removes the production package-build call to `PSPageXmlDualShip.materializeInstallTemplateDefs` from `PSPackageBuilder.stageModernPageInstallArtifacts` (parent #2630). Native `PSPageXmlNativeInstall.stageArchiveTemplateDefs` remains the only production TemplateDef emit. Dual-ship mode fails closed (inventory #3675). DualShip helper/CLI kept for tests and one-off ops; `PSLegacyDefinitionXmlShim` untouched.

## Scope

- `modules/perc-packages` package-build path + behavioral builder tests
- Retirement checklist + module README companions
- Memory patterns: portable Path/Files; ZIP entries use `/`; fail-closed dual-ship; native archive + ACL mapping

**Cross-platform path review:** builder still uses `Path.resolve` / `Files.walkFileTree`. Tests locate packages via `Path.of("src", "main", "resources", "Packages", …)`, copy with `Path.relativize`/`resolve`, and assert ZIP entry names with `/` (ZIP form, not OS separators). Line splits use `\\R`. TempDir only — no Unix `/tmp` or drive-letter hardcodes.

## Issues

None (bug / missing tests / non-portable I/O).

Behavioral tests cover: native package-build writes `TemplateDef-N/` + ACL mapping and emits native log (no dual-ship marker); explicit dual-ship fails closed without writing `.ppkg` or root templateDefs; packages without `pages/` still build; product `perc.responsiveTemplates` archive + ACL parity.

Change-class companions: builder path + Surefire + inventory still fail-closed + retirement checklist call-site row + README. Product-docs N/A (not operator-facing). Playwright N/A.

C2: private method signature only; `PSPackageBuilder` already `final`; `materializeInstallTemplateDefs` production call site gone (tests/CLI remain). No reverse-dep modules compile against the private builder method.

## Gate

approve — no bug, missing behavioral tests, or non-portable path I/O.
