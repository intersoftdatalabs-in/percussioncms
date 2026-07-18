# Erlang review — PSDeployService rollback policy + filter install

| Field | Value |
|-------|--------|
| **Date** | 2026-07-18 |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |

## Summary

08:51 failure is still `perc_public` / UnexpectedRollbackException. Installed jars were **08:30**; by-name fix was committed **08:47** — rebuild did not include latest code.

Systemic bug: `@Transactional(noRollbackFor = Exception.class)` on `PSDeployService`. Nested Hibernate `RuntimeException` marks TX rollback-only; checked `PSDeployServiceException` then causes Spring to **attempt commit**, reporting only UnexpectedRollbackException.

Fix: `rollbackFor = Exception.class` on PSDeployService; keep by-name filter merge + flush; drop misleading `@Transactional` on non-proxied handler.

## Gate

approve · May commit/push: yes
