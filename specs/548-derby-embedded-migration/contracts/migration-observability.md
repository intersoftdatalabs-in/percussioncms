# Contract: Migration observability (FR-017)

## Durable migration report (required)

In addition to logs, each component migration MUST write a **durable report file** under the install tree (suggested path: `<install-root>/rxconfig/Installer/migration-report-<component>.properties` or `.log` — exact name frozen at implement time and documented in operator docs).

The file MUST survive process exit and contain at least: `outcome`, `backupGate`, `sourceBackend`, `targetBackend`, `finishedAt`, `failureReason` (if any). No secrets.

## Required operator-visible signals

For each CMS/DTS component upgrade, logs **and** the durable migration report MUST allow support to answer:

|           Question            |                                                           Required signal                                                           |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Did migration run?            | Explicit start line with component id                                                                                               |
| Outcome?                      | One of: `SUCCESS`, `FAILED`, `SKIPPED_NON_DERBY`, `BLOCKED_BACKUP_GATE`, `ALREADY_MIGRATED` (canonical enum; data-model must match) |
| Backup gate?                  | `PRODUCT_BACKUP` or `EXTERNAL_CONFIRM` or not satisfied                                                                             |
| Active backend after upgrade? | Logged backend label + driver class (no secrets)                                                                                    |
| Failure reason?               | Human-readable cause; **no passwords/tokens**                                                                                       |

## Log hygiene

- Never log `PWD` or full JDBC URLs embedding passwords.
- Paths may be logged.
- Prefer structured key=value or clearly tagged lines for support grep.

## Idempotency signal

Re-running upgrade after `SUCCESS` MUST log skip/idempotent outcome without rewriting live data destructively.
