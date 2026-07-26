# CMS + DTS upgrade sequence (T065 / FR-017)

**Feature**: #548  
**Contract**: [migration-upgrade.md](../contracts/migration-upgrade.md)

## Recommended order

When **either or both** CMS and DTS still use product-managed Derby:

1. **Stop** CMS and all DTS Tomcat instances cleanly (Windows services / scripts).
2. **Backup gate** (FR-018):
   - Product offline backup of each repository tree **or**
   - `-Dperc.migration.externalBackupConfirmed=true` after verified external offline backup.
3. **Migrate CMS** (if Derby):
   - Triggered by upgrade installer: `PSMigrateEmbeddedRepository` / pre-upgrade plugin.
   - Report: `<cms-root>/rxconfig/Installer/migration-report-CMS.properties`
4. **Migrate each DTS service** independently (if that service has Derby data):
   - Triggered by `installDts.xml` via `PSMigrateDtsEmbeddedRepository` after legacy `PSUpgradeDerby` (if present).
   - Services: `percmetadata`, `perccomments`, `percfeeds`, `percforms`, `percmembership`, `percpolls`, `percakamaiqueuedata`
   - Report per service: `migration-report-DTS_<service>.properties`
5. **Start** CMS then DTS; verify login / service health.

## Mixed estate

|  CMS  | DTS service |    CMS migrator     |  DTS that service   |
|-------|-------------|---------------------|---------------------|
| Derby | Derby       | migrates            | migrates            |
| Derby | MySQL       | migrates            | `SKIPPED_NON_DERBY` |
| H2    | Derby       | `ALREADY_MIGRATED`  | migrates            |
| MySQL | H2          | `SKIPPED_NON_DERBY` | `ALREADY_MIGRATED`  |

## Dry-run checklist

- [ ] Inventory backends from `rxrepository.properties` and each DTS `perc-datasources.properties`
- [ ] Confirm disk free ≥ 3× largest Derby tree
- [ ] Stop all processes
- [ ] Run upgrade / migrators
- [ ] Confirm reports under `rxconfig/Installer/`
- [ ] Confirm live configs no longer list `jdbc:derby` for migrated components
- [ ] Confirm Derby residue still on disk under `Repository/` / `derbydata/` (FR-019)
- [ ] Start and smoke-test

## Operator CLI reminders

```text
# External backup confirm (CMS and DTS migrators honor the same property)
-Dperc.migration.externalBackupConfirmed=true
```

