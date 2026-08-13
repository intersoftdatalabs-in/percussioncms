# Erlang review — issue #3338 profile change-password HTTP 500

**Date:** 2026-08-13  
**Branch:** `fix/issue-3338-profile-change-password-500`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no path/file I/O)

## Summary

My Profile → Security → Change Password persisted the new hash then returned HTTP 500. `PSUserService.changePassword` could return a null JAX-RS entity (CXF writers 500), cloned the request body after persist (`BeanUtils.cloneBean`), and historically risked a post-write `getCurrentUser()`/`find()` reload. The fix validates first, rejects DIRECTORY with 400, loads the session user once before `USERLOGIN` save, always returns a non-null password-cleared `PSUser`, and does not re-fetch after persist.

## Memory patterns hit

- Behavioral unit tests for new/changed logic (success persist + validation + persist failure)
- Secrets must not appear in logs/responses (password cleared; JSON assertion rejects plaintext/hash)
- False green / success-then-exception after a committed write
- Incomplete change-class: no new rest adaptor surface; CM1 `PUT /user/user/changepw` only; WebUI already shows success on 2xx (no screen change)

## Issues

None (hard-gate).

### Notes (non-blocking)

- Playwright success+restore already exists in `modules/perc-qa-automation/frontend/tests/profile-password.spec.js`. This PR does not change WebUI; C5 live QA cell not required for the Java-only fix.
- Test passwords are dummy literals, not production secrets; they must not be logged.

## Tests

- `PSUserChangePasswordTest` — 6 tests, 0 failures
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1189, Failures: 0, Errors: 0
