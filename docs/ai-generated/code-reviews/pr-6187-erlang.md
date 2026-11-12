# Erlang review — PR #6187 (independent, this-run)

- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/6187
- Title: fix(relationships): unresolvable ids return 403/404 instead of 500 (#6130)
- Head: `7ba44ca917`; Base: `main`; State at review: OPEN, checks green
- Reviewer: Erlang (independent; own-model Work PR, model:muse-spark-1.3-contributor)
- Machine: `mkd-code-review 0.1.21 --pack percussion --format markdown --gate advisory --git-base origin/main --models config/models.ollama-dev-coder.toml` (Ollama dev-coder, localhost:11434), run on PR-head checkout
- Persona: erlang 0.1.1

## Machine report (full)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: 7ba44ca917
- Files: 6 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Independent Erlang verdict — LGTM, merged

- **C1 (build evidence):** present in PR body (sitemanage `clean install` BUILD SUCCESS 4258/0/0/125-skipped; rest BUILD SUCCESS 1833/0/0). Independently reproduced focused suites on PR head: sitemanage `PSRelationshipSummaryServiceTest` 22/22 + `PSExplorerRelationshipRemoveServiceTest` 16/16 green; rest `RelationshipSummaryResourceTest` 11/11 green. Missing-build-evidence: none.
- **C2 (signature-adjacent change):** `summariseFromCataloger` is private; the only caller of the changed `summarise*` surface is `RelationshipSummaryAdaptor` (same module), mapping `Optional.empty()` → 403 and `absence()` NOT_FOUND → 404. `IRelationshipSummaryAdaptor` untouched. Closure complete.
- **Strict gate:** no in-diff bugs; 10 new behavioral tests pin the 708 shape per dimension plus the classifier boundary (`cataloger down` still propagates); no file I/O → no portability surface; no rule files; docs N/A justified (restores the already-documented 403/404 contract).
- One judgment call (non-blocking suggestion, not filed): the `isMissing` message-substring heuristic could over-match an infra error whose text contains "not found" — accepted tradeoff, documented in code, boundary-tested, unmatched failures still propagate as 5xx.
- **Checks:** green — Analyze (java-kotlin) SUCCESS, CodeQL SUCCESS, js skipped N/A.
- **Residuals:** machine reported 0 preexisting rows in touched files → no residual issues filed (cap 12 this run untouched by this PR).
- **Close-gate:** body opens with `Fixes #6130` → squash-merge auto-closes #6130.
- **Recommendation: approve. May merge: yes.**
