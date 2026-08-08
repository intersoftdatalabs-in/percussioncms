# Workbench replacement REST + Dev / QA test modes

|    Field     |                                                                                                                                        Value                                                                                                                                        |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**   | **Direction of record** for agents and humans                                                                                                                                                                                                                                       |
| **Audience** | Anyone adding Developer-module (Workbench replacement) APIs or Playwright against CMS                                                                                                                                                                                               |
| **Related**  | [rest/AGENTS.md](../../rest/AGENTS.md), [sitemanage/AGENTS.md](../../projects/sitemanage/AGENTS.md), [perc-qa-automation/AGENTS.md](../../modules/perc-qa-automation/AGENTS.md), [adaptor-design-ws-audit.md](../ai-generated/tasks/developer-module-p0/adaptor-design-ws-audit.md) |

---

## 1. Workbench replacement REST (HARD RULES)

Classic design tools went **SOAP → design webservices (`IPS*DesignWs` / `*DesignSOAPImpl`) → system**.  
The modern SPA must **not** reimplement Workbench by gluing partial sitemanage “already REST-like” product APIs just because they are convenient.

### Required shape

```text
Workbench / SOAP design operations  (behavioral reference)
        │
        ▼
Clean public REST  (rest module: resource + wire DTOs + IXxxAdaptor)
        │
        ▼
Thin sitemanage apibridge  (@PSSiteManageBean implements IXxxAdaptor)
        │
        ▼
Same backing capabilities SOAP used
  (prefer IPS*DesignWs / assembly design WS / system design WS when they exist)
        │
        ▼
perc-system / objectstore / services
```

|              Layer              |                Module                |                      Role                      |
|---------------------------------|--------------------------------------|------------------------------------------------|
| HTTP + OpenAPI + DTOs           | **`rest`**                           | New, intentional Workbench-replacement surface |
| Adaptor interface               | **`rest`**                           | Contract only — **no** sitemanage dependency   |
| Adaptor impl                    | **`sitemanage` apibridge**           | Thin map to design/system call sites           |
| Domain truth for design objects | **webservices design APIs + system** | Same stack Workbench trusted                   |

### DO

1. Design the **REST contract** for the FR (list/detail/save/lock gaps explicit).
2. Use **SOAP / `IPS*DesignWs` method lists** as the reference for operations, locks, and failure modes.
3. Implement adaptors that call those design/system APIs when they exist (see audit matrix).
4. Keep sitemanage as **glue**, not a second design product.
5. Document `designGaps[]` when REST is thinner than Workbench.

### DO NOT

1. **Lazy-wire** an existing sitemanage/CM1 endpoint into Developer because it “looks REST.”
2. Invent a **parallel domain stack** (new DAO / random managers) that Workbench never used for that design object **when** a design WS already owns it.
3. Put design-object business logic only in the SPA or only in rest without a clear system/design backend.
4. Depend **rest → sitemanage** (reactor cycle).

### Not every surface is design-WS

Some catalogs have **no** SOAP design twin (e.g. CE control managers, extension registry, server config files). Those are **ALT** paths: still **clean REST + thin adaptor**, backend = the real system owner, documented in the audit — not “use a random sitemanage REST.”

---

## 2. Test environments: **dev mode** vs **QA mode**

Two supported modes. Agents must know which they are in. **Developers/agents do not invent one-off “copy jars by hand forever” workflows** as the product process — they use these modes; **automation owns packaging and full installs**.

### Dev mode (fast iteration)

**Purpose:** day-to-day feature + Playwright development on a real CMS tree.

|     Piece      |                                                          Expectation                                                          |
|----------------|-------------------------------------------------------------------------------------------------------------------------------|
| CMS            | **Local install on the developer machine** (e.g. `C:\Installs\…`, `/opt/Percussion`)                                          |
| Docker         | **Binds/mounts that install** (or otherwise uses that tree) — not a throwaway empty container as the only copy of the product |
| Code changes   | Build → **copy artifacts into the install webapp** (hot deploy)                                                               |
| Server restart | **Not required** for typical JS/CSS/JSP (and other hot-copy paths); see WebUI / perc-qa-automation hot-deploy tables          |
| Tests          | Playwright against `DEV_PERCUSSION_INSTALL` / discovered URL (e.g. `localhost:9992`)                                          |
| Loop           | edit → build/copy → **re-run test** (cache-buster on SPA URLs when needed)                                                    |

