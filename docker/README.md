# docker/ — Percussion CMS Dev/Test Stack

This directory holds the docker-compose stack, operator-facing control scripts, and container entrypoints for the cms-dts **dev** environment and the **install matrix smoke** harness (#1500).

Per spec 994 (`specs/994-python-build-scripts/spec.md`): the original `.sh` wrappers around these scripts have been removed (FR-004). Windows, Linux, and macOS operators now invoke the Python entry points identically (`python3 scripts/<name>.py` or `python3 docker/scripts/<name>.py`).

## Credentials (no secrets in compose)

DB passwords are **not** hardcoded in `docker-compose.yml`. Copy the example env file and set local-only values:

```bash
cp .env.compose.example .env.compose
# edit MYSQL_*/POSTGRES_*/MSSQL_* passwords as needed
docker compose --env-file .env.compose --profile postgres up -d
```

`.env.compose` is gitignored. Matrix install smoke loads the same file (or `--env-file`) so cell `DB_PASSWORD` matches the compose DB.

## Two stacks (do not confuse them)

|          Stack           |                                    Purpose                                    |                   How to run                    |
|--------------------------|-------------------------------------------------------------------------------|-------------------------------------------------|
| **Dev (`cms-dts`)**      | Day-to-day coding: host install bind-mounted, hot-deploy jars                 | `perc-devctl.py` + `scripts/install-cms-dev.py` |
| **Matrix install smoke** | Ephemeral silent install of **CMS and/or DTS** per DB, probe, record, destroy | `docker/scripts/matrix-install-smoke.py`        |

## Layout

|     Path      |                                 Purpose                                  |
|---------------|--------------------------------------------------------------------------|
| `cms/`        | Dockerfile + image for the long-lived cms-dts **dev** container          |
| `matrix/`     | Dockerfile + in-cell entrypoint for ephemeral install matrix cells       |
| `dev-data/`   | Persistent bind-mount volume (CMS install + DB) for **dev** only         |
| `entrypoint/` | Dev container service-start scripts (`install-update.py`)                |
| `scripts/`    | Host-side operator control (`perc-devctl.py`, `matrix-install-smoke.py`) |
| `logs/`       | Timestamped logs + matrix JSON results                                   |

## Host-side scripts

### `docker/scripts/perc-devctl.py`

Cross-platform replacement for the legacy `perc-devctl.sh`. The Python entry point exposes every subcommand the original `.sh` provided:

```
perc-devctl.py install [--reset] [--no-bootstrap] [--install-root <path>]
perc-devctl.py up [--build]
perc-devctl.py down [--volumes]
perc-devctl.py status
perc-devctl.py verify [--timeout-seconds N] [--interval-seconds N]
perc-devctl.py it-verify
perc-devctl.py deploy-jar --jar <path> [--target cms|dts|both|/abs/path] [--restart] [--verify]
perc-devctl.py verify-fix --jar <path> [--target ...] [--restart|--no-restart] [--timeout-seconds N]
perc-devctl.py logs-path
perc-devctl.py inspect-install
perc-devctl.py show-generated-passwords
# QA mode — H2-in-Docker CMS for Playwright (no host install) — #1827 / #1927
perc-devctl.py qa-up [--timeout-seconds N] [--skip-image-build]
perc-devctl.py qa-health [--timeout-seconds N] [--interval-seconds N] [--url URL]
perc-devctl.py qa-down [--container NAME]
```

Each subcommand writes full output to a timestamped file under `docker/logs/<label>-<ts>.log` and emits a single `RESULT:OK STEP:<label> LOG:<path>` (or `RESULT:FAIL`) line on stdout so agent workflows can parse the result without parsing free-form output.

#### QA mode (`qa-up` / `qa-health` / `qa-down`)

Starts an ephemeral **CMS + H2** matrix cell (same stack as `matrix-install-smoke.py --product cms --db h2 --keep`), waits for `http://127.0.0.1:9993/Rhythmyx/login`, prints `TEST_CMS_URL` / admin username (password from generated install file when available), and tears down with `docker rm -f perc-matrix-cms-h2` so ports and disk are freed (no multi-GB named volume by default). Full operator flow: [workbench-rest-and-qa-modes.md](../docs/developer-module/workbench-rest-and-qa-modes.md) → **QA mode** section.

### `docker/scripts/hot-deploy-jar.py`

Cross-platform replacement for the legacy `hot-deploy-jar.sh`. Copies a built module jar into a running cms-dts container:

```
python3 docker/scripts/hot-deploy-jar.py --jar modules/utils/target/utils-8.2.0.jar --target both --restart
```

Default container: `percussion-cms-dts`. Default target: `both` (CMS + DTS lib dirs). `--target` accepts `cms`, `dts`, `both`, or an absolute container path.

## Container entrypoint

### `docker/entrypoint/install-update.py`

Cross-platform replacement for `install-update.sh`. Runs inside the cms-dts container at startup. **Service-only mode**: the install lives entirely on the host (see `scripts/install-cms-dev.py`); this script only starts the service.

Container-side invocation (set in `docker/cms/Dockerfile`):

```dockerfile
ENTRYPOINT ["/usr/local/bin/python3", "/usr/local/bin/install-update.py"]
```

`--service-mode` defaults to `cms-dts` (start both). Other values: `cms`, `dts`.

## Matrix install smoke (Layer 1 — CMS + DTS)

Ephemeral cells mount the real installer jars (not a fictional `perc-preinstall.jar`):

| Product |            Installer jar (customer-shipped assembly only)            |        Start after install        |           Default host probe           |
|---------|----------------------------------------------------------------------|-----------------------------------|----------------------------------------|
| CMS     | `modules/perc-distribution-tree/target/perc-distribution-tree.jar`   | `jetty/StartJetty.sh`             | `http://127.0.0.1:9993/Rhythmyx/login` |
| DTS     | `…/delivery-tier-distribution/target/delivery-tier-distribution.jar` | `TomcatStartup.sh` / `startup.sh` | `http://127.0.0.1:9983/`               |

Do **not** use `*-SNAPSHOT.jar` — those are plain module jars without the runnable installer main class. Package with `mvn package` so the assembly `finalName` jars exist and are non-empty.

```bash
# Prereq: package installers (CMS required; DTS if --product includes dts)
cd modules/perc-distribution-tree && ../../mvnw package -DskipTests
cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution && ../../../../mvnw package -DskipTests

# CMS + H2 (no external DB container)
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2

# CMS + PostgreSQL (starts compose profile postgres)
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql

# Both products × H2 and PostgreSQL
python3 docker/scripts/matrix-install-smoke.py --product cms,dts --db h2,postgresql

# DTS only × full Layer-1 DB matrix (H2 + external compose profiles)
python3 docker/scripts/matrix-install-smoke.py --product dts --db h2,postgresql,mysql,sqlserver

# Leave cell up for Playwright Layer 2 (perc-qa-automation)
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep
TEST_CMS_URL=http://localhost:9993 TEST_DB_TYPE=postgresql \
  npm test --prefix modules/perc-qa-automation/frontend -- tests/install.spec.js

# Destroy cells but keep external DBs for a follow-up matrix run
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep-db

# Force-stop every external DB used by this matrix (including pre-existing)
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql,mysql --stop-db

# Dry-run (no docker)
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --dry-run --skip-image-build
```

### External DB teardown policy (#1516)

Matrix cells (`perc-matrix-*`) are destroyed after each cell unless `--keep`.
External compose DBs (`percussion-postgres` / `percussion-mysql` / `percussion-sqlserver`) follow a separate ownership rule:

|    Flag     |            Cells            |                              External DBs                               |
|-------------|-----------------------------|-------------------------------------------------------------------------|
| **Default** | Destroyed                   | Stop services **this process started** (`compose stop`; no volume wipe) |
| `--keep`    | Left running                | Left running (Layer 2 / debugging)                                      |
| `--keep-db` | Destroyed (unless `--keep`) | Left running (reuse across runs)                                        |
| `--stop-db` | Destroyed (unless `--keep`) | Stop **all** external DBs used by the matrix, even if pre-existing      |

If a DB container was already running before the harness (e.g. long-lived dev stack), the default path **reuses** it and **does not** stop it at the end. Use `--stop-db` only when you intentionally want those containers stopped. Never uses `compose down -v` by default (named volumes / operator data are preserved).

### DTS packaging notes (HTTP 9980)

DTS Tomcat listens on **`${http.port}`** from `conf/perc/perc-catalina.properties` (default **9980**), not stock Tomcat 8080. The shipping tree requires:

|                 File                  |                                              Requirement                                              |
|---------------------------------------|-------------------------------------------------------------------------------------------------------|
| `conf/server.xml`                     | Property-driven connectors (`port="${http.port}"`) + `PSSimpleRedirectorValve`                        |
| `conf/catalina.properties`            | `common.loader` includes `common/lib`; `PROPERTY_SOURCE=com.percussion.tomcat.PSTomcatPropertySource` |
| `common/lib/perc-tomcat-common-*.jar` | Must contain `com.percussion.tomcat.valves.PSSimpleRedirectorValve` (correct package)                 |

Matrix host probe: `http://127.0.0.1:9983/` → container **9980**.

Each run writes `docker/logs/matrix-results-<ts>.json` and a `RESULT:OK|FAIL STEP:matrix LOG:…` line.

Unit tests (no docker):

```bash
python3 -m pytest docker/scripts/test_matrix_install_smoke.py -v
```

## Tests (spec 994 script suite)

```sh
# Linux / macOS
bash scripts/run-python-tests.sh --skip-install --pytest-args "-q"
# Windows
scripts\run-python-tests.cmd --skip-install --pytest-args "-q"
```

The pytest collection covers all in-scope script dirs per spec 994; the docker scripts above are included automatically.

## When to add a new script here

Per root `AGENTS.md`, scripts that CI or an operator runs must be cross-platform. For docker tooling, that means: implement the logic in Python, invoke `docker` / `docker compose` via `subprocess.run([...], shell=False, ...)` (FR-008), use `pathlib.Path` (FR-007), and add a pytest module under the same directory. Avoid introducing new `.sh` files — every shell script in this directory is a candidate for spec 994 migration.
