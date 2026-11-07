# Erlang review: PR #5698 (this-run, independent)

### Erlang verdict (independent review, 2026-11-07)
- Persona: erlang 0.1.1 | Pack: percussion | Gate: advisory(tool) + strict(manual) | Models: dev-coder via Ollama
- Machine: 14 findings, 4 bugs (LLM layer skipped: secret_hit) -> Recommendation: request-changes (tool only)
- Manual adjudication: all 4 in-diff `secrets.heuristic` "bugs" are FALSE POSITIVES — no credential material in the diff:
  - `DatabaseDeliveryPasswordPanel.tsx:211` fires on the summary line `Password: {DATABASE_PASSWORD_NEVER_SHOWN}` ("Stored password is never shown."), i.e. the never-echo control itself.
  - `deliveryServerDatabasePassword.ts:29,38` fire on the constant names/messages `DATABASE_PASSWORD_PROPERTY = "password"` and `DATABASE_PASSWORD_NEVER_SHOWN` — label strings, not secrets.
  - `PSDatabaseDeliveryPasswordUpdate.java:49` fires on the message constant `DATABASE_PASSWORD_REQUIRED = "Database password is required."` — same class of hit.
  - Issues 7-14 (suggestion): test/Playwright hits are fixture dummy passwords (`"N3w-secret!"`, generated `DbP<stamp>` names) — permitted fixture data per PATTERNS.md false-positive guards.
  - Issues 5-6 are preexisting (out of diff scope; not gating): FTP-peer secret hit on the untouched line and `updatePubServer` cognitive complexity (the new `||` guard adds one branch to an already-complex dispatch; chain-wide extraction is out of scope).
- Manual substance: `PSDatabaseDeliveryPasswordUpdate` mirrors the FTP peer exactly (password-only shape check with added null-guard, stored-driver dispatch, blank/placeholder/overlong rejection before `savePubServer`, no secret in error messages); private rename `updateFtpPasswordOnly` -> `updateDeliveryPasswordOnly` has zero remaining references; TS layer validates client-side, base64-encodes (product norm), redacts secrets for logs (`redactDatabasePasswordBodyForLog` handles the `{key,value}` shape the generic redactor misses). Closure complete: sitemanage unit tests (7 new + 7 FTP preserved) + Vitest (10) + Playwright surface spec + product-docs section. Copyright headers 2026 Intersoft on new files. No file I/O (cross-platform N/A). REST surface reused, no new adaptor needed.
- **LGTM. No true in-diff bug. (Review-only: own-model PR, no APPROVE/merge per hard ban.)**

---

## Full machine report (`mkd-code-review analyze --pack percussion --gate advisory`)

## Summary

Machine analysis found **14** finding(s), **4** bug(s). LLM skipped (secret_hit)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
- In-diff: 12 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 4
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/publishing/components/DatabaseDeliveryPasswordPanel.tsx:211 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 211)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open
- Erlang: FALSE POSITIVE — line renders the never-echo status string, not a secret (see verdict).

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:29 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 29)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open
- Erlang: FALSE POSITIVE — `DATABASE_PASSWORD_PROPERTY = "password"` label constant (see verdict).

### Issue 3 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:38 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 38)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open
- Erlang: FALSE POSITIVE — `DATABASE_PASSWORD_NEVER_SHOWN` status string (see verdict).

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSDatabaseDeliveryPasswordUpdate.java:49 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 49)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open
- Erlang: FALSE POSITIVE — `DATABASE_PASSWORD_REQUIRED` message constant (see verdict).

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSFtpDeliveryPasswordUpdate.java:49 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 49)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open
- Erlang: PREEXISTING, out of scope (untouched peer line; same false-positive class).

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=36 (max 15), cyclomatic=36 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Erlang: PREEXISTING, out of scope (dispatch chain already complex; this slice adds one `||` guard).

### Issues 7-14 -- Severity: suggestion

- In-diff `secrets.heuristic` hits on Vitest/Playwright fixture passwords (`"N3w-secret!"`, generated names). Fixture dummy data, not credentials. Non-blocking (see verdict).
