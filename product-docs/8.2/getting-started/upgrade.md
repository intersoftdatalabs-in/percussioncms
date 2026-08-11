---
id: upgrade-overview
title: Upgrade Overview
description: How to upgrade to Percussion CMS 8.2
version: "8.2"
order: 30
tags: [upgrade, admin]
---

# Upgrade Overview

This page describes upgrade planning into Percussion CMS **8.2**. Always follow the release notes
and installer guidance for the exact build you are deploying.

## Before you upgrade

1. **Inventory** current CMS and DTS versions, OS, JDK, and database platform.
2. **Read** the target release notes for breaking changes, property renames, and required JDK level (**JDK 21** for 8.2).
3. **Back up**:
   - Install tree configuration (Jetty base, `rxconfig`, server properties, custom extensions)
   - Database(s)
   - Published output if you need a rollback snapshot of delivery files
4. **Schedule** maintenance: upgrades typically require a CMS (and often DTS) outage window.
5. **Validate** on a lower environment that mirrors production data shape before production.

## Recommended paths

| From | Guidance |
|------|----------|
| **8.1.x** | Preferred path into 8.2. Apply latest 8.1.x security patches first when possible, then run the 8.2 installer upgrade mode against a clone of production. |
| **Older 8.x / CM1 lineage** | Plan multi-hop upgrades (stable intermediate releases) rather than jumping multiple major lines at once. Engage Intersoft support for complex multi-version leaps. |
| **Customized installs** | Catalog custom Java extensions, XSL/variants, WebUI overlays, and third-party JARs; recompile against the 8.2 toolchain (JDK 21) and retest. |

## Upgrade steps (high level)

1. Put the system in maintenance (stop traffic, freeze content freezes if required by policy).
2. Stop CMS and DTS services cleanly.
3. Take final backups.
4. Run the **8.2 installer** in upgrade/update mode for the existing install root (or follow the package-specific silent upgrade flags).
5. Allow schema / tablefactory and package install steps to complete; capture logs.
6. Re-apply environment-specific configuration only when the installer does not preserve it (custom ports, reverse proxy headers, TLS keystores, external auth).
7. Start services; run smoke tests (login, open Site, edit page, publish sample, DTS widgets if used).
8. Monitor logs for extension load failures and missing resources.

## Default embedded repository (Derby → H2)

Apache Derby is retired upstream. Percussion CMS and DTS **new installs** default to multiuser **H2** (no Derby Network Server / port **1527** on the default path).

| Your repository backend | On 8.2 upgrade |
|-------------------------|----------------|
| **Product-managed Derby** (embedded or ClientDriver) | Automatic **Derby → H2** migration after a required **backup gate** |
| **Already on H2** | No re-migration |
| **MySQL / SQL Server** (or other external enterprise DB) | **Unchanged** — no embedded migrator pump/cutover |

If you are on product-managed Derby:

1. Schedule a maintenance window sized for repository volume.
2. Satisfy the backup gate: product offline pre-migration backup **or** a verified external offline backup plus  
   `-Dperc.migration.externalBackupConfirmed=true`.
3. Complete the supported upgrade; confirm the durable migration report outcome (for example under `rxconfig/Installer/migration-report-CMS.properties`).
4. Keep pre-migration Derby files until you deliberately clean them up (the product does not auto-delete residue).

New defaults do **not** promise Derby Network Server / DRDA remote access. Steady-state backup for the default embedded repository remains **offline only**.

Engineering release-note draft and support FAQ (Am I affected?): see repository folder  
`docs/ai-generated/tasks/548-derby-embedded-migration/` (issue **#548**).

## Post-upgrade smoke tests

- [ ] Login and role-based navigation work.
- [ ] Finder shows Sites and folders; open a representative page and asset.
- [ ] Workflow transitions complete without server errors.
- [ ] Publish to a non-production location succeeds.
- [ ] REST health / authenticated sample call succeeds if you integrate headlessly.
- [ ] DTS endpoints used by the site respond as before.

## Rollback notes

True in-place rollback depends on how far schema and package updates progressed. Prefer:

1. Restore database from pre-upgrade backup.
2. Restore install tree (or reinstall previous version into a clean path and restore config).
3. Restore publish targets if delivery files were partially overwritten.

Document your environment-specific rollback runbook before production cutover.

## Related

- [Installation Overview](id:install-overview)
- [Server operations](id:admin-server-ops)
- [Publishing](id:admin-publishing)
