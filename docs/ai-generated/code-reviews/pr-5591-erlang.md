<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5591

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5591
- Base: origin/main (`f21d22e5433626cefc0d4113d40581d9b506095c`)
- Head: 1f78d4a09b1ddd4cfb269ecc6dbb075e8ccacdde
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/pr-5591`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

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

- File: WebUI/src/test/ts/publishing/DatabaseDeliverySchemaPanel.test.tsx:63 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 63)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabaseSchema.test.ts:103 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 103)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=30 (max 15), cyclomatic=30 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

Gate on in-diff **bugs**, missing behavioral tests, and non-portable paths only.

- Issues 1–2 are fake test passwords (`s3cret-ora-password` / `secret-pass`) so Vitest can assert the PUT body and panel never include the stored secret. Same fixture pattern as Oracle SID / MSSQL owner peers. Not a real secret. Not a blocker.
- Issue 3 is preexisting cognitive complexity on `updatePubServer`; this slice adds a schema-only branch peer to SID/owner. Not a blocker.
- Behavioral tests: `PSDatabaseDeliverySchemaUpdateTest` (schema-only body, apply leaves host/port/user/SID/password, blank/overlong/non-Oracle do not write), Vitest unit + panel, Playwright `sitesDeliveryServerDatabaseSchema.spec.js`.
- Change-class closure: sitemanage apply + service branch, WebUI panel + SiteWorkspace, product-docs publishing, H2 Playwright.
- PUT body is only `{ key: schema }`. Password is not sent. No new filesystem path I/O.

**Recommendation: approve. May merge: yes.**
