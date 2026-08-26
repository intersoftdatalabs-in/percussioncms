# Erlang review: issue #3859 utils leftover IPS*Errors typed ErrorCodes

- **Branch:** `fix/issue-3859-utils-typed-errorcodes`
- **Base:** `origin/main`
- **Date:** 2026-08-26
- **Reviewer:** Erlang (independent of implementer)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** missing behavioral tests; incomplete change-class closure; non-portable path joins; tests that only grep source strings

## Summary

Retypes eleven leftover `modules/utils` production `IPS*Errors` sites to utils-local `IPSErrorCode` peers (`UtilErrorCode`, `XmlErrorCode`, `JBossErrorCode`) matching perc-auditlog catalogs. Utils cannot depend on `audit-log` (circular Maven graph). Allow-list rows for those exact paths are removed. Tests exercise production exception types (`PSRuntimeException`, `PSException`, `PSInvalidXmlException`, `PSMissingApplicationPolicyException`) and assert `isAuditable() == false`.

## Issues

None blocking.

### Cross-platform path checklist

- [x] No new `".../" +` filesystem path construction in production (XML/URL `/` only where already domain)
- [x] Tests use `Path.of` / `Files` / JUnit `@TempDir` (not `/tmp` or `C:\`)
- [x] HttpServer binds loopback + ephemeral port
- [x] Line-ending sensitive assertions not used

## Notes (non-blocking)

- `PSXmlTreeWalker.getLowestLevelElement` still wraps the typed `PSInvalidXmlException` in `RuntimeException(message)` (pre-existing). Test asserts typed construction plus message equality.
- `PSRuntimeException` gained typed constructors and `getTypedErrorCode()` / `isAuditable()` delegates. Existing subclasses compile (added API, type not `final`).
- perc-auditlog enums were not grown; dual-write skip is asserted on utils-local peers (`isAuditable() == false`).
