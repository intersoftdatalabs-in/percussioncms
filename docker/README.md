# docker/ — Percussion CMS Dev/Test Stack

This directory holds the docker-compose stack, operator-facing control scripts, and container entrypoints for the cms-dts **dev** environment and the **install matrix smoke** harness (#1500).

Per spec 994 (`specs/994-python-build-scripts/spec.md`): the original `.sh` wrappers around these scripts have been removed (FR-004). Windows, Linux, and macOS operators now invoke the Python entry points identically (`python3 scripts/<name>.py` or `python3 docker/scripts/<name>.py`).

## Credentials (no secrets in compose)

DB passwords are **not** hardcoded in `docker-compose.yml`. Copy the example env file and set local-only values:

```bash
cp .env.compose.example .env.compose
# edit MYSQL_*/POSTGRES_*/MSSQL_*/ORACLE_* passwords as needed
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
perc-devctl.py qa-preflight [--strict] [--no-content-hash]
perc-devctl.py qa-rebuild-chain [--skip-tests] [--dist-only] [--then-qa-up] [--dry-run]
perc-devctl.py qa-health [--timeout-seconds N] [--interval-seconds N] [--url URL]
perc-devctl.py qa-deploy-webui [--src DIR] [--container NAME]
perc-devctl.py qa-down [--container NAME]
```

#### Rebuild-chain preflight (#2486 / #2532 / matrix wire-up #2531)

`perc-devctl.py qa-preflight [--strict]` (or the standalone `python3 docker/scripts/qa_preflight.py`) detects a **stale WebUI WAR** vs a freshly built sitemanage SNAPSHOT **before** `qa-up`. Without this gate, re-running only `sitemanage → install` and then `qa-up` leaves the old `sitemanage-*.jar` bundled inside the `perc-web-ui-*.war` (which `perc-distribution-tree` unpacks into `Rhythmyx/WEB-INF/lib`). The container then loads the stale classpath and the cycle / DI fix appears to regress even though the m2 snapshot is correct.

**Matrix CMS path (strict by default):** `matrix-install-smoke.py` runs the same preflight **before install/start** whenever `--product` includes `cms` (including GHA / operator matrix entrypoints that call the harness directly without `perc-devctl qa-up`). On `STALE` it exits with code `2` and prints `RESULT:FAIL STEP:matrix-preflight DETAIL:preflight_stale` — no cell is started. Opt out only when intentionally debugging:

```bash
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --skip-preflight
```

DTS-only matrices (`--product dts`) skip this gate (no WebUI WAR on that path). See **Matrix install smoke** below for the full CLI matrix.

The preflight compares:

- `~/.m2/repository/com/percussion/sitemanage/sitemanage-*.jar` — the freshly installed snapshot, and
- `WebUI/target/perc-web-ui-*.war` — the WAR whose `WEB-INF/lib/sitemanage-*.jar` the dist tree will unpack.
- Optionally a loose `sitemanage-*.jar` under `modules/perc-distribution-tree/target` when present.

**Content-hash mode (default, #2532):** SHA-256 of the m2 jar bytes is compared to the `WEB-INF/lib/sitemanage-*.jar` zip entry inside the WAR (streamed; no full WAR extract). When both hashes are available they are the **primary** signal:

- Hashes differ → `STALE: sitemanage content hash mismatch (m2 vs war)` even if mtimes look fresh (clock skew, cache restore, `touch`-only rebuilds).
- Hashes match → `FRESH` even if WAR mtime is older than m2 (mtime false positive).
- Hashes unavailable → fall back to WAR mtime vs m2 mtime (legacy #2486 behaviour).

Use `--no-content-hash` for mtime-only comparison. With `--strict`, STALE returns exit code `2`; without `--strict` it prints the same line and returns `0` so callers can log without blocking.

Matrix CMS cells always use the **strict** policy unless --skip-preflight is set.

#### Rebuild-chain driver (#2533)

When preflight reports `STALE:` (or after any sitemanage / WebUI SNAPSHOT change that must land in the installer jar), run the portable driver instead of hand-typed `cd` + `mvnw` sequences:

```bash
# Plan only (prints PLANNED STEP:… ARGV:… and RESULT:OK; no Maven)
python3 docker/scripts/perc-devctl.py qa-rebuild-chain --dry-run

# Full chain: sitemanage clean install → WebUI package -DskipTests →
# modules/perc-distribution-tree clean package -DskipTests
python3 docker/scripts/perc-devctl.py qa-rebuild-chain --skip-tests

# Same via the standalone module (identical argv contract)
python3 docker/scripts/qa_rebuild_chain.py --skip-tests

# Installer packaging only (WAR inputs already fresh)
python3 docker/scripts/perc-devctl.py qa-rebuild-chain --dist-only

# Optional: rebuild then qa-up in one invocation
python3 docker/scripts/perc-devctl.py qa-rebuild-chain --skip-tests --then-qa-up
```

Cross-platform notes: each step `cwd`s into the module directory and invokes the **repo-root** `mvnw` / `mvnw.cmd` with `subprocess.run([...], shell=False)` (no shell quoting). Per-step and overall `RESULT:OK` / `RESULT:FAIL` lines are agent-parseable; Maven stdout/stderr goes under `docker/logs/`.

Run order recommended for agents and CI:

```bash
# 1) Detect stale WAR vs m2 sitemanage
python3 docker/scripts/perc-devctl.py qa-preflight --strict
# 2) If STALE / FAIL — drive the full Maven order (do not skip WebUI)
python3 docker/scripts/perc-devctl.py qa-rebuild-chain --skip-tests
# 3) Confirm fresh, then bring up QA H2 cell
python3 docker/scripts/perc-devctl.py qa-preflight --strict \
  && python3 docker/scripts/perc-devctl.py qa-up
# or matrix directly (preflight is built-in for --product cms):
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2
```

Standalone (same flags):

```bash
python3 docker/scripts/qa_preflight.py --strict --repo-root .
# mtime-only fallback:
python3 docker/scripts/qa_preflight.py --strict --no-content-hash
```

Each subcommand writes full output to a timestamped file under `docker/logs/<label>-<ts>.log` and emits a single `RESULT:OK STEP:<label> LOG:<path>` (or `RESULT:FAIL`) line on stdout so agent workflows can parse the result without parsing free-form output.

#### Host ports / freeport (multi-worktree) — #2001 / #2005 / #2004

`perc-devctl.py` and `matrix-install-smoke.py` resolve published host ports via shared helpers in `docker/scripts/perc_host_ports.py` (cross-platform stdlib `socket` bind to port `0` — no Unix-only tooling):

1. **Env override** (wins):
   - Dev stack: `CMS_PORT`, `DTS_PORT` (compose already maps these), or full `VERIFY_CMS_URL` / `VERIFY_DTS_URL`
   - Compose DB host publishes: `MYSQL_PORT`, `POSTGRES_PORT`, `MSSQL_PORT`, `ORACLE_PORT` (compose maps `${*_PORT:-…}:container`)
   - QA / matrix CMS cell: `QA_CMS_HOST_PORT` or `CMS_HOST_PORT`
   - Matrix DTS cell: `DTS_HOST_PORT`
2. **Preferred baseline when free** on loopback: compose CMS `9992`, compose DTS `9980`, matrix/QA CMS `9993`, matrix DTS `9983`, MySQL `3306`, Postgres `5433`, SQL Server `1433`, Oracle `1521`
3. Else **ephemeral freeport** so a second worktree does not hit `address already in use`

Process-env pins override `.env.compose` / `.env.compose.example` defaults when `docker compose` interpolates host ports. **Container listen ports stay fixed** (MySQL `3306`, Postgres `5432`, SQL Server `1433`, Oracle `1521`); matrix cells reach DBs via Docker DNS on `perc-matrix-net`, not the host publish side.

**Discover allocated ports:**

- `perc-devctl.py up` prints `CMS_PORT=…`, `DTS_PORT=…`, `MYSQL_PORT` / `POSTGRES_PORT` / `MSSQL_PORT`, and the resolved `VERIFY_*_URL` lines
- `perc-devctl.py qa-up` prints `QA_CMS_HOST_PORT=…` and `TEST_CMS_URL=http://127.0.0.1:<port>`
- `matrix-install-smoke.py` pins `CMS_HOST_PORT` / `QA_CMS_HOST_PORT` / `DTS_HOST_PORT` for the cell it starts; probe URL and docker `-p` always use that same port. External DB cells also pin and print `MYSQL_PORT` / `POSTGRES_PORT` / `MSSQL_PORT` / `ORACLE_PORT` before compose up.

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

Starts an ephemeral **CMS + H2** matrix cell (same stack as `matrix-install-smoke.py --product cms --db h2 --keep`), waits for docker `Health.Status=healthy` **and** the resolved host probe URL (`http://127.0.0.1:<QA_CMS_HOST_PORT>/Rhythmyx/login`) with host log scan, prints `TEST_CMS_URL` / admin username (password from generated install file when available), and tears down with `docker rm -f perc-matrix-cms-h2` so ports and disk are freed (no multi-GB named volume by default). Full operator flow: [workbench-rest-and-qa-modes.md](../docs/developer-module/workbench-rest-and-qa-modes.md) → **QA mode** section. Concurrent freeport checklist: **Two-worktree concurrent freeport smoke** above.

**Sample sites under Explorer `/Sites` (#3001 / #2989):** CMS+H2 matrix cells pass installer `--demo-sites` by default (`docker/matrix/cell-entrypoint.py` → ANT `installSampleSites`) so `GET …/path/folder/Sites` and the modern Explorer tree list Corporate/Enterprise Investments after a fresh `qa-up`. Opt out with env `DEMO_SITES=false` or cell flag `--no-demo-sites`. External-DB matrix cells stay off unless `DEMO_SITES=true`. Requires a **new** silent install (re-run `qa-up` after image/entrypoint change); an already-installed empty `RXSITES` cell is not backfilled.

##### Rhythmyx ApplicationContext fail-fast (#2462 / #2480 / #2423)

Jetty can report the HTTP connector **Started** while the ROOT/Rhythmyx Spring `ApplicationContext` failed (e.g. `BeanCurrentlyInCreationException` / circular `folderHelper` wiring). A port-up or HTTP-only probe is **not** sufficient. The same markers apply to **QA matrix cells** and the **cms-dts compose stack**.

| Signal | Where | Operator meaning |
|--------|--------|------------------|
| `RESULT:OK STEP:qa-health HTTP:… HEALTH:healthy …` | `perc-devctl.py qa-health` | Probe URL ready, docker `Health.Status=healthy` for the QA cell, **and** recent `docker logs` have **no** Rhythmyx context-failure markers (#2537 / #2481). Default probe URL is the matrix-recommended primary `/Rhythmyx/rest/mimetypes` (Spring-managed `MimeTypeResource.ping()` — returns 404 when the Rhythmyx Spring context is dead; #2482). Override with env `RHYTHMYX_HEALTH_PATH=…` |
| `RESULT:OK STEP:verify CMS_HTTP:… DTS_HTTP:… HEALTH:healthy` | `perc-devctl.py verify` (and `verify-fix` / `deploy-jar --verify`) | CMS+DTS HTTP ready, docker health healthy, **and** cms-dts logs have **no** Rhythmyx context-failure markers; `VERIFY_CMS_PATH` is the matrix-recommended secondary (`/Rhythmyx/rest/folders/by-path/Assets`) (#2482) |
| `RESULT:… HEALTH:healthy\|unhealthy\|starting\|none` | `qa-health` (matrix cell) and `verify` / `_verify_inline` (cms-dts) | Inspect status from `_docker_health` on every RESULT (OK and FAIL); `none` = container has no Health block; `unknown` = docker/container missing |
| `RESULT:FAIL … DETAIL:rhythmyx_context_failed MATCH:…` | `qa-health`, compose `verify` / `_verify_inline`, or matrix cell `detail` | **Fail-fast**: Spring/Jetty context death detected in logs; treat stack as unusable even if HTTP / docker health answered green |
| `RESULT:FAIL … DETAIL:server_log_errors MATCH:…` | same | **Fail-fast**: ERROR/FATAL/SEVERE in product/install logs (`server.log`, InstallPackages, install, tablefactory — same set as `perc-doctor check-logs` / #2556) |
| `RESULT:FAIL … DETAIL:timeout after Ns (last_http=… health=…)` | `qa-health` | Probe never became ready (HTTP and/or Health not green); if logs later show context fail, re-run `qa-health` or `docker logs perc-matrix-cms-h2` |
| `RESULT:FAIL … DETAIL:timeout after Ns (cms_http=… dts_http=… health=…)` | `verify` | Stack never became ready; scan `docker logs percussion-cms-dts` for context markers |
| Matrix `status=fail` + `detail` containing `rhythmyx_context_failed` | `matrix-install-smoke.py` CMS cells | Same fail-fast during cell HTTP wait (container log scan) |
| Matrix `status=fail` + `detail` containing `docker_health_unhealthy` | `matrix-install-smoke.py` CMS cells / `qa-up` | **Fail-fast** when `docker inspect` already reports `Health.Status=unhealthy` — does **not** burn full `--probe-timeout` (#2535 / #2481) |
| Matrix `status=fail` + `detail` containing `docker_health_timeout` | same | Probe timed out while health was still `starting` / not `healthy` |
| Docker `Health.Status=healthy` | matrix cell / cms-dts image HEALTHCHECK (#2481) | In-container probe ready **and** local Jetty logs have **no** context-failure markers |
| Docker `Health.Status=unhealthy` | same | Context failed and/or probe not ready — **do not** attach Playwright; inspect health log + `docker logs` |
| Docker `Health.Status=starting` | same | Still inside HEALTHCHECK `start_period` (matrix cells: long install window) |
| Docker `Health.Status=none` | container without HEALTHCHECK | Inspect has no `.State.Health` block (`_docker_health` → `none`) |
| **Probe URL matrix (#2482)** | `docs/ai-generated/tasks/2482-readiness-signal/rhythmyx-readiness-probe-matrix.md` + `PROBE_URL_MATRIX` constant in `docker/scripts/rhythmyx_ready.py` | Capability matrix of existing product endpoints (login, REST `ping()`, REST `Folders`, openapi) + which ones **actually** imply the Rhythmyx Spring `ApplicationContext` is up. Default in `qa-health` already flips to the matrix-recommended primary |

##### Matrix / qa-up wait policy (CMS cells, #2535)

After the in-image HEALTHCHECK lands (#2481), **CMS** matrix cells (and therefore `perc-devctl qa-up`, which delegates to `matrix-install-smoke.py --product cms --db h2 --keep`) wait with this combined policy:

1. **Host log scan** (`rhythmyx_ready` markers) — fail-fast on context death (unchanged belt-and-braces from #2462).
2. **Docker `Health.Status`** — require `healthy`; **fail-fast** when inspect already reports `unhealthy` (do not wait full `--probe-timeout`).
3. **Host HTTP probe** — `/Rhythmyx/login` ready codes (200/302/401/403).

HTTP alone during HEALTHCHECK `start_period` (status still `starting`) is **not** enough. Rebuild the matrix image after pulling HEALTHCHECK changes so the cell has a real health block.

```bash
# CMS cell / qa-up wait DETAIL tokens (matrix JSON detail / probe failed: …):
#   docker_health_unhealthy health=unhealthy status=running …
#   docker_health_timeout health=starting status=running last=HTTP 200 …
#   rhythmyx_context_failed match='Failed startup of context' …
docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}" perc-matrix-cms-h2
```

Markers (shared helper `docker/scripts/rhythmyx_ready.py`): `Failed startup of context`, `BeanCurrentlyInCreationException`, `Requested bean is currently in creation`, `Is there an unresolvable circular reference`.

**CMS product/install log gate (#2556):** host probes also `docker exec` tail `jetty/base/logs/server.log`, `rxconfig/Installer/InstallPackages.log` (or `logs/…`), `install.log`, and `tablefactory.log`. Operator CLI: `perc-doctor check-logs` on an install root.

##### Docker `Health.Status` (in-image HEALTHCHECK, #2481)

Matrix cells (`percussion-matrix-cell:local`) and the cms-dts image bake `docker/scripts/rhythmyx_healthcheck.py`, which reuses `rhythmyx_ready` markers:

| Container | Inspect | When **unhealthy** means |
|-----------|---------|---------------------------|
| `perc-matrix-cms-h2` (qa-up / matrix `--keep`) | `docker inspect -f "{{.State.Health.Status}}" perc-matrix-cms-h2` | Dead Rhythmyx context and/or `/Rhythmyx/login` not ready **inside** the cell |
| `percussion-cms-dts` (compose) | `docker inspect -f "{{.State.Health.Status}}" percussion-cms-dts` | Same for bind-mounted host install |

```bash
# After qa-up (or matrix --keep), re-check readiness with log scan + Health.Status (#2537):
python docker/scripts/perc-devctl.py qa-health
# OK example:
# RESULT:OK STEP:qa-health HTTP:200 HEALTH:healthy URL:… CONTAINER:perc-matrix-cms-h2 LOG:…
# FAIL example (do not attach Playwright):
# RESULT:FAIL STEP:qa-health DETAIL:rhythmyx_context_failed MATCH:Failed startup of context … HEALTH:unhealthy …
docker logs perc-matrix-cms-h2 2>&1 | findstr /i "Failed startup BeanCurrently circular folderHelper"
# Unix: docker logs perc-matrix-cms-h2 2>&1 | grep -E 'Failed startup|BeanCurrently|circular'

# Docker HEALTHCHECK (orchestrators / docker ps HEALTH column) — #2481:
docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}" perc-matrix-cms-h2
# healthy | unhealthy | starting | none
docker inspect -f "{{json .State.Health}}" perc-matrix-cms-h2
# Last health log lines include assessor detail (e.g. rhythmyx_context_failed …)

# After compose up, verify also scans cms-dts logs (#2480) and prints HEALTH: (#2537):
python docker/scripts/perc-devctl.py verify
# OK example:
# RESULT:OK STEP:verify CMS_HTTP:200 DTS_HTTP:200 HEALTH:healthy LOG:…
# FAIL example:
# RESULT:FAIL STEP:verify DETAIL:rhythmyx_context_failed MATCH:Failed startup of context … HEALTH:… …
docker logs percussion-cms-dts 2>&1 | findstr /i "Failed startup BeanCurrently circular folderHelper"
# Unix: docker logs percussion-cms-dts 2>&1 | grep -E 'Failed startup|BeanCurrently|circular'
docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}" percussion-cms-dts
```

**Rebuild note:** matrix image build context is `docker/` (not `docker/matrix/` alone) so HEALTHCHECK scripts are copied in. After pulling this change, rebuild with `matrix-install-smoke.py` (default) or `docker build -t percussion-matrix-cell:local -f docker/matrix/Dockerfile docker/`. Compose cms-dts: `docker compose build cms-dts`.

Unit tests: `python -m pytest docker/scripts/test_rhythmyx_ready.py docker/scripts/test_rhythmyx_healthcheck.py docker/scripts/test_healthcheck_unhealthy_inject_proof.py docker/scripts/test_perc_devctl.py docker/scripts/test_matrix_install_smoke.py -q`.

##### HEALTHCHECK unhealthy inject proof (#2536 residual of #2481)

Agent-safe harness that proves the assessor (and optionally a keep cell) reports **unhealthy** when a known Rhythmyx context-failure marker is present in the Jetty log path the in-image HEALTHCHECK tails.

| Mode | Needs Docker / full install? | What it proves |
|------|------------------------------|----------------|
| **mock** (default) | No — temp fixture tree + HTTP override | Healthy path (clean logs + HTTP 200) → exit 0; inject `Failed startup of context` into `jetty/base/logs/jetty.log` → exit 1 + `rhythmyx_context_failed`; recovery after clean rewrite → exit 0 |
| **live** | Yes — running keep cell (`perc-matrix-cms-h2` or `--container`) | `docker exec` appends marker into container Jetty log; re-runs baked `rhythmyx_healthcheck.py`; optional `--require-inspect-unhealthy` for `docker inspect` Health.Status |

```bash
# CI / overnight (no Docker, no CMS install):
python docker/scripts/healthcheck_unhealthy_inject_proof.py
# RESULT:OK STEP:healthcheck-unhealthy-inject-proof MODE:mock DETAIL:healthy+inject_unhealthy_ok

# Quiet (RESULT line only):
python docker/scripts/healthcheck_unhealthy_inject_proof.py --quiet

# After qa-up / matrix --keep (optional live inject):
python docker/scripts/perc-devctl.py qa-up
python docker/scripts/healthcheck_unhealthy_inject_proof.py --mode live
# RESULT:OK STEP:healthcheck-unhealthy-inject-proof MODE:live …
# Optional: also wait for Docker HEALTHCHECK interval to flip inspect:
python docker/scripts/healthcheck_unhealthy_inject_proof.py --mode live --require-inspect-unhealthy
docker inspect -f "{{.State.Health.Status}}" perc-matrix-cms-h2
# unhealthy
```

Cross-platform: host paths use `pathlib.Path` (Windows/macOS/Linux); container paths always use `/`. Unit tests: `docker/scripts/test_healthcheck_unhealthy_inject_proof.py`.

### `docker/scripts/freeport-concurrent-smoke.py`

Allocation-only smoke for multi-worktree freeport (#2006). No Docker and no CMS install — holds preferred ports with stdlib sockets, asserts second-cell freeport and env override, prints `RESULT:OK|FAIL STEP:freeport-concurrent-smoke`. See **Two-worktree concurrent freeport smoke** above. Unit tests: `test_freeport_concurrent_smoke.py`.

### `docker/scripts/hot-deploy-jar.py`

Cross-platform replacement for the legacy `hot-deploy-jar.sh`. Copies a built module jar into a running cms-dts container:

```
python3 docker/scripts/hot-deploy-jar.py --jar modules/utils/target/utils-8.2.0.jar --target both --restart
```

Default container: `percussion-cms-dts`. Default target: `both` (CMS + DTS lib dirs). `--target` accepts `cms`, `dts`, `both`, or an absolute container path.

**H2 QA cell (`perc-matrix-cms-h2`):** this default is **not** the Rhythmyx WAR classpath. Product SNAPSHOTs (`perc-system`, `sitemanage`, …) load from `/opt/Percussion/jetty/base/webapps/Rhythmyx/WEB-INF/lib/`. After `docker cp` into that dir, restart Jetty **inside** the cell and run `perc-devctl.py qa-health`. Do not `docker restart` the cell (silent install wipes copies). `qa-up --skip-image-build` does not refresh `perc-system`; if you copy a newer `sitemanage`, copy a matching `perc-system` too or ROOT fails at startup (`NoClassDefFoundError`).

### `docker/scripts/hot-deploy-webui-modern.py`

Hot-copy the **full** built WebUI modern SPA into the H2 QA WAR (`#3893`). Cycle Verify failed when only hashed files under `cm/modern/assets/` were copied: `spa.jsp` still loaded a stale `perc-modern-ui.js` that imported an older `developer-<hash>.js` (csv/sql/http-json present, `option[value=object-storage]` count 0).

```
python docker/scripts/perc-devctl.py qa-deploy-webui
# equivalent:
python docker/scripts/hot-deploy-webui-modern.py
```

Default source: `WebUI/target/generated-webui/cm/modern` (entry `assets/perc-modern-ui.js`, `assets/perc-modern-ui.css`, hashed chunks, optional `index.html`). Default container: `perc-matrix-cms-h2`. Dest: `/opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/`. The script refuses a bundle whose `assets/*.js` lacks the `object-storage` marker. It does **not** `docker restart` the cell — restart Jetty inside the cell, then `qa-health`. Unit tests: `docker/scripts/test_hot_deploy_webui_modern.py`.

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
# Strict rebuild-chain preflight runs first (#2531 / #2486); STALE → exit 2
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2

# CMS + PostgreSQL (starts compose profile postgres)
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql

# Both products × H2 and PostgreSQL (CMS preflight still runs once before cells)
python3 docker/scripts/matrix-install-smoke.py --product cms,dts --db h2,postgresql

# DTS only × Layer-1 DB matrix without Oracle (H2 + common external profiles)
# DTS-only skips WebUI WAR preflight
python3 docker/scripts/matrix-install-smoke.py --product dts --db h2,postgresql,mysql,sqlserver

# CMS + Oracle XE (opt-in; heavy image — see Oracle section below)
# Requires ORACLE_PASSWORD + ORACLE_APP_PASSWORD in .env.compose
python3 docker/scripts/matrix-install-smoke.py --product cms --db oracle

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

# Dry-run (no docker) — preflight still runs (filesystem-only) unless skipped
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --dry-run --skip-image-build
# Dry-run Oracle cell metadata (still needs passwords in env / .env.compose)
python3 docker/scripts/matrix-install-smoke.py --product cms --db oracle --dry-run --skip-image-build

# Opt out of strict CMS preflight only when debugging a known-stale tree
# (not recommended for CI). See Rebuild-chain preflight above.
python3 docker/scripts/matrix-install-smoke.py --product cms --db h2 --skip-preflight
```

### Oracle compose profile (#1508 / live residual #2083)

Opt-in Oracle XE for matrix Layer-1 only (not started by default `perc-devctl.py up`).

|              Item              |                                                                                                    Value                                                                                                     |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Image**                      | `gvenzl/oracle-xe:21-slim` (community packaging of Oracle Database XE). Accept Oracle Free Use Terms when pulling; operators may pin a company-approved mirror by changing `image:` in `docker-compose.yml`. |
| **Image pin (observed)**       | Digest `sha256:ecdf4302ac3d134e1bac5ef6e0c223c2d0f4d4d2b6d551aa79b2346f1ab8f792` (~2.6 GiB compressed layers on pull). Tag may move; pin by digest for locked operator envs. |
| **Compose profiles**           | `oracle`, and `db-all` (starts all external matrix DBs)                                                                                                                                                      |
| **Container name**             | `percussion-oracle`                                                                                                                                                                                          |
| **Network alias**              | `oracle` on `perc-matrix-net` (cells use `DB_HOST=oracle`)                                                                                                                                                   |
| **Container port**             | `1521` (fixed)                                                                                                                                                                                               |
| **Host publish**               | `${ORACLE_PORT:-1521}:1521` (freeport / env when 1521 is taken)                                                                                                                                              |
| **Service name (`--db.name`)** | `XEPDB1` (default pluggable DB service name; override with `ORACLE_SERVICE`). **Not** a SID — structured install uses Easy Connect `@//host:port/XEPDB1`.                                                    |
| **CMS user / schema**          | `APP_USER` / `ORACLE_APP_USER` (default `percuser`); password `ORACLE_APP_PASSWORD`                                                                                                                          |
| **SYS bootstrap**              | `ORACLE_PASSWORD` (required by the image; **not** the CMS connect password)                                                                                                                                  |
| **SSL**                        | Off for compose cells (`--db.ssl.enabled=false`), same as postgres/mysql/sqlserver                                                                                                                           |
| **Resources**                  | Image ~2.6 GiB; container often ≥2 GiB RAM once open; first volume init can take minutes. Docker Desktop should leave headroom for the CMS matrix cell (recommend ≥8 GiB Docker memory for this cell alone). |
| **Start / wait**               | Compose healthcheck `start_period` 120s, retries 20×30s. Harness waits up to **600s** for `healthy` before `docker run` cell; cell `WAIT_DB_SECONDS=600`. Use `--probe-timeout 1800` for login probe.        |
| **Image notice**               | Container logs may say `gvenzl/oracle-xe` is legacy and suggest `gvenzl/oracle-free`; matrix still pins `oracle-xe:21-slim` until an intentional image migration.                                           |

```bash
# Start Oracle only (after .env.compose has ORACLE_* set)
docker compose --env-file .env.compose --profile oracle up -d oracle

# Matrix harness (starts profile, waits healthy, network-connects alias, install cell)
python3 docker/scripts/matrix-install-smoke.py --product cms --db oracle --probe-timeout 1800
```

**Live smoke (#2083):** unattended `RESULT:OK` requires the installer Easy Connect service form for `XEPDB1` (SID form `@host:port:XEPDB1` yields ORA-12505). Classic pure-SID operators can still set `DB_SERVER=@host:port:SID` via `-Ddbprops`.

### External DB teardown policy (#1516)

Matrix cells (`perc-matrix-*`) are destroyed after each cell unless `--keep`.
External compose DBs (`percussion-postgres` / `percussion-mysql` / `percussion-sqlserver` / `percussion-oracle`) follow a separate ownership rule:

|    Flag     |            Cells            |                              External DBs                               |
|-------------|-----------------------------|-------------------------------------------------------------------------|
| **Default** | Destroyed                   | Stop services **this process started** (`compose stop`; no volume wipe) |
| `--keep`    | Left running                | Left running (Layer 2 / debugging)                                      |
| `--keep-db` | Destroyed (unless `--keep`) | Left running (reuse across runs)                                        |
| `--stop-db` | Destroyed (unless `--keep`) | Stop **all** external DBs used by the matrix, even if pre-existing      |

If a DB container was already running before the harness (e.g. long-lived dev stack), the default path **reuses** it and **does not** stop it at the end. Use `--stop-db` only when you intentionally want those containers stopped. Never uses `compose down -v` by default (named volumes / operator data are preserved).

### EC2 IMDS / S3 publish from containers (issue #2284)

When CMS runs **on EC2** (including this compose stack on an EC2 host) and uses Amazon S3 publish with **instance profile** credentials:

* Prefer **IMDSv2** (`HttpTokens=required` on Amazon Linux 2023+). CMS probes IMDS with a session token; IMDSv1-only is no longer required for EC2 detection.
* If CMS is **containerized**, set the instance metadata option **`HttpPutResponseHopLimit` ≥ 2** so the IMDSv2 token PUT can leave the container network namespace. Hop limit `1` is a common cause of “not on EC2” false negatives and forced Access Key / Secret fields.
* See system site doc `s3-publish-ec2-imds.md` for the full operator checklist.

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
