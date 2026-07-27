# Contract: Effective `rxrepository.properties`

## Location

```text
{install.dir}/rxconfig/Installer/rxrepository.properties
```

Written or updated on **new install** by the installer when applying database target configuration. On **upgrade**, existing values for backend identity must be preserved by this feature.

## Canonical keys (CMS repository)

|            Key             |                Description                |                                                            Example (illustrative)                                                             |
|----------------------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_BACKEND`               | Backend alias                             | `DERBY`, `MYSQL`, `MSSQL`, `ORACLE`                                                                                                           |
| `DB_DRIVER_NAME`           | Logical/JDBC driver name                  | `derby`, `mysql`, `sqlserver`, `oracle:thin`                                                                                                  |
| `DB_DRIVER_CLASS_NAME`     | JDBC `Driver` class                       | `org.apache.derby.jdbc.EmbeddedDriver`, `org.mariadb.jdbc.Driver`, `com.microsoft.sqlserver.jdbc.SQLServerDriver`, `oracle.jdbc.OracleDriver` |
| `DB_SERVER`                | Server identity string (backend-specific) | See samples                                                                                                                                   |
| `DB_NAME`                  | Database name                             | `percussion` or empty (Oracle often empty)                                                                                                    |
| `DB_SCHEMA`                | Schema                                    | `CMDB`, `dbo`, Oracle schema name                                                                                                             |
| `UID`                      | User                                      |                                                                                                                                               |
| `PWD`                      | Password                                  |                                                                                                                                               |
| `PWD_ENCRYPTED`            | `Y` or `N`                                | Install may encrypt after write via existing steps                                                                                            |
| `DSCONFIG_NAME`            | Datasource config name                    | `PercussionData`                                                                                                                              |
| `DB_SSL_ENABLED`           | SSL enabled flag                          | `true`/`false`                                                                                                                                |
| `DB_SSL_VERIFY`            | Verify server cert                        | `true`/`false`                                                                                                                                |
| `DB_SSL_ALLOW_SELF_SIGNED` | Allow self-signed                         | `true`/`false`                                                                                                                                |

Additional keys may exist historically for tools/notifications; this feature does not require them for new-install targeting.

## Backend-specific `DB_SERVER` shapes (reference)

Integrators supplying **dbprops** own the final `DB_SERVER` string. Structured CLI composition will produce equivalent forms:

|    Backend    |                           Typical `DB_SERVER` form                            |
|---------------|-------------------------------------------------------------------------------|
| Derby         | `CMDB;create=true` (product default)                                          |
| MySQL/MariaDB | `//host:port/dbname?useUnicode=yes&characterEncoding=UTF-8&...`               |
| SQL Server    | `//host:port;databaseName=dbname;encrypt=...;trustServerCertificate=...`      |
| Oracle thin   | `@host:port:serviceOrSid` (document exact recommended sample in shipped file) |

## Consumers (must keep working)

- `com.percussion.ant.install.PSConfigureDatasource`
- `com.percussion.ant.install.PSMakeLasagna`
- `com.percussion.ant.install.PSExecSQLStmt` and related SQL install actions
- `com.percussion.tablefactory.PSJdbcDbmsDef` / table factory
- Runtime container datasource configuration derived during install

## Stability

Key names are a **long-standing product contract**. This feature must not rename keys. New optional SSL keys are additive and already partially used by the fresh-install ANT target.
