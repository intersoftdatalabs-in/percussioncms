# Erlang review — issue #2421 sitemanage Xlint batch 2

**Reviewer persona:** independent of implementer  
**Date:** 2026-08-10  
**Scope:** residual `serialVersionUID` on main-source DTOs / nested exceptions after batch 1 (#2032 / PR #2422)

## Change class

Mechanical serial lint cleanup on Serializable DTOs and exception types. No product behavior, REST contracts, paths, or Spring wiring changes.

## Checklist

| Gate | Result |
|------|--------|
| Bugs / logic regressions | **Pass** — only `private static final long serialVersionUID = 1L` fields; `@SuppressWarnings("serial")` removed where replaced with real UID (`PSCreateRedirectRequest`) |
| Behavioral unit tests | **Pass** — `PSSerializableListWrappersTest#residualDtoAndExceptionTypesDefineSerialVersionUid` asserts UID fields exist for batch-2 types |
| Cross-platform paths | **N/A** — no path/I/O changes |
| Change-class companions | **Pass** — list-wrapper pattern from batch 1 extended; no new beans / REST surfaces |
| API shape / `final` / signature changes | **N/A** — no public API signature changes; C2 not required |
| Product docs | **N/A** — non-product-facing tech debt |

## Findings

None (no bugs). Residual main-source Xlint remains dominated by **serial-field**, **this-escape**, and **unchecked** — tracked as residual child under #2032 / #2200.

## Build evidence

```text
cd projects/sitemanage && ../../mvnw.cmd clean install
BUILD SUCCESS
Tests run: 968, Failures: 0, Errors: 0, Skipped: 127
```

Main-source `serialVersionUID` warnings in capped Maven log: **23+ → 0** (additional DTO UIDs also added beyond the first-100 cap).
