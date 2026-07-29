# Erlang review — PR #1459 fix pack (`fix/922-date-format-patterns`)

**Reviewer:** Erlang (independent pre-merge reviewer; not the author)
**Branch:** `fix/922-date-format-patterns` (worktree
`987-jcr-2-0-api-migration`)
**Base:** `development` HEAD `5a4a4cd3a1`
**Head:** uncommitted fix pack addressing four PR review threads

**Files:** `deployer/pom.xml`,
`deployer/src/main/java/com/percussion/deployer/server/PSLogHandler.java`,
`deployer/src/test/java/com/percussion/deployer/server/PSLogHandlerTest.java`

**Recommendation:** `approve` — may commit/push: yes.

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Verification of the four PR review threads

|                 Thread                  |                                                    Concern                                                    |  Status   |                                                                                                                                                                                                                   Evidence                                                                                                                                                                                                                    |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PRRT_kwDOKZBp3M6SusWD` (db 3625805015) | Constructor `try/catch(Exception)` nullifies `m_dbmsHandle` + four schemas; breaks checked-exception contract | **fixed** | `PSLogHandler.java:66-84` — catch removed; only `throws PSDeployException` declared. Grep for `m_dbmsHandle\s*=\s*null` returns zero matches — no dead null-set residue. Javadoc `@throws PSDeployException If fail to get any of the table schemas` now honored.                                                                                                                                                                             |
| `PRRT_kwDOKZBp3M6SusWK` (db 3625805023) | Hardcoded `"localhost:9992"` fallback silently writes bogus `TGT_SERVER_NAME`                                 | **fixed** | `PSLogHandler.java:797` (private path) and `:750` (public wrapper) call `PSServer.getHostName() + ":" + PSServer.getListenerPort()` with **no try/catch**. Both are simple static getters on `ms_hostName` / `ms_listenerPort`; they do not throw checked exceptions so the catch was both a data-corruption hazard and a code smell. Misconfiguration now propagates loudly.                                                                 |
| `PRRT_kwDOKZBp3M6SusWO` (db 3625805029) | Hardcoded `mockito-core 5.12.0` instead of `${mockito.version}` (5.21.0)                                      | **fixed** | `deployer/pom.xml:121-125` — `<version>` element removed; dependency inherits from parent `dependencyManagement` (`mockito.version` = 5.21.0). Build succeeds with 5.21.0.                                                                                                                                                                                                                                                                    |
| `PRRT_kwDOKZBp3M6SusWT` (db 3625805034) | Test only stubs `getServerBuildDate()`; other methods return null → `PSXmlDocumentBuilder.toString(null)` NPE | **fixed** | `PSLogHandlerTest.java:43-117` — both tests stub `getArchiveRef`, `getUserName`, `getServerName`, `getServerVersion`, `getServerBuildId`, `getServerBuildDate`. `archiveManifest.toXml(Document)` is answered with a real DOM Element via `PSXmlDocumentBuilder.createRoot(...)`. Two behavioral tests assert every column by name + value (incl. `TGT_SERVER_NAME=rhythmx-target:9992` and `ALS_SRC_SERVER_BUILD_DATE=2026-07-21 15:30:45`). |

## Cross-platform / path review

No path or filesystem I/O in the diff. `host + ":" + port` is the
URL-style server identity used across the codebase (`PSExportJob`,
`PSCatalogHandler`, `PSLogError`, `PSInlineLinkField`, etc.) — platform
agnostic. **Clean.**

## Findings (non-blocking)

### Issue 1 — suggestion (pre-existing)

- File: `PSLogHandler.java:730-748`
- Description: Two stacked `/** */` Javadoc blocks above
  `createArchiveLogSummaryTableData`. The legacy block (lines 730-738)
  still says "Inserts the given parameters into the archive summary and
  archive package tables" and lists `@throws PSDeployException`, but the
  method body has been replaced by a delegate to
  `buildArchiveLogSummaryTableData`. The legacy block should be deleted
  in a follow-up; the surviving "Public wrapper for..." block (lines
  739-748) is correct. **Pre-existing drift**, not a regression.

### Issue 2 — nit (resolved post-review)

- File: `PSLogHandlerTest.java`
- Description: Local helper `private static <T> T any(Class<T> ignored)`
  existed solely to forward to `org.mockito.ArgumentMatchers.any(...)`.
- Fix applied: replaced with `import static org.mockito.ArgumentMatchers.any;`.

## Edge cases checked

- Caller of `createArchiveLogSummaryTableData`: only the test, plus the
  declaration. No external module depends on the public wrapper.
- Caller of `getTableDataForSaveArchiveSummary`: only `createArchiveLog`
  at line 774. Safe.
- Thread safety of new static `buildArchiveLogSummaryTableData`: only
  locals + `FastDateFormat.getInstance(...)` (commons-lang3, thread
  safe). No shared mutable state. No concern.
- `PSServer.getHostName()` / `getListenerPort()` NPE risk: cached
  `ms_hostName` / `ms_listenerPort` set during server init; never
  return `null` / 0 after init. A genuinely uninitialized Rx would
  return sentinel `"localhost"` / `0` — same behavior as every other
  call site. The point of this fix is that we **no longer mask that
  with a hardcoded bogus string**.
- `@throws` on `createArchiveLogSummaryTableData`: not declared, and
  correct — no checked exception is thrown now.
- Spotless: touched files clean. Pre-existing violations in
  `PSServletRequesterTest*.java` are unrelated drift and out of scope.

## Verification

- `cd deployer && ../mvnw -o clean install`: BUILD SUCCESS,
  157 / 0 / 0 / 19 skipped.
- `cd deployer && ../mvnw -o -Dtest=PSLogHandlerTest test`:
  2 passed.

## Handoff

- All four prior review threads (`SusWD` / `SusWK` / `SusWO` / `SusWT`)
  are addressed at the code level with verifiable, behavioral evidence.
- No blocking bugs. Recommendation: **approve** — may commit/push.
- **PR-thread protocol reminder** (per `AGENTS.md`): the merge gate also
  requires inline mitigation replies on each of the four review threads
  citing the commit hash, **and** `resolveReviewThread` GraphQL mutations.
  Code-only fixes are not merge-ready on GitHub.

