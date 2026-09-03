# Erlang review — #4197 leftover PSDtdTree IPSXmlErrors typed XmlErrorCodes

**Scope:** uncommitted branch `fix/issue-4197-psdtdtree-xmlerrorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; do not delete `IPS*Errors` interfaces; exact production exception types in tests; incomplete change-class closure (allow-list companion).  
**Cross-platform path checklist:** missing-file DTD URL uses `@TempDir` + `Path.toUri().toURL()` (no hardcoded separators). DTD fixtures are in-memory bytes.

## Summary

Parent #2616 leftover slice: remaining production `IPSXmlErrors` throws in `system/src/main/java/com/percussion/xml/PSDtdTree.java` (`DTD_IO_ERROR`, `DTD_ROOTNOTFOUND_ERROR`, `DTD_MULTIPLE_OCCURRENCE_NOTSUPPORTED_ERROR`, `DTD_ELEMENT_NOTFOUND_ERROR`) now use typed `XmlErrorCodes`. `IPSXmlErrors` stays as the numeric bridge. Dual-write skip tests cover leftover non-auditable XML catalog codes. Residual allow-list no longer lists `PSDtdTree.java`. CodeQL `java/ssrf` sink-line suppression and URLValidation scheme rebuild were not touched. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

## Verification

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS (Tests run: 2840, Failures: 0, Errors: 0, Skipped: 248)
- `PSDtdTreeTypedXmlErrorCodesSliceTest` Tests run: 6, Failures: 0
- `PSDtdTreeSsrfTest` Tests run: 3, Failures: 0
- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 26 passed

## Notes

- C2: no `final`/`sealed` and no public/protected signature changes. `downstream_checked=none`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip when non-auditable.
