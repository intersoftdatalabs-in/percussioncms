# Erlang pre-push review — issue #5655 (S3 delivery-server secret key)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Second run confirmed deterministic (same 4 findings, LLM skipped `secret_hit` both times).

## Summary

Machine analysis found **4** finding(s), **3** bug(s). LLM skipped (secret_hit)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 3 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 3
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerS3SecretKey.ts:28 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 28)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/publishing/deliveryServerS3SecretKey.ts:37 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 37)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSAmazonS3DeliverySecretKeyUpdate.java:50 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 50)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=35 (max 15), cyclomatic=35 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Worker disposition (author of this slice)

- Issues 1–3 (`secrets.heuristic`) are **false positives, not in-diff bugs**.
  Flagged lines are user-facing message constants, not credential material:
  - `deliveryServerS3SecretKey.ts:28` → `S3_SECRET_KEY_REQUIRED = "S3 secret key is required"`
  - `deliveryServerS3SecretKey.ts:37` → `S3_SECRET_KEY_NEVER_SHOWN = "Stored secret key is never shown."`
  - `PSAmazonS3DeliverySecretKeyUpdate.java:50` → `S3_SECRET_KEY_REQUIRED = "S3 secret key is required."`
  No credential, token, or key material exists in source. Test/Playwright
  fixtures use dummy values (`secret-s3-key`, `new-secret-s3-key`,
  `NightS3Secret2`) — same shape as merged peers #5653 (FTP password), #5654 /
  #5678 (S3 access key), whose reports closed with 0 blocking bugs. The secret
  handling itself follows the peer hard gates: PUT body base64-encoded (product
  norm), redacted in log snapshots, encrypted with `PSEncryptor` server-side,
  never written into React state, never rendered, cleared on save/cancel.
  Renaming this UX copy to dodge the heuristic would diverge from the
  established slice copy (`S3 access key is required` → `S3 secret key is
  required`) and is not justified.
- Issue 4 (`complexity.cognitive` on `updatePubServer`) is **preexisting and
  out of diff scope**. The slice adds one dispatch guard
  (`isSecretKeyOnly → updateS3SecretKeyOnly`) following the exact pattern of
  slices 105–124; extracting helpers for the whole chain is out of scope for
  this slice (same disposition as slice #5653).
- Behavioral tests: new sitemanage JUnit (6 tests), new vitest helper (7) +
  panel (4) suites, and H2 surface Playwright (2 tests) all pass — the
  behavioral-test gate is met.
- Portability: no filesystem paths in the diff (all UI strings, React, and
  in-memory property updates) — portability gate N/A.
- Tool verdict is `May commit/push: yes`; proceeding to PR with this
  disposition recorded. The LLM stage skip (`secret_hit`) is a runner guard
  tripping on the word "secret" in identifiers/fixtures, not a finding.
