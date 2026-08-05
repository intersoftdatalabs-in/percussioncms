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

|     Path      |                                                    Purpose                                                    |
|---------------|---------------------------------------------------------------------------------------------------------------|
| `cms/`        | Dockerfile + image for the long-lived cms-dts **dev** container                                               |
| `matrix/`     | Dockerfile + in-cell entrypoint for ephemeral install matrix cells                                            |
| `dev-data/`   | Persistent bind-mount volume (CMS install + DB) for **dev** only                                              |
| `entrypoint/` | Dev container service-start scripts (`install-update.py`)                                                     |
| `scripts/`    | Host-side operator control (`perc-devctl.py`, `matrix-install-smoke.py`, freeport helpers / concurrent smoke) |
| `logs/`       | Timestamped logs + matrix JSON results                                                                        |

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

#### Host ports / freeport (multi-worktree) — #2001 / #2005 / #2004

`perc-devctl.py` and `matrix-install-smoke.py` resolve published host ports via shared helpers in `docker/scripts/perc_host_ports.py` (cross-platform stdlib `socket` bind to port `0` — no Unix-only tooling):

1. **Env override** (wins):
   - Dev stack: `CMS_PORT`, `DTS_PORT` (compose already maps these), or full `VERIFY_CMS_URL` / `VERIFY_DTS_URL`
   - Compose DB host publishes: `MYSQL_PORT`, `POSTGRES_PORT`, `MSSQL_PORT` (compose maps `${*_PORT:-…}:container`)
   - QA / matrix CMS cell: `QA_CMS_HOST_PORT` or `CMS_HOST_PORT`
   - Matrix DTS cell: `DTS_HOST_PORT`
2. **Preferred baseline when free** on loopback: compose CMS `9992`, compose DTS `9980`, matrix/QA CMS `9993`, matrix DTS `9983`, MySQL `3306`, Postgres `5433`, SQL Server `1433`
3. Else **ephemeral freeport** so a second worktree does not hit `address already in use`

Process-env pins override `.env.compose` / `.env.compose.example` defaults when `docker compose` interpolates host ports. **Container listen ports stay fixed** (MySQL `3306`, Postgres `5432`, SQL Server `1433`); matrix cells reach DBs via Docker DNS on `perc-matrix-net`, not the host publish side.

**Discover allocated ports:**

- `perc-devctl.py up` prints `CMS_PORT=…`, `DTS_PORT=…`, `MYSQL_PORT` / `POSTGRES_PORT` / `MSSQL_PORT`, and the resolved `VERIFY_*_URL` lines
- `perc-devctl.py qa-up` prints `QA_CMS_HOST_PORT=…` and `TEST_CMS_URL=http://127.0.0.1:<port>`
- `matrix-install-smoke.py` pins `CMS_HOST_PORT` / `QA_CMS_HOST_PORT` / `DTS_HOST_PORT` for the cell it starts; probe URL and docker `-p` always use that same port. External DB cells also pin and print `MYSQL_PORT` / `POSTGRES_PORT` / `MSSQL_PORT` before compose up.

**Playwright / `TEST_CMS_URL` contract:**

|       Consumer        |                                                        Source of base URL                                                        |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------|
| After `qa-up`         | Use the printed `TEST_CMS_URL` (or `http://127.0.0.1:$QA_CMS_HOST_PORT`)                                                         |
| After matrix `--keep` | `http://127.0.0.1:$CMS_HOST_PORT` (export pinned by the harness)                                                                 |
| CI / local override   | Export `TEST_CMS_URL` **and** matching `QA_CMS_HOST_PORT` / `CMS_HOST_PORT` before `qa-up` so publish + probe + Playwright agree |

Do **not** hardcode `http://127.0.0.1:9993` in agent scripts — 9993 is only the preferred baseline when free. Pin ports across sessions by exporting those env vars before `up` / `qa-up` / `matrix-install-smoke` / `verify`. Tear-down (`down` / `qa-down` / cell destroy) frees the docker publish mapping; the host port itself is not reserved after the container exits.

**Host-side installer + MySQL freeport:** if `MYSQL_PORT` freeports away from `3306` and you run `scripts/install-cms-dev.py` against published MySQL on localhost, also set `PERC_DB_PORT` to the same host port (matrix cells do not need this — they use container DNS).

