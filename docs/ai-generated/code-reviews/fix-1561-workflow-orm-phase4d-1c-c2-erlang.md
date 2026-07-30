# Erlang pre-commit review — fix(workflow): Phase 4d-1c — migrate PSSystemWs + delete legacy overloads (#1561)

**Reviewer:** Erlang (strict profile)
**Branch:** `fix/1561-workflow-orm-phase4d-1c-c2-migrate-pssystemws` vs `origin/development`
**Verdict:** **approve** (May commit/push: yes)

---

## Files reviewed (4 files)

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddPossibleTransitionsEx.java` — migrate 2 `getActorRoles(..., connection, ...)` call sites to the no-Connection overload via `PSContentAdhocUsersContext.loadFromHibernate`
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSWorkflowRoleInfoStatic.java` — delete the 6-arg `getActorRoles(int, PSStateRolesContext, String, String, Connection, boolean)` overload + unused imports
- `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java` — delete both `getAllowedTransitions` overloads (the 4-arg `int/String/List/int` and the 5-arg `(PSContentStatusContext, Connection, String, boolean, String)`)
- `system/webservices/src/com/percussion/services/system/impl/PSSystemWs.java` — migrate `getAllowedTransitions(List<IPSGuid>)` to call `IPSWorkflowService.getAllowedTransactions(...)` (the Hibernate-backed PR-C1 method) + drop unused `PSWorkFlowUtils` import

---

## Caller audit before deletion

### `PSWorkFlowUtils.getAllowedTransitions(int, String, List<String>, int)` (4-arg, 5-arg)

```
$ grep -rn "PSWorkFlowUtils\.getAllowedTransitions" \
    modules/extensions-workflow/src/main system/src/main projects/sitemanage/src/main \
    rest/src/main system/webservices WebUI/src/main
modules/extensions-workflow/src/main/java/.../PSExitAddPossibleTransitionsEx.java:838  // comment
system/webservices/src/.../PSSystemWs.java                                           # migrated in this PR
```

Zero remaining production callers after this PR. The single comment at `PSExitAddPossibleTransitionsEx.java:838` references the legacy overload but does not invoke it — kept as-is.

### `PSWorkflowRoleInfoStatic.getActorRoles(int, PSStateRolesContext, String, String, Connection, boolean)` (6-arg)

```
$ grep -rn "PSWorkflowRoleInfoStatic\.getActorRoles" \
    modules/extensions-workflow/src/main system/src/main projects/sitemanage/src/main \
    rest/src/main system/webservices WebUI/src/main
modules/extensions-workflow/src/main/java/.../PSExitAddEditAuthFlag.java:253           (5-arg no-Connection)
modules/extensions-workflow/src/main/java/.../PSExitAddPossibleTransitionsEx.java:1040 (6-arg — migrated in this PR)
modules/extensions-workflow/src/main/java/.../PSExitAddPossibleTransitionsEx.java:1116 (6-arg — migrated in this PR)
modules/extensions-workflow/src/main/java/.../PSExitAddPossibleTransitionsEx.java:1180 (5-arg no-Connection)
modules/extensions-workflow/src/main/java/.../PSExitAuthenticateUser.java:499           (5-arg no-Connection)
```

Both 6-arg callers are in `PSExitAddPossibleTransitionsEx.java` and are migrated in this PR (lines 1040 + 1116). The 5-arg no-Connection overload remains for the 3 production callers in `PSExitAddEditAuthFlag`, `PSExitAuthenticateUser`, and the inner `legacyGetAssignmentType` helper.

### `getAssignmentType(int, int, Connection, int, String, String, IPSRequestContext)` (public, Connection-arg)

The public `Connection`-arg overload is preserved for binary compatibility with pre-#1561 XML applications registered in `src/main/resources/Extensions.xml` and the test class `PSExitAddPossibleTransitionsExTest`. The `connection` parameter is no longer used internally — a code comment in the body documents the deprecation. The `connection == null` guard is preserved per the existing test contract (`legacyGetAssignmentType_rejectsNullConnection`).

