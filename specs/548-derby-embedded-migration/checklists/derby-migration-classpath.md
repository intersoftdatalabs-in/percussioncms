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
| `modules/perc-ant`     | compile (`PSUpgradeDerby`, migration)        | Installer tasks need EmbeddedDriver at upgrade              |
| `system`               | test (migration IT)                          | Runtime upgrade uses installer classpath                    |
| DTS services           | no live default                              | Defaults are H2; Derby only if customer still has derbydata |

## After support window

1. Deprecation notice in release notes.
2. Remove Derby from packaging and migration entry points.
3. Close FR-021 capability.