**Agent rules (dev mode):**

1. Set `DEV_PERCUSSION_INSTALL` (or rely on project `.env`); never hardcode another machine’s path in committed files.
2. Prefer **hot copy** over full reinstall for UI/rest jar iteration when docs allow.
3. **Do not** treat “I didn’t manually redeploy” as an acceptable reason to skip Playwright for a screen change — use the install + hot copy path.
4. Full reinstall of a local install is **ops/automation**, not the default every PR.

### QA mode (contained pass/fail)

**Purpose:** clean, reproducible gate — “does this revision work?”

|      Piece      |                                    Expectation                                    |
|-----------------|-----------------------------------------------------------------------------------|
| CMS             | **Fully contained in Docker** (image/stack owns the install)                      |
| Host install    | Not required for the test run                                                     |
| Code under test | Built into the image / stack under test (or the pipeline’s defined artifact path) |
| Tests           | Playwright (or suite) runs **against that stack only**                            |
| Outcome         | **Pass or fail** — no “works on my July-29 folder” caveat                         |

**Agent rules (QA mode):**

1. Do not assume a host bind mount or `C:\Installs\…`.
2. Do not ask the developer to copy jars into a personal install to “make QA green.”
3. Failures are **product or pipeline** defects; file issues, don’t invent local-only workarounds as the fix.

#### H2 Docker one-shot (agents / operators) — up → health → down

Productized entrypoint on `docker/scripts/perc-devctl.py` (cross-platform Python; no shell required). Implements **#1827** slice 1 / **#1927**: CMS on **H2 in Docker**, published URL, no host install.

**Prereq (once per machine / after installer or sitemanage/WebUI SNAPSHOT changes):** rebuild the chain that feeds the installer. The matrix cell **bind-mounts** `modules/perc-distribution-tree/target/perc-distribution-tree.jar`; that jar unpacks **`WebUI/target/perc-web-ui-*.war`** into `Rhythmyx/WEB-INF/lib` (including `sitemanage-*.jar`). Packaging only `perc-distribution-tree` after a sitemanage-only install **does not** pick up a new sitemanage if the WebUI WAR is stale.

```bash
# After sitemanage (or other Rhythmyx WEB-INF/lib) changes — full QA rebuild chain:
cd projects/sitemanage && ../../mvnw clean install   # or package if tests already green
cd ../../WebUI && ../mvnw package -DskipTests
cd ../modules/perc-distribution-tree && ../../mvnw clean package -DskipTests
# Windows: use mvnw.cmd and the same module order
```

If only installer packaging scripts/resources changed (no Java SNAPSHOT under the WAR):

```bash
cd modules/perc-distribution-tree && ../../mvnw package -DskipTests
# Windows: cd modules\perc-distribution-tree && ..\..\mvnw.cmd package -DskipTests
```

