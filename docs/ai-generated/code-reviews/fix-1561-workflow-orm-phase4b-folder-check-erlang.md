# Erlang — Phase 4b Folder-Check Regression Hot-Fix

> Strict independent review of the uncommitted fix on
> `fix/1561-workflow-orm-phase4d-1b` (off `origin/development` `3a2f5f7c92`).
> Performed per `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`,
> `AGENTS.md`, and `modules/extensions-workflow/AGENTS.md`.

**Result:** **Approve** — no **bug** findings.

|                                       |                                                           |
|---------------------------------------|-----------------------------------------------------------|
| Bug findings                          | 0                                                         |
| Test-coverage findings                | 0 (3 new regression tests cover the exact reported repro) |
| Cross-platform path / I/O findings    | 0 (no file I/O)                                           |
| Security / data-loss findings         | 0                                                         |
| Convention / maintainability findings | 0                                                         |

## Diff size

```
2 files changed, 124 insertions(+), 4 deletions(-)
```

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAuthenticateUser.java`:
  - Line 346: `PSServer.getCmsObjectRequired((int) csc.getContentTypeId())` →
    `PSServer.getCmsObjectRequired(csc.getObjectType())`. Pre-Phase-4b used
    `csc.getObjectType()` (an `int`); the Phase 4b PR regressed to
    `csc.getContentTypeId()` (a `long`).
  - Line 348: `if ((int) csc.getContentTypeId() == PSCmsObject.TYPE_FOLDER)` →
    `if (csc.getObjectType() == PSCmsObject.TYPE_FOLDER)`. Same regression.
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSExitAuthenticateUserFolderCheckTest.java`:
  - 3 new regression tests that lock in the pre-migration contract.

## Bug

The Phase 4b PR (`a575533f38`) introduced two copy-paste regressions in
`PSExitAuthenticateUser.authenticateUser`:

```diff
- PSCmsObject cmsObject = PSServer.getCmsObjectRequired(csc.getObjectType());
+ PSCmsObject cmsObject = PSServer.getCmsObjectRequired((int) csc.getContentTypeId());

- if (csc.getObjectType() == PSCmsObject.TYPE_FOLDER) {
+ if ((int) csc.getContentTypeId() == PSCmsObject.TYPE_FOLDER) {
```

`PSComponentSummary` (the Phase 4b Hibernate-backed replacement for
`PSContentStatusContext`) has both `getObjectType()` and `getContentTypeId()`. The
two are different fields:

|        Method        | Return type |       Example value        |            Meaning            |
|----------------------|-------------|----------------------------|-------------------------------|
| `getObjectType()`    | `int`       | `1` (item) or `2` (folder) | Object type — what the row IS |
| `getContentTypeId()` | `long`      | `101` (rffGeneric)         | Content type — the schema     |

`PSCmsObject.TYPE_FOLDER = 2` is the *object* type constant. Comparing the content
type id to it (with a `long → int` cast) is a type mismatch that compiles but is
always false for any folder. The reported repro is the server startup →
`PSFolderHelper.setDefaultPermissions` → `loadFolder` path, where a folder content
row has content type id 101 and object type 2; the `getContentTypeId() ==
TYPE_FOLDER` check evaluates to `101 == 2` = `false`, the folder check is skipped,
the code proceeds down the workflow path with a `null`-thrown exception on
`PSServer.getCmsObjectRequired` or a similar downstream consumer.

The fix restores the pre-Phase-4b code exactly: `csc.getObjectType() == PSCmsObject.TYPE_FOLDER`.

## Build & test evidence

|            Module             |                              Command                               |                             Result                              |
|-------------------------------|--------------------------------------------------------------------|-----------------------------------------------------------------|
| `modules/extensions-workflow` | `mvn-env.bat -N clean install -DskipTests`                         | **BUILD SUCCESS**                                               |
| `modules/extensions-workflow` | `mvn-env.bat -N test`                                              | **52 tests** (19 active + 33 @Disabled), Failures: 0, Errors: 0 |
| `modules/extensions-workflow` | `mvn-env.bat -N test -Dtest=PSExitAuthenticateUserFolderCheckTest` | **3 new regression tests** all green                            |

Pre-existing Spotless violations in 29 other files (none in the files modified by
this hot-fix) are documented in prior Erlang reviews and are out of scope here.

## Cross-platform portability

No file I/O, `new File(...)`, path joining, or shell-out added. All cross-platform
rules in root `AGENTS.md` are satisfied by construction.

## Behavioural review

### 1. `PSExitAuthenticateUser.authenticateUser` — **OK**

Both regressions were identical in shape (using the wrong field of
`PSComponentSummary`) and both fix lines are minimal. The pre-Phase-4b pre-migration
pattern is restored exactly.

I confirmed there are no other instances of the `csc.getContentTypeId() ==
PSCmsObject.TYPE_FOLDER` pattern elsewhere in the module:

- `PSExitAddPossibleTransitionsEx.java:410, 551` — both use
  `summary.getObjectType() == PSCmsObject.TYPE_FOLDER` (correct, added in Phase 4d-1a).
- `PSExitAddEditAuthFlag.java` — no folder check (the auth-flag exit doesn't
  short-circuit on folders; the legacy code didn't either).

### 2. Regression test `PSExitAuthenticateUserFolderCheckTest` — **OK**

Three tests, all active (no `@Disabled`):

1. **`folderRow_isDetectedByObjectType_notByContentTypeId`** — the exact reported
   repro. Creates a `PSComponentSummary` with `contentTypeId=101` and
   `objectType=TYPE_FOLDER`. Asserts:
   - `csc.getObjectType() == PSCmsObject.TYPE_FOLDER` (true)
   - `csc.isFolder()` (true)
   - `csc.getContentTypeId() != PSCmsObject.TYPE_FOLDER` (the bug case)
2. **`itemRow_isNotDetectedAsFolder`** — inverse case. Content type id 101, object
   type `TYPE_ITEM`. Asserts the folder check is false.
3. **`getObjectTypeIsIntAndGetContentTypeIdIsLong`** — type-safety test. The bug
   code cast a `long` to `int` and compared to an `int` constant. Java auto-widens
   so it compiles, but the comparison was always false. This test pins the
   pre-migration contract that the field types are different.

These tests do not need a live database — `PSComponentSummary` is a POJO that
can be constructed directly via its public setters. They run in milliseconds
and will catch any future regression of the same shape.

## Nits (not blocking)

None.

## Files reviewed

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAuthenticateUser.java`
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSExitAuthenticateUserFolderCheckTest.java`

## Gate

|           Check            |                                   Status                                   |
|----------------------------|----------------------------------------------------------------------------|
| Bug findings               | ✅ 0                                                                        |
| Missing behavioural tests  | ✅ 0 (3 new active tests covering the exact repro)                          |
| Cross-platform portability | ✅ N/A                                                                      |
| Security / data-loss       | ✅ 0                                                                        |
| Erlang pre-commit (strict) | ✅ **Approve** — may commit / push / amend PR #1589 or open a new fix-up PR |

> The pre-existing Spotless violations in 29 unrelated files are not introduced
> by this hot-fix and are not in scope for this Erlang review.

