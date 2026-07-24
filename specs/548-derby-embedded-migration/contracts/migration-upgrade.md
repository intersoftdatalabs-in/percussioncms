# Contract: Derby → new default migration on upgrade

## Preconditions

1. Component is CMS or a product-managed DTS service.  
2. Detection classifies repository as **product-managed Derby**.  
3. **Backup gate** satisfied (FR-018):
   - **(a)** Product-produced pre-migration offline backup completed successfully, **or**  
   - **(b)** Operator **affirmative** confirmation that a verified external backup exists (not silent default), via the **primary UX only**:
     - System/upgrade property: `perc.migration.externalBackupConfirmed=true`
     - Set by installer checkbox **or** CLI/JVM `-Dperc.migration.externalBackupConfirmed=true`
     - Must be affirmative and non-default; never inferred from silence
4. Sufficient disk space for target repository + temporary working space (precheck).  
5. No concurrent migration already running for the same component (exclusive lock file under install root via `FileChannel.tryLock`; second process fails with clear message).

## Happy path

1. Record outcome start (`MIGRATING`).  
2. Open source Derby **read-only** (embedded or networked ClientDriver, as installed).  
3. Create empty target repository; apply schema:
   - **CMS**: TableFactory  
   - **DTS**: Hibernate schema update and/or Liquibase with new-engine changesets (not TableFactory-only)  
4. Copy product-managed data (FK-safe order; **explicit primary keys**; CMS NEXTNUMBER re-sync).  
5. Validate (table-set equality, counts, boolean/LOB/NEXTNUMBER probes per [data-model.md](../data-model.md)).  
6. **Multi-file cutover** (all-or-nothing): rewrite every live config surface for the component (`rxrepository.properties`, Jetty `perc-ds.*`, DTS datasource props, start-script JVM options as applicable); durable write; on failure restore previous configs from backup artifact.  
7. Record `SUCCESS`.  
8. Subsequent starts use **only** new default (FR-013).  
9. Leave Derby files on disk until operator cleanup (FR-019).

## Failure path

| Failure | Required behavior |
|---------|-------------------|
| Backup gate not satisfied | Do not migrate; `BLOCKED_BACKUP_GATE`; clear operator message |
| I/O / disk full / validation fail | Abort; Derby config + data intact; no live cutover to partial target; `FAILED` + reason |
| Already on new default | Idempotent skip; no double migration |
| Non-Derby backend | `SKIPPED_NON_DERBY`; no config rewrite |

## Multi-component upgrade sequence (CMS + DTS)

Recommended operator order when **both** CMS and DTS use product-managed Derby (or either does):

1. **Stop** CMS and all DTS services (clean shutdown).  
2. Satisfy backup gate **per component** being migrated (or one estate-wide offline backup covering all repos if documented).  
3. **Migrate CMS** (if Derby) → verify CMS outcome `SUCCESS` or `SKIPPED_*`.  
4. **Migrate each DTS service** independently (if that service is Derby) → log per-service outcome.  
5. **Start** components; verify health.

Mixed estate (e.g. CMS Derby + DTS MySQL): only Derby components migrate; external configs unchanged. Detection and outcomes are **per component** (FR-017).

## Support window (FR-021)

- Migration capability required for: **GA release introducing new default** + **one subsequent product line**.  
- Deprecation notice required before removal.  
- After window: product lines not required to migrate from Derby.

## Explicit non-promises

- No supported downgrade to live Derby after `SUCCESS`.  
- No guarantee for non-product objects inside Derby.  
- No multi-node shared embedded repository.  
- No Derby Network Server / DRDA remote access on the new default (document in release notes).  