**Remaining multi-worktree gaps** (parent [#2001](https://github.com/intersoftdatalabs-in/percussioncms/issues/2001)): fixed compose `container_name` values still limit true concurrent full stacks; freeport alone does not rename containers. Operator two-worktree checklist: [#2006](https://github.com/intersoftdatalabs-in/percussioncms/issues/2006) when present.

#### Two-worktree concurrent freeport smoke (#2006)

Operator / agent checklist proving freeport multi-cell stacks do not collide on **published host ports**. Parent [#2001](https://github.com/intersoftdatalabs-in/percussioncms/issues/2001); freeport implementation [#2003](https://github.com/intersoftdatalabs-in/percussioncms/pull/2003) / matrix wire-up [#2014](https://github.com/intersoftdatalabs-in/percussioncms/pull/2014) (issue #2005). Sibling residual surface: [#2004](https://github.com/intersoftdatalabs-in/percussioncms/issues/2004). Companion notes also in [workbench-rest-and-qa-modes.md](../docs/developer-module/workbench-rest-and-qa-modes.md).

##### Tier 0 — allocation dry-run (no Docker, no CMS install)

Fast, CI-friendly proof of the freeport contract (preferred → freeport fallback → env override → tear-down frees):

```bash
# From repo root (Windows: py -3 or python)
python docker/scripts/freeport-concurrent-smoke.py
# optional: --quiet  (RESULT line only)
# Expected: RESULT:OK STEP:freeport-concurrent-smoke

python -m pytest docker/scripts/test_freeport_concurrent_smoke.py -q
```

What it asserts (simulates two “worktree cells” by holding ports with stdlib sockets):

|         Check          |                     Expected                      |
|------------------------|---------------------------------------------------|
| Cell A, no env pin     | Preferred baseline when free                      |
| Cell B, preferred held | Distinct freeport (not cell A)                    |
| Env override           | `QA_CMS_HOST_PORT` / `CMS_PORT` / `DTS_PORT` wins |
| Compose pair freeport  | Second CMS+DTS pair distinct from first           |
| After release          | Preferred free again (when this process held it)  |

##### Tier 1 — dry-run CLI discovery (no real containers)

Use two shells / two worktree checkouts of the same repo. Do **not** set port env vars (unless testing override).

```bash
# Worktree A (or terminal 1)
cd /path/to/worktree-a
python docker/scripts/perc-devctl.py qa-up --dry-run
# Note QA_CMS_HOST_PORT=… (often 9993 when free) and TEST_CMS_URL=…

python docker/scripts/perc-devctl.py up --dry-run
# Note CMS_PORT=… DTS_PORT=… and VERIFY_*_URL=…

# Worktree B — hold preferred first so freeport must kick in, then dry-run
# (Tier 0 script already holds preferred; or start a real cell in A first)
cd /path/to/worktree-b
# Unset any pin:
#   Unix: unset QA_CMS_HOST_PORT CMS_HOST_PORT CMS_PORT DTS_PORT
#   Windows PowerShell: Remove-Item Env:QA_CMS_HOST_PORT -ErrorAction SilentlyContinue  (etc.)
python docker/scripts/perc-devctl.py qa-up --dry-run
# Assert QA_CMS_HOST_PORT differs from worktree A when A still holds preferred
```

**Env override wins** (either worktree):

```bash
# Unix
export QA_CMS_HOST_PORT=18001
python docker/scripts/perc-devctl.py qa-up --dry-run
# Expect QA_CMS_HOST_PORT=18001 and TEST_CMS_URL=http://127.0.0.1:18001

export CMS_PORT=19111 DTS_PORT=19112
python docker/scripts/perc-devctl.py up --dry-run
# Expect CMS_PORT=19111 DTS_PORT=19112 in printed VERIFY_* URLs
```

##### Tier 2 — live stacks (optional; real Docker + probes)

Requires Docker, packaged CMS installer for QA (`modules/perc-distribution-tree` assembly jar), and free machine resources. **Container names are still global** (`perc-matrix-cms-h2`, `percussion-cms-dts`); two checkouts cannot both keep the same named container. Live concurrent smoke therefore uses **sequential ownership of the shared name** or **one live cell + freeport dry-run for the second** unless #2004 / follow-ups add worktree-scoped names.

**Path A — sequential QA (same host, prove freeport + probes + tear-down):**

```bash
# 1) Worktree A — no port pin
cd /path/to/worktree-a
python docker/scripts/perc-devctl.py qa-up
# Record PORT_A from QA_CMS_HOST_PORT=… ; curl/qa-health the printed TEST_CMS_URL
python docker/scripts/perc-devctl.py qa-health

# 2) Tear-down A; confirm publish freed
python docker/scripts/perc-devctl.py qa-down
# Port PORT_A should be free for bind again (Tier 0 or a short Python is_port_free check)

# 3) Worktree B — no port pin (gets preferred when free)
cd /path/to/worktree-b
python docker/scripts/perc-devctl.py qa-up
python docker/scripts/perc-devctl.py qa-health
python docker/scripts/perc-devctl.py qa-down
```

**Path B — concurrent freeport under a live holder (port collision only):**

```bash
# Terminal A — occupy preferred QA port without full install (or leave a cell up)
python -c "import socket;s=socket.socket();s.bind(('127.0.0.1',9993));s.listen(1);input('holding 9993; Enter to release')"

# Terminal B — worktree B, no pin
python docker/scripts/perc-devctl.py qa-up --dry-run
# Expect QA_CMS_HOST_PORT != 9993
# Optional full: qa-up (if no other perc-matrix-cms-h2), qa-health, qa-down
```

**Path C — dev compose `up` / `verify` (single container name `percussion-cms-dts`):**

```bash
# Worktree A
python docker/scripts/perc-devctl.py up
# Record CMS_PORT / DTS_PORT; perc-devctl.py verify when stack ready
python docker/scripts/perc-devctl.py down

# Worktree B (after A down, or with preferred held) — no pin
python docker/scripts/perc-devctl.py up --dry-run   # assert freeport when preferred taken
# Env override: CMS_PORT=19111 DTS_PORT=19112 python docker/scripts/perc-devctl.py up
python docker/scripts/perc-devctl.py down
```

**Pass criteria (summary):**

|          Step           |                             Pass when                             |
|-------------------------|-------------------------------------------------------------------|
| No pin, preferred free  | Cell uses preferred baseline                                      |
| No pin, preferred taken | Second cell uses freeport ≠ first                                 |
| Env set                 | Printed / published port equals env                               |
| Probe (live)            | `qa-health` / `verify` RESULT:OK (or HTTP 200/302 on login)       |
| Tear-down               | `qa-down` / `down`; host ports free; no orphan cell for that name |

##### Related helpers

|                   Artifact                    |                                            Role                                            |
|-----------------------------------------------|--------------------------------------------------------------------------------------------|
| `docker/scripts/perc_host_ports.py`           | `find_free_port` / `is_port_free` / `resolve_host_port`                                    |
| `docker/scripts/perc-devctl.py`               | `up` / `verify` / `qa-up` pin + print ports                                                |
| `docker/scripts/matrix-install-smoke.py`      | Matrix CMS/DTS freeport publish + probe                                                    |
| `docker/scripts/freeport-concurrent-smoke.py` | Tier 0 allocation smoke (#2006)                                                            |
| Unit tests                                    | `test_perc_devctl.py`, `test_matrix_install_smoke.py`, `test_freeport_concurrent_smoke.py` |

#### QA mode (`qa-up` / `qa-health` / `qa-down`)

Starts an ephemeral **CMS + H2** matrix cell (same stack as `matrix-install-smoke.py --product cms --db h2 --keep`), waits for the resolved probe URL (`http://127.0.0.1:<QA_CMS_HOST_PORT>/Rhythmyx/login`), prints `TEST_CMS_URL` / admin username (password from generated install file when available), and tears down with `docker rm -f perc-matrix-cms-h2` so ports and disk are freed (no multi-GB named volume by default). Full operator flow: [workbench-rest-and-qa-modes.md](../docs/developer-module/workbench-rest-and-qa-modes.md) → **QA mode** section. Concurrent freeport checklist: **Two-worktree concurrent freeport smoke** above.

### `docker/scripts/freeport-concurrent-smoke.py`

Allocation-only smoke for multi-worktree freeport (#2006). No Docker and no CMS install — holds preferred ports with stdlib sockets, asserts second-cell freeport and env override, prints `RESULT:OK|FAIL STEP:freeport-concurrent-smoke`. See **Two-worktree concurrent freeport smoke** above. Unit tests: `test_freeport_concurrent_smoke.py`.

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

| Product |            Installer jar (customer-shipped assembly only)            |        Start after install        |        Preferred host probe (when free / no env)        |
|---------|----------------------------------------------------------------------|-----------------------------------|---------------------------------------------------------|
| CMS     | `modules/perc-distribution-tree/target/perc-distribution-tree.jar`   | `jetty/StartJetty.sh`             | `http://127.0.0.1:<CMS_HOST_PORT\|9993>/Rhythmyx/login` |
| DTS     | `…/delivery-tier-distribution/target/delivery-tier-distribution.jar` | `TomcatStartup.sh` / `startup.sh` | `http://127.0.0.1:<DTS_HOST_PORT\|9983>/`               |

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
# Prefer printed/pinned CMS_HOST_PORT (or set CMS_HOST_PORT before matrix).
# Example when preferred 9993 was free:
TEST_CMS_URL=http://127.0.0.1:${CMS_HOST_PORT:-9993} TEST_DB_TYPE=postgresql \
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
