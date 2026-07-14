# Release Readiness — 8.2 Code-Scanning Remediation (in-progress snapshot)

**Branch**: `004-zero-code-scanning-alerts`
**Status**: IN PROGRESS — Java side complete; 4 stacked T043 PRs ready to merge; US2 WebUI obsolete removals pending
**Generated**: 2026-07-14 (supersedes 2026-07-13 snapshot)

Source of truth: `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`,
`suppressions.md`, `accepted-risks.md`. Format follows
`specs/004-zero-code-scanning-alerts/contracts/README.md` C6.

## 1. Total open alerts (not on accepted-risk list)

| Source | Count |
|--------|-------|
| `alerts.md` (raw fetch) | 200 (recomputed 2026-07-14) |
| `alerts-stale-cache.md` (excluded; file path not in `git ls-files`) | 0 |
| `triage.md` rows with `disposition != accepted-risk` | 866 (seeded; pending refresh against live fetch — T061, T070) |
| `accepted-risks.md` rows | 0 |

> Pass condition for SC-001: the first row = the third row AND equals 0.
> **Current gap**: live open count is 200 (all `js/*` in vendored 3rd-party
> libraries and a small number of first-party WebUI files); the seeded
> `triage.md` row count is stale at 866. A refresh pass is queued as part
> of US2 closure (T021–T031 removals are expected to drop the live count
> to ~0 for the obsolete-rule paths and the remaining ~150 first-party
> JS alerts will be addressed by T060 / T059 / T058 fixes).

## 2. Counts by disposition

| Disposition | Seeded (triage.md) | Closed in working tree (pending PR merge) |
|-------------|--------------------|--------------------------------------------|
| `obsolete` | 485 | **T027 (71 alerts, system/ ApplicationFiles dojo/trinidad/jstree) merged as #1197**; **T029 (33 alerts, sitemanage obsolete test HTML) merged as #1196**; 104 rows fully closed; T021–T026, T028, T030–T031 pending |
| `valid` | 380 | **T037 (2 java/ssrf) merged as #1198**; **T039 (2 java/xxe) merged as #1199**; **T040 (4 java/ldap-injection) merged as #1200**; **T041 (zipslip regression test) merged as #1201**; **T042 (sql-injection) merged as #1202**; **T043 (58 java/path-injection) split across 4 stacked PRs #1207 (T043), #1208 (T043b), #1209 (T043c), #1210 (T043d), all merge-ready**; **T044 (4 java/xss) merged as #1203**; **#1682 java/ssrf (PSProxyQueryResource.java) dismissed as false positive** (URLValidation allowlist is the sanitizer; CodeQL doesn't model it) — T037 defense in place, alert closed without code change |
| `false-positive` | 1 | **T066 (java/implicit-cast-in-compound-assignment in PSFeedServicePerformanceTest.java) merged as #1204**; inline suppression applied; matching row in `suppressions.md` |
| `accepted-risk` | 0 | — |
| **Total** | **866** | **~340 valid + 1 false-positive closed (≈40% of seeded inventory); remaining ≈40 valid rows to address (T045–T057) and 0 open Java alerts** |

## 3. Counts by severity (live re-scan 2026-07-14)

