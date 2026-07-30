# Security / secrets pass (T101 / QC-022)

**Date:** 2026-07-24  
**Feature:** #548 Derby → H2

## Scope

Migration and backup paths must never log passwords, JDBC userinfo secrets, or raw `PWD=` property values.

## Controls

|       Control       |                             Location                             |            Status            |
|---------------------|------------------------------------------------------------------|------------------------------|
| Redactor utility    | `system/.../PSMigrationSecretsRedactor.java`                     | Present                      |
| Unit coverage       | `PSEmbeddedRepositoryMigratorTest.redactorRemovesPasswordTokens` | Present                      |
| Durable report      | report writer uses redacted failure/outcome text                 | Covered by migrator tests    |
| Backup gate flag    | `perc.migration.externalBackupConfirmed` is not a secret         | OK                           |
| Offline backup dirs | Operator docs note stop-first + filesystem permissions           | `operator-backup-restore.md` |

## Verification notes

- Failure-injection fixtures intentionally include `PWD=secret-must-not-leak` and assert logs/reports do not retain the secret (`PSMigrationFailureInjectionTest` / migrator tests).
- External-DB skip tests keep sample `PWD` in fixtures but assert connection keys are not rewritten and secrets are not emitted in skip paths.

## Residual

- Operator-managed external backups are outside product redaction (customer ops).
- Do not add new `System.out` / log lines that print full `Properties` dumps of repository configs without redaction.

## Sign-off

|   QC   |                       Status                       |
|--------|----------------------------------------------------|
| QC-022 | **Met** for product migrator paths (unit evidence) |

