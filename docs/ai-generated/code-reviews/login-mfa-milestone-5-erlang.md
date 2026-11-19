## Summary

Machine analysis found **38** finding(s) (29 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 56 analyzed
- In-diff: 28 finding(s); preexisting: 10
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/profile/SecuritySection.tsx:288 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 288)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/profile/SecuritySection.tsx:334 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 334)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/updateConfiguration.xml:259 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 259)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:843 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 843)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:1548 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1548)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:2113 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2113)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:2145 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2145)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:2147 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2147)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/user/service/impl/PSUserService.java:2088 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `doValidation` cognitive=27 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (29)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:80` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:85` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:102` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:109` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:121` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:130` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:150` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ForgotPasswordPage.tsx:161` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:125` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:130` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:147` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:157` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:167` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:174` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:186` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:202` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:209` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:221` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:230` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/auth/ResetPasswordPage.tsx:251` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `WebUI/src/main/ts/login/LoginPage.tsx:269` -- *Justification*: : adjudicated FP (issue #52, precedent #6481) - React DOM/test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/updateConfiguration.xml:291` -- *Justification*: : adjudicated FP (issue #52, precedent #50) - Ant propertyfile numeric rate-limit key name, not a secret. --> (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/updateConfiguration.xml:294` -- *Justification*: : adjudicated FP (issue #52, precedent #50) - Ant propertyfile numeric rate-limit key name, not a secret. --> (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/auth-password-recovery.spec.js:28` -- *Justification*: : adjudicated FP (issue #52, precedent #5946) - Playwright test suite title / test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/auth-password-recovery.spec.js:40` -- *Justification*: : adjudicated FP (issue #52, precedent #5946) - Playwright test suite title / test ID, not credential material. (via in_source_mkd)
- `secrets.heuristic` on `system/services/src/com/percussion/services/auth/impl/PSAuthSecurityDao.java:862` -- *Justification*: : adjudicated FP (issue #52, precedent #50) - parameterized SQL UPDATE column assignment, not a hardcoded secret. (via in_source_mkd)
- `secrets.heuristic` on `system/services/src/com/percussion/services/auth/recovery/PSPasswordResetResult.java:24` -- *Justification*: : adjudicated FP (issue #52, precedent #50) - public status code constant, not a secret. (via in_source_mkd)
- `secrets.heuristic` on `system/services/src/com/percussion/services/auth/recovery/impl/PSAccountRecoveryService.java:77` -- *Justification*: : adjudicated FP (issue #52, precedent #50) - server.properties rate-limit key name constant, not a secret. (via in_source_mkd)
- `paths.hardcoded_sep` on `system/src/main/java/com/percussion/servlets/PSAuthServlet.java:718` -- *Justification*: : adjudicated intentional (issue #50, same as PSLoginServlet) — "/" is HTTP URL context path delimiter, not an OS path. (via in_source_mkd)

</details>
