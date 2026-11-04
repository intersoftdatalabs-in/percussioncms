<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5590

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5590
- Base: origin/main (`f21d22e5433626cefc0d4113d40581d9b506095c`)
- Head: f5388eee35cb70625552427b47f177d0cf226fd4
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/pr-5590`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
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

- File: rest/src/main/java/com/percussion/rest/folders/FoldersResource.java:857 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 857)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

## Agent interpretation

Gate on in-diff **bugs**, missing behavioral tests, and non-portable paths only.

- Issue 1 is preexisting `paths.hardcoded_sep` on a CMS finder/URL path (always `/`). Not in this slice's added lines. Not a blocker.
- `rejectMoveDestBlank` runs after NUL on `move/item` and `move/folder`. UI `moveDestIsBlank` refuses before `onPick`; OK stays enabled so the error is reachable.
- Behavioral tests: REST `FoldersTest` blank dest 400 + adaptor never called; Vitest dialog/shell/reduced-actions/`moveDest`; Playwright `explorer-move-dest-blank.spec.js`.
- Change-class closure: REST + WebUI + product-docs + perc-qa-automation. Copy dest is out of this slice (shared picker still refuses blank on Copy UI).
- No new OS filesystem path construction; dest is a CMS finder path; blank uses `StringUtils.isBlank` / `trim().length === 0`.

**Recommendation: approve. May merge: yes.**
