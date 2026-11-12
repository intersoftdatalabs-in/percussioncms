# Erlang review — PR #6112 (issue #5902)

- Status: **lgtm_open — HOLD, DO NOT MERGE (human product gate)**
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 19 files (WebUI prod + Vitest + Playwright + product-docs)
- Verdict: `lgtm_open`, merged: false, bugs_found: [] (in-diff)

## Gate

- In-diff bugs: **0**. Recommendation on code alone: approve.
- Merge: **BLOCKED by human product gate, not by code.**

## Machine findings (independent re-run + PR-embedded report agree)

- `mkd-code-review analyze --pack percussion --format markdown --gate advisory
  --diff pr6112.diff` (Ollama dev-coder pool): **2 findings, 0 blocking**,
  both **preexisting** `complexity.cognitive` on
  `WebUI/src/main/ts/app/deepLinks/parseEntryQuery.ts`
  (`parseEntryQuery` cognitive 37, `parseClientPath` cognitive 43).
  Already tracked by residual #6113 — dedupe hit, no new residual filed.

## Manual verification

- All `+` tinymce references are tests/docs asserting removal (zero-prod-fetch
  asserts, legacy-param-ignored cases); no prod stragglers (`HtmlEditorEngine`,
  `htmlEngine` prop, `?engine=` read all deleted).
- `sys_tinymce` → `html` catalog alias untouched (file not in diff), as claimed.
- C1 evidence present (WebUI clean install: surefire 71, vitest 894 files /
  8570 tests, tsc clean, docs smoke 52 pages); C5 present (H2 cell, 20 editor
  specs + golden/login smoke, zero console errors, no new server.log ERROR).
- product-docs checkbox: done (`8.2/admin/content-explorer.md`).
- Checks: all green (CodeQL pass, JS/TS analyze pass, docs smoke pass).
- No unresolved review threads.

## Human gate (why open, not merged)

- Owner product decision on #5874 (2026-11-09): **"Hatch: stays for 8.2,
  removed in 8.3"**; #5902 carries milestone **8.3** while repo main is
  **8.2.0-SNAPSHOT**. QA tracker #5802 (hatch as supported fallback) still OPEN.
- Merging now would implement an 8.3-milestoned removal on the 8.2 line against
  the recorded owner decision. Requires human re-confirmation (retarget to 8.3
  line or explicit 8.2 exception) before merge.
- Hold comment posted on the PR; no APPROVE (approval would signal
  merge-readiness).

## Residuals

- None new (#6113 covers the preexisting complexity rows).
