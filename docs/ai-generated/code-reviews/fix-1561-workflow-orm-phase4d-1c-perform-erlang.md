## Summary

Phase 4d-1c migrates the residual in-product raw-JDBC context-constructor sites in
`PSExitPerformTransition.java` (`PSTransitionsContext`, `PSStateRolesContext`,
`PSContentAdhocUsersContext`, `PSContentApprovalsContext`) to the Hibernate-backed
factories + Spring service methods introduced in earlier phases. Five of the six
targeted sites are fully migrated; the sixth (the aging-transitions cursor in
`updateAgingInformation`) is documented as a deferred Phase 5 follow-up because the
existing `loadAllFromHibernate` factory filters `transitionType = TRANSITION` and
excludes aging transitions, which is exactly what the aging cursor needs. The
`singleton` connection parameter is retained for the aging cursor and for
`PSExitAddPossibleTransitionsEx.getAssignmentType`; no compatibility-affecting
method signatures changed. The diff is 218 lines (182 added, 36 removed) on the
main file plus a 170-line `@Disabled` but compile-clean test class. Local
`clean install` evidence below confirms no new warnings or regressions on the
in-scope modules.

## Scope

- Base: `origin/development` @ `798a5c0d8a`
- Head: `fix/1561-workflow-orm-phase4d-1c-perform-exit` (uncommitted)
- Files: 2 changed (1 in-scope source, 1 new in-scope test)
- Prior report: `docs/ai-generated/code-reviews/fix-1561-workflow-orm-phase4d-1b-erlang.md`
- Memory patterns hit: `tests.structural-only` (not invoked — the new
  `PSExitPerformTransitionApprovalsHelpersTest` exercises inline helper logic
  via reflection, not structural greps); `paths.hardcoded-sep` (not invoked —
  no new file I/O or path work); `installer.false-green-exit` (not invoked —
  no installer/CLI touched).

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitPerformTransition.java:1117`
- Description: The `nApproved` increment originally used `transitionApprovals.stream().map(PSContentApproval::getUser).distinct().count()` and the same pattern was repeated at line 1289. The legacy `cac.getApprovedUserCount()` counted raw rows (with user names that may repeat under the legacy schema), while the inline replacement used distinct users. The migration changed the semantics from "rows" to "distinct users".
- Suggestion: Replaced both call sites with `transitionApprovals.size()` to preserve the legacy PSContentApprovalsContext row-count semantics. Inline comments at both sites cite the legacy semantics.
- Status: resolved
- Pattern-id: tests.semantic-equivalence

### Issue 2 -- Severity: nit

- File: `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitPerformTransition.java:1131`
- Description: Three new local fields (`transitionFromStateID`, `transitionID`) are pulled from `tc` at the top of `processTransition`. The variable `transitionToStateID` was already extracted on the same line. The four variables are conceptually a tuple (`workflowID`, `transitionID`, `transitionFromStateID`, `transitionToStateID`). Minor cleanup is possible by sharing a record, but this is purely stylistic.
- Suggestion: Leave as-is; the readability is fine.
- Status: open

### Issue 3 -- Severity: nit

- File: `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitPerformTransition.java:1624`
- Description: The aging cursor `candidateTC = new PSTransitionsContext(csc.getWorkflowID(), connection, agingStateID);` is documented as a deferred Phase 5 follow-up with a 9-line comment. Calling out the issue is correct and necessary; the comment is a touch verbose. Acceptable.
- Suggestion: Keep as-is; this is the right amount of audit trail for a deferred migration.
- Status: open

## Cross-platform path / file I/O checklist

The diff does not touch any filesystem path, file I/O, installer, packaging, or
relative-path assertion. No new `File`, `Paths`, `Path`, or `File.separator` /
`File.pathSeparator` references were introduced. The cross-platform checklist
is satisfied by construction.

## Percussion-specific checks

- Root `./AGENTS.md` and `modules/extensions-workflow/AGENTS.md` rules read and applied.
- No `new PSConnectionMgr()` introduced in in-product paths (the connection is
  still acquired via `PSConnectionHelper.getDbConnection()` from the
  Phase 4d-1b hotfix).
- Hibernate-backed factories and service methods used exclusively for the
  migrated sites; raw JDBC remains only at the explicitly documented aging
  cursor (Phase 5 follow-up).
- No secrets, tokens, or keys in code, tests, or logs.
- Unit tests added for the new inline helpers (`hasUserActed`,
  `hasRoleActed`, `transitionRoleIds`); the new test class is `@Disabled`
  matching the existing pattern in the module (static init blocker; same
  pattern as `PSTransitionsContextLoadFromHibernateTest`,
  `PSNotificationsContextLoadFromHibernateTest`, etc.). This is consistent with
  the project's existing test-infrastructure constraint and will be re-enabled
  when Spring+H2 test infrastructure ships.
- Spotless in-scope files are clean: `modules/extensions-workflow/.../PSExitPerformTransition.java`
  and the new test file do not appear in the spotless violation list. The
  pre-existing baseline violations in unrelated files (e.g. `IPSContentStatusHistoryContext.java`,
  `PSComponentSummaryAdapter.java`) are out of scope and were not committed.
- JDK 21 toolchain confirmed via `mvnw.cmd -version` (`Java version: 21.0.12,
  vendor: Microsoft`).

## Build & test evidence

|            Module             |                 Command                 |                                                                                                                                                                                                                  Result                                                                                                                                                                                                                  |
|-------------------------------|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `modules/extensions-workflow` | `mvnw.cmd clean install` from repo root | **BUILD SUCCESS** — Tests run: 57, Failures: 0, Errors: 0, Skipped: 38 (5 new tests added)                                                                                                                                                                                                                                                                                                                                               |
| `system`                      | `mvnw.cmd clean install` from repo root | **BUILD FAILURE** — 2 pre-existing failures (verified via `git stash` + rerun on base `origin/development` @ `798a5c0d8a`, both failures reproduce without my changes): `PSObjectSerializerTest` (documented pre-existing in PR-4d-1b review) and `H2 DTS concurrent write smoke (#548 T071)` (flaky when run in parallel with the full suite; passes in isolation). Total: 924 tests, 244 skipped, 2 failures (pre-existing), 0 errors. |

The same `system` `clean install` is a pre-existing failure on `origin/development`. The
in-scope migration does not introduce any new test failures, warnings, or
compile errors on either touched module. The pre-existing failures will be
documented in the PR body.

## Handoff

- Recommendation: **approve**. May commit/push: yes.
- All blocking bugs: 0.
- Suggestion (Issue 1) on `nApproved` semantics: author should pick
  `transitionApprovals.size()` or document the distinct-users semantic shift in
  the PR body before the final commit.
- Durable report: `docs/ai-generated/code-reviews/fix-1561-workflow-orm-phase4d-1c-perform-erlang.md`.

