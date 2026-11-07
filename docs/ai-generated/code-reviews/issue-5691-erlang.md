# Erlang pre-push review — issue #5691 (database delivery-server password)

## Summary

Machine analysis found **14** finding(s), **4** bug(s). LLM skipped (secret_hit)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:29 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 29)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:38 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 38)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSDatabaseDeliveryPasswordUpdate.java:49 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 49)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSFtpDeliveryPasswordUpdate.java:49 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 49)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=36 (max 15), cyclomatic=36 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliveryPasswordPanel.test.tsx:72 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 72)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 8 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliveryPasswordPanel.test.tsx:122 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 122)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 9 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliveryPasswordPanel.test.tsx:162 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 162)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 10 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabasePassword.test.ts:28 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 28)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 11 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabasePassword.test.ts:29 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 29)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 12 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabasePassword.test.ts:55 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 55)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 13 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerDatabasePassword.spec.js:138 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 138)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 14 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerDatabasePassword.spec.js:290 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 290)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open


## Author note (sub-agent work, 2026-11-07)
- Issues 1–4 (`secrets.heuristic`, in-diff) are **false positives on UI label
  strings**, not credential material: `DATABASE_PASSWORD_REQUIRED =
  "Database password is required."`, the `{DATABASE_PASSWORD_DRIVER_ONLY}`
  render branch, and the Java constant twin. The merged FTP mirror slice
  (#5653, PR #5659) ships byte-analogous constants (`FTP_PASSWORD_REQUIRED =
  "FTP password is required."`) and passed with zero in-diff findings; there
  is no vault/env change to make for user-facing message text.
- Issues 7–14 (`secrets.heuristic`, suggestion, in-diff tests/specs) are test
  **fixture** passwords (`s3cret-password`, `N3w-secret!`, `NightDbPw1`,
  `pw-will-not-save-<status>`) — the same fixture pattern as the merged FTP
  (#5653) and S3 (#5655) slices. No real credentials; nothing to vault.
- Issues 5–6 are **preexisting** (not in-diff): the `updatePubServer`
  dispatch chain grows by one password guard (cognitive 33→36), following the
  exact pattern of slices 105–125. Chain-wide extraction stays out of scope.
- In-diff secrets handling self-checked against the hard gates:
  - PUT body carries only the password, base64-encoded (product norm); the
    typed secret is never written into React state, never rendered, cleared on
    save/cancel. `redactDatabasePasswordBodyForLog` redacts the value.
  - GET responses mask the stored secret as `passwordEntry`; the panel never
    reads the `password` property into the form and never echoes the mask.
  - `apply()` rejects blank, overlong, the masked placeholder, and non-database
    drivers before `savePubServer`; error messages never include the secret.
  - FTP path preserved: stored-driver dispatch routes FTP bodies to the
    unchanged `PSFtpDeliveryPasswordUpdate.apply` (its 7 unit tests still pass).
  - No filesystem paths in the diff (portability gate N/A).
- Gate verdict: no true in-diff bug, behavioral tests present at all three
  layers (sitemanage 7/7, Vitest 10/10, Playwright surface pending at review
  time), no non-portable paths. Report footer says `May commit/push: yes`.
- Command: `mkd-code-review analyze --pack percussion --format markdown
  --gate advisory --git-base origin/main --models .../models.ollama-dev-coder.toml`
