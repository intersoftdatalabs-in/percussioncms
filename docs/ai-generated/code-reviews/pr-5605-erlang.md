<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5605

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5605
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4599
- Base: origin/main
- Head: b71c21aafc
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/slice-4599`)
- Reviewer: independent Erlang pass (not the implementer persona)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: docker/scripts/hot-deploy-rhythmyx-war-jars.py:377 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `resolve_module_jars` cognitive=21 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

- **0 blocking bugs.** The single in-diff finding is a `complexity.cognitive`
  *suggestion*, not a bug. `resolve_module_jars` already carried the
  utils-specific noscript-shield branch (#5323) before this change; the
  slice adds one sibling `if` for the general m2 fallback and does not
  materially widen the function. The cyclomatic count stays under the
  limit (12 of 15); only the cognitive score is over it, and the peer
  locale slices ship the same shape.
- Correctness points checked by the reviewer:
  - The fallback only accepts the **project version** read from the root
    POM (`<artifactId>core</artifactId>` → `<version>`), so a stale
    release sitting in the local repository is never deployed. That is
    the risk the naive "any SNAPSHOT in m2" version would have carried.
  - A module's `target/` jar still wins over the local repository, so a
    freshly built module is never shadowed by an older local copy.
  - The `utils` noscript-shield path is unchanged: `resolve_utils_from_m2`
    still requires the shield marker, and `deploy()` still rejects a
    `utils` jar without it (`EXIT_MARKER_MISSING`).
  - The `perc-system` sitemap-xml allow-list marker check is unchanged and
    still runs on whichever jar was resolved.
  - When neither the module `target/` nor the local repository has the jar,
    the deploy step stops with `EXIT_JAR_NOT_FOUND` and the error names the
    module path and the `mvnw clean install` command to run.
  - The mail2 extra-jar resolution path is untouched.
- No missing behavioral tests: six new cases in
  `test_hot_deploy_rhythmyx_war_jars.py` cover the fallback, the
  target-wins-over-m2 precedence, stale-release rejection, the
  missing-in-both-places failure, and both `parse_project_version` outcomes.
- No non-portable path/file I/O: the script stays stdlib-only and
  cross-platform (`Path` throughout, no literal separators).
- Gate: **PASS**.

## Independent verification (reviewer, not the implementer)

- `python3 docker/scripts/test_hot_deploy_rhythmyx_war_jars.py` — Ran **32** tests, **OK** (26 before this slice, 6 new).
- Live check on a fresh worktree with no module `target/` jars
  (`~/.grok/worktrees/intersoft-workspace-percussioncms/slice-4599`):
  `resolve_module_jars(repo_root)` returns `rc: 0` and resolves all five
  modules — `perc-system`, `rest`, `sitemanage`, `extensions-workflow`,
  `utils` — from `~/.m2/repository/com/percussion/<artifactId>/8.2.0-SNAPSHOT/`.
  Before this change the same call returned `EXIT_JAR_NOT_FOUND` for
  `extensions-workflow`, which is the exact symptom issue #4599 records.
- Docs: `docker/README.md` records the fallback and the version rule.

Verdict: **LGTM** — independent review, unit tests re-verified.
