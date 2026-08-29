# Erlang review — PR #3973 review-thread follow-up

**Scope:** uncommitted review-thread fixes on `fix/issue-3964-systemdef-field-create-delete` vs `HEAD`.
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** missing behavioral tests for new/changed non-trivial logic; Public helper Javadoc that contradicts implementation (not applicable here)

## Summary

Kilo threads on #3973: add a behavioral companion for `isSystemInternal()` delete 400; drop redundant `isSafeFieldName` after the letter/digit/underscore loop.

## Issues

None. `deleteField_systemInternalIs400` exercises the documented 400 path with `isSystemMandatory=false` and asserts no save. Path characters (`/`, `\`, `..`, NUL) remain rejected by the remaining character-class loop (tests extended). No product-docs change (review-only). No file I/O.

## Cross-platform path checklist

N/A — no path/file I/O in this diff.

## Evidence

`cd projects/sitemanage && ../../mvnw.cmd clean install` → BUILD SUCCESS. Tests run: 1799, Failures: 0, Skipped: 125. `SystemDefAdaptorTest` 33/33 including `deleteField_systemInternalIs400`.
