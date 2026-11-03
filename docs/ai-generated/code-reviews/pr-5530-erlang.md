# Erlang review — PR #5530 (issue #5502 rename delivery server)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml \
    WebUI/src/main/ts/publishing/components/DeliveryServerRenamePanel.tsx \
    WebUI/src/main/ts/publishing/deliveryServerRename.ts \
    WebUI/src/main/ts/publishing/sections/SiteWorkspace.tsx \
    WebUI/src/test/ts/publishing/DeliveryServerRenamePanel.test.tsx \
    WebUI/src/test/ts/publishing/deliveryServerRename.test.ts \
    modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerRename.spec.js \
    product-docs/8.2/admin/publishing.md \
    projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSDeliveryServerRename.java \
    projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java \
    projects/sitemanage/src/test/java/com/percussion/pubserver/impl/PSDeliveryServerRenameTest.java
```

Cwd: PR head `7499308467985b19f5bf0559d627554a901e60aa` (`fix/issue-5502-rename-delivery-server`).

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- CLI: mkd-code-review 0.1.18
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerRename.test.ts:103 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 103)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

Disposition: not a bug. Fixture password on a mocked delivery-server object so the rename body can be asserted not to include it. Same pattern as `deliveryServerFtpHost.test.ts` / `FtpDeliveryHostPanel.test.tsx`. PATTERNS.md secret-scanning false-positive guard: fixture test data.

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=22 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: preexisting. This change adds a name-only guard that returns before the full replace; it does not expand the full-update body. Preexisting rows do not BLOCK.

## Independent reading

Overnight Erlang (Grok Build 1.0.70, grok-4.6). Did not author the PR. `llm.error` not observed.

Name-only delivery-server PUT:

- `PSDeliveryServerRename.isNameOnly` is true when `serverName` is present (including blank, so the full replace cannot run) and type, description, default flag, and properties are absent.
- `normalizedName` rejects blank / overlong / invalid before `apply`. Uniqueness is `validateServerName` in `updateNameOnly`. Same-name no-op returns without save.
- `apply` sets only `server.setName`. Driver, folder, type, default flag, and password stay. Database servers also rename on-disk config files via the existing helper.
- Client PUT body is `{ serverInfo: { serverName } }` only. Password is never rendered. Blank, duplicate (case-insensitive vs siblings excluding current), invalid, and cancel do not PUT. HTTP 400/403/409 keep the previous name and leave the form open.
- `NAME_PATTERN` matches the existing full-update character class (`\w`, `.`, `-`); Java `Matcher.matches()` is whole-string.

Companions: `PSDeliveryServerRenameTest`, Vitest unit + panel, Playwright `sitesDeliveryServerRename.spec.js`, `product-docs/8.2/admin/publishing.md` section “Rename a site delivery server (Sites)”. Wired in `SiteWorkspace` next to the FTP/local field-only panels. No rest module change (existing publishmanagement PUT). No rule files. No new filesystem path joins in the rename helper (DB file rename reuses existing NIO path).

Gate: PASS. Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs-erlang.
