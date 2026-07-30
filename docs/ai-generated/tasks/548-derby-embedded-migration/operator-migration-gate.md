# Pre-migration gate, external confirmation, Derby residue

## Why a gate exists

Upgrade will **not** pump Derby → H2 until a backup obligation is satisfied (FR-018). This protects operators from irreversible cutover without a recovery path.

## How the gate is satisfied

Exactly one of:

### A. Product offline backup (FR-018a) — default on upgrade when offline

When the CMS/DTS is **stopped**, the supported upgrade path **automatically** takes a full-directory offline copy under `PreInstall/migration-backup/` (CMS) or `PreInstall/dts-migration-backup/` (DTS) before the Derby→H2 pump. Operators do **not** need to set `perc.migration.externalBackupConfirmed` for a normal offline upgrade.

|       Rule        |                                                                                                                               Detail                                                                                                                               |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Service running   | **Install/upgrade fails hard** for CMS (`PSCheckRunningServer` + migrate task) and DTS (`PSCheckRunningDtsServer` Production/Staging + migrate task) — stop instances first                                                                                        |
| Service stopped   | Product offline backup runs automatically (FR-018a)                                                                                                                                                                                                                |
| Stale Derby locks | After offline confirmation, upgrade **removes** leftover `db.lck` / `dbex.lck` (and similar engine lock markers) from the live repository tree, and **never archives** them into the product backup — so restore cannot reintroduce files that block clean startup |
| Online/hot backup | Unsupported — stop first (FR-020)                                                                                                                                                                                                                                  |

### B. External backup confirmation (FR-018b) — primary non-product UX

After **you** have verified an external offline backup:

```bash
# Example: JVM / upgrade system property
-Dperc.migration.externalBackupConfirmed=true
```

|    Rule     |                                  Detail                                   |
|-------------|---------------------------------------------------------------------------|
| Affirmative | Must be explicitly `true` / yes / 1 — not default                         |
| Non-silent  | Logged on the durable migration report (`backupGate=EXTERNAL_CONFIRM`)    |
| No secrets  | Confirmation flag is not a password; do not put secrets in property names |

If neither A nor B is satisfied, outcome is **`BLOCKED_BACKUP_GATE`**: no pump, no cutover, Derby config unchanged.

## Outcomes operators may see

|        Outcome        |                                       Meaning                                       |
|-----------------------|-------------------------------------------------------------------------------------|
| `SUCCESS`             | Cutover complete; live configs point at H2                                          |
| `ALREADY_MIGRATED`    | Already H2 — no work                                                                |
| `SKIPPED_NON_DERBY`   | MySQL/MSSQL/other external — untouched                                              |
| `BLOCKED_BACKUP_GATE` | Gate not open                                                                       |
| `FAILED`              | Error after gate (disk, lock, source, validation, …). **Live config remains Derby** |

Durable report (CMS example):

```text
rxconfig/Installer/migration-report-CMS.properties
```

DTS reports are per service under the DTS install tree (see migration observability contract).

## After SUCCESS — Derby residue (FR-019)

- Migrator **does not auto-delete** Derby data files or Network Server install bits.
- Residue may remain under the previous repository path for operator-controlled cleanup.
- Cleanup is optional and should wait until:
  1. Post-upgrade smoke tests pass,
  2. A post-migration H2 offline backup exists,
  3. Rollback plan no longer requires Derby files.

Document retention period per your change policy, then remove only after sign-off.

## Failure behavior (FR-008)

On `FAILED` / `BLOCKED_*`:

- No live cutover to a partial H2 store
- Source Derby config remains active
- Report includes a redacted failure reason

See failure-injection coverage in `PSMigrationFailureInjectionTest` (T047 / SC-004).
