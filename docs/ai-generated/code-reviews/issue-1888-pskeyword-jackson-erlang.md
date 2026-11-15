# Erlang review — issue #1888 PSKeyword Jackson migrate

**Branch:** `fix/issue-1888-pskeyword-jackson`  
**Date:** 2026-08-04  
**Scope:** `PSKeyword` / `PSKeywordChoice` domain Jackson wire + utils `IPSGuid` string serde  
**Recommendation:** **approve**  
**Gate:** May commit/push: **yes**

## Summary

Migrates content keyword design objects to the Jackson-backed `PSXmlSerializationHelper` (default since #1887) with package-shaped nested element `choice`, golden/round-trip tests, offline `Adhoc_Type.keyword` package smoke, and a shared `IPSGuid` string converter required for `<guid>` package XML (also unblocks pre-existing `PSTemplateSlotXmlRestoreTest` failures on main after #1887).

## Issues

None blocking.

### Observations (non-blocking)

- `PSKeyword.betwixt` retained for dual-engine rollback until #1824.
- Catalog interface default methods suppressed via `@JsonAutoDetect(getterVisibility=NONE)` + explicit `@JsonProperty` opt-in — correct companion pattern for other catalog domain types later.
- `setChoice` / `addChoice` `@JsonIgnore` avoids Jackson conflicting with collection item name `choice`.

## Cross-platform path checklist

- Tests load classpath resources via `ClassLoader.getResourceAsStream` (portable).
- Package fixture is a resource copy, not filesystem path string assertions.
- **Outcome:** clean.

## Memory patterns hit

- Prefer annotations/mix-ins on domain types; keep public `fromXML`/`toXML` call sites.
- Document approved XML deviations vs Betwixt.
- Shared converter in utils when domain batch proves IPSGuid binding needed.

## Verification

- `cd modules/utils && ../../mvnw clean install` — BUILD SUCCESS (`PSJacksonXmlSerializationHelperTest` 13 tests)
- `cd system && ../mvnw clean install` — BUILD SUCCESS (`PSKeywordXmlSerializationTest` 6 tests; suite green)
- Module Spotless apply then check on system; utils Spotless before commit
