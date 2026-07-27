# #1500 — PostgreSQL as supported CMS repository backend

**Issue:** [GitHub #1500](https://github.com/intersoftdatalabs-in/percussioncms/issues/1500)  
**Does not replace** default embedded **H2** (#548).

## Locked labels (foundation)

| Surface | Value |
|---------|--------|
| `DB_BACKEND` | `POSTGRES` |
| Installer `db.type` | `postgresql` (alias: `postgres`) |
| JDBC driver name | `postgresql` |
| JDBC driver class | `org.postgresql.Driver` |
| Maven coordinates | `org.postgresql:postgresql:${postgresql.version}` (parent POM; **42.7.7** at foundation) |
| Hibernate dialect key | `postgresql` → `org.hibernate.dialect.PostgreSQLDialect` |
| Default TCP port | **5432** |
| Default schema | **`public`** when omitted |
| Product `DB_SERVER` form | `//host:port/database` (same shape as MySQL-style server field) |
| Full JDBC URL | `jdbc:postgresql://host:port/database` |

## Foundation PR scope (this work)

- Platform constants / `isPostgres` / URL map
- TableFactory `DataTypeMap for="POSTGRES"`
- Installer resolver accepts POSTGRES / postgresql
- Sample `rxrepository.postgresql.properties`
- Driver packaging into `jetty/base/lib/jdbc/`
- Hibernate dialect registration in server-beans copies
- Unit tests (utils + perc-distribution-tree)

## Install matrix smoke (Layer 1)

Ephemeral Docker cells for **CMS and DTS** per backend (silent install → start → probe → destroy):

```bash
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql
python3 docker/scripts/matrix-install-smoke.py --product cms,dts --db h2,postgresql
```

Details: `docker/README.md`, `docker/matrix/`, `docker/scripts/matrix-install-smoke.py`.

Playwright gate (Layer 2, after `--keep`): `modules/perc-qa-automation/frontend/tests/install.spec.js`.

## Later PRs

- CI workflow wiring for the matrix harness (subset on PR, fuller nightly)
- Expand perc-qa-automation full suite against matrix cells (on demand + CI)
- `sys_DatabaseFunctionDefs.xml` postgres function rows parity
- Product SQL branches that still assume MySQL/MSSQL/Oracle only
- DTS service default external-Postgres as a first-class install target (hints only today)
- Support matrix / operator docs for backup (customer-owned Postgres ops)

## Out of scope

- Replacing H2 embedded default
- Certifying Aurora / forks as separate products (document compatibility later)
