# Erlang review: issue #2368 PSExecutableSearch rawtypes residual batch

Date: 2026-08-07
Branch: fix/issue-2368-psexecutablesearch-rawtypes
Reviewer persona: Erlang (pre-commit gate)

## Scope

- Parameterize `PSExecutableSearch` constructors, search result children, row-data maps, iterators, category lists, and display-format table meta
- Typed `List<PSNode>` return from `executeSearch`; call-site typing in ActionManager adapters, SearchView, FolderActionManager, ItemRelationshipsManager
- Behavioral tests: category label comparator + `sortCategoryChildren` (order, recurse, skip non-category)

## Findings

### Bugs

None. No product behavior change intended; casts removed only where types are now proven. Parent `PSBaseExecutableSearch`/`PSDisplayFormat` raw boundaries use `Iterator<?>` / single unchecked cast for `getContentIdList()` / raw prop-set copy.

### Behavioral tests

- `PSExecutableSearchTest`: comparator order, top-level category sort, nested recurse, non-category skip
- Module suite green: Tests run: 20, Failures: 0

### Cross-platform paths

No new path/file I/O. Existing URL/document base handling unchanged.

### Change-class companions

- Consumer call sites updated for `List<Integer>` / `List<PSNode>` / `List<String>` constructors and return types
- `asNodeListIterator(List<PSNode>)` no longer needs rawtypes suppress
- Residual this-escape on ctor→init (6 diags) pre-existing pattern; not rawtypes; out of scope for #2368

## Gate

PASS for commit/PR (no bug findings; tests present; module clean install green).

> Co-Authored by Grok Build using grok-4.5 with agent main.

