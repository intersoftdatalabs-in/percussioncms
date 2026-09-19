## Summary

Malformed (non-GUID) item ids on the `sitemanage-jax-rs` item endpoints now surface
`IllegalArgumentException("Invalid item id")` (400 via the bus `runtimeExceptionMapper`)
instead of a flattened 500. Fix is adapter-local: wrapped-`NumberFormatException`
detection plus raw `IllegalArgumentException` sanitization with a fixed message
(never echoes the path-param id). 10 behavioral tests pin the mapping per endpoint;
full `sitemanage` suite green (2735 run, 0 failures).

## Scope

- Base: `origin/main`
- Head: branch `fix/issue-4598-malformed-item-ids-400` (uncommitted at review time)
- Files: 3 changed (1 production, 2 tests)
- Prior report: none
- Memory patterns hit: wrong-type fakes (checked — mocks use exact declared types);
  structural-only tests (avoided — all tests invoke behavior); swallowed exceptions
  (avoided — new catch blocks rethrow); secrets (none)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishServiceWebAdapter.java`
- Description: `catch (IllegalArgumentException)` also normalizes operational IAEs
  (e.g. a `Validate` failure deep in service code) to "Invalid item id". Status is
  unchanged (the bus already mapped raw IAE to 400), but the message could mislead
  future debugging of genuine server-side arg bugs.
- Suggestion: Acceptable for this slice; if it ever bites, narrow the catch to
  `NumberFormatException` plus the explicit blank-id throw. No action required now.
- Status: open (accepted)

### Issue 2 -- Severity: nit

- File: `projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSitePublishServiceWebAdapterMalformedIdTest.java`
- Description: `publishThrowingOnPublish` stubs with `isNull(), any(), ...` — correct
  for these endpoints (adapter passes null siteName/server), but brittle if an
  endpoint ever passes non-null site args.
- Suggestion: Already scoped per-endpoint; leave as is.
- Status: open (accepted)

Cross-platform path review: no issues (no file I/O, paths, installers, or path
assertions in the diff).
