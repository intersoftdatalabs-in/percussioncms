# Plan: QA cell serves published site + DTS (2402-qa-dts-publish)

**Spec:** `spec.md` · **Epic:** #6237 · **Slice 1:** 2026-11-12

## Approach

Build the mode the way dev mode already works (CMS+DTS side-by-side), but
contained in Docker with H2, driven by `perc-devctl`, and verified by
surface-filtered Playwright. Each slice stays vertical (cell + fixture +
one widget surface + docs per slice); one implement per run per parent.

## Phase 1: Spec + seed (slice 1, this run)

- [x] P1 Author `specs/2402-qa-dts-publish/` (spec.md, plan.md, tasks.md).
- [x] P2 Seed `published-site-smoke.spec.js`: `PUBLISHED_SITE_URL` skip
      contract (mirrors `published-category-tree-fancytree.spec.js`);
      proves the skip path exits 0 with 2 skipped when the cell provides
      no env.
- [x] P3 Roadmap pointer in workbench-rest-and-qa-modes.md §2 (planned
      mode, spec link, no hardcoded ports).
- [x] P4 File slices 2–5 as children of #6237 (`[#6237]` title tag).

## Phase 2: Cell DTS bring-up (slice 2)

1. `qa_rebuild_chain.py`: add a `delivery-tier-distribution` packaging step
   (same `RESULT:OK/FAIL`, `--dry-run`-safe, `pathlib.Path` joins only —
   spec 994 FR-001 cross-platform rule).
2. Matrix cell: second silent DTS install beside `/opt/Percussion`
   (`StartTomcat.sh start`); DTS host port via the existing
   `DTS_PORT`/freeport convention (#2005), pinned into the process env the
   way `ensure_qa_cms_host_port` does for CMS.
3. `perc-devctl`: `qa-up [--with-dts]` flag (default path byte-identical;
   new flag prints `PUBLISHED_SITE_URL=`, `PUBLISHED_CATEGORY_URL=`,
   `DTS_URL=` in the endpoint banner), `qa-health` gates Jetty + Tomcat
   probe, `qa-down` stops/removes both containers.
4. Tests: `test_perc_devctl.py` unit coverage for the flag + dual-gate
   parsing; default-`qa-up` no-regression run.
5. Evidence: `qa-up --with-dts` + `qa-health` logs in the PR body.

## Phase 3: Fixture edition + static serve (slice 3)

1. Fixture publishing server in the cell pointing at the in-cell DTS host
   alias; fixture edition publishes to the Tomcat static dir.
2. Register the published-site dir in Tomcat
   (`Deployment/Server/conf/.../web.xml` on a host alias) so the site is
   served from the cell.
3. Green runs (not skipped): seed `published-site-smoke.spec.js` +
   `published-category-tree-fancytree.spec.js` (#6189 checkbox 1); assert
   zero 404s on `web_resources` incl. FancyTree bytes from #6052.
4. Evidence: surface-run pass counts + zero console/`server.log` errors.

## Phase 4: DTS widget surfaces (slice 4)

1. One published page per widget class: poll, RSS feed, metadata, form,
   comments (+ secure-membership if the cell serves it).
2. One surface spec per class reusing the `PUBLISHED_SITE_URL`/`DTS_URL`
   skip contract (skip cleanly without the env; green with it).
3. Evidence: per-class green runs from the `--with-dts` cell.

## Phase 5: Docs + closeout (slice 5)

1. Workbench doc §2 documents the shipped mode (replaces the slice-1
   roadmap pointer); `modules/perc-qa-automation/AGENTS.md` documents the
   new env vars; no hardcoded ports anywhere.
2. Full-suite default still surface-filtered (no new always-run weight).
3. Epic closeout: all five AC boxes ticked with evidence; `Fixes #6237`
   only when every box is met.

## Risks / mitigations

| Risk | Mitigation |
|------|------------|
| DTS install time blows up cell bring-up | Silent install beside CMS; `--with-dts` opt-in keeps default fast (AC5) |
| Port collisions on shared runners | Reuse `DTS_PORT`/freeport pinning (#2005); never hardcode |
| Fixture edition drift vs product fixtures | Rebuild fixture from docs alone (mirrors #6238 SC-005) |
| Tomcat log noise mistaken for product errors | Gate on Jetty + Tomcat probes in `qa-health`, not log grep |

## Test strategy

- Python: `test_perc_devctl.py` + `test_qa_rebuild_chain.py` unit tests
  for every flag/parse change (existing pattern).
- Playwright: surface-filtered runs per new spec
  (`npm run test:surface -- --path <spec>`); skip-contract proven by
  no-env runs (exit 0, all skipped) before green runs exist.
- Gates per run: C1 standalone `clean install` on touched Maven modules
  (none touched in slice 1 — specs + Playwright seed + roadmap docs only);
  Erlang CLI before PR; C5 H2 evidence from slice 3 on (no WebUI touched).
