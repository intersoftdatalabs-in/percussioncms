# CMS + DTS upgrade sequence and mixed estate

## Recommended order

1. **Stop** CMS and all DTS instances that share the upgrade window.
2. **Verify offline backups** (product or external) for every component that still uses product-managed Derby.
3. **Upgrade / migrate CMS first** (embedded repository migrator + remaining CMS upgrade plugins).
4. **Migrate DTS services** that still use product-managed Derby (**per service** — metadata, forms, comments, etc. as applicable).
5. **Start** CMS, then DTS services.
6. **Smoke-test** CMS and each migrated DTS service.
7. **Optional:** schedule Derby residue cleanup after retention policy (FR-019).

Engineering checklist: `specs/548-derby-embedded-migration/checklists/upgrade-sequence.md`.

## Mixed estate (QC-020)

| Component | Backend | Migrator behavior |
|-----------|---------|-------------------|
| CMS | MySQL / MSSQL / other non-Derby | `SKIPPED_NON_DERBY` — connection keys unchanged |
| CMS | H2 already | `ALREADY_MIGRATED` |
| CMS | Product-managed Derby | Gate → pump → cutover |
| DTS service A | Derby | Migrate that service only |
| DTS service B | MySQL | Skip that service only |

Detection and cutover are **service-scoped** for DTS: migrating metadata must not rewrite forms (or other) configs.

## Operator checklist (dry-run friendly)

- [ ] Inventory backends (`rxrepository.properties`, each DTS datasource)
- [ ] Confirm stop/start runbooks for Windows and Unix
- [ ] External or product backup complete
- [ ] Set `perc.migration.externalBackupConfirmed=true` only if external backup verified
- [ ] Free disk ≥ guidance in [migration-sizing.md](./migration-sizing.md)
- [ ] Run upgrade; read durable migration reports
- [ ] Start and smoke-test
- [ ] Plan Derby residue cleanup (do not rush)

## Related

- [operator-backup-restore.md](./operator-backup-restore.md)
- [operator-migration-gate.md](./operator-migration-gate.md)
