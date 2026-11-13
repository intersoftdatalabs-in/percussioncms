# Tasks: QA cell serves published site + DTS (2402-qa-dts-publish)

**Spec:** `spec.md` · **Plan:** `plan.md` · **Epic:** #6237

## Slice 1 — spec + seed (this run, #6237)

- [x] T001 Author `specs/2402-qa-dts-publish/spec.md` (why, scope, env
      contract, acceptance AC1–AC5, slice breakdown).
- [x] T002 Author `plan.md` (phases 1–5, risks, test strategy).
- [x] T003 Author `tasks.md` (this file; per-slice boxes mapped to child
      issues).
- [x] T004 Seed `frontend/tests/published-site-smoke.spec.js` with the
      `PUBLISHED_SITE_URL` skip contract; prove no-env run exits 0 skipped.
- [x] T005 Roadmap pointer in workbench doc §2 (planned mode + spec link).
- [x] T006 File slices 2–5 as children of #6237; record URLs in
      `scratch/work-6237.json` (`residuals_filed`).
- [x] T007 Erlang CLI review before PR; PR body carries the report.
- [x] T008 Upsert `## Agent progress (night-issue-prs)` on #6237 + comment.

## Slice 2 — cell DTS bring-up (child issue)

- [ ] T009 `qa_rebuild_chain.py` packages `delivery-tier-distribution.jar`
      (`RESULT:OK/FAIL`, `--dry-run`-safe, `pathlib` joins).
- [ ] T010 Matrix cell performs second silent DTS install beside
      `/opt/Percussion`; `StartTomcat.sh start`; `DTS_PORT`/freeport
      pinning (#2005 convention).
- [ ] T011 `qa-up --with-dts` flag + endpoint banner
      (`PUBLISHED_SITE_URL=`, `PUBLISHED_CATEGORY_URL=`, `DTS_URL=`);
      default path unchanged.
- [ ] T012 `qa-health` gates Jetty + Tomcat; `qa-down` tears both down.
- [ ] T013 Unit tests (`test_perc_devctl.py`) + default-path no-regression
      evidence in PR body.

## Slice 3 — fixture edition + static serve (child issue, #6243)

- [x] T014 Fixture publishing server → in-cell DTS; fixture edition
      publishes to the Tomcat static dir; site registered on a host alias.
      (Shipped: `docker/scripts/qa_fixtures/published-site/` edition
      output + `publishing-server.json` manifest; `qa-publish-fixture`
      copies it to `/opt/Percussion/Deployment/Server/published-site`
      and installs per-Host `published-site.xml` / `web_resources.xml`
      descriptors — path-prefix contexts, no `server.xml` edit; hooked
      into `qa-up --with-dts`; PR #<n>.)
- [x] T015 `published-site-smoke.spec.js` green (not skipped) with zero
      `web_resources` 404s. (2 passed vs the fixture served over the
      two-context mapping; PR #<n>.)
- [x] T016 `published-category-tree-fancytree.spec.js` green (#6189
      checkbox 1 unblocked); FancyTree bytes from #6052 verified live.
      (3 passed incl. expand; first live run exposed a DynaTree-era
      locator — status classes live on `span.fancytree-node` in 2.x —
      fixed in the spec; real pages also load `jquery-ui.js` first
      because fancytree asserts `$.ui` at load; PR #<n>.)
- [x] T017 Surface-run evidence (pass counts, zero console errors, zero
      `server.log` ERROR/FATAL) in PR body. (Console sweep 0/0/0;
      `server.log` N/A — no product server in the fixture-serve loop,
      same constraint as slice 2; cell wiring covered by 118 pytests +
      dry-run banner; PR #<n>.)

## Slice 4 — DTS widget surfaces (child issue)

- [ ] T018 One published page per widget class: poll, feed, metadata,
      form, comments.
- [ ] T019 One surface spec per class (`PUBLISHED_SITE_URL`/`DTS_URL`
      skip contract); green against the `--with-dts` cell.
- [ ] T020 Evidence per class in PR body.

## Slice 5 — docs + closeout (child issue)

- [ ] T021 Workbench §2 documents the shipped mode (replaces roadmap
      pointer); qa-automation `AGENTS.md` documents new env vars; no
      hardcoded ports.
- [ ] T022 Full-suite default still surface-filtered.
- [ ] T023 Epic closeout with AC1–AC5 evidence; `Fixes #6237` only when
      every box is met.
