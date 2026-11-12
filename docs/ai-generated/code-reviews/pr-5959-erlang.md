# Erlang review — PR #5959 (issue #5942) — OUTAGE MODE

- Status: reviewed locally, **no forge action taken** (GitHub API 401; zero `gh` calls per outage constraint; no comments posted, no APPROVE, no merge).
- Persona: erlang 0.1.1 (Scope Base origin/main / Head fix/issue-5942-explorer-item-takedown set; machine findings below).
- Engine: mkd-code-review 0.1.21, pack percussion, gate advisory; Ollama dev-coder reachable (LLM stage short-circuits on zero active findings).
- Run: night-issue-prs outage-run 2026-11-10 (GitHub API down: no comments posted, no APPROVE/merge — all branches own-model)
- Branch: `fix/issue-5942-explorer-item-takedown`
- Verdict: **LGTM** (report-only; MUST NOT approve/merge — own-model branch)
- Manual gate notes: 1-line-class prod change (HTTP 400 joins 403/404/409 named-failure path) + behavioral vitest asserting kind/messageKey/messageText/no-refresh + Playwright spec + product-docs. Residual: preexisting complexity.cognitive on dispatchAction (WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1767, cognitive 489) — out-of-diff, p8, NOT filed (API down; see scratch/erlang-review.json).

---
## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1767 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=489 (max 15), cyclomatic=298 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