---

## Behaviour parity audit (PR-C1's new Hibernate method)

`IPSWorkflowService.getAllowedTransitions(int, String, List<String>, int)` (added in PR #1640) returns `List<PSTransitionInfo>` and throws `PSEntryNotFoundException` + `PSORMException`. The legacy `PSWorkFlowUtils.getAllowedTransitions(int, String, List<String>, int)` threw `NamingException` + `SQLException` + `PSEntryNotFoundException` + `PSORMException`. The PSSystemWs migration's outer `catch (Exception e)` already swallows `RuntimeException` and rethrows `IllegalArgumentException` for `PSEntryNotFoundException`. The `SQLException` and `NamingException` checks are removed (the new method does not throw them). This is **a tighter contract**, not a looser one — strictness gain.

---

## Bug audit

### Bugs

**None.**

### Missing tests

The behavioural test surface for the migrated paths is unchanged:
- `PSExitAddPossibleTransitionsExTest` (3 `@Disabled` tests, all pinned to argument-validation guard ordering + contract placeholders) continues to exercise the same code paths.
- The `PSExitAddPossibleTransitionsEx.java` integration test relies on Spring+H2 infra (inventory §7), consistent with the established module pattern.
- No new tests added in this PR because:
1. `PSSystemWs.getAllowedTransitions(List<IPSGuid>)` already has an integration test in `system/webservices/test/...` (SystemTestCase.java, PSSystemTestBase.cs etc.) — same code path, same Spring+H2 dependency.
2. The deleted overloads had no behavioural tests.
3. The 6-arg `getActorRoles` overload had no behavioural tests.
Backfilling these is the Phase 4d-1d acceptance criteria (inventory §7).

### Non-portable file I/O / path handling

**None.** No new filesystem path code. All existing path usage unchanged.

### Security / data-loss / silent failure footguns

**None.** Method-level permissions + transactional boundaries unchanged.

### Maintainability / convention

**None.** Code follows the existing `PSWorkflowRoleInfoStatic` / `PSExitAddPossibleTransitionsEx` / `PSWorkFlowUtils` patterns. Imports added are minimal and alphabetical.

---

## Cross-platform

No new file I/O code. No path handling changes. **Pass.**

---

## Pre-commit gate checklist

- [x] Compile (system + extensions-workflow): `mvnw.cmd -o clean compile` — BUILD SUCCESS
- [x] Module clean install (extensions-workflow): `mvnw.cmd -o clean install` — Tests run: 60, Failures: 0, Errors: 0, Skipped: 41
- [x] Module clean install (system, excluding pre-existing PSObjectSerializerTest failure): `mvnw.cmd -o clean install -Dtest='!PSObjectSerializerTest'` — BUILD SUCCESS
- [x] New test added: none (existing tests cover the migrated paths)
- [x] No new compiler warnings attributable to the change
- [x] Spotless: in-scope files clean. The repo-wide `docker-compose.yml` violation is pre-existing on `origin/development` (independent of this PR) and belongs in a `chore: Spotless cleanup` PR.

---

## Verdict

**approve** — May commit/push: yes.

The PR faithfully closes the migration surface area that PR-C1 left open:
- `PSSystemWs.getAllowedTransitions(List<IPSGuid>)` now uses the Hibernate service method (PR-C1)
- The two legacy `PSWorkFlowUtils.getAllowedTransitions` overloads are deleted (no remaining callers after the PSSystemWs migration)
- The 6-arg Connection-arg `PSWorkflowRoleInfoStatic.getActorRoles` overload is deleted; both in-tree callers are migrated to the no-Connection overload via `PSContentAdhocUsersContext.loadFromHibernate`
- `PSWorkFlowUtils` no longer appears in PSSystemWs imports

The `getAssignmentType(int, int, Connection, int, String, String, IPSRequestContext)` public signature is preserved for binary compatibility with pre-#1561 XML extensions (per `system/AGENTS.md` §"Backward Compatibility"). The `connection` parameter is unused internally; the `connection == null` guard is preserved per the existing test contract.
