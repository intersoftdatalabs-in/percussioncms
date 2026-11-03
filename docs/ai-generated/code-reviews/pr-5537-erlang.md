# Erlang review — PR #5537 (issue #5503 delivery-server default)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: PR head `707195d184d35db19855e292c6a0a114d2e59b21` (`fix/issue-5503-delivery-server-default`).
CLI: mkd-code-review 0.1.18. Reviewer did not author this PR.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

Independent review found **1 in-diff bug**. Gate: **BLOCK**.

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 1 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- Gate: advisory CLI (in-diff bugs only) plus Erlang strict gate on intent/companions

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: no
- May merge: no

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:1348 (preexisting)
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

### Issue 4 -- Severity: bug (Erlang, in-diff)

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:583
- Rule: change-class / behavioral correctness
- Description: `updateDefaultOnly` stores `site.defaultPubServer` then calls `updatePreviousDefaultPubServer(currentDefaultServer, server, siteMod, true)`. That helper demotes the **previous** default (`filesystem` → `filesystem_only`, edition rewrite, `savePubServer` on the old server) and does **not** promote the new default (`setPublishType(..., true)`, `setPublishServerAsDefault`, `updateServerEditions` for the selected server). After a successful flag-only PUT both servers can be `*_only` while full editions still follow the demoted previous server. The comment at 576–578 correctly refuses `setPublishServerAsDefault` (NPE + site-root rewrite). The remaining demote-without-promote is worse than a true flag-only write (`setDefaultPubServer` only), which is what the slice AC and product-docs describe (badge / `isDefault` only; name, driver, type, properties stay).
- Suggestion: For this slice, drop the `updatePreviousDefaultPubServer` call and the incremental-queue side effect. Persist only `siteMod.setDefaultPubServer(server.getServerId())` so GET/list ★ follows the new id and no publish-type/edition rewrite runs. If full default-switch semantics are required, promote the new server as well (without the site-root rewrite / NPE walk) and add a service-level test that both servers' `publishType` and editions match the intended end state.
- Status: open
- Tests: `PSDeliveryServerDefaultUpdateTest` covers the helper only (`isDefaultOnly` / `validate` / `alreadyDefault`). Nothing asserts `updateDefaultOnly` does not demote the previous server's publish type. Playwright only checks the ★ badge.

## Gate notes

- CLI in-diff bugs: 0 (machine). Erlang in-diff bugs: 1 (blocking).
- Companions present: helper unit test, Vitest, Playwright, product-docs. Missing: service-level test of the write path that actually mutates site default / previous publish type.
- Non-portable paths: none
- May merge: no
