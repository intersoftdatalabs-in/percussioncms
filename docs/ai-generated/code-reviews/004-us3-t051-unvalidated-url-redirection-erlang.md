# Erlang Review — 004/us3-t051-unvalidated-url-redirection

**Date**: 2026-07-17  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted T051 open-redirect cluster vs `origin/development`

## Summary

Closes six `java/unvalidated-url-redirection` alerts by routing every redirect through `PSRedirectValidation` (or a fixed local path):

| Alert | Site | Fix |
|-------|------|-----|
| #1081 | `PSUncaughtError` | Drop Referer-based host rebuild; always `/context/error.html` (internal) |
| #643/#644 | `PSCommentsRestService` | `seeOtherIfSafe` — relative path or Host-allow-listed absolute URL |
| #645–#647 | `PSSecurityFilter` | `sendValidatedRedirect` before every `sendRedirect` |

CodeQL model pack barrier for `PSRedirectValidation` (`url-redirection`). Existing 46-test suite for the helper remains the primary regression net.

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

### Issue 1 — Severity: nit
- Proxy+login path no longer double-appends `loginUrl` when proxy base is empty (pre-existing footgun). Behavior change is safer and matches intent.

## Handoff

Safe to commit and open PR against `development`.
