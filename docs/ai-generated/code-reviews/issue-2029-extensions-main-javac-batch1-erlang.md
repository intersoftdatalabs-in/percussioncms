# Erlang review — issue #2029 extensions-main javac batch 1

**Date:** 2026-08-08  
**Branch:** `fix/issue-2029-extensions-main-javac-batch1`  
**Reviewer persona:** Erlang (strict pre-commit)  
**Base:** `origin/main`

## Summary

Clears all project `-Xlint` javac diagnostics in `modules/extensions-main` (baseline inventory: 40 main-source warnings; post-fix: 0). Prefer real fixes over suppressions: static constant/method qualification, removal-deprecation constructors → `valueOf`, redundant cast removal, `@Deprecated` + `serialVersionUID` on compatibility shims, try-with-resources without redundant `close()`, remove `return` from `finally` (exception-swallowing bug), and harden `PSExtensionParamsHelper` (final class, instance logger, private helpers, null-safe `doLog`).

## Scope

- **Module:** `modules/extensions-main` only
- **Production files:** 20 Java sources under cas/general/publishing/usersearch/utils/validate/xmldom
- **Tests:** `PSExtensionParamsHelperTest`, `PSDateDifferenceTest` (new)
- **Memory patterns:** similar overnight javac batches (#2034 nav, #2035 sfp, #2036 workflow)
- **Cross-platform path review:** no path/file I/O changes; N/A clean

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs | none found |
| Behavioral tests for non-trivial logic | present (helper + date UDF) |
| Portable paths / I/O | N/A (no path changes) |
| Scope creep | none |
| New suppressions | none |
| Module clean install | BUILD SUCCESS, Tests run: 55, Failures: 0; 0 javac `warning:` lines |
| May commit/push | **yes** |

## Issues

_None at bug/suggestion severity._

### Notes (nit)

- `PSDatabasePublisher` finally-return removal is a correctness fix (exceptions no longer swallowed). Not unit-tested with a full request stack; acceptable for this warning cleanup given private method surface and high fixture cost.
- Legacy Percussion copyright headers left unchanged on pre-2023 files; new test files use Intersoft 2026 headers per AGENTS.md.
