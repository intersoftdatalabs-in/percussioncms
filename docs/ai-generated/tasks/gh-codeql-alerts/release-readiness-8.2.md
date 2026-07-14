# Release Readiness — 8.2 Code-Scanning Remediation (in-progress snapshot)

**Branch**: `004-zero-code-scanning-alerts`
**Status**: IN PROGRESS — 3 cluster PRs staged, tooling complete
**Generated**: 2026-07-13

Source of truth: `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`,
`suppressions.md`, `accepted-risks.md`. Format follows
`specs/004-zero-code-scanning-alerts/contracts/README.md` C6.

## 1. Total open alerts (not on accepted-risk list)

| Source | Count |
|--------|-------|
| `alerts.md` (raw fetch) | 866 |
| `alerts-stale-cache.md` (excluded; file path not in `git ls-files`) | 0 (recomputed 2026-07-13) |
| `triage.md` rows with `disposition != accepted-risk` | 866 |
| `accepted-risks.md` rows | 0 |

> Pass condition for SC-001: the first row = the third row AND equals 0.

## 2. Counts by disposition

| Disposition | Count | Closed in working tree (pending PR merge) |
|-------------|-------|-------------------------------------------|
| `obsolete` | 485 | 104 rows affected by T027 (ApplicationFiles dojo/trinidad/jstree) + T029 (CM1094-SamplePage.html + 5 home_*.html); PR bodies in `tmp/gh-codeql-alerts/cluster-logs/T{027,029}-pr-body.md` |
| `valid` | 380 | 6 rows affected by T037 + T039 + T040 + T041 + T042 + T044; PR bodies in `tmp/gh-codeql-alerts/cluster-logs/T{037,039,040,041,042,044}.md` |
| `false-positive` | 1 | 1 row closed by T066 (java/implicit-cast-in-compound-assignment in PSFeedServicePerformanceTest.java); inline suppression applied; row added to `suppressions.md` |
| `accepted-risk` | 0 | — |
| **Total** | **866** | **115 rows ready to close on PR merge** |

## 3. Counts by severity

| Severity | Count |
|----------|-------|
| `critical` | 13 (5 ready to close: T037 + T039 + T040; 4 reclassified from T038 to obsolete via T027) |
| `high` | 535 (4 ready to close: T041 + T042 + T044) |
| `medium` | 318 (104 ready to close: T027 + T029) |
| `low` | 0 |
| `note` | 0 |

## 4. Accepted risks

(none at this snapshot)

## 5. Pass / fail decision

