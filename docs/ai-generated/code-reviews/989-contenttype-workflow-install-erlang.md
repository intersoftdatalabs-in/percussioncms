# Erlang review — content type package install workflow fixes

|          Field          |                                                                    Value                                                                    |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **Date**                | 2026-07-17                                                                                                                                  |
| **Branch**              | `989-react-cui-widget-builder`                                                                                                              |
| **Scope**               | Uncommitted local changes vs `HEAD`                                                                                                         |
| **Intent**              | Fix content-type package install: inverted workflow association removal, Spring proxy cast for default workflow, empty deploy error message |
| **Recommendation**      | **approve**                                                                                                                                 |
| **Gate**                | **May commit/push: yes**                                                                                                                    |
| **Memory patterns hit** | Spring JDK proxy cast to concrete impl (hard gate family); modernization inverted condition; missing behavioral tests (avoided)             |

## Summary

Three related package-install defects after content-type save path became reachable (post–fileType fix):

1. **`removeWorkflowAssociations` inverted** (`wf != null` remove vs original `wf == null` remove) — modernization regression; drops workflows that exist on target.
2. **Cast of `IPSWorkflowService` proxy to `PSWorkflowService`** when resolving system default workflow — ClassCastException under Spring JDK proxies.
3. **Exception detail lost** in thrown `PSDeployException` (`Error was: {}` literal) while SLF4J placeholder only filled the log line.

Fixes restore pre-modernization remove semantics, call interface `getDefaultWorkflowId()`, null-safe UUID extract, inclusion-list guard before save, and real detail in thrown errors. Pure helpers extracted to `PSContentTypeWorkflowInstallUtils` (no Spring static init) with 7 unit tests (all pass).

No hard-gate bugs. Cross-platform path checklist: N/A (no filesystem path logic in this pack).

## Files

|                           Path                            |                               Role                               |
|-----------------------------------------------------------|------------------------------------------------------------------|
| `deployer/.../PSContentTypeDependencyHandler.java`        | remove associations, default WF, error message, ensure inclusion |
| `deployer/.../PSContentTypeWorkflowInstallUtils.java`     | pure helpers (new)                                               |
| `deployer/.../PSContentTypeWorkflowInstallUtilsTest.java` | unit tests (new)                                                 |

## Verification

- `PSContentTypeWorkflowInstallUtilsTest`: 7 run, 0 failures

## Issues

### Bugs

_None._

### Suggestions

1. When `idMap == null`, associations still drop (original behavior) even if the same numeric id exists on the target without a mapping. Pre-existing; leave unless product wants load-by-source-id.
2. `ensureDefaultWorkflowInInclusionList` no-ops on empty inclusion lists; remapping path is responsible for creating the list. Acceptable.

### Nits

_None material._

## Gate

| | |
|--|--|
| **Recommendation** | approve |
| **May commit/push** | **yes** |
| **Blockers** | none |

## Handoff

Safe to commit/push. Redeploy deployer + restart package install; re-tally content-type failures. Residual gaps may include `perc.workflow` package if workflows never installed.
