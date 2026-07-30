# Erlang pre-commit review — Phase 4d-1b hotfix #2

**Branch:** `fix/1561-workflow-orm-phase4d-1b`
**Commits under review:** uncommitted (hotfix #2 for PR #1589 review thread databaseId 3670378976)
**Reviewer:** Erlang (strict, independent)
**Date:** 2026-07-28
**Verdict:** **Approve** — gate green for commit / push.

## Scope

1. `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java` — replace
   the CONTENTID-only `columns` map built in `commit()` (no-arg overload, added in
   Phase 4d-1b) with a full 15-column map mirroring the legacy raw-JDBC
   `commit(Connection)`. Implementation: extracted a package-private
   `buildLegacyColumnMap(...)` helper (15 keys) plus a `formatDate(...)` static helper.
2. `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSLoadFromHibernateTest.java`
   — flip the class from `@Disabled` at class level to `@Disabled` per-method on the 8
   pre-existing Phase 4b Hibernate factory tests, and add an active test
   `commitColumnMap_populatesStateIdAndRevisions` that pins the produced map.
3. `system/src/test/java/com/percussion/services/system/PSSystemServicePhase4d1bWritesTest.java`
   — comment block pointing the reader at the new test location (no code change).
4. `modules/extensions-workflow/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
   — new file, contents `mock-maker-inline`. Enables `Mockito.mockStatic()`.

## Pre-PR build (HARD GATE)

|           Module           |                                                  Command                                                  |                                                            Result                                                             |
|----------------------------|-----------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `extensions-workflow`      | `cd modules\extensions-workflow & mvn clean install -Dtest=PSLoadFromHibernateTest -DfailIfNoTests=false` | BUILD SUCCESS — 9 tests, 1 active passing (`commitColumnMap_populatesStateIdAndRevisions`), 8 `@Disabled` skipped, 0 failures |
| `system`                   | `cd system & mvn clean install -Dtest=PSSystemServicePhase4d1bWritesTest -DfailIfNoTests=false`           | BUILD SUCCESS — 16/16 passing                                                                                                 |
| Full `extensions-workflow` | `cd modules\extensions-workflow & mvn clean install`                                                      | BUILD SUCCESS — 53 tests, 20 active, 33 `@Disabled`, 0 failures                                                               |
| Spotless                   | `mvn spotless:apply` on `system` and `extensions-workflow`                                                | BUILD SUCCESS on both                                                                                                         |

## Findings

### Blocking bugs

None. No correctness, security, data-loss, silent-failure, or non-portable-path bugs.

### Doc / convention fixes applied during this review

Two stale Javadoc / comment issues fixed in-line before commit (these are not
"blocking" findings per the Erlang gate, but the Erlang reviewer should call them out):

1. `PSContentStatusContext.java:452` — Javadoc continuation line lost its `*` prefix
   after a previous edit (read as `* <p>Phase 4d-1b hot-fix...` rather than ` * <p>...`).
   Fixed.
2. `PSContentStatusContext.java:449` and `:500` and `PSLoadFromHibernateTest.java:213`
   — three stale comments said "14 columns" when the legacy raw-JDBC `commit(Connection)`
   populates 15 keys (`CONTENTSTATEID`, `CONTENTCHECKOUTUSERNAME`, `CURRENTREVISION`,
   `EDITREVISION`, `TIPREVISION`, `REVISIONLOCK`, `LASTTRANSITIONDATE`, `STATEENTEREDDATE`,
   `NEXTAGINGTRANSITION`, `NEXTAGINGDATE`, `CONTENTSTARTDATE`, `CONTENTEXPIRYDATE`,
   `REMINDERDATE`, `REPEATEDAGINGTRANSSTARTDATE`, `CONTENTID`).
   Fixed — all three now say "15 columns".

### Behavioral tests

`PSLoadFromHibernateTest.commitColumnMap_populatesStateIdAndRevisions` is a real
behavioral test that pins:

- The exact value of each populated key (CONTENTSTATEID = "11", checkout user = "alice",
  revisions = "5"/"6"/"7", REVISIONLOCK = "Y", NEXTAGINGTRANSITION = "3", CONTENTID = "7").
- That every populated column from the legacy 15-column set is present.
- That the map size is exactly 15.

The test is wired with mocks so the deep `PSContentStatusContext.<clinit>` chain can
complete without a live DB / Spring context:

- `Files.createTempDirectory("percussion-test-rx-")` + `deleteOnExit` pins
  `rxdeploydir` so `PathUtils.getRxDir(null)` returns a non-null File
  (otherwise `PSWorkFlowUtils.encryptWorkflowProps` NPEs at line 1974 on
  `String.startsWith`).
- A writable `<rxRoot>/rxconfig/Workflow/rxworkflow.properties` (with `SMTP_PASSWORD=`
  so `PSEncryptProperties.encryptFile` does not NPE on `isEncrypted(null)` for the
  hard-coded `SMTP_PASSWORD` key in `PSWorkFlowUtils.encryptProps`).
- `Mockito.mockStatic(PSConnectionHelper.class, CALLS_REAL_METHODS)` so
  `PSConnectionHelper.getConnectionDetail(null)` returns a mock `PSConnectionDetail`
  with `getDatabase()="RX"` / `getOrigin()="PERCUSSION"`, satisfying
  `PSConnectionMgr.getQualifiedIdentifier("CONTENTSTATUS")` which is what
  `PSContentStatusContext.<clinit>` (line 893) calls.
- `@AfterAll` closes the `MockedStatic` to avoid the static-mock leaking into other
  tests in the JVM.

The 8 pre-existing Phase 4b Hibernate factory tests in the class are marked
`@Disabled` individually with the same reason string they had before, so they do
not run (they still need Spring+H2 infra). The pattern of disabling-old + activating-new
is reusable for the other 4 `@Disabled` Hibernate factory test classes in this module
(`PSContentTypesContextLoadFromHibernateTest`, `PSNotificationsContextLoadFromHibernateTest`,
`PSTransitionNotificationsContextLoadFromHibernateTest`,
`PSTransitionsContextLoadFromHibernateTest`) — out of scope for this PR.

### Non-portable path / file I/O

None. The new code uses `java.nio.file.Files.createTempDirectory` and
`Files.writeString` — portable across Windows, Linux, macOS. No `".../" +` or
`"...\\" +` filesystem path construction. The `rxdeploydir` system property is set
to `Path.toAbsolutePath().toString()` (uses the platform separator). The temp dir
path uses `java.io.tmpdir` which is platform-correct.

### Security

No secrets / credentials introduced. The test fixture `rxworkflow.properties` contains
only `TESTWITHOUTSERVER=false`, `PSCONSOLETRACEMESSAGES=false`, and an empty
`SMTP_PASSWORD=` line — no real passwords, no encrypted keys.

### Suggestions (non-blocking)

- `PSContentStatusContext.buildLegacyColumnMap` is package-private. If any future
  caller wants the same column set for diagnostics or instrumentation, the test would
  have to be moved alongside it. Not worth exposing publicly right now; leave as-is.
- The `setField(...)` / `newContext(...)` helpers in `PSLoadFromHibernateTest` use raw
  reflection against private fields. This is consistent with the other `@Disabled`
  Hibernate factory tests in the package and is the simplest way to wire a mockable
  instance without driving the full `PSConnectionHelper` / Hibernate factory path.
- `PSConnectionHelper.MOCKED_HELPER` is held in an `AtomicReference` so `@AfterAll`
  can close the `MockedStatic`. Acceptable for one test class but a static cleanup
  hook in a `Junit5` extension would be more reusable. Out of scope for this PR.

## Gate

**May commit/push: yes.**

- All blocking findings closed (none opened).
- New / changed non-trivial logic has a behavioral test that runs green.
- Cross-platform path / file I/O is portable.
- No security or data-loss footguns.
- Pre-PR Maven HARD GATE green: `mvn clean install` on `extensions-workflow` (53 tests)
  and `system` (16 tests) both pass with no new warnings on the modules I changed.
- `mvn spotless:apply` green on both modules.

Author may proceed with `git commit` and `git push` to update PR #1589.
