# Docker Compose Dev Runtime (CMS-DTS + MySQL 8)

## Agent-friendly command interface

Use `./docker/scripts/perc-devctl.py` for concise pass/fail output with full logs saved to `docker/logs/`.

Each command prints a single summary line:

- Success: `RESULT:OK STEP:<step> LOG:<path>`
- Failure: `RESULT:FAIL STEP:<step> LOG:<path>`

When a command fails, open the referenced log file for full diagnostics.

## Prerequisites

- Docker Engine with Compose plugin
- Built artifacts:
  - `modules/perc-distribution-tree/target/perc-distribution-tree.jar`
  - `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar`

Use project wrappers to build artifacts:

```bash
./mvn-env.sh clean install -DskipTests=true
```

## Configure environment

```bash
cp .env.compose.example .env.compose
```

Update secrets in `.env.compose` before first run.

Set install behavior for your scenario using `PERC_INSTALL_MODE`:

- `install-if-missing` (default): install/update once, then fast restarts
- `install-always`: always run installer/update flow on container startup
- `skip-install`: never run installer/update flow on startup

## Installer DB input contract

The CMS and DTS installer jars accept DB configuration through `--db.*` params.

Resolution order:

1. `--db.*` CLI params
2. `--db.config.env.file` (one `ENVVAR=value` per line)
3. environment variables
4. defaults

Defaults:

- `db.type=derby`
- `db.ssl.enabled=true`
- `db.ssl.verify=true`
- `db.ssl.allowSelfSigned=false`

Supported fresh-install DB types:

- `derby`
- `mysql`
- `sqlserver`

For non-Derby installs, required parameters are:

- `db.host`
- `db.port`
- `db.name`
- `db.user`
- `db.password`

If any required non-Derby parameter is missing, installer fails fast with a clear error.

Developer self-signed mode (explicit opt-in):

- `db.ssl.allowSelfSigned=true`

## Database prerequisites and verification

- MySQL container bootstraps database/user from `.env.compose` values:
  - `MYSQL_DATABASE`
  - `MYSQL_USER`
  - `MYSQL_PASSWORD`
  - `MYSQL_ROOT_PASSWORD`
- The MySQL service runs with UTF-8 defaults (`utf8mb4`, `utf8mb4_unicode_ci`).
- Installer defaults are historically Derby-oriented unless installation/runtime configs are changed.

To confirm what the running server actually uses (CMS repo + DTS datasource), run:

```bash
./docker/scripts/perc-devctl.py inspect-install
```

The referenced log file includes values from:

- `/opt/Percussion/rxconfig/Installer/rxrepository.properties`
- `/opt/Percussion/Deployment/Server/conf/perc/perc-datasources.properties`

## Start stack

```bash
docker compose --env-file .env.compose up -d --build
```

Agent-friendly equivalent:

```bash
./docker/scripts/perc-devctl.py up --build
```

Maven lifecycle equivalent (profile-driven):

```bash
./mvn-env.sh -P docker-compose pre-integration-test
```

Services:

- MySQL 8: `localhost:${MYSQL_PORT}`
- CMS-DTS container exposing:
  - CMS: `localhost:${CMS_PORT}`
  - DTS: `localhost:${DTS_PORT}`

## Startup update flow

On container startup, the entrypoint runs installer/update jars in this order into `/opt/Percussion`:

1. CMS distribution jar
2. DTS distribution jar

Then the combined app container starts both CMS and DTS processes.

Container env equivalents for installer contract are provided in `.env.compose.example` as `PERC_DB_*` keys.

## Persistent writable data (`var/config` contract)

**Design intent:** under RX root (`/opt/Percussion`), **`var/config`** is the home for
**user-writable / instance-specific** data that Docker (and similar deployments) should
persist on a volume. Product code and packaging should prefer writing runtime-generated
or operator-mutable files under `var/config` (or paths that resolve there) rather than
into the immutable distribution tree.

| Path | Role |
|------|------|
| `var/config/` | Root for persistent, instance-specific config and writable artifacts |
| `var/config/generated/` | Auto-generated secrets and similar (e.g. first-boot passwords) |
| `var/config/generated/passwords` | Generated CMS credentials on first startup |
| `var/config/CustomXMLCatalog.xml` | Instance XML catalog overrides (when present) |

**Also treated as persistent / user-writable** (and often volume-mounted next to or via
the same durability strategy as `var/`):

