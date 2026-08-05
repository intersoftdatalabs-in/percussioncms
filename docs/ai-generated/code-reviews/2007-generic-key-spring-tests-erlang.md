# Erlang review — issue #2007 membership Generic Key Spring tests

**Branch:** `fix/issue-2007-generic-key-spring-tests`  
**Scope:** test resources only under `deliverytiersuite/delivery-tier-suite/membership`  
**Date:** 2026-08-05  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no production file I/O; DTD fix is classpath URI, portable)

## Summary

Root cause of `Failed to load ApplicationContext` for `PSGenericKeyServiceTest` /
`PSGenericKeyDaoTest` was `test-beans-generic-key.xml` loading Hibernate via
`configLocation` → test `hibernate.cfg.xml`, whose DOCTYPE system id was
`file:hibernate-configuration-3.0.dtd` (CWD-relative). Under Maven Surefire the
CWD is the module directory, so Woodstox/Hibernate failed with
`FileNotFoundException` → `Error accessing StAX stream`. Cascading
`ApplicationContext failure threshold` errors followed.

## Change class

**Spring/Hibernate test context wiring for Generic Key slice** — companion is
alignment with peer `test-beans.xml` (`packagesToScan` + H2 / JpaTransactionManager
pattern already proven green for membership DAO/service tests).

## Issues

None (bugs / missing behavioral tests / non-portable path I/O).

### Notes (non-blocking)

- Production `WEB-INF/beans.xml` still uses `configLocation` with
  `/WEB-INF/hibernate.cfg.xml` and classpath DTD — not changed; production
  security/config untouched as required by issue scope.
- Existing behavioral tests now exercise the fixed context (create/validate/delete
  keys). No new production logic; no new unit-test class required.
- Dependency-analyze warnings on clean install are pre-existing baseline.

## Evidence

- `cd deliverytiersuite/delivery-tier-suite/membership && ../../../mvnw clean install`
- Tests run: 20, Failures: 0, Errors: 0 — BUILD SUCCESS

## Memory patterns hit

- Prefer peer test wiring (`packagesToScan`) over fragile CWD-relative config
- Diagnose first `Failed to load ApplicationContext` cause, not threshold cascade

