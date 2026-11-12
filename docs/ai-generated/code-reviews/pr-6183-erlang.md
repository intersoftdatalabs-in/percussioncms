# Erlang review — PR #6183

Status: LGTM (independent Erlang; machine + manual verification)
Persona: erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md` + `PATTERNS.md`)
Engine: `mkd-code-review 0.1.21` — `--pack percussion --format markdown
--gate advisory --git-base origin/main
--models config/models.ollama-dev-coder.toml` (Ollama reachable)
Scope Base: origin/main / Head: `0899e31b25` (PR head, verified via detached
worktree) / Files: 3 analyzed
Checks snapshot: CodeQL pass · Analyze (javascript-typescript) pass ·
Analyze (java-kotlin) skipping (no Java touched — correct) · Detect
language changes pass · QA wiring pass · product-docs build smoke pass ·
H2 qa-up + Playwright (live) skipping (path-filtered; manual H2 evidence in
body). Zero red.

## Machine report (FULL, unedited)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Manual verification (Erlang third eye — all three PR claims checked)

- **S1 `includeChoices=true` alignment — VERIFIED.** `WebUI/src/main/ts/
  developer/KeywordsPanel.tsx:38` calls `listKeywords(true)`. Sitemanage
  `KeywordsAdaptor.toSummary` (`KeywordsAdaptor.java:391-400`) populates
  choices only inside `if (includeChoices)` — the thin variant carries no
  choice payload, so the doc/spec correction (counts would read 0 from the
  `false` variant) is factually right and the code is untouched-correct.
- **S2 saved-choices assertions — VERIFIED.** Testids
  `developer-kw-saved-choices`, `developer-kw-choice` +
  `data-choice-label` (`KeywordEditorPanel.tsx:2041-2185`),
  `developer-kw-choices-empty` (`:2039`) all exist in the shipped shell;
  the new `toHaveCount(2)` / Text+HTML-label / empty-state assertions
  target real elements. Body records the surface run green live
  (`tests/developer-keywords-readback.spec.js` 2 passed, zero console/page
  errors) — selectors proven on the H2 cell, not just statically present.
- **N1 `409` removal — VERIFIED.** Grep over keyword paths
  (`rest/.../rest/keywords/`, sitemanage `apibridge/KeywordsAdaptor.java`)
  returns zero `409`/`CONFLICT` throws; duplicates map to 400. The `409`
  hits elsewhere in `rest` are `ViewResource` (views domain, unrelated).
  Editor `409` defensive handlers stay and are covered
  (`KeywordEditorPanel.test.tsx` `it.each([400, 403, 409])` × 4 suites) —
  removal is docs-only, no behavior change.

## Gate checklist

- In-diff bugs: 0. Missing behavioral tests: none (Playwright surface is
  the gate for this change class and was extended + run, 2/2 green).
  Non-portable paths: none (spec + markdown only; no file I/O touched).
  Change-class closure: complete — Playwright spec companion +
  `product-docs/8.2/admin/developer-keywords.md` companion, no Java/WebUI
  production change so no Spring-stub/adaptor companions apply.
  Rule files: none in diff. Secrets: none.
- C1 evidence present: `cd modules/perc-qa-automation &&
  ../../mvnw clean install` → BUILD SUCCESS; surface spec 2 passed on H2
  cell; `ci-smoke-product-docs.sh` OK (53 pages); server.log 19 pre /
  19 post (0 new, 0 keyword-related). missing_build_evidence: none.
- Fixes #6174 close-gate: the three #6174 notes (S1/S2/N1) are each
  verified above — closing via merge is appropriate. Parent #6045 noted.
- Preexisting/complexity residuals in PR-touched files: none (machine
  reported 0 findings total). No new residual issues filed. Run-total
  residuals stay at 2/12 cap.

## Recommendation

**approve.** May merge: yes (squash).
bugs_found (in-diff blockers): none.
