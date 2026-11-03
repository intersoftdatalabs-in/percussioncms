# Erlang review — issue #5515 (SiteBaseUrlPanel remount Vitest)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- In-diff bugs: none
- Missing behavioral tests: no (this change is the remount wait; production GET-after-mount already covered)
- Non-portable paths: none
- Change-class: Vitest timing/assertion only; product-docs N/A; Playwright N/A (no screen change; cycle-verify residual)

## Issues

_No issues._

## Intent / context

Cycle Verify residual #5515: `SiteBaseUrlPanel` remount assertion used `findByTestId` then a one-shot `toHaveTextContent`. The testid is present on first paint with placeholder `—`, so under Maven Vitest contention the remount assertion failed before `getSite` resolved. The panel already re-GETs on mount (`useEffect` on site key). This diff waits for the GET + saved URL after unmount/remount.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
