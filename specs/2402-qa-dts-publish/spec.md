# Spec: QA cell serves published site + DTS (2402-qa-dts-publish)

**Epic:** #6237 · **Status:** slice 1 (spec authoring) · **Date:** 2026-11-12

## Why

The H2 QA cell (`perc-devctl qa-up`) is CMS-only. No DTS is deployed and no
site is published to a web server, so **no publishing output can be verified
in automation**:

- Dynamic DTS widgets (polls, RSS feeds, metadata, forms, comments, and
  secure-membership) have zero coverage without a running Tomcat DTS to point
  a publishing server at.
- Static publishing output (published pages, category-list trees,
  `web_resources`) has zero coverage without a published site served from a
  webapp dir.

Standing symptom: #6189 checkbox 1
(`PUBLISHED_CATEGORY_URL=... npm run test:surface -- --path
tests/published-category-tree-fancytree.spec.js`) **skips in every agent
cell** — the spec header requires an env no cell provides. #6236 shipped the
static half (SDK doc FancyTree repoint) and left the issue open for exactly
this reason.

## What this spec defines

A `--with-dts` QA-cell mode that brings up CMS (H2) + DTS (Tomcat) from a
single command and serves a fixture-published site, so the stranded specs go
green instead of skipping. This mirrors how dev mode already runs CMS+DTS
side-by-side (`docker/cms/Dockerfile`, `install-update.py`,
`DEV_PERCUSSION_DTS_INSTALL` / `DTS_URL` in `perc-qa-automation`).

## Scope

### In scope

1. **Cell:** `qa-rebuild-chain` also packages
   `delivery-tier-distribution.jar`; the cell performs a second silent DTS
   install beside `/opt/Percussion` and runs `StartTomcat.sh start`.
2. **Fixture:** a publishing server pointing at the in-cell DTS
   (`http://localhost:<dts-port>`); the published-site dir registered in
   Tomcat so a host alias serves it.
3. **CLI contract:** `qa-up [--with-dts]` publishes the DTS port and prints
   `PUBLISHED_CATEGORY_URL=...`; `qa-health` gates Jetty + Tomcat;
   `qa-down` tears both down.
4. **Seed coverage:** one published page per widget class (poll, feed,
   metadata, form, comments) + one static page/tree; surface specs per
   class, all **skipping cleanly without the env** until the cell provides
   it (skip-contract precedent:
   `tests/published-category-tree-fancytree.spec.js`).
5. **Docs:** `docs/developer-module/workbench-rest-and-qa-modes.md` §2 +
   `modules/perc-qa-automation/AGENTS.md` updated (new mode documented, no
   hardcoded ports).

### Out of scope

- Multi-RDBMS matrix for the DTS leg (H2 CMS + default DTS store only).
- Host-install / customer-environment publishing verification (#2358 stays
  human-owned).
- Removing the `?engine=tinymce` escape hatch (targets 8.3, #5902).
- Full-suite default runs (stay surface-filtered; new specs join existing
  surfaces by tag).

## Environment contract (no hardcoded ports)

| Variable                 | Meaning                                              | Set by        |
|--------------------------|------------------------------------------------------|---------------|
| `PUBLISHED_SITE_URL`     | Origin of the in-cell published site                 | `qa-up --with-dts` banner |
| `PUBLISHED_CATEGORY_URL` | Page rendering a collapsible category list          | `qa-up --with-dts` banner |
| `DTS_URL`                | In-cell DTS base URL (Tomcat)                        | `qa-up --with-dts` banner |
| `TEST_CMS_URL`           | CMS base URL (existing contract, unchanged)          | `qa-up` banner |

Ports are resolved at runtime (freeport / `DTS_PORT` convention, cf. #2005);
specs and docs MUST reference the variables, never literal port numbers.
Existing precedent: `DEV_PERCUSSION_DTS_URL` in
`modules/perc-qa-automation/.env` and `DTS_URL` in
`frontend/tests/helpers/auth.js`.

## Acceptance (maps to epic #6237 checkboxes)

- [ ] AC1 — `qa-up --with-dts` brings up CMS (H2) + DTS (Tomcat) from a
  single command; `qa-health` gates both; `qa-down` removes both.
- [ ] AC2 — A fixture edition publishes to the in-cell Tomcat static dir;
  `PUBLISHED_CATEGORY_URL` serves the published pages with zero 404s on
  `web_resources` (incl. FancyTree bytes from #6052).
- [ ] AC3 — Surface specs go green (not skipped) for: static page,
  category-list tree (#6189 checkbox 1), poll, RSS feed, metadata, form,
  comments.
- [ ] AC4 — Workbench doc §2 + qa-automation `AGENTS.md` updated (new mode
  documented, no hardcoded ports).
- [ ] AC5 — No regression: default `qa-up` (no flag) behavior unchanged;
  full-suite default still surface-filtered.

## Slice breakdown (one implement per run per parent)

| Slice | Issue | Covers |
|-------|-------|--------|
| 1 (this run) | #6237 | Spec authoring (`specs/2402-qa-dts-publish/`: spec, plan, tasks) + seed static-site smoke spec with skip contract + child filing |
| 2 | child | Cell DTS bring-up: rebuild-chain DTS packaging, `qa-up --with-dts`, `qa-health` dual gating, `qa-down` teardown, default-path no-regression |
| 3 | child | Fixture edition + published-site serve: edition publishes to in-cell Tomcat static dir; static page + category-tree specs green; `web_resources` zero-404 |
| 4 | child | DTS widget surface specs: poll, feed, metadata, form, comments green against in-cell DTS |
| 5 | child | Docs (workbench §2 + qa-automation `AGENTS.md`, no hardcoded ports) + full-suite surface-filter check + epic closeout |

## Links

- Blocked consumer: #6189 (live DTS publish verification)
- Coverage parent depending on this cell: #6238 (US2 needs published site, US3 needs DTS)
- Static-half precedent: #6236 (FancyTree SDK repoint, dynamic half deferred here)
- Provisioning precedent: #6052 (FancyTree `web_resources` copies)
- Separate-cell precedent: `matrix-install-smoke.py --product cms,dts`
