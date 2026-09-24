<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4800

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/4800
- Base: origin/main
- Head: 074a925b2ce6ea8222bd7a1262eb4b29b2c30d80
- Files analyzed: 9 (PR diff paths)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **1** bug(s). LLM skipped (hard_pattern)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/editor-host-rename.spec.js:254 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 254)
- Status: open

## Erlang interpretation

The in-diff hit is `lookup.json().catch(() => ({}))` inside the H2 route stub that reflects the path-item name into the item-fields GET. It is not an empty `catch {}` and it is not production code. If that JSON parse fails, the stub falls back to the pre-rename name, `renameLanded` stays false, and the spec's POST 200 / renamed-title expects fail. It cannot false-green a failed rename. Not a product bug. HTTP 409 name-in-use is surfaced as the generic rename failure plus `formatApiError` detail (not success). Recommendation: approve.
