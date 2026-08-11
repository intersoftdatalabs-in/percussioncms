# #548 — Derby embedded → multiuser H2 migration (operator docs)

This folder holds **operator-facing** notes for the default embedded repository migration from product-managed Apache Derby to multiuser H2.

|                                      Doc                                       |       Audience       |                          Contents                           |
|--------------------------------------------------------------------------------|----------------------|-------------------------------------------------------------|
| [am-i-affected.md](./am-i-affected.md)                                         | Operators / support  | Yes/no decision tree (SC-008)                               |
| [release-notes-8.2-derby-migration.md](./release-notes-8.2-derby-migration.md) | Release / docs       | Draft release notes (FR-012)                                |
| [fr-021-migration-window.md](./fr-021-migration-window.md)                     | Product / release    | GA +1 migration support window checklist                    |
| [operator-backup-restore.md](./operator-backup-restore.md)                     | Operators            | Offline backup/restore (CMS + DTS), stop/start, paths       |
| [operator-migration-gate.md](./operator-migration-gate.md)                     | Operators            | Pre-migration gate, external confirm, Derby residue cleanup |
| [operator-upgrade-sequence.md](./operator-upgrade-sequence.md)                 | Operators            | CMS+DTS sequence, mixed estate (MySQL/MSSQL + Derby DTS)    |
| [migration-sizing.md](./migration-sizing.md)                                   | Operators / capacity | Duration and disk sizing guidance                           |

**Spec / contracts (engineering):** `specs/548-derby-embedded-migration/`

**Product docs (operator-facing site):** `product-docs/8.2/getting-started/upgrade.md` (embedded Derby → H2 section) and install overview defaults.

**Tracking:** epic #548 · US6 residual #3065 · T038 human QA #2332 · QC-023 hard #2333 (closed)

**Primary UX for external backup confirmation (FR-018b):**

```text
-Dperc.migration.externalBackupConfirmed=true
```

or installer / upgrade property of the same name. Non-default; must be affirmative after a verified external backup.
