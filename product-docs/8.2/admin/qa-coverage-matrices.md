---
id: admin-qa-coverage-matrices
title: QA coverage matrices
description: Automated creation, static-render, and DTS interaction coverage for content types and widgets in Percussion CMS 8.2
version: "8.2"
order: 43
tags: [admin, qa, publishing]
---

# QA coverage matrices

Three automated Playwright matrices prove that every shipped content type can still be created
and that every widget renders — statically on a published site and dynamically against the
Delivery Tier Services (DTS). Together they are the executable answer to
"is anything unaccounted for": a cross-matrix index spec
(`tests/coverage-matrix-index.spec.js`) fails the run when a type, widget, or service has no
proving case.

## The three matrices

| Matrix | Spec | Inventory | Cell | What it proves |
|--------|------|-----------|------|----------------|
| content-create-matrix | `tests/content-create-matrix.spec.js` | 54 built-in content types | CMS-only | Every built-in type creates end-to-end (create round-trip + required-field negative); the live catalog holds nothing unaccounted |
| published-widget-matrix | `tests/published-widget-matrix.spec.js` | 44 widgets | Published site (`PUBLISHED_SITE_URL`) | Every widget renders its static shell on a published fixture page (`widgets/<Name>.html`) |
| dts-interaction-matrix | `tests/dts-interaction-matrix.spec.js` | 6 DTS services (polls, feeds, forms, comments, metadata, membership) | DTS cell (`DTS_BASE_URL`) | Every dynamic service round-trips (vote persists, feed serves, form confirms, comment moderates, index answers, login round-trips) |

Per-service deep matrices add detail on top of the interaction matrix
(polls voting, feeds entries, metadata index-search, forms confirmation, comments
moderation, membership registration). The index spec
(`tests/coverage-matrix-index.spec.js`) ties the family together: it asserts every
dynamic widget names a service with an interaction case, every service has at least one
proving artifact, and this page still lists the same suites and counts — docs and code
cannot drift apart silently.

## Operator workflow

All commands run from `modules/perc-qa-automation/frontend`.

1. Bring up the CMS-only cell and check health:

   ```bash
   python docker/scripts/perc-devctl.py qa-up
   python docker/scripts/perc-devctl.py qa-health
   ```

2. Run the index (works on the CMS-only cell; no publish path needed):

   ```bash
   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
     ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up> \
     npm run test:surface -- --path tests/coverage-matrix-index.spec.js
   ```

   Use the `TEST_CMS_URL` printed by `qa-up` (freeport contract — never hardcode the port).

3. For the static matrix, publish the fixture site first, then point at it:

   ```bash
   python docker/scripts/perc-devctl.py qa-publish-fixture
   PUBLISHED_SITE_URL=<from-qa-up-or-fixture> \
     PUBLISHED_CATEGORY_URL=<from-fixture> \
     npm run test:surface -- --path tests/published-widget-matrix.spec.js
   ```

4. For the interaction matrix, bring up the DTS-providing cell:

   ```bash
   python docker/scripts/perc-devctl.py qa-up --with-dts
   DTS_URL=<from-banner> \
     npm run test:surface -- --path tests/dts-interaction-matrix.spec.js
   ```

5. Tear down what you started: `python docker/scripts/perc-devctl.py qa-down`.

## Skip contracts (no cell, no failure)

Every matrix is provisioning-aware: without its cell it skips by named reason and exits 0
instead of failing.

- No CMS cell → every case skips naming the missing cell.
- No `PUBLISHED_SITE_URL` → static cases skip naming the missing publish path.
- No `DTS_BASE_URL` (or its `DTS_URL` alias) → interaction cases skip naming the service.
- Unseeded fixtures skip with their own named reason — the DTS cell turns cases green one
  fixture at a time (`qa-seed-widget-fixtures` provisions them).

A full run with skips is still a passing run; the results JSON
(`test-results/coverage-matrix-index.json`) records which services were proven and which
skipped, so there are no unexplained passes.

## Fixture rebuild

The fixture site is rebuildable from the cell tooling alone:

- `python docker/scripts/perc-devctl.py qa-publish-fixture` stages the
  `widgets/<Name>.html` fixture pages on the in-cell published site.
- `python docker/scripts/perc-devctl.py qa-seed-widget-fixtures` provisions the DTS
  interaction fixtures (poll, form, feed, user; delivery credentials via
  `--delivery-password` for the metadata extractor block).

If a fixture step stops reproducing the matrices, file it against the coverage epic so the
rebuild path stays honest.

## Related

- [Publishing](id:admin-publishing)
- [Server operations](id:admin-server-ops)
