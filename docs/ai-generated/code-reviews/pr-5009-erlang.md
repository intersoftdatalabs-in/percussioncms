<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5009

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5009
- Base: origin/main (`a6f548cd531d5ad72cfc8d8c32b22e9aa0bf593d`)
- Head: 2f7baa9e213109671f4d05fe6e5c27aee0fc4f31
- Reviewed: published PR head (worktree `.kilo/worktrees/pr-5009-erlang-fix`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1691 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=338 (max 15), cyclomatic=209 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSExitDefDependencyHandler.java:109 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getDependencyFiles` cognitive=19 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSExitDefDependencyHandler.java:176 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=19 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Erlang judgment (peer)

Approve. Prior blocking findings on `2f7baa9e` are closed. Do not merge on this snapshot: CodeQL `Analyze (java-kotlin)` and `Analyze (javascript-typescript)` were still in progress. Preexisting complexity rows are out of diff and do not block.

`PSCoreItem.setTextField` now clears before `addValue` only when `PSItemField.isMultiValue()` is true. Parent `no_externalurl` stays `isMultiValue` false, so `PSItemField.addValue` already replaces the single value. The extra clear is no longer the claimed fix. Case-insensitive lookup covers `no_externalurl` vs `no_externalUrl` (`PSCoreItemSetTextFieldTest.setTextFieldMatchesExternalUrlIgnoreCase`).

GET section omission is the read path, not another clear on write. `PSManagedNavService.getNavonProperties` used `hasProperty(name)` then `getProperty("rx:" + name)`, which misses assembly name `rx:no_externalUrl` (`PSNavonNodeInvocationHandler`). `readNavonProperty` resolves the `rx:` name and a case-insensitive property scan. `PSManagedNavServiceReadNavonPropertyTest` covers that name, a blank `;` starter, and a multi-value blank-then-URL. `PSSiteSection.getExternalLinkUrl` is a `String` with `@XmlElement`, and `PSSiteSectionExternalLinkUrlJsonTest` asserts the sitemanage mapper emits `externalLinkUrl` (an `Optional` getter was dropped by `NON_NULL` / the JAXB introspector). `FolderAdaptor` passes that string through without `ApiUtils.orNull`.

Suggestion, not a gate: `firstNonBlankToken` splits any `*externalurl*` value on `;`. A single URL that itself contains `;` is truncated. That matches a `;`-delimited column, not a general URL parser.

> Co-Authored by Grok Build 1.0.46 using grok-4.6 with agent night-issue-prs-erlang.
