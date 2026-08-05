# Erlang review: issue #1977 systemd unit templates + dry-run docs

**Branch**: `fix/issue-1977-systemd-unit-templates-docs`  
**Date**: 2026-08-05  
**Reviewer**: Erlang (pre-commit, implementer session)  
**Recommendation**: **approve**  
**May commit/push**: **yes**  
**Gate**: clean

## Summary

Slice 2 residual for GH-962 / #1977: contract alignment of CMS + DTS systemd unit
templates (privilege-model documentation), operator dry-run / non-root docs parity
in both `README-systemd.md` files, quickstart dry-run checklist, extended structural
tests for contract keys and README flags. **init.d path retained** (helper +
fallback). No live root install; no SysV removal.

## Scope

- `modules/perc-jetty` unit template, README-systemd, module README, tests
- `delivery-tier-distribution` unit template, README-systemd, module README, tests
- `specs/988-linux-systemd-services/quickstart.md`

## Change class

Operator packaging / docs + structural tests for shipped unit templates (not a new
REST surface or WebUI screen). Companions: dual CMS/DTS docs + dual template tests
+ install-script root/flag assertions.

## Cross-platform path checklist

- [x] Tests use `Path.of(...)` for template/README paths
- [x] No new filesystem path concatenation with hardcoded separators in product code
- [x] Operator docs intentionally document Linux systemd/init.d bash paths
- [x] No Unix-only absolute path assertions in unit tests

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Nits (non-blocking)

- DTS unit still omits `Documentation=` (CMS has it); would need a new
  `@CATALINA_HOME@` (or similar) placeholder in both DTS install scripts — deferred
  as optional polish; privilege docs close the User= contract item.

## Verification

- `cd modules/perc-jetty && ../../mvnw clean install` → BUILD SUCCESS
  (`SystemdUnitTemplateTest` 5 tests; `InstallJettyServiceScriptTest` 6 tests)
- `cd deliverytiersuite/.../delivery-tier-distribution && ../../../mvnw clean install`
  → BUILD SUCCESS (`DtsSystemdUnitTemplateTest` 4; `DtsServiceInstallScriptTest` 6)
- Root `mvnw spotless:apply` then `spotless:check`; out-of-scope Spotless hits
  discarded; only in-scope files staged

## Memory patterns hit

- Partition Spotless out-of-scope rewrites from feature commit
- Structural packaging tests over live root systemd
