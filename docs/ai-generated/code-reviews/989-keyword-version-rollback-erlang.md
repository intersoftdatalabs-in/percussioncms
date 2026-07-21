# Erlang review — keyword install Hibernate version / UnexpectedRollbackException

| Field | Value |
|-------|--------|
| **Date** | 2026-07-18 |
| **Branch** | `989-react-cui-widget-builder` |
| **Scope** | Uncommitted local changes vs `HEAD` |
| **Intent** | Fix perc.openGraphWidget (and similar) package install: `UnexpectedRollbackException` on keyword install |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |
| **Memory patterns hit** | Forced Hibernate `@Version` bump before merge; Spring proxy / rollback masking root cause |

## Summary

Log stack at `PSKeywordDependencyHandler.installDependencyFiles:187` → deploy service `@Transactional` proxy. Symptom is `Transaction silently rolled back because it has been marked as rollback-only` with **no** nested Hibernate message (masked by `noRollbackFor = Exception` then commit attempt).

Root cause analysis: `doInstallDependencyFiles` forced `version = loaded + 1` before `saveKeyword`/`merge`. Under Hibernate 7 optimistic locking that desyncs entity version from the DB and marks the TX rollback-only — same family as the GUID version fix.

Changes:
1. Stop force-bumping version (policy flag + dead path retained only for documentation via `shouldForceHibernateVersionBump() == false`).
2. Reinstall path: if keyword missing by dependency id but label exists, load by label instead of `createKeyword` (avoids unique-label failure).
3. Catch `RuntimeException` from deploy service and rethrow with root-cause message formatter.

Tests: `PSKeywordInstallUtilsTest` 3/0.

## Issues

### Bugs

_None in this pack._

### Suggestions

1. Other dependency handlers only call `setVersion(null)` before save (not +1). Keyword was unique; consider later audit of other version manipulations under Hibernate 7.
2. Keyword package XML root element is historically `<null>` (Betwixt); out of scope — deserialization still works when reading into a `PSKeyword` instance.

### Nits

_None._

## Gate

| | |
|--|--|
| **Recommendation** | approve |
| **May commit/push** | **yes** |
