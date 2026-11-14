# QA results — issue #6206 (QA for merged #6190, parent #6052)

Merged PR under test: #6190 — `fix(packages+dist): provision FancyTree to
published pages, drop dead DynaTree CSS` (MERGED 2026-11-12).
Verification run on tip of `main` (branch `fix/issue-6206-fancytree-provisioning-qa`).

**Result: PASS** (with step 3 live-cell half deferred to #6189 as the issue
itself prescribes).

## Test plan results (from #6206)

### 1. Category-list widget publishes FancyTree JS/CSS + skin images — PASS (static + packaging tests)

- `modules/perc-distribution-tree/.../installDistributionFiles.xml` provisions
  the published copies (FancyTree comment block, ~line 391):
  `web_resources/cm/jslib/jquery.fancytree.js`,
  `web_resources/cm/css/fancytree/skin/ui.fancytree.css`,
  `web_resources/cm/css/fancytree/skin-win8/` (skin images incl. `loading.gif`).
- `FancyTreeWebResourcesPackagingTest` (perc-distribution-tree) re-run on this
  tip: **Tests run: 3, Failures: 0, Errors: 0** — BUILD SUCCESS.
- `percCategoryList.xml` (published category-list DTS resources): 4 `fancytree`
  references, 0 `dynatree` references.
- `PercCategoryListView.js` invokes `.fancytree(` (init) and
  `.fancytree("destroy")` (teardown); no `.dynatree(` anywhere in shipped JS.
- Render half (tree actually renders on a published page) is env-gated: the
  shipped spec `published-category-tree-fancytree.spec.js` requires
  `PUBLISHED_CATEGORY_URL` pointing at a DTS-served published site — the
  default H2 QA cell has no DTS publish path, and the spec self-skips without
  it. Live render proof stays with #6189 per the #6206 plan ("see residual
  #6189 for the live cell run").

### 2. No `.dynatree-*` CSS references remain in shipped pages — PASS

- `PSDynaTreeRetirementTest` (perc-packages) re-run on this tip:
  **Tests run: 5, Failures: 0, Errors: 0** — BUILD SUCCESS. Covers widget XSL
  `FileLocation`s, shipped `*.js` plugin invocations, `percCategoryList.xml`
  DTS resources, shipped `*.css` selectors, plus positive `.fancytree(`
  controls.
- Repo-wide grep over shipped sources
  (`modules/*/src/main`, `projects/*/src/main`, `system/cms`, `WebUI/src`):
  zero `dynatree` hits except (a) `buildLegacyBundles.test.js`, which asserts
  the DynaTree **absence** gate (no mapping / no bundle line — expected), and
  (b) `WebUI/.../node_modules/jquery.fancytree` dist internals (third-party
  library text mentioning its DynaTree predecessor; `node_modules` is not
  packaged output).
- `Widget_SDK.doc` follow-up half is done: #6236 MERGED; current
  `system/installResources/cm1sdk/Widget_SDK.doc` contains 0 `dynatree` and 0
  stale `fancytree` mentions (binary checked via grep).

### 3. DTS publish path smoke — DEFERRED to #6189 (OPEN, as the plan prescribes)

- The #6206 plan explicitly scopes the live cell run out
  ("Confirm DTS publish path smoke still passes (see residual #6189 for the
  live cell run)").
- #6189 remains OPEN for the live DTS-publish verification; its doc half
  (#6236) is MERGED. No separate smoke run here to avoid duplicating the
  owned residual's cell work.

## Commands run

- `cd modules/perc-distribution-tree && ../../mvnw surefire:test
  -Dtest=FancyTreeWebResourcesPackagingTest` → BUILD SUCCESS,
  Tests run: 3, Failures: 0, Errors: 0
- `cd modules/perc-packages && ../../mvnw surefire:test
  -Dtest=PSDynaTreeRetirementTest` → BUILD SUCCESS,
  Tests run: 5, Failures: 0, Errors: 0
- `grep -rli dynatree` over shipped source trees
  (`modules/*/src/main`, `projects/*/src/main`, `system/cms`, `WebUI/src`) →
  only the two documented exceptions in step 2 (DynaTree-absence test gate +
  third-party `node_modules` lib internals); zero hits in packaged product
  output

No production code, REST, WebUI, or Java sources changed **in this QA
verification run** — QA evidence only. (The product changes under test were
shipped by already-merged #6190; this branch adds only the results checklist
above.)
