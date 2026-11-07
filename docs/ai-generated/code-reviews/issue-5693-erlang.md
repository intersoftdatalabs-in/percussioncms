# Erlang pre-push review — issue #5693 (SFTP delivery-server private key)

Slice 127 of parent #4531. One vertical PR: sitemanage key-only PUT facade +
PublishingShell secret panel + surface Playwright + product-docs.

## CLI invocation (skill CLI, own run — no nested reviewer)

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base "origin/main" \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Full report

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
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
- Description: Function `updatePubServer` cognitive=37 (max 15), cyclomatic=37 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Adjudication

- In-diff bugs: **0**. Missing behavioral tests: none — new sitemanage facade ships
  `PSSftpDeliveryPrivateKeyUpdateTest` (6 tests: routing, replace-leaves-others,
  blank/overlong no-write, wrong-driver rejects, case-insensitive SFTP); WebUI ships
  helper (9 tests) + panel (4 tests) vitest coverage; Playwright covers set + blank +
  cancel + 400/403/409.
- The single finding is **preexisting** complexity on `updatePubServer`. This slice adds
  one guard-clause dispatch branch in the exact shape of the ten prior shipped slices
  (#5659 FTP password, #5689 S3 secret, #5698 database password) — extracting the
  dispatcher is out of scope and would diverge from the peer pattern.
- No non-portable paths: new Java uses no file I/O; new TS uses no filesystem joins;
  new Python: none. The Playwright spec uses only URL regexes and `Buffer` base64.
- No rule/instruction files in the diff.

**Verdict: LGTM — no blocking findings (own-model PR: review only, no APPROVE/merge).**
