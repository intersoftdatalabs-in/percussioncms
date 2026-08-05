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

**Prereq (once per machine / after installer changes):** package the customer CMS installer assembly:

```bash
cd modules/perc-distribution-tree && ../../mvnw package -DskipTests
# Windows: cd modules\perc-distribution-tree && ..\..\mvnw.cmd package -DskipTests
```

**Lifecycle (always use this order for unattended QA):**

```bash
# 1) UP — silent install + start CMS on H2; waits until /Rhythmyx/login is ready
python docker/scripts/perc-devctl.py qa-up
# optional: --timeout-seconds 900  --skip-image-build
# Host port: QA_CMS_HOST_PORT / CMS_HOST_PORT env, else preferred 9993 when free, else freeport.
# qa-up prints QA_CMS_HOST_PORT=… and TEST_CMS_URL=http://127.0.0.1:<port>

# 2) HEALTH — re-check readiness (clear RESULT:FAIL + timeout if not ready)
python docker/scripts/perc-devctl.py qa-health
# optional: --timeout-seconds 120  --interval-seconds 5

# 3) Playwright against the stack only — no DEV_PERCUSSION_INSTALL (#2064 / #1928 slice A)
#    Use the TEST_CMS_URL printed by qa-up (do not hardcode :9993 — multi-worktree freeport).
#    Auth helpers resolve: TEST_CMS_URL > QA_CMS_HOST_PORT/CMS_HOST_PORT > DEV_PERCUSSION_URL
#    > install discovery > fallback. Unit tests: npm run test:unit in perc-qa-automation/frontend.
#    TEST_CMS_URL=http://127.0.0.1:$QA_CMS_HOST_PORT  TEST_DB_TYPE=h2  TEST_PRODUCT=cms
#    ADMIN_USERNAME=Admin  ADMIN_PASSWORD=... (from qa-up output / docker exec — never commit)
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

Playwright env defaults, surface filters, and CI jobs are **later slices** of #1827 (#1928–#1930).

**Failure artifacts (night-issue attach):** when Playwright fails in QA or dev mode, collect paths under `modules/perc-qa-automation/frontend` (`test-results/`, optional `playwright-report/`, screenshots) and attach them to the PR/issue using the conventions in [playwright-failure-artifacts.md](./playwright-failure-artifacts.md). Full CI upload pipeline remains #1930.

### What is *not* a supported long-term process

- “Merged to `development` so the shared QA box should magically update” without **automation**.
- “Agents must remember to copy `rest.jar` and restart Jetty” as the primary workflow.
- Using **dev-mode local install drift** as the only CI signal for Workbench-replacement APIs.

**Automation (target):** pipelines (or `perc-devctl` / matrix scripts) own **build → package → install or image → start → Playwright**. Dev mode remains the fast loop; QA mode remains the contained gate.

---

## 3. Pointers for agents

|             Need             |                                   Doc                                    |
|------------------------------|--------------------------------------------------------------------------|
| REST adaptor layout          | `rest/AGENTS.md`, `projects/sitemanage/AGENTS.md`                        |
| Which adaptors hit design WS | `docs/ai-generated/tasks/developer-module-p0/adaptor-design-ws-audit.md` |
| Playwright + hot copy        | `modules/perc-qa-automation/AGENTS.md`, `WebUI/AGENTS.md`                |
| Playwright failure attach    | [playwright-failure-artifacts.md](./playwright-failure-artifacts.md)     |
| FR parity                    | `docs/developer-module/workbench-functional-inventory.md`                |
| Progress tracker             | GitHub **#1690** (post-P0); closed **#1622** was P0                      |

When adding a Workbench-replacement API or Playwright coverage, **read this file first** and state **dev mode** vs **QA mode** in the PR body.
