# QA results — issue #6189 (live DTS publish verification, parent #6052)

Residual scope from #6052 (delivery-side FancyTree provisioning). The
packaging + static gates shipped in #6190 (verified #6206); the doc half
shipped in #6236 (MERGED). This run closes the last open half: **live
publish-path verification** of the category-tree spec against a real
in-cell DTS publish, plus the final `dynatree` reference audit.

Verification run on tip of `main` (branch
`fix/issue-6189-publish-verify`).

**Result: PASS** — 5/5 live Playwright tests green (not skipped),
zero 404s on FancyTree bytes, dead DynaTree copies stay 404, collapsed
node expands to visible children.

## Cell under test

- CMS cell (shared, started by a sibling seat): `perc-matrix-cms-h2`,
  `http://127.0.0.1:53573` — publish-tree source
  (`/opt/Percussion/web_resources`): all six fixture-required assets
  PRESENT (`jquery.js`, `jquery-migrate.js`, `jquery-ui.js`,
  `jquery.fancytree.js`, `css/fancytree/skin/ui.fancytree.css`,
  `css/fancytree/skin-win8/loading.gif`); both dead DynaTree copies
  ABSENT.
- DTS cell (started by this run, `--skip-image-build` on
  `percussion-matrix-cell:local`): `perc-matrix-dts-h2`,
  `http://127.0.0.1:42665` — matrix install RESULT:OK (35.5s, HTTP 200).
- Fixture deploy (`perc-devctl qa-publish-fixture`): RESULT:OK, 53 paths
  staged; `PUBLISHED_SITE_URL=http://127.0.0.1:42665`,
  `PUBLISHED_CATEGORY_URL=http://127.0.0.1:42665/category.html`.

## Acceptance results (from #6189)

### 1. Published collapsible category list verified zero-404 — PASS

- `tests/published-site-smoke.spec.js` → **2 passed** (tree bytes serve,
  DynaTree stays gone; home page zero `web_resources` failures).
- `tests/published-category-tree-fancytree.spec.js` → **3 passed**:
  FancyTree assets 200 + DynaTree copies 404; `.fancytree(` initializes
  with zero script 404s and zero tree page-errors; expanding a collapsed
  node reveals visible child entries (`@category-expand`).
- Spot HTTP contract: `category.html` → 200,
  `web_resources/cm/jslib/jquery.fancytree.js` → 200,
  `web_resources/cm/jslib/jquery.dynatree.js` → 404.
- No fixture/spec source changes were needed — the #6237-slice-3 fixture
  and the #6189 expand case ran green as shipped.

### 2. No `dynatree` matches in tracked files — PASS (pins + history only)

- `system/installResources/cm1sdk/Widget_SDK.doc` and
  `docs/Widget_SDK.pdf`: **0 matches** (binary grep on this tip; fixed by
  #6236).
- Repo-wide tracked grep (`git ls-files | xargs grep -il dynatree`,
  excluding `docs/ai-generated/`): remaining hits are all intentional —
  absence-pin tests (`PSDynaTreeRetirementTest`,
  `FancyTreeWebResourcesPackagingTest`, `buildLegacyBundles.test.js`
  no-mapping gate, `classic-tree-widgets-npm.spec.js`,
  `published-site-smoke.spec.js`,
  `published-category-tree-fancytree.spec.js` 404/absence assertions),
  the fixture retirement contract + its unit tests
  (`qa_publish_fixture.py`, `test_qa_publish_fixture.py`,
  `test_perc_devctl.py`, adjacent `perc-devctl.py` verify-loop comment),
  one engineering-doc line
  (`docs/developer-module/workbench-rest-and-qa-modes.md`), and the
  #6206 evidence checklist (`specs/992-...`, history). Zero functional
  `dynatree` references in shipped product output.

## Health notes (not blockers)

- `qa-health` reports FAIL on the **shared CMS cell only**, matching a
  preexisting `server.log` line from 09:26
  (`PSDeliveryClient Fatal transport error`, before this run's 10:46
  fixture deploy; probe HTTP 200, container healthy). Owned by the
  sibling seat's cell, not by this slice.
- DTS cell `docker logs` show no Tomcat ERROR/FATAL beyond benign
  install-time lines (Log4j provider notice, Spring INFO chains).

## Commands run

- `python3 docker/scripts/matrix-install-smoke.py --repo-root . --product dts --db h2 --keep --probe-timeout 1200 --skip-image-build` → RESULT:OK (DTS cell HTTP 200)
- `DTS_URL=http://127.0.0.1:42665 python3 docker/scripts/perc-devctl.py qa-publish-fixture` → RESULT:OK (53 paths; SITE/CATEGORY URLs printed)
- `PUBLISHED_SITE_URL=... PUBLISHED_CATEGORY_URL=.../category.html DTS_URL=... npm run test:surface -- --path tests/published-site-smoke.spec.js` → 2 passed
- `... npm run test:surface -- --path tests/published-category-tree-fancytree.spec.js` → 3 passed
- Pin suites re-run on this tip (`clean install`, unchanged modules):
  `FancyTreeWebResourcesPackagingTest` 3/3, `PSDynaTreeRetirementTest`
  5/5; module totals dist-tree 272 (0 failures, 2 preexisting skips),
  perc-packages 293 (0 failures). See PR body build evidence.

No production code, REST, WebUI, or Java sources changed **in this QA
verification run** — QA evidence only. (The product changes under test
shipped in already-merged #6190/#6236; this branch adds only the results
checklist above.)
