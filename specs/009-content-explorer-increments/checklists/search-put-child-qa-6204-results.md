# QA results — REST search PUT child field/property persistence (#6204)

QA verification slice for merged PR #6186 (fix(system): reconcile search child
rows after REST PUT on H2, parent #6136). Executes the three numbered test-plan
steps from issue #6204 against the H2 QA cell and records the results here.

**Result: PASS** — field add, field-property change + field removal, and parent
row currency all round-trip on H2 with no 500; zero JS console/page errors;
zero `server.log` ERROR/FATAL.

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → container `perc-matrix-cms-h2`,
  `TEST_CMS_URL=http://127.0.0.1:43677` (H2)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK`,
  `HTTP:200 HEALTH:healthy`

## Test plan

Seed: borrowed two known-good field criteria from packaged `Default_Search`
(`GET /Rhythmyx/services/searches/Default_Search` → 200, 6 fields) so the
probe only uses field names the server already accepts (unknown field is 400).

### 1. PUT adds a field; GET confirms it persists — PASS

- `POST /Rhythmyx/services/searches` created custom `QASearch6204vagp91pa`
  carrying one field → 200, name round-trips.
- `PUT /Rhythmyx/services/searches/{name}` with both fields → 200.
- `GET` detail shows **both** field names: the added field is present and the
  original field was not lost (pre-#6186 behavior silently dropped the add).

### 2. PUT changes a field property and removes another field — PASS

- `PUT` with only the second field and its operator flipped (`like` →
  `equals`) → 200.
- `GET` detail: the removed field name is absent; the kept field carries
  `operator: "equals"` (property change persisted, removal honored).

### 3. Parent search row still current, no 500 — PASS

- Every write/read in steps 1–2 returned 200 (no 500 anywhere); final `GET`
  detail still carries the parent `name`.
- `DELETE /Rhythmyx/services/searches/{name}` → 200/204; following `GET` →
  404 (cleanup proven, no orphan).
- One orphan from the first probe attempt (which exited on a spec-parser bug
  before its cleanup step) was removed with the same REST DELETE + 404 probe.

Live proof: `docs/ai-generated/proofs/issue-6204-search-put-child.png`
(Developer → Searches catalog on the H2 cell showing the QA search rows
created via REST POST during the run).

## Quality signals

- `npm run test:surface -- --path tests/zz-qa-6204-search-put-child-tmp.spec.js`
  (temporary probe spec, removed after the run) → **1 passed**,
  `assertConsoleClean` in-spec (zero pageerror / console errors)
- `docker exec perc-matrix-cms-h2 grep ' ERROR '` server.log → **0 lines**;
  `grep ' FATAL '` → **0 lines**
- `cd modules/perc-qa-automation && ../../mvnw clean install` → BUILD SUCCESS

## Out of scope (per #6204)

- Non-H2 RDBMS matrix; search execution semantics.

## Related

- Parent issue: #6136 (closed by #6186)
- Merged fix: PR #6186
- Tracking: [night-issue-prs] run 2026-11-14
