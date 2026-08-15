# Erlang review — #3388 REST DTO wire getters must not return Optional

**Date:** 2026-08-15  
**Branch:** `fix/issue-3388-rest-dto-wire-getters`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve

## Summary

First incremental slice of #3388: convert REST `User`, `Role`, and `ObjectSummary`
wire getters from `Optional<T>` to plain nullable types with `@JsonInclude(NON_NULL)`.
Callers that used `.orElse()` / `.ifPresent()` on those getters were updated in
`rest` and `projects/sitemanage`. Production-mapper round-trips assert no
`empty`/`present` Optional-bean keys.

`rest/AGENTS.md` wire-getter convention is drafted in the working tree and is
**not** part of this commit (root AGENTS.md human-review gate for rule files).

## Scope

Uncommitted product work vs `origin/main` on `fix/issue-3388-rest-dto-wire-getters`.
Excluded from commit: `rest/AGENTS.md` (agent-instruction draft).

## Memory patterns hit

- Missing behavioral unit tests — **addressed** (JacksonContextResolver production mapper + UsersTest/RolesTest contract)
- Incomplete change-class closure — **addressed** (DTO + rest callers + sitemanage UserAdaptor/ApiUtils + reverse-dep install)
- Agent rule file without human review — **honored** (`rest/AGENTS.md` left uncommitted)
- Cross-platform path checklist — N/A (no new filesystem I/O)

## Issues

None that block commit.

## Notes (not blocking)

- Remaining ~47 `public Optional<…>` DTO getters stay as later incremental PRs
  (DisplayFormatProperty, Template, Page/Widget/SeoInfo, Acl leftovers, etc.).
- Nested `Guid.getUntypedString()` is still Optional; ObjectSummary tests with a
  constructed Guid did not emit `empty`/`present` keys (field-level `stringValue`
  already uses `@JsonProperty` + `@JsonIgnore` on the Optional helper).
- This is a public getter signature change (`Optional<T>` → `T`). Grep found no
  anonymous subclasses; sitemanage standalone clean install is the reverse-dep gate.

## Tests

- `JacksonContextResolverOptionalTest` — 7 tests (prior ContentType/TemplateSummary + User/Role/ObjectSummary)
- `UsersTest` / `RolesTest` — unset scalars nullable; list getters still never-null
- `cd rest && ../mvnw.cmd clean install` — Tests run: 421, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — Tests run: 1217, Failures: 0, Skipped: 125

## Cross-platform path checklist

N/A — no new `File`/`Path` construction.
