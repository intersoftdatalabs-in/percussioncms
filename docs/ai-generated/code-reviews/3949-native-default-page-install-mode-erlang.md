# Erlang review: #3949 native default page install mode

| Field | Value |
|-------|--------|
| **Date** | 2026-08-28 |
| **Branch** | `fix/issue-3949-native-default-page-install-mode` |
| **Base** | `origin/main` |
| **Recommendation** | approve |
| **Gate** | May commit/push: **yes** |

## Summary

Flips `PSPageXmlInstallPolicy` default from `DUAL_SHIP` to `NATIVE` (parent #2630). Dual-ship remains explicit opt-in (`perc.packages.page.installMode` or package-local `page.installMode=dual-ship`). Shared `DEFAULT_MODE` is used by both `resolve()` and committed inventory so CI cannot treat unconfigured packages as dual-ship emitters. `PSPageXmlDualShip.materializeInstallTemplateDefs` is **not** deleted (sibling #3950). Runtime definition-XML shim is untouched (#2852).

Memory patterns hit: behavioral tests for changed policy; change-class companions (policy + inventory + tests + module/engineering docs); portable `Path`/`Files`.

## Scope

- `modules/perc-packages` policy, enum/javadoc, inventory committed default, package-builder javadoc
- `PSPageXmlInstallPolicyTest` precedence (sysprop > package-local > native default)
- Inventory tests: unconfigured modern `pages/` is native/clean; explicit dual-ship still fails the gate; JVM dual-ship cannot fake a committed dual-ship
- Engineering docs (`docs/ai-generated/tasks/template-assembler-normalization/**`) + `modules/perc-packages/README.md` + `scripts/README.md`
- Product-docs N/A (package-build policy, not operator CMS screens)
- Playwright N/A (no WebUI)

**Cross-platform path review:** tests write `package-install.properties` via `Path.resolve` + `Files.writeString`. Packages root still located with `Path.of("src", "main", "resources", "Packages")`. No hardcoded `/` or `\` filesystem joins, no Unix-only roots, no `:`-only path lists. Line endings not asserted as raw `\n`-only file blobs for policy props.

## Issues

None (bug / missing tests / non-portable I/O).

Behavioral tests cover: null/unconfigured/empty-props → native; package-local dual-ship and native; sysprop overrides both directions; sysprop > package-local > default chain; `dualShip=false` sysprop forces native over package-local dual-ship; unknown mode throws; committed inventory default native; JVM dual-ship ignored for committed scan; JVM native does not hide explicit dual-ship.

Change-class companions: policy default + inventory alignment + unit tests + README/retirement/ADR/implementer-guide. Dual-ship materialize and runtime shim left in place on purpose.

## Gate

approve — no bug, missing behavioral tests, or non-portable path I/O.
