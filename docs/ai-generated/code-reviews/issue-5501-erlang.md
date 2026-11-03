# Erlang review — issue #5501 (local delivery-server folder)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed (implementation commit); follow-up commit adds test/spec/erlang artifacts only
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=21 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent / closure

- Change class: Sites PublishingShell folder-only PUT for a Local (file) delivery server, with WebUI panel, Vitest, H2 Playwright, and product-docs.
- Companions present: `PSLocalDeliveryFolderUpdate` + unit tests; `PSPubServerService` driver dispatch; `LocalDeliveryFolderPanel` + `deliveryServerLocalFolder.ts` + Vitest; Playwright `sitesDeliveryServerLocalFolder.spec.js`; `product-docs/8.2/admin/publishing.md`.
- Folder values are stored as trimmed operator text (same VARCHAR(255) property as FTP). They are not joined as filesystem paths; Unix `/` in test fixtures matches the FTP peer.
- First machine pass flagged Windows `\\` literals in tests (`paths.hardcoded_sep`). Those assertions were removed; remaining fixtures use the same `/night-local` style as #5469.

## Verdict

Gate: PASS. Preexisting complexity on `updatePubServer` is not blocking.
