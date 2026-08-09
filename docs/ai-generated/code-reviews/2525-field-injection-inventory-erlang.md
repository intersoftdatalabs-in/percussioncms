# Erlang review — issue #2525 field-injection inventory

**Issue:** [#2525](https://github.com/intersoftdatalabs-in/percussioncms/issues/2525)
**Branch:** `fix/2525-field-injection-inventory` (uncommitted at review time)
**Base:** `origin/main` (commit `4d7b64e7c`, PR #2555 / #2515 tip)
**Worktree:** `C:\workspaces\intersoft-workspace\percussioncms\.kilo\worktrees\issue-2525`
**Reviewer:** Erlang (pre-commit, Kilo sub-agent)
**Date:** 2026-08-08

## Summary

Slice is a documentation + reflection-test freeze, not a production code change. The diff adds a new
"folderHelper field / setter injection inventory (#2525)" section to the parent cycle inventory
that documents every observed field-injected dependency on the recycle-subgraph interfaces, finds
zero live reverse field edges on the construction subgraph, and freezes that state with a new
JUnit 5 reflection test. Build is green (`mvnw.cmd clean install` BUILD SUCCESS), 766 tests pass,
0 failures / 0 errors, no new warnings attributable to the slice, copyright header present, no
cross-platform path or file I/O code touched (pure Java reflection).

## Scope

- Base: `origin/main` (commit `4d7b64e7c`)
- Head: uncommitted on detached HEAD off `origin/main`
- Files changed: **2** (1 modified, 1 added)
  - `docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md`
    (modified; +95/-5 lines; new #2525 section, disposition table, residual item 10 closed, related
    line added)
  - `projects/sitemanage/src/test/java/com/percussion/share/dao/impl/PSFolderHelperFieldInjectionInventoryWiringTest.java`
    (new file; 260 lines; 5 JUnit 5 reflection tests)
- Prior reports loaded: `docs/ai-generated/code-reviews/2423-home-bookmarks-ui-erlang.md` (topic
  continuity; previous issue in the same epic; no direct conflict)
- Memory patterns hit: none (no new patterns promoted by this slice)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Non-findings scope (explicitly checked)

- **Cross-platform file I/O / paths** — N/A. Pure Java reflection; no `File`, `Path`, or
  `Filesystem` access in either file. Checklist applied; nothing to flag.
- **JDK 21 / Jakarta baseline** — JDK 21 used (`C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot`);
  no `javax.*` migration debt introduced. Test uses `jakarta.annotation.Resource` /
  `jakarta.inject.Inject` which are already in sitemanage test classpath via `pom.xml` lines
  178-183 (jakarta.inject-api) and 391-394 (jakarta.annotation-api).
- **Tests are behavioral** — Each reflection assertion inspects declared fields/methods of the
  cycle-subgraph classes and asserts that no `@Autowired` / `@Resource` / `@Inject` annotation
  appears. A change that reintroduces field injection will fail the test, not just pass through
  silently.
- **Test class coverage** — All seven cycle-subgraph beans (`PSFolderHelper`, `PSRecycleService`,
  `PSWidgetAssetRelationshipService`, `PSAssetDao`, `PSContentItemDao`, `PSPageIndexService`,
  `PSPageDaoHelper`) are scanned in a loop driven from a hard-coded list; the list is also
  sanity-asserted non-empty by two explicit tests so accidental fixture emptying is caught.
- **Copyright header** — New file uses `Copyright (c) 2026 Intersoft Data Labs, Inc.` +
  Apache 2.0 block (HARD GATE compliant).
- **Co-Authored footer** — Will be added at commit time per `.kilo/rules/co-author-attribution.md`.
- **Test results** — `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` in
  `PSFolderHelperFieldInjectionInventoryWiringTest`. Module suite: 766 tests, 0 failures,
  0 errors, 128 skipped (pre-existing skipped count; the slice does not change any `@Disabled` /
  `assumeTrue`).
- **No new warnings** — Build log shows pre-existing javadoc warnings on unrelated source
  (`PSWorkflowHelper`, `IPSMetadataService` — `IPSGenericDao` not imported) that are not
  attributable to the slice. No new `surefire` / `compiler` / `enforcer` warnings introduced.
- **No duplication of hub ctor work** — Slice does not touch ctor `@Lazy` work covered by
  #2476–#2478 / #2514–#2521; field-only scope respected.
- **Documentation-as-data** — Inventory table lists 45 downstream consumer classes with field
  `@Autowired` of a target interface, each marked "Not on folderHelper ctor path" with reasoning
  (consumer / hub / REST facade / importer / etc.). This is the "non-findings scope" of the
  diff and is what the issue asked for.

## Issues

### Issue 1 — Severity: suggestion (NOT blocking)
- File: `docs/ai-generated/tasks/2423-spring-injection-cycle/sitemanage-injection-cycle-inventory.md:6`
- Description: Header date reads "updated 2026-08-08" twice (once for #2485 / #2515 era, then
  again for #2525). Cosmetic; could be consolidated to "updated 2026-08-08 for #2485 / #2515 /
  #2525".
- Suggestion: Optionally fold into a single "updated 2026-08-08 for #2485 / #2515 / #2525"
  entry on next pass; not a blocker.
- Status: open (minor)
- Pattern-id: docs.formatting

### Issue 2 — Severity: suggestion (NOT blocking)
- File: `projects/sitemanage/src/test/java/com/percussion/share/dao/impl/PSFolderHelperFieldInjectionInventoryWiringTest.java:120`
- Description: `cycleSubgraphBeansKeepConstructorInjectionForCycleInterfaces` declares
  `throws Exception` but only calls APIs that throw `NoSuchMethodException` (already part of the
  `Constructor.getDeclaredConstructors()` contract). Could narrow to `NoSuchMethodException` to
  match the sibling tests' tighter signatures.
- Suggestion: Narrow the throws clause to `NoSuchMethodException` on next pass for stylistic
  consistency; not a blocker (compile-clean, behavior correct).
- Status: open (minor)
- Pattern-id: tests.throws-narrowing

## Re-review

No re-review triggered. All findings are suggestion-tier and do not block commit/push.