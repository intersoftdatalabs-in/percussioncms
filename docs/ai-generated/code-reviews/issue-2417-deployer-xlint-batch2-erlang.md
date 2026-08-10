# Erlang review — issue #2417 (perc-deployer Xlint batch 2)

**Reviewer persona:** independent (not implementer)  
**Date:** 2026-08-10  
**Module:** `deployer` (perc-deployer)  
**Verdict:** PASS (with residual this-escape / remaining rawtypes out of batch)

## Scope reviewed

Real-generics Xlint cleanup on preferred objectstore/server paths after batch 1 (#2418):

- `PSApplicationIDTypes` — full Map/List/Iterator parameterization; null-safe `copyFrom` / `hashCode` for choice filters
- `PSDeployableObject` — `List<String>` / `Iterator<String>` required classes; static `PSXmlTreeWalker.getElementData`; **fromXml** correctly reads `RequiredClasses` sibling (pre-existing gap exposed by new test)
- `PSDbmsHelper` — `PSEntrySet<String,String>`, `Iterator<String>`, schema/enumeration typing
- `PSImportCtx` — `Set<String>` on installed package deps (field already typed)
- Call-site cleanup in `PSDataObjectDependencyHandler`
- Unit tests: choice filters / copyFrom, required classes, filter-in, import-ctx installed deps

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new/changed logic | None found — removeMapping iterates a snapshot (avoids CME); choice-filter reset still only when complete + id type |
| Behavioral unit tests | Present and green (183 tests, 0 fail, 19 skip) |
| Cross-platform paths | N/A for production path I/O; no new path construction |
| Suppress-only Xlint | Avoided — real generics preferred |
| Change-class companions | Call sites for typed registration entries updated; module suite green |
| Over-scope | No monorepo reformat; no unrelated modules |

## Residual (not this PR)

- this-escape on Element/XML constructors across objectstore (incl. remaining 1 each on PSApplicationIDTypes / PSDeployableObject)
- Further rawtypes (e.g. `PSDependencyContext`, idtypes, handler surfaces)
- Test-source rawtypes / for-removal constructors

## Build evidence

```
cd deployer && ../mvnw.cmd clean install
BUILD SUCCESS
Tests run: 183, Failures: 0, Errors: 0, Skipped: 19
```

Target-file rawtypes/unchecked for the four primary classes: **cleared** (capped Maven report still 100 for other files that previously sat below the report ceiling).
