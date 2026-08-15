# Erlang review — `chore/dts-quiet-test-logging`

**Reviewer:** Erlang Shen (independent; did not author this change)  
**Date:** 2026-08-15  
**Scope:** uncommitted vs `HEAD` on `chore/dts-quiet-test-logging`.  
**Memory patterns hit:** test noise / false-green from ignored child output; change-class lockstep (all DTS service test classpaths).

## Summary

DTS unit tests flooded the Maven reactor with Hibernate SQL because (1) several test `hibernate.show_sql` flags were `true` (writes to System.out regardless of Log4j) and (2) quiet configs were named `log4j2-tester.xml`, which Log4j2 never auto-loads (`log4j2-test.xml` is the test-classpath name).

Fix: `show_sql=false` on test Hibernate configs; replace tester files with quiet `log4j2-test.xml` (Hibernate/Spring WARN); parent Surefire `redirectTestOutputToFile`. `DtsTestLoggingQuietTest` locks the invariant.

## Recommendation

approve

## Gate

- **May commit/push: yes** (feature branch)
- Bugs: none
- Behavioral tests: present
- Agent rule files: none
- Cross-platform: **clean** (`Path.of` / `Files.walk` over module-relative `src/test`)

## Tests

- `DtsTestLoggingQuietTest`: Tests run: 4, Failures: 0
- `comments` `mvnw test`: Tests run: 72, Failures: 0 (reactor log no longer dumps Hibernate SQL)
