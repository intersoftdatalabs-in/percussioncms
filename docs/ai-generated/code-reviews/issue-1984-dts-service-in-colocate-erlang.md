# Erlang review: issue #1984 — DTS systemd unit template co-location

**Branch:** `fix/issue-1984-dts-service-in-colocate`  
**Reviewer persona:** Erlang (independent pre-commit)  
**Date:** 2026-08-05  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes

## Summary

`installDts.xml` installed Linux service scripts under `Deployment/Server/` but left
`dts-tomcat.service.in` (and `README-systemd.md`) only at product surface root via
the root `*` fileset. `DTSProductionService.sh` / `DTSStagingService.sh` resolve the
unit template as `dirname $0`/dts-tomcat.service.in, so systemd install failed unless
operators copied the template manually.

This change co-locates the template (and README) on **fresh and upgrade** Linux paths
in the same `Deployment/Server` copy that places the service scripts. Structural tests
assert both script resolution and installer path wiring. Windows `.bat` behavior is
unchanged (`if="${isLinux}"` gates). Parent `pom.xml` nested unclosed `<excludes>`
tags on `main` were fixed so Maven can parse (build blocker unrelated to packaging
logic but required for verification).

## Scope

- `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/rxconfig/Installer/installDts.xml`
- `.../DtsServiceInstallScriptTest.java` (new co-location + resolution tests)
- Docs: module `README.md`, `README-systemd.md`
- `pom.xml` (bannedDependencies excludes well-formedness)
- Cross-platform path review: no new filesystem path joins; tests use `Path.of(...)`
  segments; Ant destination paths retain product layout conventions (`Deployment/Server`
  is a product-relative install tree, not OS-specific). Scripts under review still use
  Unix shell path forms (Linux-only service installers). **Cross-platform path review:
  clean for this slice.**

Prior report / memory: GH-962 / #1977 systemd dual-ship patterns; Gap B residual of
#1975 inventory.

## Issues

None at `bug` severity.

### suggestion (non-blocking)

1. **Surface root still retains a copy** of `dts-tomcat.service.in` via the root `*`
   fileset. Harmless dual ship; operators may still find the template at surface root.
   No change required for acceptance (co-location beside scripts is the fix).

### nit

1. Parent POM parse fix is slightly outside the packaging residual; documented in PR
   body so reviewers know why `pom.xml` is in the diff.

## Test evidence

- `cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution && ../../../mvnw clean install` → **BUILD SUCCESS**
- `DtsServiceInstallScriptTest` 8 tests, `DtsSystemdUnitTemplateTest` 4 tests — all pass
- Spotless: `mvnw spotless:apply` then `mvnw spotless:check` → SUCCESS; feature PR commits
  only in-scope paths (out-of-scope Spotless rewrites left uncommitted)

## Recommendation

Approve for PR against `main`. Does not close #1975 (inventory PR #1985 owns that).
Links: #1984, #1975, #962.
