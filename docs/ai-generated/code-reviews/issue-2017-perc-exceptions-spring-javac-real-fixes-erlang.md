# Erlang review: issue #2017 perc-exceptions-spring javac real fixes

**Branch:** `fix/issue-2017-perc-exceptions-spring-javac`  
**Date:** 2026-08-07  
**Reviewer:** Erlang (self-review, Grok Build / night-issue-prs)

## Summary

Replaces residual `@SuppressWarnings` left by suppress-only PR #2098 with real fixes in
`modules/perc-exceptions-spring` (team strategy for reopened javac cleanup under parent #2200).

- **this-escape:** constructors install Spring `Errors` via parent constructors with direct field
  write; `PSBeanValidationException` / `PSPropertiesValidationException` marked `final`;
  `PSErrorCause` uses final helpers + field-direct init.
- **serial:** `transient` on non-`Serializable` validation/error payloads that travel via JAXB/REST
  (not Java serialization); `HashMap` field type for properties map.
- **unchecked:** `Class.cast` via `getType()` for properties validator; optional `getFullType()` +
  narrow private unchecked bridge only when no Class token (documented).
- **statics:** qualified `Validate.notNull` instead of star static imports.
- **Tests:** 19 unit tests covering constructors, throwIfInvalid, PSErrorCause, PSErrorUtils,
  properties validator.

## Scope

- `modules/perc-exceptions-spring/src/main/java/**` (exception hierarchy + validators + utils)
- `modules/perc-exceptions-spring/pom.xml` (junit-jupiter-api; drop unused log4j-api)
- New tests under `modules/perc-exceptions-spring/src/test/java/**`
- Base: `origin/main`

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (19 tests, 0 failures)
- Cross-platform path review: N/A (no path/file I/O in diff)
- **May commit/push: yes**

## Issues

None.

## Residual suppressions (documented)

| Site | Reason |
| --- | --- |
| `PSAbstractBeanValidator.uncheckedCast` | Spring `Validator.validate(Object, Errors)` + type erasure when subclass does not supply `getFullType()` |
| `PSSpringOvalValidator` OVal `getContext()` | Pre-existing deprecation; OVal API still required for field-context mapping |

## Verification

- `cd modules/perc-exceptions-spring && ../../mvnw.cmd clean install` — BUILD SUCCESS
- Tests: 19 run, 0 failures
- Main compile: no this-escape / serial / unchecked `[WARNING]` on changed sources after fixes
- Class-level `@SuppressWarnings` for this-escape/serial/unchecked removed from #2098 sites
