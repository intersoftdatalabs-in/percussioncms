# Erlang review — PR #6223 (night-issue-prs, review-only)

**Status:** `LGTM-REVIEW-ONLY` (own-model PR — no approve, no merge, no LGTM review posted)
**Persona:** erlang 0.1.1 (`/home/nate/.local/share/mkd/agents/erlang`)
**PR:** intersoftdatalabs-in/percussioncms#6223 — `fix(deployer): remove dead legacy-crypto fallback from password decrypt paths (#6057)`
**Head:** `ea7835b817` (branch `fix/issue-6057-deployer-legacy-crypto`)
**Reviewer independence:** independent subagent; did not author the change; no fixes implemented.
**Machine run:** from detached-head worktree at PR head (`pr-6223-review`), command:

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Ollama `dev-coder-64k:latest` reachable on `localhost:11434`. Gate evaluated on **in-diff** bugs only.

---

## Full CLI markdown (verbatim)

## Summary

Machine analysis found **1** finding(s) (1 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (1)

- `paths.hardcoded_sep` on `deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:1274` -- *Justification*: RFC 2046 wire literal (CRLF, quoted disposition) (via in_source_mkd)

---

## Erlang interpretation

### Verdict: LGTM-REVIEW-ONLY (no blockers)

**bugs_found:** none (in-diff: 0; the single machine row is preexisting + validly suppressed).

### What was checked manually (security-sensitive area: deployer legacy crypto)

1. **"Dead fallback" evidence claim — CONFIRMED.** `PSEncryptionKeyFactory.getKeyGenerator(String)`
   rejects every algorithm except AES/GCM unconditionally (throws `IllegalArgumentException`
   before touching keys). `PSCryptographer.decrypt(key1,key2,str)` calls
   `getKeyGenerator(DES_ALGORITHM)` as its first statement, so it throws before decrypting
   anything. The three deployer call sites used exactly this `decrypt()` overload — the
   fallback could never succeed. Removal is behavior-preserving for the success path.
   (Note: `decryptWithOldAlgo` bypasses the factory via direct `PSDESKey` instantiation and
   may still function — but no deployer call site used it, so the PR's scoped claim holds.)
2. **Behavior delta on the failure path — intended, documented, escalated.** `PSDeploymentServerConnection.decryptPwd`
   and `PSDbmsInfo.decryptPwd` previously let the fallback's `IllegalArgumentException` propagate;
   they now log and return `""`. `PSDeploymentHandler.decryptPwd` already returned `""` on
   fallback failure, so it is unchanged in effect. Callers (`PSDeploymentServerConnection:940`,
   `PSDbmsInfo:208,447,451`) do plain assignment/return — none depends on the exception.
   Javadoc on all three methods now documents the `""`-on-undecryptable contract. No
   silent migration: the PR body explicitly escalates legacy-decrypt restoration as an owner
   accept-risk decision (same bar as PSAesCBC #1347, open question on #6057).
   Escalate-instead-of-silent-migration gate: **honored**.
3. **No secrets in diff.** Test fixtures use synthetic values (`compat-test-user`,
   `c0mpat-s3cret!`); keystore material is generated fresh in JUnit `@TempDir`. No tokens,
   passwords, or keystore blobs in code, tests, logs, or the PR body.
4. **Tests (change-class closure).** New `PSDeploymentPasswordCryptoCompatTest` (6 tests):
   AES round-trip through both `PSDbmsInfo` and `PSDeploymentServerConnection` public APIs,
   empty-input contract, undecryptable-input returns `""` without throwing. Behavioral, not
   string-grep. `PSDeploymentHandler.decryptPwd` (instance method, same try-AES/catch-`""`
   shape) has no dedicated test — acceptable, noted as a suggestion below, not a gate item.
   Copyright header on the new file is correct (Intersoft 2026). Build evidence in the PR
   body: `cd deployer && ../mvnw clean install` → 374 tests, 0 failures (6/6 new green).
5. **Suppression re-verified.** The one suppressed row sits on an untouched line with a valid
   inline `// mkd-code-review[paths.hardcoded_sep]` comment (RFC 2046 CRLF/disposition wire
   literals — correctly not filesystem paths). Exempt from the gate per persona spec.
6. **Cross-platform.** No path-join changes; `PathUtils` usage untouched. No new portability risk.
7. **Change-class closure.** No public/protected signature changes; removed method was private
   with zero callers (verified); `DES_ALGORITHM` constant untouched in perc-legacy;
   `PSCryptographer`/`PSLegacyEncrypter` untouched for other modules' upgrade paths.
   Product-docs N/A is reasonable (no operator-visible behavior change).

### Suggestions (non-blocking)

- `PSDeploymentPasswordCryptoCompatTest.teardown`: if `rxdeploydir` was originally unset
  (`null`), teardown leaves the temp-dir value in `System` properties (deleted `@TempDir`
  afterwards). Prefer `System.clearProperty` on the null path for JVM-fork hygiene. (nit)
- `PSDbmsInfo.decryptPwd` (private) and the public `decryptPwd` overloads now ignore `uid`;
  kept for signature compat — fine, but a one-line comment on the private one would save the
  next reader a grep. (nit)
- Consider a direct `PSDeploymentHandler.decryptPwd` round-trip test for symmetry with the
  two covered call sites. (suggestion)

### Out-of-scope observations (not filed, not blocking)

- Other repo-wide `PSCryptographer.decrypt()` callers (`system/.../PSAuthentication`,
  `PSBaseDSConverter`, `PSSecurityProvider`, `Simple/.../PSPrepForConvert`,
  `uploader/.../XmlUploader`) hit the same unconditionally-throwing DES factory path. Those
  files are **not touched by this PR**, so per the residuals rule (PR-touched files only) no
  residual issues are filed here; flagged as a possible follow-up audit for the #6057 owner,
  not this slice.
- Residuals: zero qualifying rows (the only preexisting machine row in a PR-touched file is
  validly suppressed). `residual_issue_urls`: [].

### Recommendation restated

**LGTM-REVIEW-ONLY.** No in-diff bugs, tests behavioral and green per PR evidence, no secrets,
no silent-migration footgun, suppression valid. Because this is an **own-model PR**
(`model:muse-spark-1.3-contributor` + `operator:opencode`), this review is advisory only:
no APPROVE, no merge, no LGTM review posted — host decides next step.
