# Release Readiness — 8.2 Code-Scanning Remediation (in-progress snapshot)

**Branch**: `004-zero-code-scanning-alerts`
**Status**: IN PROGRESS — Java side partial; large JS-cluster remediation deferred to US2 obsolescence removals
**Generated**: 2026-07-14 (supersedes 2026-07-13 snapshot)

Source of truth: `docs/ai-generated/tasks/gh-codeql-alerts/triage.md`,
`suppressions.md`, `accepted-risks.md`. Format follows
`specs/004-zero-code-scanning-alerts/contracts/README.md` C6.

## 1. Total open alerts (not on accepted-risk list)

| Source | Count |
|--------|-------|
| `alerts.md` (raw fetch) | 759 (recomputed 2026-07-14) |
| `alerts-stale-cache.md` (excluded; file path not in `git ls-files`) | 0 |
| `triage.md` rows with `disposition != accepted-risk` | 759 |
| `accepted-risks.md` rows | 0 |

> Pass condition for SC-001: the first row = the third row AND equals 0.
> **Current gap**: 759 open alerts (165 valid Java + 593 obsolete-vendored js/* + 1 false-positive).
> US2 obsolete removals (T021–T031) are expected to drop the JS side to ~150 first-party JS,
> and US3 valid fixes (T037–T057) target the remaining 165 Java alerts.

## 2. Counts by disposition (live re-scan 2026-07-14)

| Disposition | Count | Closed in working tree (pending PR merge) |
|-------------|-------|--------------------------------------------|
| `obsolete` | 593 | US2 clusters pending: T021–T026 (WebUI), T028 (dojo non-AppFiles), T030–T031 (DTS/InstallDir) |
| `valid` | 165 | Already merged via #1198 (T037 java/ssrf × 6 — but 5 still open), #1199 (T039 java/xxe × 2 — 1 still open), #1200 (T040 java/ldap-injection × 4 — 1 still open), #1201 (T041 zipslip test), #1202 (T042 sql-injection — 6 still open), #1207-#1210 (T043 path-injection — 51 still open), #1203 (T044 java/xss — 31 still open). Phase 5 follow-ups required for residual alerts. |
| `false-positive` | 1 | T066 + #1204 merged (java/implicit-cast-in-compound-assignment in PSFeedServicePerformanceTest); 2 more implicit-cast alerts remain in HTTPClient/ (likely accepted-risk for legacy HTTPClient library) |
| `accepted-risk` | 0 | PSAesCBC.java (3 weak-crypto + 2 static-IV) queued for accepted-risk to 9.0 |
| **Total** | **759** | |

## 3. Counts by severity (live re-scan 2026-07-14)

| Severity | Count | Notes |
|----------|-------|-------|
| `critical` | 7 open | java/ssrf (5, defense-in-depth follow-ups), java/xxe (1), java/ldap-injection (1). js/code-injection in dojo resolved via US2 T027. |
| `high` | 461 open | Heavy concentration in js/xss-through-dom (164), js/incomplete-sanitization (107), js/unsafe-jquery-plugin (84), js/html-constructed-from-input (96) — all in vendored 3rd-party JS slated for US2. Java high: java/path-injection (58), java/xss (35), java/regex-injection (6), java/sql-injection (6), etc. |
| `medium` | 291 open | Includes js/incomplete-multi-character-sanitization (27), js/functionality-from-untrusted-source (24), java/error-message-exposure (22), js/xss (21), etc. |

## 4. Accepted risks

| Alert | Rule | File | Reason | target_milestone | Recorded |
|-------|------|------|--------|------------------|----------|
| (queued) | java/weak-cryptographic-algorithm (3) | `modules/perc-legacy/.../PSAesCBC.java` | legacy deprecated crypto; upgrade requires API-breaking change to PSAesCBC API | 9.0 | T047/T048 follow-up |
| (queued) | java/static-initialization-vector (2) | `modules/perc-legacy/.../PSAesCBC.java` | same root cause as weak-crypto | 9.0 | T047/T048 follow-up |

## 5. Pass / fail decision

`IN PROGRESS` — 759 open alerts; substantial Phase 5 work remaining.

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 — Setup (T001–T005) | DONE | branch confirmed; `gh auth status` green; JDK 21 active; CodeQL workflow trigger policy unchanged; `modules/p13n-api` added to `AGENTS.md` |
| Phase 2 — Foundational (T006–T011) | DONE | fetch script + stale-cache filter; `suppressions.md` and `accepted-risks.md` seeded; 5 umbrella tracking issues filed (#1189–#1193) |
| Phase 3 — US1 Triage (T012–T018) | DONE | all 5 verify scripts pass; release-readiness report live |
| Phase 4 — US2 Obsolete removals (T019–T034) | PARTIAL | T027 + T029 merged (#1197, #1196); T021–T026, T028, T030–T031, T032–T034 pending |
| Phase 5 — US3 Valid mitigations (T035–T063) | PARTIAL | T037, T039, T040, T041, T042, T043 (4 stacked), T044 merged; **5 java/ssrf + 1 java/xxe + 1 java/ldap-injection + 51 java/path-injection + 31 java/xss + 6 java/regex-injection + 6 java/sql-injection + 3 java/zipslip + 3 java/weak-crypto + 2 java/insecure-trustmanager + 6 java/unvalidated-url-redirection + 1 java/unvalidated-url-forward + 1 java/unsafe-hostname-verification + 22 java/error-message-exposure + 2 java/stack-trace-exposure + 2 java/implicit-cast-in-compound-assignment + 2 java/static-iv + 1 java/insecure-cookie + 3 java/redos/polynomial-redos + 4 js/code-injection + 6 js/polynomial-redos + 2 js/redos + 21 js/xss still open**. This snapshot is being driven by AI session(s); subsequent commits update triage.md and add closing PRs per rule cluster. |
| Phase 6 — US4 False-positive suppressions (T064–T072) | PARTIAL | T066 + #1204 merged; 2 implicit-cast alerts in HTTPClient/ pending T068 |
| Phase 7 — Polish (T073–T081) | IN PROGRESS | `verify-pr-review-resolution.sh` written; per-PR gate PR template pending; **this report (T076) updated to live snapshot 2026-07-14** |

## 6. Verification

Re-check the values above against the live triage inventory and the
rebuilt distribution archive before declaring any disposition complete:

```bash
# All verify scripts (POSIX sh, exit 0 on success)
scripts/verify-triage-inventory.sh        # 759 rows, all owners in AGENTS.md
scripts/test-verify-triage-inventory.sh   # exercises good + bad fixtures
scripts/verify-valid-fixes.sh             # every valid row has linked_pr
scripts/verify-suppressions.sh            # every suppression row is verifiable
scripts/verify-distribution-archive.sh    # rebuilt archive excludes removed files
scripts/verify-pr-review-resolution.sh    # gh pr view --json reviewThreads
scripts/filter-stale-alerts.sh \
    docs/ai-generated/tasks/gh-codeql-alerts/alerts.md \
    docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md
```

## 7. Stacked cluster PRs (work in the working tree / pushed, pending merge)

| PR | Cluster task | Files changed | Triage rows | Status |
|----|--------------|---------------|-------------|--------|
| **#1207** | **T043** — `PSPathInjectionGuard` helper (moved to `perc-security-utils`, package `com.percussion.security.io`) | rename + package + import update | 0 alerts (helper only); enabler for T043b–T043d | **MERGED** |
| **#1208** | **T043b** — `PSThemeService` import update (follow-up to T043) | import + merge from t043 | 0 alerts (follow-up); enabler for downstream T043 call sites | **MERGED** |
| **#1209** | **T043c** — `PSRegionCSSFileService` fix (8 alerts) + trusted-root containment defense | fix + merge from t043 + `PSRegionCSSFileServiceSecurityTest` | 8 alerts in PSRegionCSSFileService.java | **MERGED** |
| **#1210** | **T043d** — `PSFileSystemService` fix (6 alerts) + `validatePath` helper | fix + merge from t043 + `PSFileSystemServiceSecurityTest` | 6 alerts in PSFileSystemService.java | **MERGED** |

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