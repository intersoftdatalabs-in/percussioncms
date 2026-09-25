<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4870

Independent review of `fix/issue-4859-runtime-stop-edition` at `cfc0018f62` against `origin/main`. Reviewer did not author the change.

```
mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
```

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Intent

`stopRuntimeJob` still falls back to `stopPublishing`, but a second failure rethrows the design-stop error. `RuntimeSection.onStop` clears Last result and formats that error with `formatApiError`. Idle rows still hide Stop. Vitest covers the double-failure path. No blocking bug.
