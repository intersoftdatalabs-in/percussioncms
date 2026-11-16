# QA re-verification — #6241 US3 dynamic interaction matrix per DTS service

Agent re-verification of issue #6241 on the current `origin/main` tip
(after merged PRs #6303 + #6401 and the intervening main-line merges:
WebUI dep migrations #6417–#6432, gadget fix #6423, translations #6413).
**Verdict: PASS on the CMS-only contract** — skip contract holds,
schema-valid 6-row matrix, unit cover intact, zero code changes required.
Per-service green runs still need the DTS-backed cell (#6237, still open).

> Parent: #6238 ([8.2] Content + widget QA coverage matrices) /
> implementation PR #6303 (merged 2026-11-13) / QA evidence PR #6401
> (merged 2026-11-14, closed #6330).
> This record supplies regression evidence only — it does not close #6241;
> the with-DTS green-run criterion stays blocked on #6237.

## What was re-run (no live cell needed)

The US3 spec skips before any HTTP request when `DTS_BASE_URL` is absent
(`test.skip(!base, skipNoDts(entry))`), so the CMS-only contract is
verifiable with no running CMS — and the spec itself made zero network
calls in these runs.

| # | Command (from `modules/perc-qa-automation/frontend`) | Result |
|---|------------------------------------------------------|--------|
| 1 | `node --test tests/unit/dts-interaction-matrix.test.js` | **PASS** — 6/6 (skip builders, row builder, schema/example-matrix cover, spec cross-reference) |
| 2 | `npm run test:unit` (full suite) | **PASS** — 667/667, 0 failed |
| 3 | `env -u DTS_BASE_URL -u DTS_URL npm run test:surface -- --path tests/dts-interaction-matrix.spec.js` | **PASS** — 6 skipped, exit 0 |
| 4 | Same spec with `--reporter=json` | **PASS** — `skipped: 6, unexpected: 0, flaky: 0`; every result annotation is `service <name> (<Label>): needs DTS_BASE_URL=(DTS-providing cell origin); default CMS-only H2 QA cell serves no DTS services` |
| 5 | `frontend/test-results/dts-interaction-matrix.json` (gitignored artifact from run 3/4) | **PASS** — 6 rows (polls, feeds, forms, comments, metadata, membership), every `cases.interact = "skip"`, `suite: dts-interaction-matrix`; the spec `afterAll` re-validated every row via `validateInteractionRow` in-run (a red matrix fails the run, exit was 0) |
| 6 | `contracts/results-matrix.schema.json` contains the `dts-interaction-matrix` suite + `dts-cell` cell (unchanged since #6303) | **PASS** — no schema drift from intervening merges |

## Test-plan mapping (issue #6241)

| Issue criterion | Result |
|-----------------|--------|
| Per-service interaction specs green against a DTS-providing cell | **BLOCKED** — #6237 still open; no DTS-backed cell or seeded fixtures (`DTS_FIXTURE_*`) exist in this environment. Two-level skip contract (service-down / fixture-unseeded named skips) is unit-covered for exactly this state |
| Skip contract spec green on the CMS-only cell (all skip, all named, zero passes) | **PASS** — runs 3–4 above: 6 skipped, exit 0, every skip names its service + the `DTS_BASE_URL` gap |
| Results-matrix rows validate against `contracts/results-matrix.schema.json` | **PASS** — run 5 above (in-run `afterAll` validation + schema still carries the suite) |

## Out of scope (per issue)

- Static-render matrix (US2, separate slice #6240).
- DTS deployment / fixture seeding itself (#6237).
- The #6399 forms/traffic gadget 500s — closed via #6423 since the #6401 pass.

## Note on sign-off

Agent-executed regression re-verification, not a human-QA pass. No product
code changed (docs-only record); with-DTS green runs still require the #6237
cell + human sign-off per the issue's contract.
