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

All cell commands run from the repo root; the Playwright runs from
`modules/perc-qa-automation/frontend`. The static and interaction
matrices need the DTS-providing cell, so boot it **first**: the fixture
publisher stages pages into the DTS cell and has nowhere to deploy on a
CMS-only cell.

1. Start from a clean cell, boot CMS + DTS together, and check health:

   ```bash
   python docker/scripts/perc-devctl.py qa-down
   python docker/scripts/perc-devctl.py qa-up --with-dts
   python docker/scripts/perc-devctl.py qa-health
   ```

   The endpoint banner prints `TEST_CMS_URL`, `QA_CMS_HOST_PORT`,
   `DTS_URL` (accepted anywhere `DTS_BASE_URL` is), the published-site
   placeholders, and the admin password source. Always use the printed
   `TEST_CMS_URL` (freeport contract — never hardcode the port), and do
   not continue past an unhealthy `qa-health`: it fail-fasts on dead
   webapps that still answer HTTP.

   When `qa-up` refuses with `qa_up_preflight_refused`, the WebUI WAR is
   stale against the local-repo SNAPSHOT or the shared repo holds a
   sibling worktree's branch-built SNAPSHOTs. Rebuild from this checkout
   (`python docker/scripts/perc-devctl.py qa-rebuild-chain --then-qa-up`)
   or isolate the worktree
   (`python docker/scripts/maven_local_repo.py --repo-root .`, export the
   printed `MAVEN_ARGS`, retry). A refusal that persists after a
   from-checkout rebuild with matching content hashes is a preflight
   false positive (package-private classes compiled from shared source
   files) — re-run that boot with `--skip-preflight`, which relaxes the
   outer gate while the install still uses this checkout's packaged
   distribution (`perc-distribution-tree.jar`).

2. Run the creation matrix (CMS-only cell, no publish path needed):

   ```bash
   TEST_CMS_URL=<from-qa-up-banner> \
     ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-banner> \
     TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
     npm run test:surface -- --path tests/content-create-matrix.spec.js
   # Single type:
   npm run test:surface -- --path tests/content-create-matrix.spec.js --grep "percPage:"
   # Inventory (live catalog cross-check + row validation):
   TEST_CMS_URL=<from-qa-up-banner> \
     ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-banner> \
     npm run test:surface -- --path tests/content-create-inventory.spec.js
   ```

   Every pinned built-in type (54) gets a create round-trip case (New-item
   create, save, reopen with values round-tripped) plus a negative case
   (blank required field shows a message, nothing persisted) — 108 cases,
   one results row per type validated against
   `contracts/results-matrix.schema.json` and written to
   `test-results/content-create-matrix.json`. The run is self-accounting:
   the inventory spec fails when the live catalog holds a creatable type
   that is neither pinned nor a documented `SYSTEM_EXCLUSIONS` entry, so a
   full run contains zero unexplained passes. Without a live CMS-only cell
   every case skips by named reason (the liveness probe on
   `GET <TEST_CMS_URL>/Rhythmyx/login` answers nothing) and the run still
   exits 0 with a schema-valid all-skip matrix. To add a type the CMS ships
   later, append one entry to `BUILT_IN_TYPES` in
   `tests/helpers/content-create-matrix.js` (two cases, no other edits).

3. Run the index (works on the CMS-only cell too; no publish path needed):

   ```bash
   TEST_CMS_URL=<from-qa-up-banner> \
     ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-banner> \
     npm run test:surface -- --path tests/coverage-matrix-index.spec.js
   ```

   The banner password is a best-effort snapshot of the cell's
   first-boot rotation; when login fails, re-read the in-cell generated
   passwords with the refresh command the banner prints next to the
   values.

4. Publish the fixture site, then run the static matrix:

   ```bash
   python docker/scripts/perc-devctl.py qa-publish-fixture
   PUBLISHED_SITE_URL=<from-fixture-output> \
     PUBLISHED_CATEGORY_URL=<from-fixture-output> \
     npm run test:surface -- --path tests/published-widget-matrix.spec.js
   ```

   The publisher prints both URLs and repoints the category URL at the
   fixture category page (only the category-list case reads it). No CMS
   credentials are needed: every case is an anonymous GET against the
   published origin.

