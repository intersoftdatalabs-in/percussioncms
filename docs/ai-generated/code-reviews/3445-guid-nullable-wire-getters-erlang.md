# Erlang review — #3445 Guid plain nullable wire getters

**Date:** 2026-08-15
**Branch:** `fix/issue-3445-guid-nullable-wire-getters`
**Recommendation:** approve
**May commit/push:** yes
**Gate:** approve

## Summary

Last remaining `rest/src/main/java` public `Optional` wire getters: `Guid.getStringValue()`
and `Guid.getUntypedString()` now return plain nullable `String`. Field
`@JsonProperty("stringValue")` is kept; `getStringValue()` stays `@JsonIgnore` so
Object ACL still binds a JSON string (not an Optional bean). Production-mapper
tests assert no `empty`/`present` keys. rest/sitemanage `.orElse()` / `.isPresent()`
callers were updated.

`rest/AGENTS.md` was not changed (human rule-review gate).

## Scope

Uncommitted product work vs `origin/main` on `fix/issue-3445-guid-nullable-wire-getters`.

## Memory patterns hit

- Missing behavioral unit tests — **addressed** (`JacksonContextResolverOptionalTest`
  Guid methods + existing `GuidTest` / ACL bind tests)
- Incomplete change-class closure — **addressed** (DTO + rest callers + sitemanage
  apibridge + reverse-dep standalone install)
- Agent rule file without human review — **honored** (`rest/AGENTS.md` not committed)
- Cross-platform path checklist — N/A (no new filesystem I/O)

## Issues

None that block commit.

## Notes (not blocking)

- No remaining `public Optional<` under `rest/src/main/java`.
- Product-docs already document `guid.stringValue` as a scalar string.