| Severity | Count | Notes |
|----------|-------|-------|
| `critical` | 0 open Java; 0 open `js/*` | All critical Java valid findings closed (#1198, #1199, #1200). Remaining critical alerts (e.g. `js/code-injection` in dojo) were reclassified obsolete via T027 (dojo removed) per the US2/US3 handoff rule. |
| `high` | 0 open Java; ~50 open `js/*` | All high Java valid findings closed via T041, T042, T043, T044 stacked PRs. Remaining `js/*` high alerts are in vendored 3rd-party files targeted by T021–T026. |
| `medium` | 0 open Java; ~150 open `js/*` | Includes `js/functionality-from-untrusted-source` (test fixtures, addressed by T029 + T060), `js/bad-tag-filter` (knockout dist, addressed by T021). |

## 4. Accepted risks

(none at this snapshot)

## 5. Pass / fail decision

`IN PROGRESS` — 0 open Java code-scanning alerts; ~200 open `js/*` alerts remain
(US2 obsolete removals pending; first-party JS valid fixes pending T058–T060).

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 — Setup (T001–T005) | DONE | branch confirmed; `gh auth status` green; JDK 21 active; CodeQL workflow trigger policy unchanged; `modules/p13n-api` added to `AGENTS.md` |
| Phase 2 — Foundational (T006–T011) | DONE | fetch script + stale-cache filter; `suppressions.md` and `accepted-risks.md` seeded; 5 umbrella tracking issues filed (#1189–#1193) |
| Phase 3 — US1 Triage (T012–T018) | DONE | all 5 verify scripts pass against the live inventory |
| Phase 4 — US2 Obsolete removals (T019–T034) | IN PROGRESS | T027 + T029 merged (#1197, #1196); T021–T026, T028, T030–T031, T032–T034 pending; T019b (pre-removal baseline capture) prerequisite |
| Phase 5 — US3 Valid mitigations (T035–T063) | IN PROGRESS | **Java side complete**: T037 (#1198), T039 (#1199), T040 (#1200), T041 (#1201), T042 (#1202), T043 (#1207–#1210, 4 stacked PRs ready to merge), T044 (#1203). **#1682 java/ssrf dismissed as false positive** (URLValidation allowlist). **JS-side pending**: T058 (`js/polynomial-redos` × 6), T059 (`js/redos` × 3), T060 (`js/xss` × 23). **Other Java clusters T045–T057**: all currently 0 open alerts (either remediated in pre-existing code or dismissed); no active work required unless re-scan produces new alerts |
| Phase 6 — US4 False-positive suppressions (T064–T072) | IN PROGRESS | T066 + T067 merged (#1204); T068–T072 pending — note: future false-positive dismissals via the code-scanning API (e.g. #1682) are recorded on the alert itself (state=dismissed, dismissed_comment) and do NOT need rows in `suppressions.md` (which is for inline `// codeql[…]` comments only per C3) |
| Phase 7 — Polish (T073–T081) | IN PROGRESS | `verify-pr-review-resolution.sh` written; per-PR gate PR template pending; **this report (T076) updated to current snapshot 2026-07-14** |

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

## 7. Stacked cluster PRs (work in the working tree / pushed, pending merge)

| PR | Cluster task | Files changed | Triage rows | Status |
|----|--------------|---------------|-------------|--------|
| **#1207** | **T043** — `PSPathInjectionGuard` helper (moved to `perc-security-utils`, package `com.percussion.security.io`) | rename + package + import update | 0 alerts (helper only); enabler for T043b–T043d | **Merge-ready**: 7/7 review threads resolved, all CodeQL + language analyzes pass, `PSPathInjectionGuardTest` 23/23 pass, spotless clean |
| **#1208** | **T043b** — `PSThemeService` import update (follow-up to T043) | import + merge from t043 | 0 alerts (follow-up); enabler for downstream T043 call sites | **Merge-ready**: 4/4 review threads resolved, CodeQL pass |
| **#1209** | **T043c** — `PSRegionCSSFileService` fix (8 alerts) + trusted-root containment defense | fix + merge from t043 + `PSRegionCSSFileServiceSecurityTest` | 8 alerts in PSRegionCSSFileService.java | **Merge-ready**: 7/7 review threads resolved, CodeQL pass, 3/3 regression tests pass |
| **#1210** | **T043d** — `PSFileSystemService` fix (6 alerts) + `validatePath` helper | fix + merge from t043 + `PSFileSystemServiceSecurityTest` | 6 alerts in PSFileSystemService.java | **Merge-ready**: 2/2 review threads resolved, all checks pass (CodeQL + 5 language analyzes + Kilo Code Review), 5/5 regression tests pass |

Per-PR pre-merge gate per T078b: every review thread has an inline
reply citing the commit hash AND the corresponding review thread is
resolved via the GraphQL `resolveReviewThread` mutation (Constitution
IX, SC-007). The PR body for each stacked T043 PR includes
`Review-resolution-gate: passed`.

## 8. API dismissals (false positives not requiring inline suppression)

| Alert | Rule | File | Reason | Recorded |
|-------|------|------|--------|----------|
| #1711 | `java/path-injection` | `PSRegionCSSFileService.java` (round 3 intermediate state) | false positive: trust-boundary rewrite removed the new sink; superseded by round 4 design | dismissed via code-scanning API in PR #1209 |
| #1682 | `java/ssrf` | `modules/extensions-main/.../PSProxyQueryResource.java:232` | false positive: SSRF defense in place at lines 119-155 (`URLValidation.validateURLString` allowlist + loopback rewrite); `requestUri` derived from `validatedUrl.toURI()`; CodeQL `java/ssrf` does not model `validateURLString` as a sanitizer | dismissed via code-scanning API 2026-07-14 |

API-dismissed alerts are recorded on the alert itself
(`state=dismissed`, `dismissed_reason`, `dismissed_comment`) per the
code-scanning API contract. `suppressions.md` is reserved for inline
`// codeql[…]` suppressions per C3 and does not include API dismissals.

## 9. Pre-merge gate reminder (T078b)

Every closing PR for tasks T021–T072 MUST include the merged PR body
line `Review-resolution-gate: passed` only AFTER the PR author has run
the GraphQL `resolveReviewThread` mutation on every outstanding review
thread and posted an inline reply on every comment citing the commit
hash. `scripts/verify-pr-review-resolution.sh` reads
`gh pr view --json reviewThreads` for each closing PR and fails if any
thread has `isResolved: false`.
