# #548 — Derby embedded → multiuser H2 migration (operator docs)

This folder holds **operator-facing** notes for the default embedded repository migration from product-managed Apache Derby to multiuser H2.

|                              Doc                               |       Audience       |                          Contents                           |
|----------------------------------------------------------------|----------------------|-------------------------------------------------------------|
| [operator-backup-restore.md](./operator-backup-restore.md)     | Operators            | Offline backup/restore (CMS + DTS), stop/start, paths       |
| [operator-migration-gate.md](./operator-migration-gate.md)     | Operators            | Pre-migration gate, external confirm, Derby residue cleanup |
| [operator-upgrade-sequence.md](./operator-upgrade-sequence.md) | Operators            | CMS+DTS sequence, mixed estate (MySQL/MSSQL + Derby DTS)    |
| [migration-sizing.md](./migration-sizing.md)                   | Operators / capacity | Duration and disk sizing guidance                           |

**Spec / contracts (engineering):** `specs/548-derby-embedded-migration/`

**Primary UX for external backup confirmation (FR-018b):**

```text
-Dperc.migration.externalBackupConfirmed=true
```

or installer / upgrade property of the same name. Non-default; must be affirmative after a verified external backup.
