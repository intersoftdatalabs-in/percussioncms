# Erlang review — issue 5467 FTP delivery-server port

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

Head at review: `bcce0789d44ced7c60472305b9577885160ae97c`

The machine report is below. Interpreter notes follow it. In-diff bugs: 0. Missing behavioral tests: none. Non-portable paths: none. Gate does not block.

## Pre-push local code review

```markdown
## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/FtpDeliveryPortPanel.test.tsx:61 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 61)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerFtpPort.test.ts:87 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 87)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=18 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
```

## Interpreter

- Issues 1 and 2 are `secrets.heuristic` hits on test titles that say "password". The fixtures assert a password stays stored and is not rendered or sent. They are suggestions, not product secrets.
- Issue 3 is preexisting complexity on `updatePubServer`. The port-only path is `updateFtpPortOnly`, the same shape as the host-only short-circuit. It does not block.
- Recommendation: approve. May commit/push: yes.
