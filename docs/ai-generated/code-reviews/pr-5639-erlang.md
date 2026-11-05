# Erlang independent review — PR #5639 (fix(webui): name single-item Explorer stage 403/404/409 failure, #5631)

Status: independent Erlang review, machine gate clean, 0 blocking bugs.
Recommendation: **approve**.

- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/`, engine `mkd-code-review 0.1.18`)
- Scope base: `origin/main`
- Scope head: `review-5639` (`9a9443da39`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
  (Ollama `dev-coder` on `localhost:11434`); gate on **in-diff** bugs only.

## Machine report (verbatim, `mkd-code-review analyze --pack percussion`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1764 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=483 (max 15), cyclomatic=292 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent Erlang notes (all non-blocking)

- The single machine finding is **preexisting** (`dispatchAction`
  complexity at line 1764, untouched by this diff) and therefore does not
  block per the in-diff gate. No new complexity of note is added: the two
  new `try/catch` blocks mirror the already-merged slice-127 takedown
  shape (#5630/#5635) exactly — same `name ?? path ?? id` label fallback
  with `.trim() || "item"`, same no-refresh / selection-intact semantics,
  same rethrow of non-HTTP failures.
- `isApiError` (`WebUI/src/main/ts/api/client.ts:53`) accepts plain
  `{ status: number }` objects, so the vitest rejections
  (`{ status, body: {} }`) exercise the real guard — the 4 new tests are
  behavioral, not token assertions.
- Change-class closure is complete: production paths (Stage +
  Remove-from-Staging single-item), `STAGE_FAILED`/`UNSTAGE_FAILED`
  message keys, 4 vitest tests (192/192 green per build evidence), new
  surface spec `explorer-staging.spec.js` (5/5 H2 green, zero page
  errors), and `product-docs/8.2/admin/content-explorer.md` rows updated.
  Multi-select batch paths already name per-item failures and are
  correctly untouched.

## Gate checklist

- Bugs (in-diff): 0.
- Behavioral tests for new/changed logic: yes (4 vitest + 5 Playwright).
- Change-class closure: complete (code + keys + unit + surface + docs).
- Portable paths: yes — TS string ops only, no filesystem paths.
- Product docs: yes — content-explorer Stage / Remove-from-Staging rows.
- Headers: yes — new spec carries the Intersoft 2026 Apache header.
- Security: no secrets; error text names only the row label + HTTP status.

## Verdict

No material issues. Recommendation: approve. May commit/push: yes.