5. Seed the DTS fixtures, then run the interaction matrix:

   ```bash
   python docker/scripts/perc-devctl.py qa-seed-widget-fixtures
   DTS_URL=<from-qa-up-banner> \
     npm run test:surface -- --path tests/dts-interaction-matrix.spec.js
   ```

   `DTS_BASE_URL` works anywhere `DTS_URL` does. The membership case
   additionally needs `DTS_FIXTURE_USER` + `DTS_FIXTURE_PASSWORD`
   naming a registered account, and skips by name without them; the
   comments case probes the live service directly. The polls, forms, and
   feeds interaction cases assert the pinned static fixtures
   (`qamatrix-poll`, `qamatrix-form`,
   `EnterpriseInvestments/qamatrix-feed/localhost`) and keep their named
   skips until those fixtures are provisioned — the deep per-service
   matrices (`polls-vote`, `feeds-entries`, `forms-confirmation`,
   `comments-moderation`, `membership-registration`, `metadata-index`)
   self-seed a unique probe per run instead, so they go green with no
   pre-provisioned fixtures.

   Retrieval round-trip family (US6, beyond forms) — one block per matrix,
   same env for all six:

   ```bash
   DTS_URL=<from-qa-up-banner> \
     DTS_HTTPS_URL=https://<dts-cell-host>:<https-port> \
     DTS_DELIVERY_USER=<deliverymanager-user> \
     DTS_DELIVERY_PASSWORD=<from-seed-step> \
     npm run test:surface -- --path tests/polls-vote-matrix.spec.js
   # then feeds-entries-matrix, metadata-index-matrix,
   # comments-moderation-matrix, membership-registration-matrix,
   # forms-confirmation-matrix — same env, one --path each
   ```

6. Tear down what you started: `python docker/scripts/perc-devctl.py qa-down`.

## Live green runs

Dated proof that the matrices go green on a clean cell — not just that the
commands exist. Latest first:

| Date (UTC) | Cell | Base | Matrix | Result |
|------------|------|------|--------|--------|
| 2026-11-24 | DTS H2 (`qa-up --with-dts`, chain rebuilt from checkout + `system` clean install, preflight FRESH) | `706a05b811` (origin/main) | retrieval family: polls-vote + feeds-entries + metadata-index + comments-moderation + membership-registration + forms-confirmation | 18/18 cases pass (6/6 rows `pass`, 0 fail, 0 skip); CMS `server.log` 0 ERROR/FATAL; DTS service logs 0 except 2 feeds ERROR lines from the asserted unseeded-name 404→redirect probe (`unseededRedirects` case); 4 boot-time Tomcat security-constraint warnings pre-run |
| 2026-11-24 | CMS-only H2 (`qa-up`, no DTS) | `eede3df951` (origin/main; `qa-rebuild-chain` from checkout, preflight FRESH) | content-create-matrix + inventory + index | 108/108 cases pass (54/54 rows `pass`, 0 fail, 0 skip); inventory 2/2; index 4/4; `server.log` 0 ERROR/FATAL |

Run commands are the step 2 / step 3 / step 5 blocks above; the results JSON
(`test-results/content-create-matrix.json`, one schema-valid row per
pinned type) is the per-type accounting. Re-record this table whenever a
matrix inventory changes or a run goes red — a stale date with a changed
inventory is a docs defect.

## Skip contracts (no cell, no failure)

Every matrix is provisioning-aware: without its cell it skips by named reason and exits 0
instead of failing.

- No CMS cell → every case skips naming the missing cell.
- No `PUBLISHED_SITE_URL` → static cases skip naming the missing publish path.
- No `DTS_BASE_URL` (or its `DTS_URL` alias) → interaction cases skip naming the service.
- Unseeded fixtures skip with their own named reason — the seeder provisions
  the feed-surface fixture and the delivery credential; the pinned static
  interaction fixtures (poll, form, feed, membership account) stay skipped
  by name until they are provisioned.

A full run with skips is still a passing run; the results JSON
(`test-results/coverage-matrix-index.json`) records which services were proven and which
skipped, so there are no unexplained passes.

## Fixture rebuild

The fixture site is rebuildable from the cell tooling alone — no manual
publishing, no tribal fixtures:

- `python docker/scripts/perc-devctl.py qa-publish-fixture` copies the
  publish-time `web_resources` tree from the CMS cell and stages the
  fixture home page, the category page, and one `widgets/<Name>.html`
  page per pinned widget (44) on the in-cell published site, then prints
  `PUBLISHED_SITE_URL` / `PUBLISHED_CATEGORY_URL` for the static matrix.
- `python docker/scripts/perc-devctl.py qa-seed-widget-fixtures`
  provisions the deliverymanager password, the feed-surface fixture
  (`qacell/qasurface-fixture` plus its metadata index rows, consumed by
  the published feed surface), and the metadata extractor credential.
  Pass `--delivery-password` (or set `DTS_DELIVERY_PASSWORD`) when the
  password must be reproducible by hand; otherwise a fresh random value
  is provisioned per seed and never printed.

What stays skipped after a rebuild is a contract, not a gap: the polls,
forms, and feeds interaction cases skip until their static fixtures are
provisioned, membership skips without `DTS_FIXTURE_USER` /
`DTS_FIXTURE_PASSWORD`, and the index spec still verifies the whole
family — every dynamic widget names a service with an interaction case,
every service has a proving artifact, and this page still pins the same
suites and counts.

If a fixture step stops reproducing the matrices, file it against the
coverage epic so the rebuild path stays honest.

## Related

- [Publishing](id:admin-publishing)
- [Server operations](id:admin-server-ops)
