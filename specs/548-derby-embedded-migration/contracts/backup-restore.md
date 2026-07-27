# Contract: Offline backup and restore (default embedded)

## Supported mode (FR-020)

**Offline only.** The product instance MUST be stopped for supported consistent full backup and restore of the default embedded repository.

The product MUST NOT claim a supported online/hot full backup for the default embedded repository in this release.

## Backup procedure (operator contract)

1. Stop CMS or DTS component cleanly.
2. Copy **all** files listed in product docs for the default repository data directory (and any required companion config files).
3. Store artifacts securely (same sensitivity as live repository).
4. Start instance when backup complete.

**Pre-migration product backup** (upgrade path FR-018a) MUST produce a consistent full-directory copy under upgrade control:

1. Ensure CMS/DTS (and Derby Network Server if still present) are stopped / offline-consistent.
2. Copy the resolved repository data directory plus documented companion config files using portable NIO path APIs.
3. Record size (and checksum if feasible) in upgrade logs without secrets.
4. Only then open the backup gate for migration.

There is no legacy full-repo snapshot API to reuse; implement this as part of the upgrade migrator (not only `PSUpgradeBackupTable` per-table copies).

## Restore procedure (operator contract)

1. Stop instance.
2. Replace repository files (and documented companion config) from backup artifact set.
3. Start instance.
4. Verify login / sample data access (CMS) or service health + sample reads (DTS).

## Documentation requirements (FR-011)

Docs MUST state for each supported OS family (Windows, Linux, macOS as applicable):

- Stop/start commands or service names
- Exact paths (or how to resolve them on a given install)
- What is included in the artifact set
- That online backup is **unsupported**
- Pre-migration gate behavior and external-backup confirmation meaning (`perc.migration.externalBackupConfirmed=true` primary UX)
- Post-migration Derby file location and cleanup steps (FR-019)
- CMS+DTS multi-component upgrade sequence (stop → migrate CMS → migrate DTS services → start)

