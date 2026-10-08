# Erlang review — issue 5387

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Models: /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
Head: 5a2ae9b6d540de181070c86e9d1aea1dd03770c1

Machine gate only (no separate LLM finding section). Preexisting complexity on `applyContentListFields` is not an in-diff bug. In-diff bugs: 0. Recommendation: approve. May commit/push: yes.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1634 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyContentListFields` cognitive=35 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Pre-push local code review

Independent of the author. In-diff bugs: 0. A legacy-URL-only PUT leaves name, description, type, and item filter stored. Blank and overlong URLs are HTTP 400 and write nothing. A URL sent for a modern list is HTTP 400. HTTP 403 and 409 do not write the URL. The list shows the new URL only after save succeeds. Cancel does not call the server. Ollama did not add findings beyond the machine report. No rule-file changes.
