# Erlang review — PR #6193 (night-issue-prs, REVIEW ONLY)

> Independent Erlang review. Own-model PR (model:muse-spark-1.3-contributor +
> operator:opencode) — **REVIEW ONLY. No APPROVE, no merge, no LGTM review
> posted.** Host decides next step.

- PR: intersoftdatalabs-in/percussioncms#6193 — fix(WebUI): neutralize
  spreadsheet formula injection in CSV exports (#6175)
- Head: `a7de7911f11b9c0eb71c3b6bee988c965098517a`
  (`fix/issue-6175-csv-formula-injection`)
- Base: `origin/main`
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md` + `PATTERNS.md`)
- Status: machine gate clean, manual review clean

## Verdict: LGTM-REVIEW-ONLY

No in-diff bugs. No blockers. Recommendation would be **approve**; withheld
per own-model constraint (host merges or routes).

## Machine gate (mkd-code-review)

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

- Result: **0 findings, 0 blocking bugs**, recommendation `approve`,
  `May commit/push: yes`. Persona `erlang 0.1.1`, 9 files analyzed,
  Ollama `dev-coder` backend reachable.
- Full CLI markdown appended at the bottom of this report (pre-push local
  code review artifact convention).

## What the diff does (7 files, +210/−9)

1. **New `WebUI/src/main/ts/util/csvFormula.ts`** — shared
   `neutralizeCsvFormulaCell()` + `CSV_FORMULA_TRIGGER_CHARS`. Any field
   opening with `= + - @ TAB CR` gets a single-quote text marker, per OWASP
   CSV-injection guidance. Correct 2026 Intersoft Apache header.
2. **`contentExplorer/detailCsvExport.ts`** — `csvEscapeCell` neutralizes
   *before* RFC 4180 quoting (correct order; marker survives quoting).
   Headers also flow through `csvEscapeCell`, so hostile column names are
   covered too.
3. **`publishing/logsExport.ts`** — same neutralize-before-quote in
   `csvCell`. `cell()` stringifies before `csvCell`, so numbers/unknowns
   are covered.
4. **Tests** — new `csvFormula.test.ts` (trigger leads, byte-identical
   non-triggers, exact trigger-set assertion) plus hostile-path cases
   through `csvEscapeCell`/`buildDetailCsv` and `buildPublishingLogsCsv`
   (including the quoted-`HYPERLINK` marker-survival case). Behavioral,
   not token-grepping. Gate requirement met.
5. **`docs/ai-generated/code-reviews/issue-6175-erlang.md`** — author's own
   machine report (0 findings). Consistent with this independent run.

## Erlang checks

- **In-diff bugs:** none. `charAt(0)` guarded by length check; empty string
  passthrough tested; already-quoted `'=1+1` left unchanged (no double
  marking).
- **Change-class closure:** complete. Grepped `WebUI/src/main/ts` for other
  CSV export sites (`csvEscape|toCSV|buildCsv|exportCsv|csvCell`) — only
  these two exist, both wired to the shared util. No server-side Java CSV
  export in this slice; no Playwright companion needed (pure-function
  change, Vitest is the right layer).
- **Cross-platform path/I-O:** N/A — pure string ops, no filesystem paths.
- **Leading-space bypass (`"  =1+1"` untouched):** documented trade-off,
  matches standard first-char mitigation scope; Excel/Sheets formula-bar
  behavior for space-prefixed cells is the accepted residual, not a
  blocker.
- **Residuals:** no qualifying preexisting/out-of-diff bug or complexity
  rows in PR-touched files. `residual_issue_urls: []`, `bugs_found: []`.

## Constraints honored

- Did not implement, edit, approve, merge, or post any review to the PR.
- Report file only; uncommitted in the worktree for the host to use.

---

## Appendix — full CLI markdown (machine gate output)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
