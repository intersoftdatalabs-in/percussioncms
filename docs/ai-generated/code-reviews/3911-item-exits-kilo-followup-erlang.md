# Erlang review: PR #3911 Kilo follow-up (item-exits reuse / JSON readers)

- **Scope:** uncommitted changes on `cluster/night-issue-20260827-developer-ct` addressing four unresolved Kilo threads on PR #3911.
- **Recommendation:** approve
- **Gate:** May commit/push: yes (after standalone `rest` then `sitemanage` `clean install`)
- **Cross-platform path checklist:** N/A (no filesystem path construction)

## Summary

Fixes the apply-when data-loss path for multi-rule item-level exits, replaces the NUL-joined match key with structural FQN+param comparison, mirrors `isJsonCompatible` on the field-rule JSON reader, and documents that Jackson 3.2 still publishes `@JsonRootName` under `com.fasterxml.jackson.annotation` (no `tools.jackson.annotation` package). Behavioral tests cover the 2-rule reuse path, NUL vs two-param collision, JSON media-type guard, and WRAP_ROOT_VALUE emission via `JacksonContextResolver`.

## Issues

None (no bugs, no missing behavioral tests for the new logic).

## Memory patterns hit

- Match production types in tests (PSConditionalExit / PSExtensionCall, not string keys).
- Jackson root wrap: custom readers for PUT; WRAP_ROOT_VALUE GET still uses fasterxml `@JsonRootName`.
