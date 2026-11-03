# Erlang review — PR #5528 (issue #5515 SiteBaseUrlPanel remount Vitest)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml \
    WebUI/src/test/ts/publishing/SiteBaseUrlPanel.test.tsx
```

Cwd: PR head `49b64fac505fbeac139ded300906225118127433` (`fix/issue-5515-site-base-url-remount`).

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
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
- In-diff bugs: none
- Missing behavioral tests: no (this change *is* the remount wait; production GET-after-mount already covered)
- Non-portable paths: none
- Change-class: Vitest timing/assertion only; product-docs N/A; Playwright N/A (no screen change)

## Issues

_No issues._

## Independent reading

Overnight Erlang (Grok Build 1.0.70, grok-4.6). Did not author the PR. `llm.error` not observed.

The testid `publish-site-base-url` is present on first paint with placeholder `—`. `findByTestId` therefore resolves too early under Maven Vitest contention. The remount case now `waitFor`s both `getSite("NightlySite")` and the saved URL text. `getSiteMock.mockReset()` before the remount mock avoids leaking the pre-PUT resolution. Production panel already re-GETs on mount.

No rule files. No filesystem path joins.

Gate: PASS. Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs-erlang.
