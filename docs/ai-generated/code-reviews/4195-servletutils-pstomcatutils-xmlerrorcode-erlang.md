# Erlang review: #4195 servletutils PSTomcatUtils IPSXmlErrors typed XmlErrorCode

- **Branch:** `fix/issue-4195-pstomcatutils-xmlerrorcode`
- **Parent:** #2616
- **Date:** 2026-09-02
- **Recommendation:** approve
- **Gate:** pass
- **May commit/push:** yes
- **Memory patterns hit:** leftover IPS*Errors retype + dual-write skip tests; shrink residual allow-list; behavioral XML throw coverage (not token grep only); portable `Path` / `Files` in tests

## Summary

Retype remaining production `IPSXmlErrors` throw sites in `modules/servletutils/.../PSTomcatUtils.java` to utils-local `XmlErrorCode` / typed `PSInvalidXmlException` constructors. Numeric codes stay bridged via `IPSXmlErrors`. Both leftover catalog codes (`XML_ELEMENT_MISSING`, `XML_ELEMENT_INVALID_ATTR`) are non-auditable (`isAuditable()==false`), so dual-write skip is asserted; there is no auditable XML code in this slice to dual-write. Allow-list shrinks by this path only. Behavioral tests cover missing `Server`/`Service` on load and save, invalid AJP `port` attribute, and missing AJP connector.

Varargs `Object...` on the typed constructor was initially called with `new String[]` (new compiler warning); switched to discrete varargs arguments so `clean install` introduces no new warnings.

## Issues

None (hard-gate).

## Cross-platform path checklist

- Tests write XML via `Path` + `Files.writeString` under `@TempDir` — no hardcoded `/` or `\\` filesystem joins
- No Unix-only temp roots or path-string assertions of OS `toString()`

## Change-class companions

- Production throw-site retype (peer: utils leftover #3859 / `PSXmlUtils`)
- Dual-write skip tests on typed exceptions (`PSTomcatUtilsTypedErrorCodeSliceTest`)
- `scripts/ipserrors-residual-allowlist.txt` shrink + pytest resurrection guard
- Module standalone `mvnw clean install` (servletutils)

## Product documentation

N/A — internal error-catalog retype; no operator/admin/REST/UI surface change.

## C2 API shape

Did not apply: no `final`/`sealed` types; no public/protected signature changes.

## Build

- `cd modules/servletutils && ../../mvnw.cmd clean install` → BUILD SUCCESS
- Tests run: 55, Failures: 0, Errors: 0, Skipped: 5 (pre-existing skips)
- `PSTomcatUtilsTypedErrorCodeSliceTest` Tests run: 7, Failures: 0
- `python scripts/test_verify_no_bare_ipserrors.py` — 27 passed
