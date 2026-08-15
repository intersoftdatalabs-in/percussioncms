# Erlang review — #3430 ExtensionFilter / MimeType / LinkRef / ObjectLock / RestError plain wire getters

**Scope:** uncommitted branch `fix/issue-3430-optional-wire-getters` vs `origin/main`.
**Reviewer persona:** Erlang (independent of implementer).
**Memory patterns hit:** REST DTO wire getters must be plain nullable types + `@JsonInclude(NON_NULL)` (ContentType #1693 / TemplateSummary #2189 / parent #3388); production mapper tests must use `new JacksonContextResolver().getContext(TheDto.class)`; Guid `stringValue` `@JsonIgnore` bandage must not regress (#3200 / #3378); change-class companions (DTO + rest tests + sitemanage callers of converted getters).

## Summary

Parent #3388 slice 11. Remaining small wire types still exposed `Optional<T>` getters: `ExtensionFilterOptions`, `MimeType`, `LinkRef`, `ObjectLockSummary`, `RestError`, and `RestExceptionBase.getErrorData()`.

This change:

1. Converts those getters/setters to plain nullable types with class-level `@JsonInclude(NON_NULL)` on the wire DTOs (`RestExceptionBase` stays an exception; only `getErrorData()` is unwrapped).
2. Updates `RestExceptionMapper` / `WebApplicationExceptionMapper` to pass `e.getErrorData()` without `.orElse(null)`.
3. Updates sitemanage `ExtensionAdaptor` and `FolderAdaptor` (`LinkRef` / `SectionLinkRef` name/href) that used `ApiUtils.orNull(...)` / `.orElse(null)` on converted getters.
4. Appends `JacksonContextResolverOptionalTest` family methods (production mapper, no `empty`/`present` keys) plus mapper tests for scalar `errorData`.

Does **not** convert `Guid` Optional getters. Does **not** commit `rest/AGENTS.md`. Does **not** add `OPTIONAL_FIELDS_SUPPORTED` or `jackson-datatype-jdk8`. Sibling slices #3431 (ContentList) and #3432 (action/publish Status) remain on their own PRs.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests added for the converted family. Error mapping still returns `errorData` as a scalar. No filesystem path construction.

C2: public getter return types changed `Optional<T>` → `T`. Grep found `SectionLinkRef extends LinkRef` (expected; inherits converted getters) and `RestExceptionBase` subclasses (do not override `getErrorData`). Reverse-dep `projects/sitemanage` standalone `clean install` is green.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] No path/file I/O in this diff (JSON/JAX-RS DTO + adaptor callers only)
- [x] Tests do not assert OS-specific file paths
- [x] Line-ending assertions not added

## Issues

None (hard-gate).

### Nits

- Primitive `remainingTime` on `ObjectLockSummary` still serializes as `0` when unset (`NON_NULL` does not omit primitives). Pre-existing; same as other converted families.

## Product documentation

N/A — no operator/integrator example currently shows Optional-bean JSON for this family. Wire scalars stay the same documented shape.
