# Erlang pre-push review — issue #5653 (FTP delivery-server password)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=33 (max 15), cyclomatic=33 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Author note (sub-agent work, 2026-11-06)

- The single finding is **preexisting** (not in-diff): the `updatePubServer`
  dispatch chain grows by one `isPasswordOnly` guard, following the exact pattern
  of slices 105–120. Extracting helpers for the whole chain is out of scope for
  this slice.
- In-diff secrets handling was self-checked against the hard gates:
  - PUT body carries the password base64-encoded (product norm); the typed secret
    is never written into React state, never rendered, and cleared on save/cancel.
  - GET responses mask the stored secret as `passwordEntry`; the panel never reads
    the `password` property and never echoes the mask into the input.
  - `apply()` rejects blank, overlong, the masked placeholder, and non-FTP drivers
    before `savePubServer`; error messages never include the secret.
  - No filesystem paths in the diff (portability gate N/A).
- Command: `mkd-code-review analyze --pack percussion --format markdown
  --gate advisory --git-base origin/main --models .../models.ollama-dev-coder.toml`
