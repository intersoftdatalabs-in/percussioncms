# Erlang review — issue #2419 perc-toolkit Xlint residual

**Date:** 2026-08-09  
**Branch:** `fix/issue-2419-perc-toolkit-xlint-residual`  
**Module:** `modules/perc-toolkit`  
**Parent:** #2200 / slice of #2030  

## Change class

Main-source `-Xlint` residual cleanup (constructors / serial field / parent-API cascade).

## Summary of approach

| Category | Fix | Suppress? |
|----------|-----|-----------|
| `this-escape` on `Error` / `Field` | Direct field assignment + private `applyStringValue` | No |
| `this-escape` on `Http*Response` | Package-visible `headers` field assignment | No |
| `this-escape` on AA relationship builders | `super(boolean)` on `PSRelationshipBuilder` / intermediate; classes `final` | No |
| `this-escape` on `PSOExtensionParamsHelper` | `doLog` / `doParameters` made `private` | No |
| `this-escape` on `FolderTools` | `init` made `final` (matches `PSOFolderTools`) | No |
| `PSORequestContext` parent raw→generic cascade | Class `@SuppressWarnings({"unchecked","rawtypes"})` until perc-system types `PSRequestContext`; class `final` | Yes (documented) |
| `PSORequestContext(String)` this-escape | `@SuppressWarnings("this-escape")` on ctor (`setPrivateObject` overridable on parent) | Yes (documented) |
| `PropertyData` serial field | Field type `ArrayList<ValueData>` + defensive copy in `setValues` | No |

## Hard-gate checklist

- [x] No bugs in constructor semantics (tests cover Error/Field/Http*/AA orientation/PropertyData/params helper)
- [x] Cross-platform: no path I/O changes
- [x] Unit tests for behavioral ctor paths (new test classes)
- [x] Standalone `cd modules/perc-toolkit && ../../mvnw clean install` green
- [x] Main-source project `-Xlint` diagnostics **26 → 0**
- [x] No monorepo reformat / unrelated churn
- [x] Intentional suppressions documented in code + this review

## Verification

```
cd modules/perc-toolkit
../../mvnw.cmd clean install
# BUILD SUCCESS
# Tests run: 245, Failures: 0, Errors: 0, Skipped: 16
# Main-source [WARNING] *.java: count = 0
```

## Residual

- **Main-source:** none remaining for this module under project `-Xlint` (deprecation still excluded via parent `-Xlint:-deprecation`).
- **Test-source:** pre-existing rawtypes/unchecked in tests remain (out of #2419 main-source inventory). Optional follow-up under #2030 if full module zero is required.
- **perc-system:** typing `PSRequestContext` return types would allow removing the PSORequestContext class-level suppress.

## Verdict

**PASS** — ready to commit / open PR.
