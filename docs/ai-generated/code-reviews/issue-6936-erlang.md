## Summary

Machine analysis found **8** finding(s), **1** blocking bug(s). LLM skipped (secret_hit)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 65 analyzed
- In-diff: 1 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/publishing/components/DatabaseDeliveryPasswordPanel.tsx:208 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 208)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:30 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 30)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerDatabasePassword.ts:39 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 39)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerFtpPassword.ts:30 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 30)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerFtpPassword.ts:32 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 32)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerFtpPassword.ts:38 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 38)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerS3SecretKey.ts:29 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 29)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 8 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerS3SecretKey.ts:38 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 38)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Adjudication (worker, #6936)

- Issue 1 (in-diff, `secrets.heuristic` on
  `DatabaseDeliveryPasswordPanel.tsx:208`): **verified false positive**.
  The flagged line is
  `{message(MSG.PUBLISH_DELIVERY_DATABASE_PASSWORD_DRIVER_ONLY)}` — an
  i18n key *name* containing the substring `PASSWORD` for the
  password-panel driver notice. No credential material, token, or secret
  value on the line or nearby. Suppressed inline with
  `// mkd-code-review[secrets.heuristic]` citing #6936, per the
  suppression protocol (fix-first investigated; nothing to fix).
- Issues 2–8 (preexisting, same rule on `deliveryServer*Password.ts` /
  `deliveryServerS3SecretKey.ts`): out-of-diff rows on long-standing
  `*_REQUIRED` / `*_NEVER_SHOWN` constant literals; no secret values.
  Preexisting rows do not BLOCK per the gate contract.
- LLM stage skipped (`secret_hit` — the same heuristic); machine-only
  result stands as clean under the contract's machine-only condition.
- Gate after adjudication: **PASS** (0 in-diff bugs, behavioral Vitest
  added, no non-portable paths).
