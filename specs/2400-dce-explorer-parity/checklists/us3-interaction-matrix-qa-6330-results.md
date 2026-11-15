# QA results — #6330 US3 dynamic interaction matrix per DTS service

QA-executed first pass of issue #6330 against a fresh CMS-only H2 QA cell
(`perc-devctl qa-up`, no DTS, no published demo site; image built from
`origin/main` tip containing merged PR #6303). **Verdict: PASS on the
CMS-only contract** (6 skipped, exit 0, every skip naming its service +
probed gap). Per-service green runs still need the DTS-backed cell (#6237);
the dashboard proof probe re-surfaced the already-filed #6399 residual
(forms/traffic gadget 500s) — not a US3 defect.

> Parent: #6238 ([8.2] Content + widget QA coverage matrices) / merged
> matrix PR #6303 (US3 slice #6241).
> This record supplies QA evidence only — it does not close #6330; human
> sign-off still required.

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`
  (`TEST_CMS_URL=http://127.0.0.1:43177`, container `perc-matrix-cms-h2`)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK HTTP:200
  HEALTH:healthy` (`/Rhythmyx/rest/mimetypes`)

## Test-plan mapping (issue #6330)

| # | QA step (issue #6330) | Result |
|---|------------------------|--------|
| 1 | CMS-only cell: run `npx playwright test tests/dts-interaction-matrix.spec.js` — confirm 6 skipped, exit 0, each skip naming its service + probed endpoint | **PASS** — 6 skipped, exit 0 (see spec output). JSON-reporter annotations confirm every skip carries `service <name> (<Label>): needs DTS_BASE_URL=…; default CMS-only H2 QA cell serves no DTS services (#6237)` |
| 2 | DTS-backed cell (if available): set `DTS_BASE_URL` and re-run — confirm per-service dynamic interactions execute and pass, zero JS console errors | **NOT RUN** — no DTS-backed cell exists in this environment; #6237 (cell dependency) is still open. The two-level skip contract (service-down / fixture-unseeded named skips) is designed for exactly this, and is unit-covered |
| 3 | Review the matrix doc/table the spec encodes — confirm every DTS service (poll, feed, metadata, form, comments) has a row | **PASS** — closed inventory `DTS_SERVICES` in `frontend/tests/helpers/dts-interaction-matrix.js` pins 6 rows: polls, feeds, forms, comments, metadata, membership (a superset of the 5 named in the issue; membership backs the `percLogin` dynamic widget). Each row pins JAX-RS root + `HEAD <root>/csrf` liveness probe + fixture env + US2 widget. Mirrored in `contracts/results-matrix-dts.example.json` (6 rows, all schema-valid) and the `modules/perc-qa-automation/README.md` US3 table |

## Spec output (same cell)

- `env -u DTS_BASE_URL TEST_CMS_URL=… npx playwright test
  tests/dts-interaction-matrix.spec.js` → **6 skipped, exit 0**; the
  `afterAll` hook still wrote `frontend/test-results/dts-interaction-matrix.json`
  (gitignored) with 6 rows, every `cases.interact = "skip"`, all passing
  `validateInteractionRow` schema validation (a red matrix would fail the run).
- `tests/golden-unattended-smoke.spec.js` → **2 passed** (env resolve +
  Admin login + Explorer shell) on the same cell.
- `npm run test:unit` → **667 passed, 0 failed** (includes
  `tests/unit/dts-interaction-matrix.test.js`, the skip-contract unit cover).
- `cd modules/perc-qa-automation && ../../mvnw clean install` →
  **BUILD SUCCESS** (no Java tests in module; frontend `npm ci` clean).

## server.log note

7 `ERROR` lines, all at 2026-11-14 23:19:56–57 UTC — the dashboard
proof-probe window (the US3 matrix spec itself makes zero HTTP calls: all 6
cases skip before any request). All 7 are the already-filed #6399 residual:
`PSFormDataService:L:149 Unable to connect to delivery server … https://localhost:8443`
(forms gadget) + `PSActivityService … '/Sites/Corporate_Investments'` /
`PSContentActivityService:L:153` (traffic gadget) + `PSDeliveryClient`
transport companions. No FATAL lines; nothing new relative to #6329/#6399.

## Browser pass

- Throwaway dashboard probe (login → `spa.jsp?entry=home&section=gadgets`,
  8 s settle): zero `pageerror`, 2 `Failed to load resource: 500` console
  errors — the #6399 forms + traffic gadget calls. Proof shot:
  `docs/ai-generated/proofs/issue-6330-us3-matrix-cell.png`

## Live proof

![Home gadgets dashboard on the fresh CMS-only H2 cell](../ai-generated/proofs/issue-6330-us3-matrix-cell.png)

Dashboard renders (Welcome, Activity, Assets-by-status, Blogs, Process
Monitor chrome) while the Form Tracker / Traffic gadget data calls 500
underneath per #6399.

## Pass / fail verdict (per issue criteria)

- **PASS**: skip contract holds without DTS (6 named skips, exit 0, schema-valid
  matrix artifact); matrix inventory covers every DTS service.
- **DEFERRED**: with-DTS green runs — blocked on the #6237 DTS-backed cell,
  not on the matrix slice.

## Out of scope (per issue)

- Static-render matrix (US2, separate slice).
- DTS deployment itself.
- The #6399 forms/traffic gadget 500s (filed residual, dashboard surface —
  not US3 matrix surface).

## Note on sign-off

This is an **agent-executed first pass**, not a human-QA pass. This PR
supplies the evidence; it does not close #6330 by itself — human sign-off
still required per the issue's QA contract.
