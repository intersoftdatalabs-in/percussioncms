# Rhythmyx readiness probe URL matrix (issue #2482)

**Issue:** [#2482](https://github.com/intersoftdatalabs-in/percussioncms/issues/2482) (residual of #2423)
**Slice role:** research / inventory. The deliverable is a **documented probe URL matrix** plus a one-line **default rewire** so existing tools prefer the stronger signal.
**Date:** 2026-08-08

> This is an analysis note for humans/agents — **not** an agent rule file.

## Why this slice exists

[#2462](https://github.com/intersoftdatalabs-in/percussioncms/issues/2462) / [PR #2479](https://github.com/intersoftdatalabs-in/percussioncms/pull/2479) fail-fast on Rhythmyx context-failure markers in Jetty logs (shared helper `docker/scripts/rhythmyx_ready.py`, `perc-devctl.py qa-health`, matrix `wait_for_http`). That gate is sound but **log-scraping is fragile** (rotation, multi-app servers, non-Docker host installs). [#2481](https://github.com/intersoftdatalabs-in/percussioncms/issues/2481) (in-image `rhythmyx_healthcheck.py`) covers Docker HEALTHCHECK.

What is **still missing** is a documented **product-side HTTP probe matrix**: which existing endpoints truly imply the Rhythmyx Spring `ApplicationContext` is up, and which ones only prove Jetty answered. Without a matrix, operators and agents pick whichever URL is convenient and may rely on a probe that says "ready" while the webapp is dead.

## Acceptance criteria status

| Acceptance item | Disposition |
|-----------------|-------------|
| Inventory existing product endpoints that already imply Spring is up and their failure modes when context is dead | **Section: Capability matrix** below |
| Adopt a documented probe URL matrix for host + Docker (or add a minimal readiness path with tests) | **Section: Recommended probe URL matrix** + `PROBE_URL_MATRIX` constant in `docker/scripts/rhythmyx_ready.py` |
| Wire recommendation into `rhythmyx_ready` / qa-health docs | `perc-devctl.py qa-health` default rewire (env `RHYTHMYX_HEALTH_PATH`), `docker/README.md` + `workbench-rest-and-qa-modes.md` pointer |
| Do not invent auth secrets or break public API contracts | **No new endpoint, no new auth, no contract change.** Reuse of the existing `MimeTypeResource.ping()` "pong" + the existing `FoldersResource` REST path. |

## Capability matrix (existing product endpoints)

Endpoints considered as candidates for an HTTP-only readiness probe on the ROOT/Rhythmyx webapp.

| Path | Source | Spring-managed? | Status when **ready** | Status when **Jetty up + Spring webapp context dead** | Verdict |
|------|--------|-----------------|------------------------|-------------------------------------------------------|---------|
| `/Rhythmyx/login` (HTTP GET) | `rxlogin.jsp` (legacy JSP via `PSDispatcherFilter`) — anonymous in `system-security-conf.xml` | **No** — the login form is rendered by the JSP/servlet pipeline; Spring auth runs only on form POST | `200` (HTML form) | **`200` (HTML form)** — the JSP renders even with dead Spring context; only the **POST** fails | **Weak** — already documented as insufficient (#2462 / PR #2479); proves Jetty is up, not Spring |
| `/Rhythmyx/rest/mimetypes` (HTTP GET) | `com.percussion.rest.mimetypes.MimeTypeResource.ping()` (CXF / JAX-RS via Spring) — existing "Ping endpoint for health check" | **Yes** — resource is registered only after Spring adapter scans `@PSSiteManageBean` beans | `200 "pong"` (anonymous) or `200/401/403` (if security filter applies) | **`404`** — CXF servlet cannot resolve the resource because Spring never registered it | **Strong primary** — proves Spring servlet adapter + `@PSSiteManageBean` scan + CXF servlet context up; same ready-code set (200/302/401/403) the assessor already accepts |
| `/Rhythmyx/rest/` (HTTP GET) | `com.percussion.rest.Root` (`@Path("/")`, `@OpenAPIDefinition(servers = {@Server(url = "/rest")})`) | **Yes** — same Spring scan as above | `200` (XML `Root` document) | `404` | Equivalent to `/Rhythmyx/rest/mimetypes` for probe purposes; carries an entity that the probe has to ignore |
| `/Rhythmyx/rest/folders/by-path/Assets` (HTTP GET) | `com.percussion.rest.folders.FoldersResource.findByPath(...)` (CXF / JAX-RS via Spring, **and** sitemanage adaptor graph) | **Yes**, plus a deeper dep graph (sitemanage `IFolderHelper`, `IAclService`, `IPSContentItemDao`) | `200 / 401 / 403 / 404` (asset present / auth required / not present) | `404` (resource not registered) or `500` (deeper init failure) | **Strong secondary** — proves Spring + JAX-RS + sitemanage adaptor stack up. Already used as `VERIFY_CMS_PATH` in `perc-devctl.py`. Slightly heavier probe (sitemanage bean graph). |
| `/Rhythmyx/openapi/openapi.json` | `modules/perc-openapi-webapp` — static JSON, **separate Jetty webapp** at `/openapi` context | **No** — separate webapp, no Rhythmyx context | `200` (static JSON) | `200` (static JSON, webapp is independent of Rhythmyx) | **Misleading** — proves the openapi webapp is up, **not** that the Rhythmyx Spring context is alive. **Do not use** as a Rhythmyx readiness signal. |
| `/Rhythmyx/openapi/index.html` | Same as above (static HTML) | **No** | `200` | `200` | **Misleading** — same as above. |
| `/Rhythmyx/sys_resources/...` (legacy XML apps) | XML application servlet (legacy Rhythmyx) | **Partial** — some XML apps use Spring, some don't; dispatcher is servlet-only | mixed | mixed | **Not recommended** — coverage varies by app; weak, inconsistent signal |

### Why `/Rhythmyx/rest/mimetypes` is the recommended primary

1. **It is the only documented "Ping endpoint for health check"** that exists in the public REST surface (`MimeTypeResource.ping()` returns `"pong"`).
2. **It is Spring-managed**, so a dead context returns `404` (CXF cannot resolve the resource) instead of `200`.
3. **The existing `assess_rhythmyx_ready` ready-code set already covers `200/302/401/403`**, so an auth-protected probe that returns `401` still counts as "endpoint answered" — no probe-side auth wiring needed.
4. **Zero new public surface, zero new secrets, zero contract change** — the endpoint has been there since `MimeTypeResource` was added; we are just adopting it as the documented primary.
5. **Symmetric with `rhythmyx_healthcheck.py`** — the in-image healthcheck already accepts `RHYTHMYX_HEALTH_PATH` as an env override. `perc-devctl.py qa-health` now honors the same env so host, Docker, and matrix cells can agree.

### Why we are not adding a new endpoint

* Adding `ReadinessResource` would duplicate `MimeTypeResource.ping()` and force a new public surface for the same signal.
* Adding `ApplicationContext` injection would create a hard runtime dependency from the rest module on the sitemanage / system module's bean wiring — risk of re-introducing the very cycles #2423 fixed.
* The matrix already gives us a stronger signal than the existing `/Rhythmyx/login` probe at the cost of one line of `qa-health` defaults.

A new minimal endpoint **may be added in a follow-up** if operators demand a JSON body with a timestamp / build info; this slice does not need it.

## Recommended probe URL matrix

The matrix below is **the recommendation** for `docker/scripts/rhythmyx_ready.py`, `perc-devctl.py qa-health`, in-image `rhythmyx_healthcheck.py`, and any external orchestrator (Docker HEALTHCHECK, K8s readinessProbe, etc.).

| Environment | Primary (fast) | Secondary (deeper) | Notes |
|-------------|----------------|--------------------|-------|
| **Host install** (developer machine, `scripts/install-cms-dev.py`) | `GET /Rhythmyx/rest/mimetypes` → `200 "pong"` (or `401/403` if auth required) | `GET /Rhythmyx/rest/folders/by-path/Assets` → `200/401/403/404` | Always combine with `rhythmyx_ready` log scan (`Failed startup of context` etc.) for **fail-fast** when context dies after probe answered |
| **Docker compose `cms-dts`** (`perc-devctl.py up` / `verify`) | `GET /Rhythmyx/rest/mimetypes` | `GET /Rhythmyx/rest/folders/by-path/Assets` (already `VERIFY_CMS_PATH`) | Same. `verify` already scans `cms-dts` logs via `rhythmyx_ready` (#2462). |
| **QA / matrix cell** (`perc-devctl.py qa-up` / `qa-health`) | `GET /Rhythmyx/rest/mimetypes` (new `QA_CMS_PROBE_PATH` default; override via env `RHYTHMYX_HEALTH_PATH`) | n/a for `qa-health` (single URL); matrix `verify` uses the folders path via `VERIFY_CMS_PATH` | `qa-health` keeps one URL; matrix cells delegate to the same shared `rhythmyx_ready` assessor so signals are uniform |
| **In-image Docker HEALTHCHECK** (`rhythmyx_healthcheck.py`) | `GET /Rhythmyx/rest/mimetypes` (env `RHYTHMYX_HEALTH_PATH`, default already `/Rhythmyx/login` in 8.2.x — operator opt-in to switch) | n/a (single URL) | Same log-scan markers as the host assessor (#2481). |
| **External orchestrator** (Kubernetes, ECS, Nomad, …) | `GET /Rhythmyx/rest/mimetypes` (Liveness **and** Readiness — same signal for both) | n/a (single URL) | Treat **any context-failure marker** in the in-container log as readiness=NOT-ready; restart on sustained liveness failure |

### How to wire

| Caller | Override | Default after this PR |
|--------|----------|-----------------------|
| `perc-devctl.py qa-health` | `--url URL` (CLI) **or** `RHYTHMYX_HEALTH_PATH` env var | `http://127.0.0.1:<port>/Rhythmyx/rest/mimetypes` |
| `docker/scripts/rhythmyx_healthcheck.py` (in-image HEALTHCHECK) | `--cms-path` (CLI) **or** `RHYTHMYX_HEALTH_PATH` env var | Unchanged (still `/Rhythmyx/login`) — opt in to the matrix by exporting the env var; this avoids a silent behavior change for installed CMS images until the matrix image is rebuilt |
| `perc-devctl.py verify` / `verify-fix` | `VERIFY_CMS_URL` (full URL) | `http://localhost:<port>/Rhythmyx/rest/folders/by-path/Assets` (unchanged — already the deeper secondary) |
| `docker/scripts/matrix-install-smoke.py` | `--cms-probe-path` (CLI) **or** `CMS_PROBE_PATH` env var | Unchanged (`/Rhythmyx/login`) — historical constant; the matrix doc tells operators to override |

### Operator workflow (host + Docker)

```bash
# Host install — verify readiness
python docker/scripts/perc-devctl.py qa-health                 # uses new /Rhythmyx/rest/mimetypes default
python docker/scripts/perc-devctl.py qa-health --url http://127.0.0.1:9992/Rhythmyx/rest/mimetypes

# In-image HEALTHCHECK — opt in to the matrix
# docker run -e RHYTHMYX_HEALTH_PATH=/Rhythmyx/rest/mimetypes …

# External orchestrator — same env var
# Kubernetes readinessProbe.httpGet.path: /Rhythmyx/rest/mimetypes
# readinessProbe.httpGet.port: 9992
```

### Operator signal (unchanged from #2462)

| RESULT | Meaning |
|--------|---------|
| `RESULT:OK STEP:qa-health` | HTTP ready **and** no context-failure markers in logs |
| `RESULT:FAIL … DETAIL:rhythmyx_context_failed` | Dead Rhythmyx context — do not run Playwright |
| `RESULT:FAIL … DETAIL:http_not_ready` | Probe URL never answered with a ready code; re-check after rebuild |

The matrix only changes **what URL gets probed**. The fail-fast log scan, the assessor contract, and the operator signal set are unchanged — operators do not need to learn a new contract.

## Companion changes (in this PR)

| File | Change |
|------|--------|
| `docker/scripts/rhythmyx_ready.py` | Add `PROBE_URL_MATRIX` constant + `assess_probe_url()` helper + `DEFAULT_PROBE_URL_PRIMARY` / `DEFAULT_PROBE_URL_SECONDARY` exports |
| `docker/scripts/perc-devctl.py` | `QA_CMS_PROBE_PATH` default → `/Rhythmyx/rest/mimetypes`; honor `RHYTHMYX_HEALTH_PATH` env var in `cmd_qa_health`; update module docstring + result line URL comment |
| `docker/scripts/test_rhythmyx_ready.py` | New tests for `PROBE_URL_MATRIX` integrity and `assess_probe_url` |
| `docker/scripts/test_perc_devctl.py` | Update the constant assertion that pinned `/Rhythmyx/login` to the new default; add env-override test |
| `docs/developer-module/workbench-rest-and-qa-modes.md` | One-line pointer to the matrix in the QA mode / health-check section |
| `docker/README.md` | Add matrix row to the **Rhythmyx ApplicationContext fail-fast** signal table |

## What this PR does **not** change

* No Maven modules touched (no `clean install` required; this PR is scripts + docs only).
* No new public REST surface, no new contracts, no new auth secrets.
* `rhythmyx_ready.py` API is additive (`HTTP_READY_CODES`, `RHYTHMYX_CONTEXT_FAIL_MARKERS`, `DETAIL_CONTEXT_FAILED`, `assess_rhythmyx_ready`, `find_rhythmyx_context_failure`, `is_http_ready_code` all unchanged).
* `qa-health` operator contract (`RESULT:OK|FAIL STEP:qa-health …`) unchanged; only the default URL flips from `/Rhythmyx/login` to `/Rhythmyx/rest/mimetypes`.
* In-image `rhythmyx_healthcheck.py` default path unchanged; operators opt in via env var (no silent behavior change for already-shipped CMS images).

## Residual recommendations (follow-up issues)

1. **Optional new minimal endpoint** — `GET /Rhythmyx/rest/system/health` returning JSON `{ "status": "ready", "webapp": "Rhythmyx", "ts": "…" }`. Only needed if operators want a stable, versioned health surface; defer to a follow-up — this slice is the inventory.
2. **Switch in-image `rhythmyx_healthcheck.py` default** — once the matrix image is rebuilt and rolled out, change `DEFAULT_CMS_PATH` to `/Rhythmyx/rest/mimetypes` (no env var required). File as a small follow-up PR after #2481 lands.
3. **Apply matrix in `matrix-install-smoke.py`** — change `CMS_PROBE_PATH` default to match `QA_CMS_PROBE_PATH` so host + matrix agree out of the box. File as a small follow-up PR after this one lands.
4. **Add `qa-health --secondary-url`** — probe both primary + secondary and emit `RESULT:OK STEP:qa-health PRIMARY_HTTP:… SECONDARY_HTTP:…` for stronger guarantees. Defer until operators ask.

## Cross-references

* Parent: [#2423](https://github.com/intersoftdatalabs-in/percussioncms/issues/2423) — Spring circular dependency blocks Jetty Rhythmyx
* Interim gate: [#2462](https://github.com/intersoftdatalabs-in/percussioncms/issues/2462) / [PR #2479](https://github.com/intersoftdatalabs-in/percussioncms/pull/2479) — log-scan fail-fast
* Docker HEALTHCHECK: [#2481](https://github.com/intersoftdatalabs-in/percussioncms/issues/2481) — in-image `rhythmyx_healthcheck.py`
* Rebuild chain preflight: [#2486](https://github.com/intersoftdatalabs-in/percussioncms/issues/2486) — companion of this slice
* Existing ping: `rest/src/main/java/com/percussion/rest/mimetypes/MimeTypeResource.java` (`@GET ping()` → `"pong"`)
* Existing assessor: `docker/scripts/rhythmyx_ready.py`
* In-image healthcheck: `docker/scripts/rhythmyx_healthcheck.py`
* Host CLI: `docker/scripts/perc-devctl.py` (`cmd_qa_health`)
* Operator docs: `docker/README.md`, `docs/developer-module/workbench-rest-and-qa-modes.md`
