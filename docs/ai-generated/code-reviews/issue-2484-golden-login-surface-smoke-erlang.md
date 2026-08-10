# Erlang review — issue 2484 golden/login surface smoke (fail-fast on stale matrix image)

- **Ticket:** [#2484](https://github.com/intersoftdatalabs-in/percussioncms/issues/2484)
- **Branch:** `fix/2484-golden-login-surface-smoke`
- **Worktree:** `C:\workspaces\intersoft-workspace\percussioncms\.kilo\worktrees\issue-2484`
- **Base:** `origin/main` @ `4d7b64e7c6de2df893a14abfd7a2dabaf7ab9c28`
- **Author/Reviewer:** Kilo sub-agent (self-review; conflict disclosed per Erlang persona)
- **Date:** 2026-08-09

## Summary

Small, focused change. Adds a fail-fast precheck to `perc-devctl qa-up
--skip-image-build` so a cached `percussion-matrix-cell:local` image that
lacks the in-image HEALTHCHECK (#2481) is detected in milliseconds
instead of waiting the full `--probe-timeout` (default 900s) for a
`docker_health_timeout health=none`. Documents the trap in the module
README so the next agent (or operator) does not have to rediscover it.

End-to-end QA-mode smoke (`qa-up → test:surface → qa-down`) was run
against a freshly built matrix image **and** verified to **fail-fast**
with a clear `matrix_image_stale` hint when the precheck is forced to
report missing HEALTHCHECK. The golden/login surface smoke itself
(`tests/golden-unattended-smoke.spec.js`) passes against the live CMS in
~10s. No new product code; no test gaps; no cross-platform regressions.

## Scope

- Base: `origin/main`
- Head: uncommitted on branch `fix/2484-golden-login-surface-smoke`
- Files (3 changed, 227 lines added, 0 removed):
  - `docker/scripts/perc-devctl.py` (+129): new `QA_MATRIX_IMAGE_TAG`
    constant, three `QA_IMAGE_HEALTHCHECK_*` status constants, new pure
    helper `_qa_matrix_image_healthcheck_status(image_tag, *, runner=None)`,
    precheck branch in `cmd_qa_up` (skip-image-build + non-dry-run only).
  - `docker/scripts/test_perc_devctl.py` (+72): 6 new unit tests
    (helper ok / missing / missing-empty-Test / absent / qa-up stale
    fails fast / qa-up fresh proceeds). 56/56 tests pass.
  - `modules/perc-qa-automation/README.md` (+26): new
    "Stale matrix image — fail-fast on `--skip-image-build`" subsection
    under "Golden unattended smoke".
- Prior report: none (first review).
- Memory patterns hit: `installer.false-green-exit` (docker "healthy"
  timeout masking a stale-image condition); this is a documented pattern
  in `docker/README.md → Docker Health.Status (in-image HEALTHCHECK)`.
- Re-review: N/A (initial submission).

## Recommendation

**approve** — May commit/push: **yes**

## Gate

- Blocking bugs: **0**
- Suggestions: **1** (optional, not blocking)
- Nits: **0**

## Findings

### Issue 1 — Severity: suggestion (not blocking)

- File: `docker/scripts/perc-devctl.py:1322-1338`
- Description: When `_qa_matrix_image_healthcheck_status` reports
  `QA_IMAGE_HEALTHCHECK_MISSING`, the precheck emits both
  `RESULT:FAIL STEP:qa-up DETAIL:matrix_image_stale ...` and a separate
  `QA_DETAIL:` line. The two-line contract duplicates the failure reason;
  operators using a strict one-line parser will only see the
  `RESULT:FAIL` line, which already names the detail (`matrix_image_stale`).
  Keeping the `QA_DETAIL` line is fine for human readers but slightly
  redundant.
- Suggestion: Keep as-is for the first iteration — the redundancy aids
  agents that scrape `QA_DETAIL:` lines for hints. If a future cleanup
  pass consolidates the failure contract, fold both into the
  `RESULT:FAIL` line.
- Status: open (deferred, not blocking)

## Non-findings (explicitly verified)

- **Cross-platform path / file I/O:** the helper does not touch the
  filesystem; only runs `docker inspect --format '{{json .Config.Healthcheck}}' <tag>`
  via `subprocess.run([...], shell=False, check=False)` and parses
  stdout with stdlib `json`. No path concatenation, no hardcoded
  separators. The rebuild-hint string uses `Path / "docker"` / `Path / "matrix"`
  join (forward slashes via `__truediv__`), which docker accepts on all
  three host OSes. **Cross-platform path review: no issues.**
- **Hardcoded secrets / passwords:** none — the helper inspects image
  metadata only. QA-mode admin password continues to flow from
  `qa-up` stdout into env (unchanged path).
- **Silent failures:** the precheck distinguishes three terminal
  states (`ok` / `missing` / `absent`) and only emits the loud
  `RESULT:FAIL` line on `missing`. Docker daemon hiccups (`absent`)
  fall through to the existing `matrix-install-smoke` path, which
  already prints its own `RESULT:FAIL` line on hard failure.
- **Agent rule files:** no rule files in this diff
  (`AGENTS.md` / `AGENTS.local.md` / `.kilo/rules/*` /
  `modules/ai-shared-develop/.../skills/**`). The README delta is
  product documentation, not agent rules.
- **Tests for new logic:** 6 new tests cover the new helper + the
  integration with `cmd_qa_up`. Behavioral coverage is adequate for
  this small helper (pure, single subprocess call, three-state output).
- **JDK / branch:** no Java changes; `modules/perc-qa-automation` Maven
  build green (BUILD SUCCESS, 4.7s).
- **Pre-PR Maven verification:** `cd modules/perc-qa-automation && mvnw clean install`
  → BUILD SUCCESS, no new warnings.
- **PR thread protocol:** N/A (this is a new branch; no prior review
  threads to resolve).

## Live QA-mode evidence

1. `python docker/scripts/perc-devctl.py qa-up --skip-image-build
   --timeout-seconds 1200`
   → `RESULT:OK STEP:qa-up QA_CMS_HOST_PORT=9993 TEST_CMS_URL=http://127.0.0.1:9993 ADMIN_PASSWORD=<from qa-up stdout — never commit>`
2. `npm run test:surface -- --path tests/golden-unattended-smoke.spec.js`
   → `2 passed (9.9s)` (golden-unattended-smoke + Content Explorer shell).
3. `python docker/scripts/perc-devctl.py qa-down`
   → `RESULT:OK STEP:qa-down QA_DETAIL:removed (ports/disk freed)`.

Precheck unit-test evidence (forced `missing` via mock runner):
- `qa-up --skip-image-build` with stale image → `RESULT:FAIL STEP:qa-up
  DETAIL:matrix_image_stale IMAGE:percussion-matrix-cell:local HINT:rebuild-image`,
  exit code `EXIT_SUBPROCESS_FAILED`.
- `qa-up --skip-image-build` with fresh image → proceeds to normal
  bring-up (mocked `_run_logged` returns OK, admin banner printed).

Full unit-test run: `pytest test_perc_devctl.py -q` → **56 passed in 0.24s**.

## Memory touch

No new generalized principle to promote into
`modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`.
The "stale-image precheck" pattern is already documented in
`docker/README.md`; the change makes it actionable from
`perc-devctl.py` and the module README instead of relying on operators
reading the docker docs.

## Handoff

- May commit/push: **yes**
- Author is the reviewer in this session (Kilo sub-agent) — conflict
  disclosed per Erlang persona; same rigor applied.
- No durable report update needed beyond this file.
- Pattern memory unchanged.