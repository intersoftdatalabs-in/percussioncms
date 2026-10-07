<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5324

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5324
- Base: origin/main
- Head: b9d3030ca14415949b0f91290fe060fe9f0b89ef
- Files analyzed: 5
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **6** finding(s), **2** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: docker/scripts/perc-devctl.py:559 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 559)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: docker/scripts/perc-devctl.py:1917 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1917)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:303
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The script does not handle the case where the 'utils' jar is missing the 'perc-noscript' shield.
- Suggestion: Add a check to ensure that the 'utils' jar contains the 'perc-noscript' shield before copying it. If not, log an error and exit.
- Status: open

### Issue 4 -- Severity: bug

- File: docker/scripts/perc-devctl.py:591
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The documentation for the '--qa-deploy-war-jars' command does not mention the jsoup 'noscript' shield.
- Suggestion: Update the documentation to include information about the jsoup 'noscript' shield and how it is handled by the script.
- Status: open

### Issue 5 -- Severity: suggestion

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:489 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `_remove_artifact_jars` cognitive=23 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:721 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `deploy` cognitive=19 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

Independent read of the diff (not the author). `qa-deploy-war-jars` copies a `utils` SNAPSHOT only when `PSHtmlUtils` contains `perc-noscript`. A target jar that lacks the marker falls back to `~/.m2/repository/com/percussion/utils`; if neither jar has the marker, deploy returns `EXIT_MARKER_MISSING` and does not copy. Removal of prior utils jars is limited to `utils-*-SNAPSHOT.jar`, so AWS SDK `utils-2.50.2.jar` and `utils-lite-*.jar` stay. Tests cover the missing shield, the m2 fallback, the extra docker cp, and the AWS names that must not be deleted. Paths use `pathlib`. No Maven sources and no agent rule files.

The CLI `request-changes` is not an in-diff bug. Scope says in-diff findings 0. The two `secrets.heuristic` rows on `perc-devctl.py` are preexisting and outside this diff. The Ollama row at `hot-deploy-rhythmyx-war-jars.py:303` is false: that line is `jar_has_noscript_shield`, and `resolve_module_jars` / `deploy` already refuse a utils jar without the marker. The Ollama row at `perc-devctl.py:591` is false: the `--qa-deploy-war-jars` help and `docker/README.md` both name the noscript shield and `#5323`.

`python-build-scripts` is red on this SHA because `scripts/test_verify_no_bare_ipserrors.py` flags `SlotRelationshipAdaptor.java`. That file is identical to `origin/main` and is not in this diff. It is already tracked as #4881. It is not a required status check on `main`. It does not block this PR.

Recommendation: approve. May commit/push: yes.
