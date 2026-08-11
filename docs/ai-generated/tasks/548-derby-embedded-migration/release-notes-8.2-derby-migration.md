# Release notes draft — Derby retirement and multiuser H2 default (8.2)

**Status:** Ready for product / release packaging copy (US6 / T091; engineering docs on `main` via #1504 + residual #3065).  
**Tracking:** [GitHub #548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)  
**Engine:** Multiuser **H2** is the locked default embedded repository (bake-off passed; HSQL not required).

Use this text as the source for official release notes and upgrade guides. Operator deep-dives live in this folder (links below). Product-line upgrade overview also summarizes the embedded-repository change under `product-docs/8.2/getting-started/upgrade.md`.

---

## Summary

Apache Derby has been **retired upstream** (project read-only; no further releases). Percussion CMS and the Delivery Tier Suite (DTS) historically used product-managed **embedded Derby** as the zero-admin default repository.

Starting with the 8.2 line that ships this change:

|                  Area                   |                                                         Behavior                                                          |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| **New installs (defaults)**             | CMS and DTS provision **multiuser H2**, not Derby. No Derby Network Server / port **1527** required for the default path. |
| **Upgrades from product-managed Derby** | Automatic **Derby → H2** migration on the supported upgrade path after a **backup gate**.                                 |
| **MySQL / SQL Server**                  | **Unchanged.** No embedded migration; connection identity is not rewritten.                                               |
| **Steady-state backup**                 | **Offline only** for the default embedded repository.                                                                     |

---

## Who is affected?

See the decision tree: **[am-i-affected.md](./am-i-affected.md)** (SC-008).

|                                      Estate                                       |    Affected?     |                                 Action                                 |
|-----------------------------------------------------------------------------------|------------------|------------------------------------------------------------------------|
| CMS/DTS on **default Derby** (embedded or product-managed networked ClientDriver) | **Yes**          | Plan a maintenance window; satisfy backup gate; run supported upgrade. |
| CMS/DTS already on **H2** after a prior migration                                 | No re-migration  | Outcome `ALREADY_MIGRATED`.                                            |
| CMS/DTS on **MySQL** or **Microsoft SQL Server**                                  | **No**           | Upgrade as usual; no Derby migrator pump/cutover.                      |
| Custom non-product Derby schemas                                                  | Best-effort only | Product guarantees cover product-owned schemas.                        |

---

## What operators should expect on upgrade (Derby default)

1. **Stop** CMS and DTS instances for the upgrade window ([operator-upgrade-sequence.md](./operator-upgrade-sequence.md)).
2. **Backup gate (required):** either product offline pre-migration backup **or** explicit external confirmation  
   `-Dperc.migration.externalBackupConfirmed=true`  
   after a verified external offline backup ([operator-migration-gate.md](./operator-migration-gate.md)).
3. **Automatic migration** (TableFactory export/import + multi-file config cutover). Progress and outcome appear in upgrade logs and a durable report under the install tree (e.g. `rxconfig/Installer/migration-report-CMS.properties`).
4. On **SUCCESS**, live configs point at **H2**. Pre-migration **Derby files are retained** until you deliberately clean them up (FR-019)—not auto-deleted.
5. On **FAILED** / **BLOCKED_BACKUP_GATE**, live config remains Derby; source data is not discarded for cutover.

Capacity planning: [migration-sizing.md](./migration-sizing.md). Offline backup/restore: [operator-backup-restore.md](./operator-backup-restore.md).

---

## Multiuser and locking

Default H2 installs are positioned for multiuser editorial and service workloads. Product acceptance includes **≥10 concurrent CMS editor sessions** with product lock semantics and DTS concurrent write smoke at default pool levels (no silent lost updates in the harness suite). High-throughput multi-server deployments continue to use **MySQL / SQL Server**.

---

## Intentional breaks / no longer promised

|              Topic              |                                                                                                                                        Detail                                                                                                                                        |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Derby Network Server / DRDA** | New defaults **do not** promise remote DRDA access on port **1527**. Default H2 is in-process file-mode multiuser (`AUTO_SERVER` as designed for single deployment unit). Remote multi-host shared-file clustering remains out of product scope (same class as prior Derby default). |
| **Online/hot full backup**      | Not supported for the default embedded repository. Stop the instance for consistent full backup and restore.                                                                                                                                                                         |
| **Downgrade to live Derby**     | Not supported after successful cutover. Recovery from failed migration uses preserved Derby + backups before cutover.                                                                                                                                                                |

---

## Derby migration support window (FR-021 / SC-012)

Automatic migration from product-managed Derby is supported for:

1. The **GA product line** that introduces H2 as the default embedded repository, and
2. **One subsequent product line** after that GA.

Before removal of Derby migration:

- Release notes **must** carry a **deprecation notice** for at least one product line.
- Customers still on Derby must upgrade **while migration is still supported**.

Tracking checklist on issue **#548** (and [fr-021-migration-window.md](./fr-021-migration-window.md)). After the window, Derby jars and migration entry points are removed from the product.

---

## Engineering / verification (summary)

Shipped under #548 work packages on **`main`**:

| Slice | PRs / issues |
|-------|----------------|
| Foundation → multiuser | #1494–#1499 |
| US6 release comms + Phase 9 packaging notes | #1504 |
| Residual QC freeze (inventory re-run, gate docs) | #3065 |
| QC-023 hard package install on H2 | **#2333** closed (QA Passed) |
| T038 full multi-OS install/login smoke | **#2332** open (human QA) |

Operators do **not** need the residual trackers to apply this release-note content. Support FAQ: [am-i-affected.md](./am-i-affected.md).

---

## Document map

|                              Doc                               |                Purpose                |
|----------------------------------------------------------------|---------------------------------------|
| [am-i-affected.md](./am-i-affected.md)                         | Support FAQ decision tree             |
| [operator-upgrade-sequence.md](./operator-upgrade-sequence.md) | CMS then DTS order; mixed estate      |
| [operator-migration-gate.md](./operator-migration-gate.md)     | Backup gate and residue cleanup       |
| [operator-backup-restore.md](./operator-backup-restore.md)     | Offline backup/restore                |
| [migration-sizing.md](./migration-sizing.md)                   | Disk and duration guidance            |
| [fr-021-migration-window.md](./fr-021-migration-window.md)     | Support window checklist              |
| Spec / contracts                                               | `specs/548-derby-embedded-migration/` |

