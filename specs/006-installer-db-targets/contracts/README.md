# Contracts: CLI Installer Database Targets

**Feature**: `specs/006-installer-db-targets`  
**Date**: 2026-07-15

This feature exposes **install-time integrator contracts**, not a new HTTP API.

| Contract | Audience | Artifact |
|----------|----------|----------|
| CLI / system-property input | Integrators, automation | [installer-db-input.md](installer-db-input.md) |
| Effective repository properties | CMS runtime + install tools | [rxrepository-properties.md](rxrepository-properties.md) |
| Sample property files | Integrators | Shipped under `rxconfig/Installer/samples/` (planned) |

## Non-contracts (out of scope)

- Public REST/SOAP APIs — unchanged.
- DTS `perc-datasources.properties` write-through — follow-on.
- GUI installer dialog contract — unchanged unless shared code path.
- Upgrade rewrite of repository identity — **must not** happen.
