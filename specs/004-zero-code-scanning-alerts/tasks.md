---
description: "Task list for zero open code-scanning alerts for 8.2 release"
---

# Tasks: Zero Open Code Scanning Alerts for 8.2 Release

**Input**: Design documents from `/specs/004-zero-code-scanning-alerts/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: REQUIRED for every behavioral code change (Constitution III — Test Discipline).
Each Valid-finding (US3) task MUST include a regression test that demonstrably fails on the pre-fix code and passes on the post-fix code.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Triage (US1) is the gate; the per-disposition phases (US2/US3/US4) operate on clusters of alerts (by rule + module) rather than one task per alert — 866 alerts is too granular to be actionable, and 80%+ of the alerts fall into a small number of clusters.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact mono-repo file paths in descriptions (module + `src/main` / `src/test`)

## Path Conventions

- **Triage artifacts**: `docs/ai-generated/tasks/gh-codeql-alerts/{triage,suppressions,accepted-risks,release-readiness-8.2}.md`
- **Suppression / CodeQL config**: `.github/codeql/codeql-config.yml`, inline `// codeql[rule-id]` comments
- **Removed obsolete files**: under `WebUI/src/main/webapp/`, `system/cms/content/applications/sys_resources/ApplicationFiles/`, `modules/perc-toolkit/src/main/resources/InstallDir/`, `deliverytiersuite/delivery-tier-suite/*/src-js/`
- **Packaging surface** (must update when removing bundled files): `modules/perc-ant/install.xml`, `modules/perc-distribution-tree/pom.xml`, `modules/perc-packages/`
- **Build / test**: `./mvn-env.sh -pl <module> -am test` against JDK 21 (per `AGENTS.md`)
- **Branch policy (per `AGENTS.md`)**: work is authored on a `004-zero-code-scanning-alerts` branch cut from `development`; per-cluster PRs target `development`. Headline `0 active alerts` is measured on `development` at the `8.2` release cut.

## Scope Numbers (seeded 2026-07-11)

- **866 open alerts** across **338 distinct files** and **38 distinct rules** on `development` for `intersoftdatalabs-in/percussioncms`.
- **By disposition (candidate, seeded in `triage.md`)**: 485 obsolete, 380 valid, 1 false-positive, 0 accepted-risk.
- **Target milestone reconciliation** (per analyze finding A1): the seed uses `8.2-must-fix` for all high-severity valid rows, but US3 strategy admits some non-trivial valid fixes will not land in `8.2`. **Per-finding target_milestone MUST be assigned in T014–T016** as part of owner sign-off, not batch-applied. The default seed value of `8.2-must-fix` is a starting point; any valid row whose fix requires a major-version upgrade (legacy crypto in `modules/perc-legacy/`, large refactors in `js/polynomial-redos` on vendored files being deleted) MUST be reclassified to `8.2-backlog` or `accepted-risk` with target_milestone ≥ `8.3` before its closing PR is opened.
- **Top 10 rules = 742 alerts (86%)**: `js/xss-through-dom` (168), `js/incomplete-sanitization` (164), `js/html-constructed-from-input` (96), `js/unsafe-jquery-plugin` (84), `java/path-injection` (58), `js/functionality-from-untrusted-source` (55), `java/xss` (35), `js/prototype-pollution-utility` (32), `js/incomplete-multi-character-sanitization` (27), `js/xss` (23).
- **Top 5 modules = 853 alerts (98%)**: `WebUI/` (498), `system/` (133), `projects/sitemanage/` (128), `modules/perc-packages/` (35), `modules/perc-common-ui-bundle/` (14).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm tooling, branch policy, and triage baseline before any disposition work.

- [ ] T001 Verify JDK 21 via `./mvn-env.sh --version` and confirm `gh auth status` is green
- [ ] T002 [P] Read root `AGENTS.md` and identify the module owner for each top-5 module (`WebUI/`, `system/`, `projects/sitemanage/`, `modules/perc-packages/`, `modules/perc-common-ui-bundle/`)
- [ ] T003 [P] Verify `scripts/fetch-gh-code-scanning-alerts.sh` works against `intersoftdatalabs-in/percussioncms` and writes a non-empty `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` (already fixed 2026-07-11)
- [ ] T004 [P] Confirm CodeQL Advanced workflow runs on `push` to `development` (`.github/workflows/codeql.yml`); do not change trigger policy in this feature
- [ ] T005 Create/checkout the `004-zero-code-scanning-alerts` branch from `development` and push so per-module PRs can target it

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Produce the triage inventory that ALL per-disposition work depends on. No alert closure work can begin until this phase is complete.

**⚠️ CRITICAL**: US1 (the completed triage inventory) is the gate for US2/US3/US4.

