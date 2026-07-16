# Contract: Installer Database Target Input (New Install)

## Purpose

Define how integrators supply a non-Derby (or explicit Derby) repository target when performing a **new** command-line CMS install.

## Invocation surface

### A. Primary — properties file (issue #949)

```text
java [jvm-args] -Ddbprops=/absolute/or/relative/path/to/repo.properties \
  -jar PercussionCMS.jar /path/to/install/root
```

Also accepted as CLI option (same semantics):

```text
java -jar PercussionCMS.jar /path/to/install/root --dbprops=/path/to/repo.properties
```

**Path resolution**: Relative paths resolve against the process working directory. Missing/unreadable path → install aborts before repository setup with an error that includes the path (not file contents).

### B. Secondary — structured flags / env (existing code, retained)

```text
java -jar PercussionCMS.jar /path/to/install/root \
  --db.type=mysql \
  --db.host=db.example.com \
  --db.port=3306 \
  --db.name=percussion \
  --db.schema= \
  --db.user=cms \
  --db.password=*** \
  [--db.ssl.enabled=true] [--db.ssl.verify=true] [--db.ssl.allowSelfSigned=false]
```

Env file:

```text
--db.config.env.file=/path/to/db.env
```

Env file lines: `KEY=VALUE`, `#` comments, blank lines ignored. Keys may be logical (`db.host`) or env-style (`DB_HOST` / `PERC_DB_HOST`).

### C. Default

If neither A nor B supplies a non-Derby type, install uses **embedded Derby** (current default).

## Precedence (highest first)

1. `--dbprops` / `-Ddbprops` file contents for repository identity fields  
2. CLI `--db.*` (and env-style aliases on CLI)  
3. Values from `--db.config.env.file`  
4. Process environment variables  
5. Built-in defaults (`db.type=derby`, SSL defaults true/true/false)

Mixing: If `dbprops` is set, structured keys may still supply SSL-only overrides when not present in the file (implementation may document exact merge rules; identity fields come from the file).

## Properties file schema

See [rxrepository-properties.md](rxrepository-properties.md) for keys. Minimum non-Derby example:

```properties
DB_BACKEND=MYSQL
DB_SERVER=//db.example.com:3306/percussion?useUnicode=yes&characterEncoding=UTF-8
DB_NAME=percussion
DB_SCHEMA=
DB_DRIVER_NAME=mysql
DB_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
UID=cms
PWD=changeit
PWD_ENCRYPTED=N
DSCONFIG_NAME=PercussionData
```

## Supported `DB_BACKEND` / `db.type` values

| File `DB_BACKEND` | Structured `db.type` | Notes |
|-------------------|----------------------|-------|
| `DERBY` | `derby` | Default |
| `MYSQL` | `mysql` | MySQL or MariaDB-compatible server |
| `MSSQL` | `sqlserver` | Microsoft SQL Server |
| `ORACLE` | `oracle` | Oracle thin (recommended sample) |

Any other value → validation error listing allowed values.

## Error contract

| Condition | Behavior |
|-----------|----------|
| Missing install path | Message; non-success exit |
| dbprops path missing/unreadable | Fail before schema; message includes path |
| Required fields missing | Fail; list missing logical keys (never values of secrets) |
| Unknown backend | Fail; list allowed backends |
| Connectivity failure | Fail after props write attempt / before or during pre-schema validate; user-readable SQL/driver message; **no password** |
| Missing JDBC driver JAR | Fail with guidance to place driver in documented `jetty/base/lib/jdbc` location |

## Success contract

- Exit success only if install chain completes.
- Effective `{install}/rxconfig/Installer/rxrepository.properties` reflects selected backend and non-secret identity fields.
- Upgrade invocations without new-install mode do not require this contract and must not reset backend to Derby.

## Versioning

Backward compatible additive contract: existing Derby-only installs with no flags remain valid. New flags are optional.