**Post-cycle-fix smoke (#2423 / #2437):** after the rebuild chain, `qa-up` logs must show `ServletContextHandler] Started` for ROOT/Rhythmyx and **must not** contain `BeanCurrentlyInCreationException` / `Failed startup of context`. `qa-health` → login page HTTP 200/302 is the operator gate before Playwright.

**Lifecycle (always use this order for unattended QA):**

```bash
# 1) UP — silent install + start CMS on H2; waits until /Rhythmyx/login is ready
python docker/scripts/perc-devctl.py qa-up
# optional: --timeout-seconds 900  --skip-image-build
# Host port: QA_CMS_HOST_PORT / CMS_HOST_PORT env, else preferred 9993 when free, else freeport.
# qa-up prints QA_CMS_HOST_PORT=… and TEST_CMS_URL=http://127.0.0.1:<port>

# 2) HEALTH — re-check readiness (clear RESULT:FAIL + timeout if not ready).
#    Also fail-fast if docker logs show Rhythmyx ApplicationContext death
#    (Failed startup of context / BeanCurrentlyInCreationException) even when
#    Jetty HTTP still answers (#2462 / #2423). Do not treat port-up alone as ready.
python docker/scripts/perc-devctl.py qa-health
# optional: --timeout-seconds 120  --interval-seconds 5
# DETAIL:rhythmyx_context_failed → cell unusable; inspect docker logs; do not run Playwright

# 3) Playwright against the stack only — no DEV_PERCUSSION_INSTALL
#    (#2064 env + #2065 golden smoke + #1929 surface filter)
#    Use the TEST_CMS_URL printed by qa-up (do not hardcode :9993 — multi-worktree freeport).
#    Auth helpers resolve: TEST_CMS_URL > QA_CMS_HOST_PORT/CMS_HOST_PORT > DEV_PERCUSSION_URL
#    > install discovery > fallback. Unit tests: npm run test:unit in perc-qa-automation/frontend.
#    Prefer path / --grep / tag filters (full suite is not the agent default).
#    cd modules/perc-qa-automation/frontend
#    TEST_CMS_URL=http://127.0.0.1:$QA_CMS_HOST_PORT  TEST_DB_TYPE=h2  TEST_PRODUCT=cms
#    ADMIN_USERNAME=Admin  ADMIN_PASSWORD=... (from qa-up output / docker exec — never commit)
#    # Golden unattended smoke (#2065): login + Content Explorer product screen
#    npm run test:golden
#    # or surface filter: npm run test:surface -- --path tests/golden-unattended-smoke.spec.js
#    # or: npm run test:surface -- --path tests/login.spec.js
#    # or: npx playwright test tests/login.spec.js --grep "Admin"
#    # or: npm run test:surface -- --tag golden
#    Failure artifacts: modules/perc-qa-automation/frontend/test-results/ (+ playwright-report/)
#    Attach conventions: docs/developer-module/playwright-failure-artifacts.md (#2066)

# 4) DOWN — destroy the cell; frees the published host port; no multi-GB orphans by default
python docker/scripts/perc-devctl.py qa-down
```

|  Output / constant   |                                  Value                                   |
|----------------------|--------------------------------------------------------------------------|
| Published base URL   | `TEST_CMS_URL` from `qa-up` (`http://127.0.0.1:<port>`)                  |
| Preferred baseline   | Host port `9993` when free and no env override                           |
| Env override         | `QA_CMS_HOST_PORT` or `CMS_HOST_PORT` (matrix docker `-p` uses the same) |
| Probe path           | `/Rhythmyx/login`                                                        |
| Container name       | `perc-matrix-cms-h2`                                                     |
| Admin user           | `Admin` (password from install generated passwords)                      |
| RESULT line contract | `RESULT:OK\|FAIL STEP:qa-up\|qa-health\|qa-down LOG:…`                   |
| Context fail-fast    | `DETAIL:rhythmyx_context_failed MATCH:…` when Jetty logs show dead Rhythmyx Spring context (#2462); helper `docker/scripts/rhythmyx_ready.py` |

**Tear-down policy:** `qa-down` runs `docker rm -f` on the QA cell. The install lives **inside** the container (no named multi-GB volume by default), so removing the container frees ports and disk. Prefer `qa-down` after every agent session; do not leave `perc-matrix-cms-h2` running overnight unless debugging.

**Dry-run (no docker):** `python docker/scripts/perc-devctl.py qa-up --dry-run` (and `qa-health` / `qa-down` likewise). Unit tests: `python -m pytest docker/scripts/test_perc_devctl.py -q`.

**Two-worktree concurrent freeport smoke (#2006):** when two agent worktrees share a host, published ports must not collide. Operator checklist + CI dry-run (no CMS install):

```bash
# Allocation-only (preferred → freeport → env override → tear-down)
python docker/scripts/freeport-concurrent-smoke.py
# Expect: RESULT:OK STEP:freeport-concurrent-smoke
```

Full tiers (CLI dry-run, live sequential `qa-up` / `up`, env override, tear-down port free): see [docker/README.md](../../docker/README.md) → **Two-worktree concurrent freeport smoke**. Shared helpers: `docker/scripts/perc_host_ports.py` (used by `perc-devctl` + `matrix-install-smoke`). Parent freeport epic [#2001](https://github.com/intersoftdatalabs-in/percussioncms/issues/2001); sibling residual [#2004](https://github.com/intersoftdatalabs-in/percussioncms/issues/2004).

Equivalent low-level harness (same cell): `python docker/scripts/matrix-install-smoke.py --product cms --db h2 --keep` then destroy with `docker rm -f perc-matrix-cms-h2`. Prefer `perc-devctl.py qa-*` for agents.

#### Playwright surface filter (PR surface under test) — #1827 slice 3 / #1929

Agents and operators validating a UI change must run a **subset** of specs for the surface under test — not the full Playwright suite by default.

|     Mechanism      | Native CLI (from `modules/perc-qa-automation/frontend`) |                       Helper / npm                        |
|--------------------|---------------------------------------------------------|-----------------------------------------------------------|
| Path               | `npx playwright test tests/login.spec.js`               | `npm run test:surface -- --path tests/login.spec.js`      |
| Title grep         | `npx playwright test --grep "Admin login"`              | `npm run test:surface -- --grep "Admin login"`            |
| Tag                | `npx playwright test --grep @smoke`                     | `npm run test:surface -- --tag smoke`                     |
| List only (no CMS) | `npx playwright test --list tests/login.spec.js`        | `npm run test:surface:list -- --path tests/login.spec.js` |

Env aliases: `SURFACE_PATH` / `SURFACE_PATHS`, `SURFACE_GREP`, `SURFACE_TAG`, `SURFACE_GREP_INVERT`.  
Arg builder (unit-tested, no live CMS): `frontend/tests/helpers/surface-filter.js` + `npm run test:unit`.  
Details: [perc-qa-automation/README.md](../../modules/perc-qa-automation/README.md) → **Surface filter**.

**Failure artifacts:** `modules/perc-qa-automation/frontend/test-results/` and `frontend/playwright-report/`. Attach conventions: [playwright-failure-artifacts.md](./playwright-failure-artifacts.md) (#2066) when present.

**Related slices:** Playwright env `TEST_CMS_URL` without host install → #1928 / #2064; golden smoke → #2065; optional CI job → #1930 (workflow + section below).

#### Agent instructions (rule files applied — #2073)

Root / module `AGENTS.md` and night-issue workflow rules encode this process (human-approved apply: **#2073**). Binding product process for unattended UI work:

1. **Do not blanket-skip Playwright** for a user-visible surface solely because `agent_safe_only` is true **when** the H2 QA path is available: `perc-devctl qa-up` → `TEST_CMS_URL` → **surface-filtered** Playwright → `qa-down`.
2. Prefer **path / grep / tag** surface filters (`npm run test:surface` or native CLI). Do not run the full suite as the default overnight gate.
3. List-only checks (`--list` / `npm run test:surface:list`) verify the filter without a live CMS; live runs need `TEST_CMS_URL` from `qa-up` (freeport contract — never hardcode `:9993` alone).
4. Collect failure artifacts under `frontend/test-results/` (+ HTML report); attach per #2066 when that doc is present.
5. **Still skip** work that needs host install only, secrets, or multi-RDBMS matrix unless the issue explicitly opts into those.

**Rule files (source of truth for agents):**

| File | Content |
|------|---------|
| `modules/perc-qa-automation/AGENTS.md` | **QA mode surface filter** — `test:surface`, `TEST_CMS_URL` / `qa-up`, artifacts |
| Root `AGENTS.md` | Unattended path = H2 QA + surface filter (not permanent Playwright skip) |
| `.grok/workflows/night-issue-prs.rhai` + README | `agent_safe_only`: host-install / secrets still skipped; **QA-mode surface Playwright** allowed |

**Failure artifacts (night-issue attach):** when Playwright fails in QA or dev mode, collect paths under `modules/perc-qa-automation/frontend` (`test-results/`, optional `playwright-report/`, screenshots) and attach them to the PR/issue using the conventions in [playwright-failure-artifacts.md](./playwright-failure-artifacts.md). CI artifact upload is wired in the optional workflow below (#1930).

#### Optional CI: `workflow_dispatch` + path-filtered wiring check (#1827 slice 4 / #1930)

Workflow file: [`.github/workflows/h2-qa-playwright.yml`](../../.github/workflows/h2-qa-playwright.yml)  
Name in Actions UI: **H2 QA Playwright (optional)**

|                     Trigger                      |                                                   What runs                                                    |                            Blocks merge?                             |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| **`workflow_dispatch`** `mode=dry-run` (default) | Freeport smoke + `perc-devctl` qa-\* dry-run + `npm run test:unit` + surface **list/print**                    | No (manual only)                                                     |
| **`workflow_dispatch`** `mode=live`              | Package installer (unless `skip_package`) → `qa-up` → surface/golden Playwright → upload artifacts → `qa-down` | No (manual only)                                                     |
| **`pull_request`** path-filtered                 | Same as dry-run wiring only (no live CMS)                                                                      | **No** — must stay **non-required** unless product owners promote it |

**Path filters (PR):** changes under `modules/perc-qa-automation/**`, selected `docker/scripts/*` (perc-devctl / freeport / matrix), this doc + playwright artifact doc, and the workflow YAML itself. Unrelated monorepo PRs do **not** get this check.

**Hard bans for operators:**

1. Do **not** mark this workflow’s jobs as branch-protection **required** checks without an explicit product decision (parent non-goal: full Playwright suite on every PR).
2. Do **not** commit passwords. Live mode reads `ADMIN_PASSWORD` from `qa-up` stdout (masked in logs) or optional Actions secret `QA_ADMIN_PASSWORD`.
3. Do **not** hardcode host port `:9993` — use `TEST_CMS_URL` / `QA_CMS_HOST_PORT` from freeport/`qa-up`.

##### How to trigger (operators / agents)

```text
GitHub → Actions → "H2 QA Playwright (optional)" → Run workflow
  mode: dry-run | live
  surface_path: tests/login.spec.js   # default; override for PR surface
  surface_grep / surface_tag: optional filters
  skip_package: false                 # live only; true if installer already on runner
  qa_up_timeout_seconds: 900
```

CLI equivalent:

```bash
# Dry-run wiring (same idea as the PR path-filtered job)
gh workflow run "H2 QA Playwright (optional)" --ref main -f mode=dry-run

# Live H2 stack + Playwright (heavy: package + Docker)
gh workflow run "H2 QA Playwright (optional)" --ref main \
  -f mode=live \
  -f surface_path=tests/login.spec.js
```

##### Env contract (live job)

|                    Variable                     |                Source                 |            Notes            |
|-------------------------------------------------|---------------------------------------|-----------------------------|
| `TEST_CMS_URL`                                  | `qa-up` stdout                        | Prefer over hardcoded ports |
| `QA_CMS_HOST_PORT`                              | `qa-up` / freeport                    | Published host port         |
| `ADMIN_USERNAME`                                | fixed `Admin`                         | Documented QA default       |
| `ADMIN_PASSWORD`                                | `qa-up` or secret `QA_ADMIN_PASSWORD` | Never in YAML or git        |
| `TEST_DB_TYPE`                                  | `h2`                                  | QA cell                     |
| `TEST_PRODUCT`                                  | `cms`                                 | QA cell                     |
| `SURFACE_PATH` / `SURFACE_GREP` / `SURFACE_TAG` | workflow inputs                       | Surface filter (#1929)      |

##### Artifacts (download after a failed live run)

|    Artifact name pattern    |                                      Contents                                      |
|-----------------------------|------------------------------------------------------------------------------------|
| `playwright-h2-qa-<run_id>` | `test-results/` + `playwright-report/` under `modules/perc-qa-automation/frontend` |
| `h2-qa-stack-logs-<run_id>` | `docker/logs/` best-effort                                                         |

UI: Actions run → **Artifacts**. CLI: `gh run download <run-id> -n playwright-h2-qa-<run-id>`.  
Night-issue attach conventions (when not using GHA): [playwright-failure-artifacts.md](./playwright-failure-artifacts.md).

##### Dependencies / first green caveats

- **Installer package:** live mode runs `modules/perc-distribution-tree` `mvnw package -DskipTests` unless `skip_package=true`. This is the heavy step on free GHA runners (disk/time). Self-hosted or pre-baked installer + `skip_package` is supported.
- **Golden smoke (#2065):** `tests/golden-unattended-smoke.spec.js` (Admin login + Content Explorer shell). npm: `npm run test:golden`. Optional CI default may still use `tests/login.spec.js`; prefer the golden path for agent one-shot proof.
- **Docker:** live job needs Docker on the runner (`ubuntu-latest` provides it). Dry-run / PR path does **not**.

##### Windows-local parity (same product path, no GHA)

Agents and humans on Windows use the same commands as Linux; only path separators and the Maven wrapper differ:

```bat
REM From repo root (PowerShell / cmd)
cd modules\perc-distribution-tree
..\..\mvnw.cmd package -DskipTests
cd ..\..
python docker\scripts\perc-devctl.py qa-up
REM capture TEST_CMS_URL=… and ADMIN_PASSWORD=… from stdout (freeport — do not hardcode 9993)
cd modules\perc-qa-automation\frontend
set TEST_CMS_URL=http://127.0.0.1:%QA_CMS_HOST_PORT%
set ADMIN_USERNAME=Admin
set ADMIN_PASSWORD=<from-qa-up>
npm ci
npx playwright install chromium
REM Golden unattended smoke (#2065): login + Content Explorer product screen
npm run test:golden
REM Or surface path: npm run test:surface -- --path tests/golden-unattended-smoke.spec.js
REM Or login-only: npm run test:surface -- --path tests/login.spec.js -- --trace=retain-on-failure --screenshot=only-on-failure --reporter=line,html
cd ..\..\..
python docker\scripts\perc-devctl.py qa-down
```

Cross-link: [perc-qa-automation README](../../modules/perc-qa-automation/README.md) (surface filter + QA mode). Python entrypoint is portable (`python docker/scripts/perc-devctl.py`); do not invent shell-only wrappers as the only supported path.

### What is *not* a supported long-term process

- “Merged to `development` so the shared QA box should magically update” without **automation**.
- “Agents must remember to copy `rest.jar` and restart Jetty” as the primary workflow.
- Using **dev-mode local install drift** as the only CI signal for Workbench-replacement APIs.

**Automation (target):** pipelines (or `perc-devctl` / matrix scripts) own **build → package → install or image → start → Playwright**. Dev mode remains the fast loop; QA mode remains the contained gate.

---

## 3. Pointers for agents

|               Need                |                                       Doc                                        |
|-----------------------------------|----------------------------------------------------------------------------------|
| REST adaptor layout               | `rest/AGENTS.md`, `projects/sitemanage/AGENTS.md`                                |
| Which adaptors hit design WS      | `docs/ai-generated/tasks/developer-module-p0/adaptor-design-ws-audit.md`         |
| Playwright + hot copy             | `modules/perc-qa-automation/AGENTS.md`, `WebUI/AGENTS.md`                        |
| Playwright failure attach         | [playwright-failure-artifacts.md](./playwright-failure-artifacts.md)             |
| QA surface filter (path/grep/tag) | `modules/perc-qa-automation/README.md` → Surface filter; `npm run test:surface`  |
| Unattended H2 QA mode             | This file §2 (qa-up → surface filter → qa-down); epic #1827                      |
| Optional CI H2 QA + Playwright    | This file §2 → **Optional CI** (#1930); `.github/workflows/h2-qa-playwright.yml` |
| FR parity                         | `docs/developer-module/workbench-functional-inventory.md`                        |
| Progress tracker                  | GitHub **#1690** (post-P0); closed **#1622** was P0                              |

When adding a Workbench-replacement API or Playwright coverage, **read this file first** and state **dev mode** vs **QA mode** in the PR body.