- [ ] T006 Generate `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` via `scripts/fetch-gh-code-scanning-alerts.sh intersoftdatalabs-in/percussioncms` and confirm 866 alerts
- [ ] T007 Verify the seeded `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` has 866 rows with all required columns (per `contracts/C1`)
- [ ] T007b [P] **Stale scanner cache filter (per analyze U1, spec edge case "file path no longer exists in git")**: extend `scripts/fetch-gh-code-scanning-alerts.sh` to filter out alerts whose `most_recent_instance.location.path` is absent from `git ls-files` on the current branch. For each filtered alert, append a row to `docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md` (new file) with `alert_id`, `rule_id`, `path`, and `last_seen_branch` for audit. The release-readiness report MUST exclude stale-cache rows from the open-alert count.
- [ ] T008 [P] Create empty `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` with C3 column header
- [ ] T009 [P] Create empty `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md` with C4 column header
- [ ] T010 [P] Open one tracking issue (or umbrella task) per module containing all rows assigned to it, so module owners have a single place to track closure
- [ ] T011 Establish a weekly re-scan cadence by running the fetch script weekly; document the cadence in `docs/ai-generated/tasks/gh-codeql-alerts/README.md` (already done — update only if cadence changes)

**Checkpoint**: Foundation ready — every open alert has a row in `triage.md`, indexes exist, module owners have a tracking artifact.

---

## Phase 3: User Story 1 - Triage Every Open Alert (Priority: P1) 🎯 MVP

**Goal**: Every open alert on `development` has a confirmed disposition (obsolete / valid / false-positive / accepted-risk), a named module owner, a target action, and a target milestone. The candidate dispositions are already seeded in `triage.md`; this phase converts them into confirmed dispositions with owners' sign-off.

**Independent Test**: Row count in `triage.md` == number of open alerts in `alerts.md`; every `false-positive` and `accepted-risk` row has non-empty `notes`; every `module_owner` is a path listed in `./AGENTS.md`.

### Tests for User Story 1

- [ ] T012 [P] [US1] Add a CI-lite check script `scripts/verify-triage-inventory.sh` that fails if (a) row count != open alert count, (b) any `false-positive`/`accepted-risk` row has empty `notes`, (c) any `module_owner` is not under `./AGENTS.md`. Script MUST be POSIX `sh` per AGENTS.md.
- [ ] T013 [P] [US1] Add an example fixture under `scripts/test-fixtures/triage-good.md` and `scripts/test-fixtures/triage-bad.md` (for tests of T012 if invoked via `shunit2` or manual run)

### Implementation for User Story 1

- [ ] T014 [US1] Confirm dispositions for all 485 obsolete rows in `triage.md` — module owners verify the flagged files are not referenced by build/runtime and are not shipped
- [ ] T015 [US1] Confirm dispositions for all 380 valid rows in `triage.md` — module owners confirm the finding is real and assign `linked_pr` once the fix lands
- [ ] T016 [US1] Confirm the 1 false-positive row in `triage.md` — security reviewer signs off and the suppression entry is queued (executed in US4)
- [ ] T017 [US1] Run `scripts/verify-triage-inventory.sh` and ensure it passes; commit any disposition-only updates to `triage.md`
- [ ] T018 [US1] Publish `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` initial version per `contracts/C6` so progress is visible at any moment

**Checkpoint**: All 866 open alerts have confirmed disposition, owner, target action, and target milestone; the verify-script passes; release-readiness report is live.

---

## Phase 4: User Story 2 - Remove Obsolete Code and Third-Party Scripts (Priority: P1)

**Goal**: Delete or de-bundle the 485 obsolete vendored 3rd-party files that the scanner is flagging, without breaking the build or the installed distribution. Target: zero obsolete alerts at re-scan.

**Independent Test**: After the removal PRs merge, `scripts/fetch-gh-code-scanning-alerts.sh` reports zero alerts at the obsolete-rule paths; the owning module's test suite passes; the rebuilt `.ppkg` / distribution archive listing does not contain the removed files.

### Tests for User Story 2

- [ ] T019 [P] [US2] Add `scripts/verify-distribution-archive.sh` that runs `./mvn-env.sh -pl modules/perc-distribution-tree -am clean package` then `unzip -l` on the resulting JARs and fails if any removed filename appears
- [ ] T019b [P] [US2] **Pre-removal baseline capture (per analyze C2, Constitution III fail-then-pass)**: before T021–T031 merges, capture `./mvn-env.sh -pl <module_owner> -am test` output as a baseline artifact (commit hash + log path under `tmp/baselines/<module>-<commit>.log`) and verify the build + test suite is GREEN on the pre-removal commit. The removal commit IS the behavioral change under Constitution III; the post-removal test run is the "PASS" half of the fail-then-pass loop. Each removal PR MUST cite the baseline log path in its PR body per `contracts/C5`.
- [ ] T020 [P] [US2] Add unit-test guarantee in each touched module that no test references the removed vendored file (e.g., grep `grep -RIn "knockoutjs" <module>/src/test/` returns empty)