`IN PROGRESS` — 866 open alerts remain; 40 are ready to close on PR merge.

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 — Setup (T001–T005) | DONE | branch confirmed; `gh auth status` green; JDK 21 active; CodeQL workflow trigger policy unchanged; `modules/p13n-api` added to `AGENTS.md` |
| Phase 2 — Foundational (T006–T011) | DONE | fetch script + stale-cache filter; `suppressions.md` and `accepted-risks.md` seeded; `removed-files.txt` inventory scaffolded; **5 umbrella tracking issues filed**: [#1189](https://github.com/intersoftdatalabs-in/percussioncms/issues/1189) (WebUI/), [#1190](https://github.com/intersoftdatalabs-in/percussioncms/issues/1190) (system/), [#1191](https://github.com/intersoftdatalabs-in/percussioncms/issues/1191) (projects/sitemanage/), [#1192](https://github.com/intersoftdatalabs-in/percussioncms/issues/1192) (modules/perc-packages/), [#1193](https://github.com/intersoftdatalabs-in/percussioncms/issues/1193) (modules/perc-common-ui-bundle/) |
| Phase 3 — US1 Triage (T012–T018) | DONE | all 5 verify scripts pass against the live 866-row inventory |
| Phase 4 — US2 Obsolete removals (T019–T034) | IN PROGRESS | **T027 + T029 staged** (483 files deleted: 477 dojo/, 3 trinidad/, 3 jstree/, 6 sitemanage HTML — 104 alerts ready; 4 Spotless/Prettier excludes removed from `pom.xml`); per-cluster logs in `tmp/gh-codeql-alerts/cluster-logs/T{027,029}*.md`; T021–T026, T028, T030–T031, T032–T034 pending |
| Phase 5 — US3 Valid mitigations (T035–T063) | IN PROGRESS | **T037 + T039 + T040 + T041 + T042 + T044 staged** (java/ssrf + java/xxe + java/ldap-injection + java/sql-injection + java/xss fixes with regression tests; java/zipslip fix was already in place — added regression test). Per-cluster logs in `tmp/gh-codeql-alerts/cluster-logs/T{037,039,040,041,042,044}.md`. T043 (path-injection 58 alerts) and T045–T063 pending |
| Phase 6 — US4 False-positive suppressions (T064–T072) | IN PROGRESS | **T066+T067 staged** (inline suppression applied in `PSFeedServicePerformanceTest.java:582`; matching row added to `suppressions.md`; `verify-suppressions.sh` passes); T068–T072 pending |
| Phase 7 — Polish (T073–T081) | NOT STARTED | `verify-pr-review-resolution.sh` written; per-PR gate PR template pending |

## 6. Verification

Re-check the values above against the live triage inventory and the
rebuilt distribution archive before declaring any disposition complete:

```bash
# All 5 verify scripts (POSIX sh, exit 0 on success)
scripts/verify-triage-inventory.sh        # 866 rows, all owners in AGENTS.md
scripts/test-verify-triage-inventory.sh   # exercises good + bad fixtures
scripts/verify-valid-fixes.sh             # every valid row has linked_pr
scripts/verify-suppressions.sh            # every suppression row is verifiable
scripts/verify-distribution-archive.sh    # rebuilt archive excludes removed files
scripts/verify-pr-review-resolution.sh    # gh pr view --json reviewThreads
scripts/filter-stale-alerts.sh \
    docs/ai-generated/tasks/gh-codeql-alerts/alerts.md \
    docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md
```

All 5 verify scripts are POSIX `sh` and exit 0 on success. They are
called out individually in the per-PR pre-merge gate per T078b.

## 7. Staged cluster PRs (work in the working tree, not yet committed)

| Cluster task | Files changed | Triage rows | Status |
|--------------|---------------|-------------|--------|
| **T027** — system/ ApplicationFiles dojo/trinidad/jstree | 483 files deleted + 4 pom.xml excludes cleaned | 71 (8 rules; 4 critical reclassified from T038 valid) | Staged, ready for `git commit` |
| **T029** — sitemanage obsolete test HTML | 6 files deleted | 33 (medium) | Staged, ready for `git commit` |
| **T037** — java/ssrf fix | 1 file modified + 1 test added | 2 (critical) in PSProxyQueryResource.java; the 4 broader java/ssrf alerts in extensions-main/system are tracked separately | Staged, regression test 7/7 pass, full module 26/26 pass |
| **T039** — java/xxe fix | 1 file modified + 1 helper added + 1 test added | 2 (critical) in PSSerializerUtils.java | Staged, regression test compiles; full system module suite requires CI run |
| **T040** — java/ldap-injection fix | 1 file modified + 1 helper added + 1 test added | 1 (critical) in PSJndiGroupProvider.java | Staged, regression test 10 cases compile; system module compiles |
| **T041** — java/zipslip regression test | 1 test added (fix was already in place) | 1 (high) in PSArchiveFiles.java | Staged, regression test 4 cases compile; system module compiles |
| **T042** — java/sql-injection fix | 1 file modified + 1 test added | 1 (high) in PSPageDaoHelper.java | Staged, regression test 8/8 pass; sitemanage module compiles and tests run |
| **T044** — java/xss fix | 1 file modified + 1 test added | 4 (high) in PSSiteDataRestService.java | Staged, regression test 16/16 pass; sitemanage module compiles |
| **T066** — false-positive suppression | 1 file modified + 1 row in `suppressions.md` | 1 (medium) | Staged, `verify-suppressions.sh` passes |

Per-cluster PR body templates are in `tmp/gh-codeql-alerts/cluster-logs/T{027,029,037,039,040,041}-pr-body.md`.
Per-PR pre-merge gate per T078b: capture `tmp/baselines/<module>-<commit>.log` before merge, resolve
every review thread via GraphQL `resolveReviewThread` after the PR opens, update
`triage.md` `linked_pr` column to the merged PR number.
