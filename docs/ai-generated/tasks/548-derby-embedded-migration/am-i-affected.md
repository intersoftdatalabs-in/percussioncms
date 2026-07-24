# Am I affected by the Derby → H2 change?

**Audience:** Operators, support, and upgrade planners (SC-008 / US6 / T092).  
**Goal:** Answer “Am I affected?” with a **yes/no path** without engineering escalation for the common case.

---

## 60-second answer

| Your CMS / DTS repository backend | Affected by automatic Derby → H2 migration? |
|-----------------------------------|-----------------------------------------------|
| **Apache Derby** (product default / product-managed embedded or ClientDriver) | **Yes** — upgrade will migrate after the backup gate |
| **H2** (already the live default after a prior successful migration) | **No** re-migration (`ALREADY_MIGRATED`) |
| **MySQL** | **No** |
| **Microsoft SQL Server** | **No** |
| **Oracle** (if configured as external enterprise DB) | **No** (same “external non-Derby” skip class) |

If you never chose an external RDBMS at install and never changed repository properties, you are almost certainly on **product-managed Derby** (or already on **H2** after migrating) — check the property files below.

---

## How to tell which backend you use

### CMS

Open (under the CMS install root):

```text
rxconfig/Installer/rxrepository.properties
```

Also check Jetty datasource labels when present:

```text
jetty/base/etc/perc-ds.properties
```

Look at **`DB_DRIVER_NAME`** / driver class (names may vary slightly by age of install):

| Signal | Backend |
|--------|---------|
| `derby`, `org.apache.derby.jdbc.*` | **Derby** → **affected** |
| `h2`, `org.h2.Driver` | **H2** → not re-migrated |
| `mysql`, MySQL connector class | **MySQL** → not affected |
| `sqlserver` / `mssql`, SQL Server driver | **SQL Server** → not affected |

Networked product-managed Derby may show a ClientDriver URL with host/port (historically **1527**). That is still **Derby** for migration purposes.

### DTS

Each service has its own datasource properties (e.g. under Tomcat `conf/perc` or service `WEB-INF`). Check **driver name / class** the same way. Migration is **per service**: one service on MySQL and another on Derby only migrates the Derby service.

---

## Decision tree

```text
Start
  │
  ├─ Is DB driver MySQL, SQL Server, or Oracle (external)?
  │     YES → Not affected. Proceed with normal 8.2 upgrade. Stop.
  │
  ├─ Is DB driver H2?
  │     YES → Already on new default. No Derby migration. Stop.
  │
  └─ Is DB driver Derby (embedded or product-managed ClientDriver)?
        YES → Affected. Continue ↓
```

### If affected (Derby)

1. **Schedule a maintenance window** sized for repository volume ([migration-sizing.md](./migration-sizing.md)).
2. **Stop** CMS/DTS as documented ([operator-upgrade-sequence.md](./operator-upgrade-sequence.md)).
3. **Satisfy the backup gate** before migration starts:
   - Product offline backup during upgrade, **or**
   - Verified external offline backup **and**  
     `-Dperc.migration.externalBackupConfirmed=true`  
     ([operator-migration-gate.md](./operator-migration-gate.md)).
4. Run the **supported upgrade**. Confirm durable migration report outcome (`SUCCESS`).
5. Smoke-test login / key workflows / DTS health.
6. **Keep Derby residue** until policy allows cleanup; product does **not** auto-delete it.

### If not affected (external DB)

- No special Derby migration steps.
- Do **not** set `perc.migration.externalBackupConfirmed` unless you intentionally have a Derby component elsewhere in a mixed estate.
- Still follow normal product upgrade backups for your external RDBMS (customer-owned ops).

### Mixed estate

Example: CMS on MySQL, DTS metadata still on Derby.

- CMS upgrade: `SKIPPED_NON_DERBY`.
- DTS metadata: migrate that service only (gate + sequence still apply).

See [operator-upgrade-sequence.md](./operator-upgrade-sequence.md).

---

## Support FAQ

**Q: Will new installs still use Derby?**  
A: No. Defaults use **H2**.

**Q: Do I need to buy MySQL/SQL Server to leave Derby?**  
A: No. Migration and default H2 are product-managed (FR-014).

**Q: Can I still open Derby Network Server on 1527 after upgrade?**  
A: New defaults do **not** promise DRDA/Network Server. After successful cutover, live store is H2. See [release-notes-8.2-derby-migration.md](./release-notes-8.2-derby-migration.md).

**Q: How long will upgrades keep migrating Derby?**  
A: GA that introduces H2 **plus one subsequent product line**, then removal after deprecation (FR-021). See [fr-021-migration-window.md](./fr-021-migration-window.md).

**Q: Migration failed — is my data gone?**  
A: On failure / blocked gate, live config stays on Derby and source data is not discarded for a partial H2 cutover. Use logs + durable report; restore from backup if needed.

---

## Related

- [release-notes-8.2-derby-migration.md](./release-notes-8.2-derby-migration.md)
- [operator-backup-restore.md](./operator-backup-restore.md)
- Engineering contracts: `specs/548-derby-embedded-migration/contracts/`
