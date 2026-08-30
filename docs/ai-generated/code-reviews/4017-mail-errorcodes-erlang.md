# Erlang review: #4017 leftover mail IPS*Errors → MailErrorCodes

**Date:** 2026-08-30  
**Branch:** `fix/issue-4017-mail-errorcodes`  
**Base:** `origin/main`  
**Issue:** #4017 (parent #2616)

## Summary

Parent #2616 leftover slice. Production `system/src/main/java/com/percussion/mail` call-sites (`PSMailMessage`, `PSMailProvider`, `PSMailSendException`, `PSSmtpMailProvider`) now construct typed `MailErrorCodes` instead of bare `IPSMailErrors` ints. Additive `IPSErrorCode` constructors on `PSMailSendException`. `PSMailMessage` no longer `implements IPSMailErrors`. Allow-list shrunk for those four paths only. Dual-write skip is `isAuditable()==false` on every `MailErrorCodes` constant. No product UI/config surface.

**Memory patterns hit:** missing behavioral tests; incomplete change-class closure; tests that only grep source strings.

## Recommendation

`approve`

## Gate

- Bugs: none
- Behavioral tests: present (`PSMailLeftoverErrorCodesSliceTest` exercises production throw sites and exact exception types)
- Change-class companions: production retype + leftover slice test + allow-list shrink + scripts/README mention
- Cross-platform paths: N/A (no filesystem path I/O)
- May commit/push: **yes**

## Issues

None.

## Notes

- C2: constructors added (overloads), not changed. `grep` found no `extends PSMailSendException` / anonymous subclasses. Reverse-dep module install not required.
- C5 / product-docs: N/A (internal catalog retype).
- `cd system && ../mvnw.cmd clean install`: BUILD SUCCESS; Tests run: 2664, Failures: 0, Errors: 0, Skipped: 247. New class: Tests run: 10, Failures: 0.
- `python scripts/verify-no-bare-ipserrors.py`: PASS. `pytest scripts/test_verify_no_bare_ipserrors.py`: 21 passed.
