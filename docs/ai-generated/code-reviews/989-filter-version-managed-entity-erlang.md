# Erlang review — filter install managed-entity version nulling

| Field | Value |
|-------|--------|
| **Date** | 2026-07-18 |
| **Branch** | `989-react-cui-widget-builder` |
| **Scope** | Uncommitted local changes vs `HEAD` |
| **Intent** | Fix perc.Baseline UnexpectedRollbackException on filter install (`deserializeAndSaveFilter`) |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |

## Summary

Only **perc.Baseline** failed on 07:36 startup. Stack is `commitTransactionAfterReturning` on `deserializeAndSaveFilter` — method returned without throwing, then commit failed because TX was already rollback-only.

Root cause: for existing filters, code loaded a managed `PSItemFilter`, set `version = null` on it, discarded the Java reference, then deserialised a fresh package object and saved. The managed entity remained in the Hibernate session with a dirty null `@Version`, poisoning flush at commit (Hibernate 7).

Fix:
1. Capture `lver` only; never mutate version on the managed load.
2. Always `generateFilterFromFile(..., null)` for a fresh instance.
3. `saveFilter` sets version once (no null-then-restore dance).
4. Better RuntimeException wrapping + tests.

## Verification

- `PSFilterInstallUtilsTest` + `PSKeywordInstallUtilsTest`: 6 run, 0 failures

## Issues

_None blocking._

## Gate

**approve** · **May commit/push: yes**
