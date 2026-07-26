# Erlang review: GH-939 Jetty log retention

**Date:** 2026-07-17  
**Branch:** `fix/939-jetty-log-retention`  
**Base:** `origin/development`  
**Issue:** https://github.com/intersoftdatalabs-in/percussioncms/issues/939  
**Reviewer persona:** Erlang (strict pre-commit / pre-PR)

## Summary

Jetty `perc-logging` log4j2 already had 10 MB size rotation and `max="10"`, but
dated `server-yyyy-MM-dd-i.log` files still accumulated because `max` only caps
`%i` within a date window. Adds `Delete` / `IfAccumulatedFileCount exceeds="10"`
on all four RollingFile appenders (same pattern as DTS Tomcat), a `logdir`
property, structural JUnit coverage, and docs.

## Scope

- `modules/perc-jetty/.../log4j2.xml`, tests, pom surefire binding, README/AGENTS
- Cross-platform path review: **clean** — tests use `Path.of(...)` segments; no
  hardcoded OS separators for filesystem ops
- Memory patterns hit: missing behavioral/structural tests; false-green config

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

### suggestion

1. **Live base overrides** — customer/install `jetty/base` may carry a customized
   `log4j2.xml` that will not pick up defaults until reinstalled/merged.
   Document in release notes if needed.

### nit

2. Structural XML tests do not exercise Log4j runtime Delete; product relies on
   Log4j2 implementation + DTS-proven pattern. Acceptable for this config change.

## Verification

- `./mvn-env.sh -pl modules/perc-jetty test -Dai.integrity.skip=true` → **4/4 pass**

## Files

|                 Path                  |                 Role                  |
|---------------------------------------|---------------------------------------|
| `…/perc-logging/resources/log4j2.xml` | Delete retention + logdir property    |
| `…/PercLoggingLog4j2ConfigTest.java`  | Structural contract tests             |
| `modules/perc-jetty/pom.xml`          | junit + test bindings (packaging=pom) |
| `README.md` / `AGENTS.md`             | Operator / agent notes                |

