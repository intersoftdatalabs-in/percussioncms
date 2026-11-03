# Erlang review — PR #5537 (issue #5503 delivery-server default) R2 after erlang-fix

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: PR branch `fix/issue-5503-delivery-server-default` at `2ed18d87f90b4a0a95d2b1f3e115261900072116`.
CLI: mkd-code-review 0.1.18. Reviewer did not author the feature or erlang-fix commits.

Status: independent re-review after erlang-fix.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

Independent review found **0 in-diff bugs**. Gate: **PASS**.

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- In-diff: 1 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- Gate: advisory CLI (in-diff bugs only) plus Erlang strict gate on intent/companions

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- May merge: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1342 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `toPSPublishServerInfo` cognitive=38 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. GET/list always sets `isDefault` from `site.getDefaultPubServer()`. Not a merge blocker.

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDefault.test.ts:58 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 58)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

Disposition: not a bug. Fixture password on a mocked delivery-server object so the default-only body can be asserted not to include it. Same pattern as `deliveryServerRename.test.ts`.

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=23 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting intercept wiring. Not a merge blocker.

### Issue 4 -- Severity: bug (Erlang, prior in-diff) — FIXED

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:557 (`updateDefaultOnly`)
- Rule: change-class / behavioral correctness
- Description: Previous `updateDefaultOnly` stored `site.defaultPubServer` then called `updatePreviousDefaultPubServer`, which demoted the previous default (`filesystem` → `filesystem_only`, edition rewrite, `savePubServer`) without promoting the new default.
- Status: resolved
- Mitigation: Commit `2ed18d87f9`. Dropped `updatePreviousDefaultPubServer` and the incremental-queue side effect. Persist is `PSDeliveryServerDefaultUpdate.apply` + `siteMgr.saveSite` only. Site lock still released via the outer `updatePubServer` `finally`. Service-level `PSPubServerServiceDefaultOnlyTest` asserts previous `publishType` stays `filesystem`, selected stays `filesystem_only`, and `savePubServer` / `updateServerEditions` / `setPublishServerAsDefault` / content-change queue are not called.

## Gate notes

- CLI in-diff bugs: 0 (machine). Erlang in-diff bugs: 0 (blocking).
- Companions: helper unit test, service-level write-path test, Vitest, Playwright, product-docs.
- Non-portable paths: none (test uses `Path.of("tmp", …)` only as a mocked `PathUtils.getRxDir` File).
- Required GitHub status checks: none on `main` (protection `required_status_checks` is null). One snapshot: pending CodeQL/product-docs/H2 QA wiring; not required.
- May merge: yes

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs-erlang.
