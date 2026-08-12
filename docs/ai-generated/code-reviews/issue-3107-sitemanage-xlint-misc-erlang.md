# Erlang review: issue #3107 sitemanage misc Xlint residual

## Summary

PR-sized cleanup of remaining **main-source** `-Xlint` diagnostics in `projects/sitemanage` after #3061 / PR #3106. Real fixes preferred: static qualification, Hibernate `Session.find`, equals/hashCode pairs, fall-through rewrite, typed collection copies, serial-field concrete types, and targeted `@SuppressWarnings("removal")` only for intentional legacy (PSAesCBC decrypt fallback, ThreadDeath stop, GenericGenerator custom id).

## Scope

- Branch: `fix/issue-3107-sitemanage-xlint-misc`
- Base: `origin/main`
- Module: `projects/sitemanage` only
- Prior: #2032 / #2200 / #3061 serial-field + this-escape batches
- Cross-platform path review: **N/A** — no path/file I/O changes
- Memory patterns: prefer real Xlint fixes over blanket suppress; Session.get → find peer pattern from taxonomy/DTS DAOs

## Recommendation

**approve**

## Gate

- May commit/push: **yes**
- Bugs: none
- Behavioral tests: `PSXlintMiscResidualTest` (8) for equals/hashCode + DTO copies; existing serial-field tests still green
- Build: `cd projects/sitemanage && ../../mvnw.cmd clean install` → BUILD SUCCESS; Tests run: 1098, Failures: 0, Errors: 0, Skipped: 125
- Main-source Xlint: **40 → 0** (project compiler args)

## Issues

None at bug severity.

### suggestion

1. **Test-source residual (~60)** — file follow-up under #2032 for test-source serialVersionUID / this-escape / unchecked / Unsafe path-injection tests. Out of scope for this main-source batch.

### nit

1. `PSAnalyticsProviderConfig.setExtraParamsMap` still does not sync `ExtraParamsClass` (pre-existing); getter rebuilds from `extraParams`. Left unchanged to avoid behavior change.

## Inventory

| Cluster | Before | After | Approach |
|---------|--------|-------|----------|
| static qualification | ~12 | 0 | Type-name qualification |
| Session.get removal | 6 | 0 | `session.find` |
| equals without hashCode | 3 | 0 | Add hashCode |
| fall-through | 2 | 0 | Explicit break + set dirty |
| unchecked conversion | 5 | 0 | Always copy into HashMap/ArrayList |
| serial-field leftovers | 3 | 0 | HashMap/ArrayList field types; transient collaborator |
| try-with-resources unused | 1 | 0 | Drop unused Reader |
| redundant cast | 1 | 0 | Remove cast |
| missing @Deprecated | 1 | 0 | Annotate |
| removal (PSAesCBC/ThreadDeath/GenericGenerator) | ~6 | 0 | Targeted suppress / type= form |

## Residual

Test-source Xlint (~60) — separate PR-sized residual under parent #2032 / #2200.
