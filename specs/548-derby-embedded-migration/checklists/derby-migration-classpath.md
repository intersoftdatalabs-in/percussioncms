# Derby jars — migration window only (T066 / FR-021)

## Policy

- **New default**: H2 (`com.h2database:h2`) on CMS Jetty JDBC lib and DTS packaging.
- **Derby retained** through **GA + one subsequent product line** solely for:
  - Upgrade-time TableFactory **export** from product-managed Derby sources
  - Legacy `PSUpgradeDerby` in-place engine bumps on still-Derby trees
  - Migration-window support tooling

## Module notes

|         Module         |                 Derby scope                  |                            Notes                            |
|------------------------|----------------------------------------------|-------------------------------------------------------------|
| Parent `pom.xml`       | `dependencyManagement` keeps `derby.version` | Coordinates for migration classpath                         |
| `modules/TableFactory` | test + tools                                 | Export from Derby fixtures                                  |
| `modules/perc-ant`     | compile (`PSUpgradeDerby`, migration)        | **Must shade both Derby and H2** — installer is `java -jar perc-ant`; TableFactory `Class.forName` uses the system classloader, not Ant `ant.deps` child loaders (`jetty/base/lib/jdbc`). Missing H2 → import fails with `Unable to connect to database server: org.h2.Driver` after a successful Derby export. |
| `system`               | test (migration IT)                          | Runtime upgrade uses installer classpath                    |
| DTS services           | no live default                              | Defaults are H2; Derby only if customer still has derbydata |

## Field failure (2026-07-27 /opt/Percussion)

- Export: 209 tables OK (Derby present in perc-ant shade).
- Import: `PSJdbcDbmsDef:L:474` → `ClassNotFoundException` / `LinkageError` message `org.h2.Driver`.
- `upgrade.chain` had also run `deleteOldJdbcDrivers` (glob-deletes `h2-*.jar`) immediately before migration; re-run `install_jdbc_drivers` after that delete for disk-level recovery.

## After support window

1. Deprecation notice in release notes.
2. Remove Derby from packaging and migration entry points.
3. Close FR-021 capability.

