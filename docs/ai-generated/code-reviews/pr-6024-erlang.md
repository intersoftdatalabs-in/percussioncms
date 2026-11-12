# Erlang review — PR #6024: fix(WebUI): developer shell catalog search/filter increment (#5995)

- Persona: erlang 0.1.1 (`~/.local/share/mkd/agents/erlang`)
- Mode: advisory leftover review — own-model-only PR (model:muse-spark-1.3-contributor). HARD BAN applies: no APPROVE, no merge.
- Scope Base: origin/main, Head: 396f881e7e, Files: 20 analyzed
- Verdict: **LGTM (advisory)** — no in-diff bugs. No approve/merge per own-model ban.
- Note: `llm.error` below is an Ollama-endpoint tooling failure (pool points at
  `http://localhost:11434/v1`, which is not served; only `/api/tags` responds),
  so the LLM stage was skipped and this review is machine-findings + manual
  static review (fail-open, documented here — not a code defect, not blocking).

## FULL CLI report (mkd-code-review 0.1.21, pack percussion, --gate advisory)

## Summary

Machine analysis found **4** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 20 analyzed
- In-diff: 3 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: endpoint http://localhost:11434/v1 unavailable (health: error sending request for url (http://localhost:11434/v1/models)); is Ollama serving?
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/CommunitiesPanel.tsx:39 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `CommunitiesPanel` cognitive=16 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/DisplayFormatsPanel.tsx:42 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DisplayFormatsPanel` cognitive=18 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/WorkflowsPanel.tsx:22 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `WorkflowsPanel` cognitive=22 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Erlang interpretation (Erlang Shen, he/him)

- **Issue 1 (`llm.error`) is tooling, not code.** No `bugs_found` entry. The CLI's
  own `request-changes` recommendation rests solely on this tooling row; with the
  LLM stage unavailable I ran the manual static path below instead of
  rubber-stamping or blocking on infrastructure.
- **Issues 2–4 are complexity suggestions, never blocking.** Filed as tech-debt
  residuals (capped at 3): #6026 (CommunitiesPanel), #6027 (DisplayFormatsPanel),
  #6028 (WorkflowsPanel). All carry `tech-debt` + `operator:night-issue-prs` +
  `model:muse-spark-1.3-contributor` + `p8`. Deduped first — no prior open
  residual covered these files.

### Manual static review (LLM fail-open path)

- `CatalogSearchFilter.tsx` (new): pure `matchesCatalogQuery` — trims,
  lowercases, tolerates null/undefined/empty, numeric-by-string-form; empty query
  matches all so clearing restores the server list without refetch. Controlled
  `type="search"` input, `label htmlFor` wired, no fetch, no mutation. Copyright
  header correct (2026 Intersoft). Nit only: JSDoc uses Java-style
  `{@code query}` in a TS file — cosmetic, not filed.
- All five panels (Communities, Roles, Slots, DisplayFormats, Workflows): identical
  closure pattern — `useState("")` query, `useMemo` filtered rows, search box +
  named `*-search-empty` status, rows switched from `sorted`/`catalog` to the
  filtered memo. Loading / error / genuinely-empty early returns verified intact
  (e.g. WorkflowsPanel still returns `WF_LOADING` / `WF_ERROR` / `WF_EMPTY`
  before the search box renders) — no empty-vs-loading confusion.
- Tests are behavioral, not token-grep: `matchesCatalogQuery` edge cases
  (blank/null/undefined/numeric/no-match), input binding + clear-restores-empty
  via `fireEvent`, panel tests mock the API and render. Pre-push bar met.
- Change-class closure complete for a WebUI increment: shared component + 5 panel
  wirings + `messages.ts` keys (established `perc.ui.developer@` inline-default
  pattern, no new locale surface) + per-panel Vitest + Playwright surface spec
  with QA-mode run instructions + `product-docs/8.2/admin/developer-*.md` "Filter
  the catalog" sections on all five pages.
- No file I/O / path joins, no secrets, no agent-rule files, no shared Spring
  context impact. Cross-platform: pure string/TSX work, nothing OS-specific.

## Verdict

**LGTM (advisory).** `bugs_found`: []. Residuals: #6026, #6027, #6028.
No APPROVE posted and no merge attempted — own-model-only PR hard ban.
