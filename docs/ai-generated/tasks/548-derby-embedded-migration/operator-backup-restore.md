# Offline backup and restore — default embedded repository

**Supported mode: offline only.** Percussion CMS does **not** claim a supported online/hot full backup for the default embedded repository (Derby before migration, H2 after).

## When to use this procedure

- Pre-migration full backup (recommended before any upgrade that may run the Derby → H2 migrator)
- Disaster recovery of the embedded repository
- Post-migration rollback to a known good tree (from product or external backup)

## 1. Stop the instance

| Platform | CMS (typical) | DTS (typical) |
|----------|---------------|---------------|
| **Linux / macOS** | Stop via service unit or install scripts under the CMS root (e.g. `./bin/RhythmyxServer` stop / product service). Ensure Jetty CMS process is not running. | Stop each DTS Tomcat instance (service, `shutdown.sh`, or product DTS scripts). |
| **Windows** | Stop Windows service or stop scripts under the CMS install root. Confirm no `java` process holds repository files. | Stop DTS Windows service(s) / Tomcat. |

Also stop **Derby Network Server** if still used for product-managed networked Derby (`//host:port/...` in `rxrepository.properties`).

**Do not** copy repository files while the process is live — results are unsupported and may be inconsistent.

## 2. What to include

### CMS

| Include | Typical relative path under install root |
|---------|------------------------------------------|
| Repository data directory | `Repository/` (or path resolved from `DB_SERVER` when using file-style URLs) |
| Repository config | `rxconfig/Installer/rxrepository.properties` |
| Jetty datasource labels (when present) | `jetty/base/etc/perc-ds.properties` and/or `jetty/base/modules/perc-ds/etc/perc-ds.properties` |

Product pre-migration backup (upgrade path) writes under:

```text
PreInstall/migration-backup/<timestamp>/
  repository-data/     # copy of repository tree
  companion-config/    # e.g. rxrepository.properties
```

### DTS (per service)

Each default embedded DTS service has its own datasource config and data directory under the DTS server root (see service-specific `database.properties` / product layout). Backup **per service** that still uses product-managed Derby:

- Service config that points at the embedded store
- The service’s repository/data directory

Mixed estate: if CMS is MySQL/MSSQL and only some DTS services are Derby, backup **only** the Derby services you will migrate.

## 3. Copy procedure

1. Confirm processes are stopped.
2. Copy the trees above with ordinary file tools (`cp -a`, `robocopy /E`, Finder, etc.). Preserve permissions where the OS supports them.
3. Store artifacts with the same sensitivity as the live repository (credentials may appear in companion configs — protect backups).
4. Record size and date in your change ticket.

## 4. Restore procedure

1. Stop the instance (same as backup).
2. Replace repository data and companion configs from the backup set (do not mix partial trees from different times).
3. Start the instance.
4. Verify: CMS login + sample content; DTS service health + sample reads for each restored service.

## 5. Unsupported

- Online / hot full backup of the default embedded store
- Incremental embedded-engine snapshots that the product does not document
- Relying on auto-delete of post-migration Derby residue (see [operator-migration-gate.md](./operator-migration-gate.md))

## Related

- Spec contract: `specs/548-derby-embedded-migration/contracts/backup-restore.md`
- Gate and cleanup: [operator-migration-gate.md](./operator-migration-gate.md)