| Path | Role |
|------|------|
| `ObjectStore/` | CMS object store (design-time apps, content editor defs, etc.) |
| `rxconfig/` | Server runtime config (Installer, I18n, ESAPI, categories, …) |
| `jetty/base/` | Jetty base (logs, webapps overlay, runtime jetty config) |
| `Deployment/Server/conf/` | DTS Tomcat/conf and perc datasources |

When adding new features that write files at runtime (generated passwords, locks,
catalogs, local overrides), put them under **`var/config`** (or a clearly versioned
subdir there) so Docker can map one durable volume for “anything the user/instance owns.”

`ObjectStore` and `rxconfig` historically live at RX root for on-prem layouts; in Docker
they are volume-mounted separately today. Future hardening may re-home or symlink more of
those trees under `var/config` — until then, treat **all of the mounts below** as the
writable set.

### Compose dev mounts (current)

CMS-DTS container host bindings (`docker-compose.yml`):

- `docker/dev-data/cms-dts/ObjectStore` → `/opt/Percussion/ObjectStore`
- `docker/dev-data/cms-dts/var` → `/opt/Percussion/var` (**includes `var/config`**)
- `docker/dev-data/cms-dts/rxconfig` → `/opt/Percussion/rxconfig`
- `docker/dev-data/cms-dts/Deployment/Server/conf` → `/opt/Percussion/Deployment/Server/conf`
- `docker/dev-data/cms-dts/jetty/base` → `/opt/Percussion/jetty/base`

These host-side folders are editable directly from your IDE and survive container rebuilds.

## Logs and status

```bash
docker compose --env-file .env.compose ps
docker compose --env-file .env.compose logs -f cms-dts
```

Agent-friendly status + verification:

```bash
./docker/scripts/perc-devctl.py status
./docker/scripts/perc-devctl.py verify --timeout-seconds 300
```

## Stop stack

```bash
docker compose --env-file .env.compose down
```

Agent-friendly equivalent:

```bash
./docker/scripts/perc-devctl.py down
```

Maven lifecycle equivalent:

```bash
./mvn-env.sh -P docker-compose post-integration-test
```

To also remove MySQL data volume:

```bash
docker compose --env-file .env.compose down -v
```

Agent-friendly with volume cleanup:

```bash
./docker/scripts/perc-devctl.py down --volumes
```

## Run integration tests against Compose stack

Use profile-driven lifecycle to start stack, run integration tests, and teardown automatically:

```bash
./mvn-env.sh -P integration-test,docker-compose verify
```

Agent-friendly equivalent:

```bash
./docker/scripts/perc-devctl.py it-verify
```

This flow performs:

1. `pre-integration-test`: `docker compose up -d --build` + readiness wait checks
2. `integration-test`/`verify`: Failsafe integration tests
3. `post-integration-test`: `docker compose down -v`

## Scenario-based usage

### 1) "I just want to start app and test manually"

- Set `PERC_INSTALL_MODE=install-if-missing`
- First run installs, subsequent restarts are fast.

### 2) "I want to run automated tests"

- Use Maven flow:

  ```bash
  ./mvn-env.sh -P integration-test,docker-compose verify
  ```
- Keep `PERC_INSTALL_MODE=install-if-missing` for speed, or use `install-always` if test isolation requires full reinstall.

### 3) "I fixed installer/deployment behavior and need full deploy"

- Set `PERC_INSTALL_MODE=install-always`
- Restart stack so startup always reapplies installers.

### 4) "Hot test a single module jar update"

Build just the module jar, then deploy into running container:

```bash
./mvn-env.sh -pl modules/utils -am package -DskipTests
./docker/scripts/hot-deploy-jar.sh --jar modules/utils/target/<your-jar>.jar --target both --restart
```

Agent-friendly equivalent with optional post-deploy verification:

```bash
./docker/scripts/perc-devctl.py deploy-jar --jar modules/utils/target/<your-jar>.jar --target both --restart --verify
```

Single-command fix verification (deploy + verify + one final result line):

```bash
./docker/scripts/perc-devctl.py verify-fix --jar modules/utils/target/<your-jar>.jar --target both --restart --timeout-seconds 240
```

Notes:

- `--target cms` copies to `/opt/Percussion/jetty/base/lib`
- `--target dts` copies to `/opt/Percussion/Deployment/Server/lib`
- `--target both` (default) copies to both locations
- Use `--target /absolute/container/path` for advanced cases

## Generated CMS passwords

On first CMS startup, generated credentials are written to:

- `/opt/Percussion/var/config/generated/passwords`

Retrieve with:

```bash
./docker/scripts/perc-devctl.py show-generated-passwords
```

If unavailable, the command returns `RESULT:FAIL` and a detailed log path.
