# Contracts: JDBC Drivers Packaging Cleanup

**Phase**: 1 — Design & Contracts
**Date**: 2026-07-11
**Spec**: [spec.md](./spec.md)
**Branch**: `002-jdbc-drivers-cleanup`

## Determination: no public contracts are produced

This feature is a follow-up bug fix that touches only build-time (`installDistributionFiles.xml`, `pom.xml`) and install-time (`install.xml`) artifacts. The project guidance for the contracts directory is:

> Skip if project is purely internal (build scripts, one-off tools, etc.) (per the speckit plan workflow rules)

The relevant `install.xml` is the *installer-side* build script, not a public REST/SOAP/API surface. The CMS installer does not expose a public contract; it is a one-shot deployer that copies files and runs database setup. Per Constitution IV (Contract & Integration Integrity):

- No public REST or SOAP surface changes.
- No CMS ↔ DTS boundary change.
- No package (`.ppkg`) content change.
- No DB schema or TableFactory change.
- The installer payload changes (one directory is no longer leaked, and the install-time delete list is narrowed), but this is a behavior *correction* on a non-public surface, not a contract change.

The only quasi-contract is the **integrator guarantee** documented in `modules/perc-distribution-tree/README.md:80`: "The install scripts (`rxconfig/Installer/install.xml`, `installServer.xml`, `installRepository.xml`) do not purge this folder." The change makes this guarantee true rather than redefining it. The README is the contract; it does not need a separate artifacts.

The behavioral contracts that *are* in scope (and that the unit tests + shell assertion enforce) are documented in [data-model.md](./data-model.md) (Entities E1, E2, E3, E4) and in the spec's FR-001 through FR-008. They are test-enforced, not interface-enforced, and therefore do not warrant a separate contracts document.

## Conclusion

The `contracts/` directory is intentionally left empty (with this README explaining why) per the plan workflow's guidance to skip the contracts artifact for purely-internal build-script changes. Future PRs that introduce or modify public REST/SOAP/.ppkg surfaces in this module should populate this directory at that time.
