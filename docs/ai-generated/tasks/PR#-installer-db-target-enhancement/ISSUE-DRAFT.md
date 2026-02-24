# Issue Draft: Fresh Install Must Support Customer-Selected RDBMS (8.2)

## Summary

Ensure new installations can reliably target and configure the customer’s selected RDBMS platform during install, instead of defaulting to Derby-oriented behavior unless configuration is pre-seeded.

Supported platforms:

- Derby
- MySQL
- SQL Server

## Customer and End-User Impact

Customers deploying new environments need to install directly to their enterprise database platform (MySQL, SQL Server, or Derby where intended). If fresh installs are not deterministic for the selected backend, installs can fail, require manual rework, or produce unsupported/incorrect runtime configurations.

This impacts first-time deployments and upgrade-to-new-environment scenarios, where predictable installer behavior is expected.

As a secondary driver, this also affects developer and CI automation flows (including Docker-based verification), which need deterministic non-Derby setup.

## Problem Statement

For 8.2, the installer must provide deterministic fresh-install behavior where database target and credentials are explicit inputs, validated during install, and reflected consistently in effective configuration.

## Scope

In scope:

- Fresh install DB target selection and config write-through for CMS installer.
- DTS datasource target selection and configuration for fresh install.
- Validation of DB connectivity with clear fail-fast messaging.
- Post-install verification output of selected backend and key connection settings.
- Automation compatibility (local/CI) as a supporting objective.
- Input contract precedence: CLI `--db.*` > env file (`--db.config.env.file`) > environment variables > defaults.
- SSL explicit defaults in effective config: `db.ssl.enabled=true`, `db.ssl.verify=true`, `db.ssl.allowSelfSigned=false`.

Out of scope:

- Derby migration strategy.
- Data migration between DB backends.
- Runtime schema migration beyond installer setup paths.

## Acceptance Criteria

1. Fresh install accepts explicit DB target and credentials for Derby/MySQL/SQL Server.
2. Installer writes effective DB configuration files consistently for CMS and DTS.
3. SetupDB uses the selected backend without manual post-editing.
4. Invalid DB connection fails with actionable, user-readable error output.
5. Post-install verification reports selected backend and key connection settings.
6. Validation coverage includes at least Derby and MySQL install paths in automation.
7. Non-Derby fresh install fails fast when required DB parameters are incomplete.

## Risks

- Existing upgrade behavior must remain backward compatible.
- SQL dialect differences can break table/view creation paths.
- Credential handling must remain secure (no plaintext leaks in logs).

## Proposed Milestone

- Target: 8.2
- Priority: High / Blocker for customer-safe new installs on non-Derby backends; also unblocks deterministic automation.

## Dependencies

- Agreement on final installer input contract (env vars/properties/CLI/system properties).
- Test matrix definition for supported databases.

