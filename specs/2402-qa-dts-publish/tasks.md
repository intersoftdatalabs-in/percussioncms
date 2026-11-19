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

- [x] T009 `qa_rebuild_chain.py` packages `delivery-tier-distribution.jar`
      (`RESULT:OK/FAIL`, `--dry-run`-safe, `pathlib` joins).
- [x] T010 Matrix cell performs second silent DTS install beside
      `/opt/Percussion`; `StartTomcat.sh start`; `DTS_PORT`/freeport
      pinning (#2005 convention).
- [x] T011 `qa-up --with-dts` flag + endpoint banner
      (`PUBLISHED_SITE_URL=`, `PUBLISHED_CATEGORY_URL=`, `DTS_URL=`);
      default path unchanged.
- [x] T012 `qa-health` gates Jetty + Tomcat; `qa-down` tears both down.
- [x] T013 Unit tests (`test_perc_devctl.py`) + default-path no-regression
      evidence in PR body.

## Slice 3 — fixture edition + static serve (child issue)

- [x] T014 Fixture publishing server → in-cell DTS; fixture edition
      publishes to the Tomcat static dir; site registered on a host alias.
- [x] T015 `published-site-smoke.spec.js` green (not skipped) with zero
      `web_resources` 404s.
- [x] T016 `published-category-tree-fancytree.spec.js` green (#6189
      checkbox 1 unblocked); FancyTree bytes from #6052 verified live.
- [x] T017 Surface-run evidence (pass counts, zero console errors, zero
      `server.log` ERROR/FATAL) in PR body.

## Slice 4 — DTS widget surfaces (child issue)

- [x] T018 One published page per widget class: poll, feed, metadata,
      form, comments.
- [x] T019 One surface spec per class (`PUBLISHED_SITE_URL`/`DTS_URL`
      skip contract); green against the `--with-dts` cell.
- [x] T020 Evidence per class in PR body.

## Slice 5 — docs + closeout (child issue)

- [x] T021 Workbench §2 documents the shipped mode (replaces roadmap
      pointer); qa-automation `AGENTS.md` documents new env vars; no
      hardcoded ports.
- [x] T022 Full-suite default still surface-filtered.
- [x] T023 Epic closeout with AC1–AC5 evidence; `Fixes #6237` only when
      every box is met.
      Live-verification slice (this run, #6237): fresh `--with-dts` cell —
      `qa-up` RESULT:OK (CMS H2 + DTS Tomcat), `qa-health --with-dts`
      HTTP:200 + DTS_HTTP:200, `qa-publish-fixture` RESULT:OK,
      `qa-seed-widget-fixtures` RESULT:OK; surface runs 62/62 green, zero
      skipped (smoke 2, category-tree 3 incl. #6189 expand, poll 2, feed 4,
      metadata 2 incl. the #6375 HTTPS seed block, form 2, comments 2,
      widget matrix 45); CMS `server.log` zero ERROR/FATAL. Cell also
      exposed a pre-fix `saveVisits` isolation failure in `metadata.log`
      (same class as #6343): fixed here (`PSBlogPostVisitDao` /
      `PSCookieConsentDao` drop explicit isolation, red-green unit proof,
      live visit-persistence proof via `topblogposts`). Residuals #6670
      (likes/forms DAOs, same class) + #6673 (percCalendar inventory,
      pre-existing). AC1–AC5 all evidenced → `Fixes #6237` + `Fixes #6375`.