### Implementation for User Story 2

Group removals by module. Each cluster = one PR. All clusters can be developed in parallel because each one touches a distinct file set.

- [ ] T021 [US2] **WebUI/ — knockout.js vendored dist**: delete `WebUI/src/main/webapp/cm/pages/cui/components/knockoutjs/`, `WebUI/src/main/webapp/cm/cui/components/knockoutjs/`, `WebUI/src/main/webapp/cm/widgets/knockoutjs/` (covers alerts 1708, 1707, 1706, 1705, … ~32 alerts of `js/prototype-pollution-utility` + `js/useless-regexp-character-escape` + `js/bad-tag-filter`); update `WebUI/pom.xml` if `knockoutjs` is referenced in `<resources>` or `<webResources>`; rebuild `WebUI/war` and verify `unzip -l WebUI/target/*.war` does not list `knockout*.js`
- [ ] T022 [US2] **WebUI/ — twitter-bootstrap-3.0.0**: delete `WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/`, `WebUI/src/main/webapp/cm/cui/components/twitter-bootstrap-3.0.0/`, `WebUI/war/cui/components/twitter-bootstrap-3.0.0/` (covers ~250 alerts across `js/xss-through-dom`, `js/unsafe-jquery-plugin`, `js/xss`, `js/xss-through-exception`); update any `<resources>` references in `WebUI/pom.xml`; verify rebuilt `war` excludes the path
- [ ] T023 [US2] **WebUI/ — jquery migrate**: delete `WebUI/src/main/webapp/cm/vendor/js/legacy/profiles/3x/jquery/jquery-migrate-3.3.2.js` (covers `js/unsafe-html-expansion` × 12) and any sibling jquery-migrate files; confirm no `WebUI/pom.xml` resource reference
- [ ] T024 [US2] **WebUI/ — shared scripts**: delete `WebUI/src/main/webapp/cm/shared-common.js`, `shared-common-minuet.js`, `shared-finder.js`, `plugins/perc_utils.js`, `app/js/legacy/plugins/perc_utils.js`, `war/plugins/perc_utils.js`, `war/shared-finder.js` only after confirming no JS or JSP references them; covers ~100 alerts of `js/incomplete-sanitization` + `js/incomplete-multi-character-sanitization`; replacement scripts must preserve any public API used by JSP pages
- [ ] T025 [US2] **WebUI/ — PercDataTable widget**: delete `WebUI/src/main/webapp/cm/widgets/PercDataTable/` (covers ~96 `js/html-constructed-from-input`); confirm no JSP or sitemanage reference; update any navigation registration in `WebUI/src/main/webapp/cm/` config
- [ ] T026 [US2] **WebUI/ — third-party vendored JS (highlight, datatables, qunit)**: delete `WebUI/src/main/webapp/cm/api/lib/highlight.7.3.pack.js`, `WebUI/war/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js`, `WebUI/war/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js`
- [ ] T026b [US2] **WebUI/ — requirejs-text vendored plugin**: delete `WebUI/src/main/webapp/cm/pages/cui/components/requirejs-text/text.js`, `WebUI/src/main/webapp/cm/cui/components/requirejs-text/text.js`, `WebUI/war/cui/components/requirejs-text/text.js` AND remove the `text:` plugin entry from `WebUI/src/main/webapp/cm/{pages,cui}/pages/_bootstrap.js` after confirming no module loads text resources via this plugin path; covers 6 `js/polynomial-redos` alerts (#1406, #1405, #1404, #1403, #1037, #1036). Required for T058 closure.
- [ ] T026c [US2] **deliverytiersuite/ — p13n-ds vendored jQuery**: delete `deliverytiersuite/delivery-tier-suite/p13n-ds/lib-js/jquery-treeview/lib/jquery.js` after confirming no module references the treeview widget's jQuery path; covers 2 `js/redos` alerts (#1038, #1039). Required for T059 closure.
- [ ] T027 [US2] **system/ — ApplicationFiles JS (dojo, trinidad, jstree)**: delete `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/`, `ApplicationFiles/trinidad/`, `ApplicationFiles/js/taxonomy/jquery.jstree*` files; update any `sys_resources/ApplicationFiles/*.xml` references; rebuild `system/` and verify
- [ ] T028 [US2] **system/ — dojo vendor files in non-ApplicationFiles paths**: confirm no other dojo vendored copies; remove if found
- [ ] T029 [US2] **projects/sitemanage/ — test sample HTML**: delete `projects/sitemanage/src/test/java/com/percussion/sitemanage/importer/helpers/CM1094-SamplePage.html` after confirming it is referenced only by tests that can be updated; covers 28 alerts of `js/incomplete-sanitization`
- [ ] T030 [US2] **modules/perc-toolkit/ — InstallDir pages**: delete `modules/perc-toolkit/src/main/resources/InstallDir/user/pages/DispatchTemplateGenerator.jsp` if dispatcher route is no longer needed; covers 1 alert (`js/script-loaded-from-untrusted-source`); alternatively add the integrity-check header rather than delete
- [ ] T031 [US2] **deliverytiersuite/ — DTS src-js legacy**: delete any vendored 3rd-party files under `deliverytiersuite/delivery-tier-suite/*/src-js/` after confirming no module references; covers ~3 obsolete
- [ ] T032 [US2] **Packaging sweep**: for every removed file in T021–T031, run `scripts/verify-distribution-archive.sh` against `modules/perc-distribution-tree` and `modules/perc-packages`; update `modules/perc-ant/install.xml` glob/delete patterns if any referenced the removed files
- [ ] T033 [US2] Run `scripts/fetch-gh-code-scanning-alerts.sh` post-merge of T021–T032; verify the obsolete-rule alert count drops by the expected number (~485) and that no new alerts have appeared
- [ ] T034 [US2] Update `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` with the new counts; mark obsolete rows in `triage.md` with the merged PR number in `linked_pr`

**Checkpoint**: All obsolete-rule paths are gone from the tree and the distribution archive; obsolete alert count is 0.

---

## Phase 5: User Story 3 - Mitigate Valid Findings (Priority: P2)

**Goal**: Fix every valid finding with the smallest correct fix + a regression test that fails on pre-fix code and passes on post-fix code. Target: zero valid alerts at re-scan.

**Independent Test**: For each valid finding, the regression test demonstrably fails on the pre-fix commit and passes on the post-fix commit (commit hash recorded in the PR body); the owning module's full test suite passes; the scanner re-scan reports the alert as resolved.

### Tests for User Story 3

- [ ] T035 [P] [US3] Add `scripts/verify-valid-fixes.sh` that loops through `triage.md` rows with `disposition == valid` and confirms each has a non-empty `linked_pr` after the PR merges; fails otherwise
- [ ] T036 [P] [US3] For each Java critical-severity finding (13 criticals), add a regression test FIRST that fails on the current (vulnerable) code per Constitution III fail-then-pass; the test must exercise the vulnerability sink with the same input pattern that triggered the CodeQL finding

### Implementation for User Story 3 (Java critical — MUST land first)

Group by rule; one PR per cluster; all clusters can run in parallel.

- [ ] T037 [US3] **`java/ssrf` (6 alerts, critical, 8.2-blocker)**: fix `modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java` (and any other flagged files) to validate the URL host against an allow-list before issuing the outbound request; reuse `modules/perc-security-utils/` `PSUrlValidator` if available; add regression test in `modules/extensions-main/src/test/`
- [ ] T038 [US3] **`js/code-injection` (4 alerts, critical, 8.2-blocker)**: fix `system/cms/content/applications/sys_resources/ApplicationFiles/dojo/src/io/RepubsubIO.js` (and any other flagged file) by replacing `eval`/dynamic dispatch with a static dispatch table; if the dojo file is removed under US2 instead, mark these alerts as obsolete and update `triage.md`
- [ ] T039 [US3] **`java/xxe` (2 alerts, critical, 8.2-blocker)**: fix `system/business/src/com/percussion/share/dao/PSSerializerUtils.java` by disabling DOCTYPE declaration and external entities on the `DocumentBuilderFactory`; reuse `modules/perc-xml-security/` safe-parser factory if available; add regression test
- [ ] T040 [US3] **`java/ldap-injection` (1 alert, critical, 8.2-blocker)**: fix `system/src/main/java/com/percussion/security/PSJndiGroupProvider.java` by parameterizing the LDAP filter with `SearchControls` and escape user-supplied values; add regression test
- [ ] T041 [US3] **`java/zipslip` (3 alerts, high)**: fix `system/src/main/java/com/percussion/system/utils/PSArchiveFiles.java` by validating every entry path resolves under the destination root before extraction; reuse `modules/utils/` `PSPathUtils` if available; add regression test
- [ ] T042 [US3] **`java/sql-injection` (7 alerts, high)**: fix `projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDaoHelper.java` (and any other flagged files) by converting dynamic SQL to parameterized queries via Hibernate `setParameter`; add regression test
- [ ] T043 [US3] **`java/path-injection` (58 alerts, high — largest single cluster)**: fix `projects/sitemanage/src/main/java/com/percussion/utils/service/impl/PSSiteConfigUtils.java` and the other 50+ flagged sites by validating paths against an allow-list of base directories and rejecting `..` traversal; add a regression test for each fix (consolidate where reasonable); prefer a single helper in `modules/utils/` shared across call sites
- [ ] T044 [US3] **`java/xss` (35 alerts, high)**: fix `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java` and other flagged sites by encoding output with `org.owasp.encoder.Encode.forHtml` or the project's standard escape helper; add regression tests
- [ ] T045 [US3] **`java/regex-injection` (6 alerts, high)**: fix `deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/rdbms/impl/PSBlogPostVisitDao.java` and other flagged sites by escaping user input via `Pattern.quote` before composing the regex; add regression tests
- [ ] T046 [US3] **`java/insecure-trustmanager` (2 alerts, high)**: fix `system/business/src/com/percussion/delivery/client/PSDeliveryClient.java` by replacing the trusting `X509TrustManager` with one that validates against the system trust store; add regression test
- [ ] T047 [US3] **`java/weak-cryptographic-algorithm` (3 alerts, high)**: fix `modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java` by upgrading to AES/GCM and a random IV; OR, if the file is in `legacy/`, mark these as accepted-risk with target_milestone=`9.0` and update `triage.md` and `accepted-risks.md`
- [ ] T048 [US3] **`java/static-initialization-vector` (2 alerts, high)**: same file as T047; resolve via the same decision
- [ ] T049 [US3] **`java/redos` (1 alert, high)**: fix `modules/extensions-main/src/main/java/com/percussion/extensions/translations/PSFormEncodeDecodeHelper.java` regex to avoid catastrophic backtracking; add regression test with adversarial inputs
- [ ] T050 [US3] **`java/polynomial-redos` (2 alerts, high)**: fix `deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/data/impl/PSCriteriaElement.java` regex; add regression test
- [ ] T051 [US3] **`java/unvalidated-url-redirection` (6 alerts, medium)**: fix `deliverytiersuite/delivery-tier-suite/common/src/main/java/com/percussion/delivery/exceptions/PSUncaughtError.java` and other flagged sites by validating redirect targets against an allow-list; add regression tests
- [ ] T052 [US3] **`java/unvalidated-url-forward` (1 alert, high)**: fix `modules/servletutils/src/main/java/com/percussion/servlet_utils/servlet/PSServletUtils.java`; add regression test
- [ ] T053 [US3] **`java/unsafe-hostname-verification` (1 alert, medium)**: fix `projects/sitemanage/src/main/java/com/percussion/sitemanage/importer/PSSiteImporter.java`; add regression test
- [ ] T054 [US3] **`java/error-message-exposure` (22 alerts, medium)**: fix `projects/sitemanage/src/main/java/com/percussion/designmanagement/service/impl/PSWebResourcesRestService.java` and other flagged sites by replacing `e.getMessage()` in HTTP responses with a generic message and logging the detail server-side; add regression tests
- [ ] T055 [US3] **`java/stack-trace-exposure` (2 alerts, medium)**: fix `modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactoryException.java`; add regression test
- [ ] T056 [US3] **`java/implicit-cast-in-compound-assignment` (3 alerts, high)**: fix `deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java` — but since these are in a test perf micro-benchmark (see triage seed), these are likely false-positives; treat as US4 instead. Update `triage.md`.
- [ ] T057 [US3] **`java/insecure-cookie` (1 alert, medium)**: fix `modules/p13n-api/src/main/java/com/percussion/soln/p13n/tracking/web/CookieGenerator.java` by setting `Secure` and `HttpOnly` flags; add regression test
- [ ] T058 [US3] **`js/polynomial-redos` (6 alerts, high)**: fix `WebUI/src/main/webapp/cm/pages/cui/components/requirejs-text/text.js` and other flagged files — if files are removed under US2, mark these as obsolete instead
- [ ] T059 [US3] **`js/redos` (3 alerts, high)**: fix `deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/perc_p13n_profile.js` regex; add regression test
- [ ] T060 [US3] **`js/xss` (23 alerts, high)**: fix `WebUI/src/main/webapp/cm/pages/cui/components/twitter-bootstrap-3.0.0/js/tests/vendor/qunit.js` and other flagged files — if removed under US2, mark obsolete
- [ ] T061 [US3] Run `scripts/fetch-gh-code-scanning-alerts.sh` post-merge of T037–T060; verify the valid-rule alert count is now 0
- [ ] T061b [US3] **Same-PR accepted-risk rule (per analyze C3, FR-008)**: for any valid alert that the closing PR cannot fully resolve (e.g., T047/T048 legacy crypto, T056 implicit-cast in test perf, any cluster where the diff doesn't actually close the alert), the closing PR MUST (a) flip `triage.md` `disposition` to `accepted-risk` and assign `target_milestone` ≥ `8.3` in the same commit, (b) append a matching row to `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md` per `contracts/C4`, and (c) cite the new accepted-risk ID in the PR body. Deferring accepted-risk creation to a later PR is forbidden.
- [ ] T062 [US3] Run `scripts/verify-valid-fixes.sh` and ensure all valid rows in `triage.md` have a non-empty `linked_pr`
- [ ] T063 [US3] Update `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` with new counts

**Checkpoint**: All 380 valid alerts have a merged closing PR with a regression test; valid alert count is 0.

---

## Phase 6: User Story 4 - Document and Suppress False Positives (Priority: P3)

**Goal**: For every `false-positive` row in `triage.md`, apply a CodeQL inline suppression with a written justification that another reviewer can locate and verify; add a row to `suppressions.md`; ensure the suppression is not silently dropped.

**Independent Test**: A fresh re-scan does not re-open the alert; the inline `// codeql[rule-id]` comment matches the `suppressions.md` row verbatim; an independent reviewer can find the justification by reading the cited file/config.

### Tests for User Story 4

- [ ] T064 [P] [US4] Add `scripts/verify-suppressions.sh` that for every row in `suppressions.md`, greps the source file at the cited line, asserts the `// codeql[…]` comment exists with matching `justification:` text, and fails if a row is older than one release cycle without a `stale-suppression` note (per FR-007)
- [ ] T065 [P] [US4] Add an integration check that confirms `.github/codeql/codeql-config.yml` is valid YAML and that any `paths-ignore` / query-filter entries introduced under this feature are also rows in `suppressions.md` (no orphan exclusions)

### Implementation for User Story 4

- [ ] T066 [US4] Confirm the 1 seeded false-positive row in `triage.md` (`java/implicit-cast-in-compound-assignment` in `deliverytiersuite/.../feeds/.../PSFeedServicePerformanceTest.java`) and apply an inline `// codeql[java/implicit-cast-in-compound-assignment] justification: <reference to the perf benchmark that intentionally narrows long→int>` comment in the flagged source file (or convert the test code so the suppression isn't needed)
- [ ] T067 [US4] Append the matching row to `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` per `contracts/C3`
- [ ] T068 [US4] For each `valid` Java finding that was downgraded to `false-positive` during US3 (e.g., T056 implicit-cast-in-compound-assignment in test perf micro-bench), apply the same inline-suppression workflow as T066–T067
- [ ] T069 [US4] For any `false-positive` finding whose path is in vendored code being removed under US2, flip the row in `triage.md` from `false-positive` to `obsolete` so it's not double-counted; do not add to `suppressions.md` (file is gone)
- [ ] T070 [US4] Re-scan: run `scripts/fetch-gh-code-scanning-alerts.sh` and confirm no false-positive alert has re-opened
- [ ] T071 [US4] Run `scripts/verify-suppressions.sh` and ensure every suppression is verifiable
- [ ] T072 [US4] Update `release-readiness-8.2.md` with final suppression counts

**Checkpoint**: Every false-positive row in `triage.md` has a corresponding inline suppression + `suppressions.md` row; re-scan shows none have re-opened.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Release-readiness sign-off, accepted-risk finalization, stale-suppression re-review, and overall report publication.

- [ ] T073 Run `scripts/fetch-gh-code-scanning-alerts.sh` and confirm **0 active code-scanning alerts** not on the accepted-risk list (per `SC-001` / FR-009)
- [ ] T074 [P] Review any row in `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md` whose `expires_at` is at or before the `8.2` release date; escalate to `8.3` milestone or move to a different disposition
- [ ] T075 [P] Re-review every suppression older than one release cycle; mark `stale-suppression` in `suppressions.md` and assign a re-review date (per FR-007)
- [ ] T076 Finalize `docs/ai-generated/tasks/gh-codeql-alerts/release-readiness-8.2.md` per `contracts/C6` with: total open alerts (target 0), counts by disposition + severity, accepted-risks list, pass/fail decision
- [ ] T077 Reference `release-readiness-8.2.md` in the `8.2` release notes; list each accepted-risk by alert ID
- [ ] T078 For every closing PR (T021–T072), confirm the PR review-comment resolution procedure in `./AGENTS.md` was followed: every review comment has an inline reply citing the commit hash AND the corresponding review thread is resolved via the `resolveReviewThread` GraphQL mutation (Constitution IX, `SC-007`)
- [ ] T078b **Per-PR pre-merge gate (per analyze C1, Constitution IX non-negotiable)**: every closing PR for tasks T021–T072 MUST include the merged PR body line `Review-resolution-gate: passed` only AFTER the PR author has run the GraphQL `resolveReviewThread` mutation on every outstanding review thread and posted an inline reply on every comment citing the commit hash. The PR template at `.github/PULL_REQUEST_TEMPLATE/` (or equivalent) MUST include this gate as a required checkbox before the merge button is enabled. `scripts/verify-pr-review-resolution.sh` (new) reads `gh pr view --json reviewThreads` for each closing PR and fails if any thread has `isResolved: false`.
- [ ] T079 [P] Update module-level READMEs / Maven site notes for any module whose public API or distribution artifact changed in this feature
- [ ] T080 [P] Confirm Spotless has run on every touched module (`./mvn-env.sh -pl <module> spotless:check`) — fixes for valid findings may introduce new lines that need formatting
- [ ] T081 Update `docs/ai-generated/tasks/gh-codeql-alerts/README.md` with the final disposition counts and a link to the release-readiness report

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories. **T007b (stale scanner cache filter) MUST complete before US1** so the seeded `triage.md` excludes already-gone files.
- **User Story 1 (Phase 3)**: Depends on Foundational completion. Completing US1 unblocks US2/US3/US4 because every per-disposition PR references a row in `triage.md`.
- **User Story 2 (Phase 4)**: Depends on US1 (triage row exists with `disposition = obsolete`). **T019b (pre-removal baseline capture) MUST complete before any T021–T031 merge** so the fail-then-pass loop for removals is provable (Constitution III, analyze C2). Can run in parallel with US3 and US4 once US1 is done.
- **User Story 3 (Phase 5)**: Depends on US1 (triage row exists with `disposition = valid`). **T061b (same-PR accepted-risk rule) is enforced as part of every closing PR in T037–T060** — deferring accepted-risk creation to a later PR is forbidden (analyze C3). Can run in parallel with US2 and US4 once US1 is done.
- **User Story 4 (Phase 6)**: Depends on US1 (triage row exists with `disposition = false-positive`). Can run in parallel with US2 and US3 once US1 is done. **Also** depends on US2 for findings that were reclassified obsolete during US2/T069.
- **Polish (Phase 7)**: Depends on US2, US3, and US4 all complete. **T078b (per-PR pre-merge review-resolution gate) is enforced during every closing PR**, not just at Phase 7 audit — T078 is the retrospective check that the gate was actually followed.

### User Story Dependencies

- **US1 (P1)**: Independent after Foundational. No dependencies on other stories.
- **US2 (P1)**: Depends on US1 only. Independent of US3 and US4 (different files; different disposition semantics).
- **US3 (P2)**: Depends on US1 only. Independent of US2 and US4 in terms of file paths (Java server-side code is disjoint from vendored JS). T056/T058/T060 may produce findings that are reclassified obsolete because US2 removed the underlying file — coordinate via `triage.md` updates.
- **US4 (P3)**: Depends on US1. Independently iterable; coordinates with US2 (T069) and US3 (T068) only at the disposition-flip level.

### Within Each User Story

- Tests MUST be written/updated and FAIL before implementation, then PASS after (Constitution III). For US3 specifically, the regression test for each Java critical finding (T036) MUST fail on the pre-fix code (run from the pre-fix commit) and pass on the post-fix code.
- Prefer service/domain changes before REST resources or UI wiring.
- For each Valid-finding fix (US3), record the pre-fix test-run commit hash in the PR body per `contracts/C5` so reviewers can reproduce the fail-then-pass.
- Story complete before moving to the next priority.

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel.
- All Foundational tasks marked [P] can run in parallel.
- Once US1 completes, US2 / US3 / US4 can all run in parallel (different module owners, different file sets).
- Within US2, every removal cluster (T021–T031) is an independent PR with a different file set; all can be developed in parallel by different contributors.
- Within US3, every per-rule cluster (T037–T060) is an independent PR; critical findings (T037–T040) should be prioritized and can still be developed in parallel with each other (different files).
- All tests for a user story marked [P] can run in parallel.
- Different user stories can be worked on in parallel by different team members when module ownership does not conflict — for US3, the per-rule clusters touch different modules (extensions-main, system, projects/sitemanage, etc.) and can be parallelized.

---

## Parallel Examples

### Parallel Example: User Story 2 (Obsolete removals)

```bash
# All four WebUI removal clusters can run in parallel (distinct files):
Task: "T021 [US2] Remove knockout.js vendored dist from WebUI/"
Task: "T022 [US2] Remove twitter-bootstrap-3.0.0 from WebUI/"
Task: "T023 [US2] Remove jquery-migrate from WebUI/"
Task: "T024 [US2] Remove shared-*.js and perc_utils.js from WebUI/"

# System-side removals can run in parallel with WebUI removals:
Task: "T027 [US2] Remove ApplicationFiles dojo/trinidad/jstree from system/"
Task: "T028 [US2] Remove dojo vendor files in non-ApplicationFiles paths from system/"

# Packaging sweep waits for all removals to land, then runs once:
Task: "T032 [US2] Packaging sweep — update modules/perc-ant/install.xml"
```

### Parallel Example: User Story 3 (Valid Java fixes)

```bash
# Four critical-severity Java fixes can run in parallel (different files):
Task: "T037 [US3] Fix java/ssrf in PSProxyQueryResource.java"
Task: "T039 [US3] Fix java/xxe in PSSerializerUtils.java"
Task: "T040 [US3] Fix java/ldap-injection in PSJndiGroupProvider.java"
Task: "T041 [US3] Fix java/zipslip in PSArchiveFiles.java"

# High-volume rule clusters can also run in parallel:
Task: "T042 [US3] Fix java/sql-injection in PSPageDaoHelper.java"
Task: "T043 [US3] Fix java/path-injection across projects/sitemanage/ (58 alerts)"
Task: "T044 [US3] Fix java/xss in PSSiteDataRestService.java (35 alerts)"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Phase 1: Setup (T001–T005)
2. Phase 2: Foundational (T006–T011) — produces the `triage.md` inventory + index files + tracking issues
3. Phase 3: US1 (T012–T018) — confirm dispositions, owner sign-off, live release-readiness report
4. **STOP and VALIDATE**: At this point, every alert has a row + a named owner + a target milestone. The work has measurable progress (e.g., "0 obsolete alerts closed yet" but "485 obsolete rows ready to be acted on").
5. This is enough to communicate progress to stakeholders; no code changes have been made yet.

### Incremental Delivery

1. Setup + Foundational + US1 → foundation ready, triage visible, no code changes yet
2. Add US2 (Obsolete) → most cheap wins, ~485 alerts closed → **largest single reduction in alert count**
3. Add US3 (Valid) → 380 alerts closed with regression tests → **highest security impact**
4. Add US4 (False Positives) → final noise reduction
5. Polish → release-readiness report published, `0 active alerts` declared (or `PASS-WITH-EXCEPTIONS` if accepted-risks remain)

### Parallel Team Strategy

With multiple developers / module owners:

1. All: Setup + Foundational (T001–T011) — one release/security engineer drives, with T002/T003 helpers
2. Once US1 is complete (T014–T018):
   - Developer A (WebUI owner): US2 clusters T021–T026
   - Developer B (system owner): US2 T027–T028, US3 T039/T040/T041
   - Developer C (projects/sitemanage owner): US3 T042/T043/T044/T053
   - Developer D (modules/extensions-main owner): US3 T037/T049
   - Developer E (modules/perc-legacy owner): US3 T047/T048 (or accept-risk)
   - Developer F (DTS owner): US3 T045/T050/T051/T056
3. US4 owner: process any false-positive reclassifications from US2/US3 (T066–T072)
4. Release engineer drives Phase 7 (T073–T081)

### Realistic 8.2 Outcome

- **If US2 + US3 + US4 complete fully**: `0 active code-scanning alerts` (PASS per `SC-001`).
- **If US2 + US3 + US4 complete but a few findings cannot be remediated**: file them in `accepted-risks.md` with `target_milestone = 8.3` and ship as `PASS-WITH-EXCEPTIONS` per `contracts/C6`.
- **If US2 only completes**: ~485 alerts closed; ~380 valid + ~1 false-positive remain; NOT release-ready.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story for traceability.
- Each user story is independently completable and testable.
- For US3 (Valid fixes), regression tests MUST fail on the pre-fix code — verify by checking out the pre-fix commit and running the test, per `contracts/C5`.
- For US2 (Obsolete removals), the build + module test suite MUST be GREEN on the pre-removal commit (baseline captured in T019b) AND on the post-removal commit (fail-then-pass loop per Constitution III / analyze C2).
- For every closing PR, follow the PR review-comment resolution procedure in `./AGENTS.md` (Constitution IX, `SC-007`). The per-PR pre-merge gate (T078b) is enforced as part of the PR template, not just at Phase 7 audit (analyze C1).
- Accepted-risk creation (T061b) MUST happen in the same PR as the failed closure (Constitution VI / FR-008 / analyze C3) — never deferred.
- Stale scanner cache (file no longer in git) is filtered by T007b before any disposition work begins (analyze U1).
- Per-finding target_milestone is assigned during owner sign-off in T014–T016 (analyze A1), not batch-applied; `8.2-must-fix` is the starting default.
- Commit after each task or logical group.
- Stop at any checkpoint to validate story independently.
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence.
- The `release-readiness-8.2.md` file is the single source of truth for whether `0 active alerts` is achieved — re-check it after every cluster of PRs lands.

## Remediation History (from `/speckit.analyze` 2026-07-11)

| Finding | Severity | Fix applied |
|---------|----------|-------------|
| C1 — Constitution IX per-PR gate | CRITICAL | Added **T078b**: per-PR pre-merge gate + PR-template checkbox + `scripts/verify-pr-review-resolution.sh`. Updated Phase Dependencies + Notes. |
| C2 — Fail-then-pass for removals | HIGH | Added **T019b**: pre-removal baseline capture before T021–T031 merge. Updated Notes + Phase Dependencies. |
| C3 — Accepted-risk in same PR | HIGH | Added **T061b**: same-PR rule for `accepted-risks.md` creation. Updated Phase Dependencies. |
| A1 — Target milestone reconciliation | HIGH | Added scope note in "Scope Numbers" + clarified T014–T016 are the per-finding milestone decision point. |
| U1 — Stale scanner cache | HIGH | Added **T007b**: stale-cache filter in `scripts/fetch-gh-code-scanning-alerts.sh` + new `alerts-stale-cache.md` audit file. Updated Phase Dependencies. |
