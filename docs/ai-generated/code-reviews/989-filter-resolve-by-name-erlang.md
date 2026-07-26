# Erlang review — filter install resolve by name + flush

|       Field        |             Value              |
|--------------------|--------------------------------|
| **Date**           | 2026-07-18                     |
| **Branch**         | `989-react-cui-widget-builder` |
| **Recommendation** | **approve**                    |
| **Gate**           | **May commit/push: yes**       |

## Summary

08:35 startup: only **perc.Baseline** failed; `perc_public(Filter Definition)` with UnexpectedRollbackException on **commitTransactionAfterReturning** (method returned without throw).

Prior fix (no null version on managed entity) is deployed. Remaining failure mode: install keyed only by package dep GUID; same filter **name** already in DB → treated as insert → unique NAME constraint fails at flush → rollback-only.

Fix:
1. Resolve existing by **name** (natural id); domain-merge package onto managed row.
2. `PSFilterManager.saveFilter`: skip self-merge; `session.merge` + **`session.flush()`** so failures surface in-method with logging.

Tests: 4/0.

## Gate

approve · May commit/push: yes
