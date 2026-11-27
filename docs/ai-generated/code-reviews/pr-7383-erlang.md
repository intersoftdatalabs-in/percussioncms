# Erlang review — PR #7383 (re-review after erlang-fix)

> Independent Erlang re-review (persona erlang 0.1.1), commit `931cebae15`,
> scope `origin/main...HEAD` (90 files). Prior BLOCK (partial-commit B1–B5 on
> `faa1dba781`) resolved by fix commit `931cebae15` (signed, forge
> Nate Chadwick). Worktree == pushed head; the prior Codex worktree-scope
> caveat is resolved.

**Status: LGTM — approve. May merge: yes.**

## Summary

Machine analysis found **2** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD (`931cebae15`)
- Files: 25 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/models/PercPageModel.js:674 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 674)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open
- Erlang adjudication: **false positive.** Line 674 is `rootPath + "/" +
  pageId + "?timestamp=" + ...` — a browser frame `src` **URL** assembly,
  not a filesystem path. URL paths correctly use `/` (PATTERNS.md
  false-positive guard). Out of scope here; no action.

### Issue 2 -- Severity: bug

- File: WebUI/src/main/webapp/cm/models/PercPageModel.js:674 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 674)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open
- Erlang adjudication: **false positive** — same URL-assembly line as Issue 1
  (twin copy). No action.

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Fix verification (B1–B5 from the prior BLOCK)

Fix commit `931cebae15` committed the 48-file remainder and re-ran all gates
from the pushed state (evidence: `scratch/erlang-fix-7383.json`):

- **B1** — all JSP/js/html consumers retargeted to npm-served `jquery-ui.css`.
- **B2** — archive metadata + 9 XSL FileDescriptors + component-package.json +
  guard test retargeted; `WidgetThemeUrlParityTest` 4/4.
- **B3** — slice-5A pin updated to MIGRATED.
- **B4** — `build-legacy-bundles.cjs` npm overlay serves the new URL + 11
  images; baseline images refreshed.
- **B5** — gates re-run from pushed state: WebUI BUILD SUCCESS (surefire
  84/84, vitest 116/116), perc-packages 368/368, sitemanage 4517 run /
  0 failures, system 3266 run / 0 failures, surface
  `classic-jquery-ui-theme-npm` 2/2, no new warnings.

## Codex review

`codex review --base origin/main` (read-only, run from pushed head —
worktree == `931cebae15`, so the prior run's worktree-scope caveat no longer
applies): *"No actionable regressions were identified in the diff."* No
`[P0]`/`[P1]`. Full output: `scratch/codex-review-7383.md`.

## Residuals

None owed. The only machine rows are the two preexisting
`paths.hardcoded_sep` FPs above (URL assembly, not filesystem paths) —
not genuine debt, so no `Residual:` issues filed. `residual_issue_urls: []`.
`bugs_found` (in-diff blockers): 0.

## Checks (one snapshot)

Detect language changes / QA wiring / signed-commits (ubuntu+windows) /
Analyze java-kotlin + javascript-typescript / CodeQL — all SUCCESS; H2
qa-up + Playwright (live) SKIPPED per path filter. No red.

## Recommendation

**Approve + squash-merge.** No in-diff bugs, no genuine preexisting debt in
touched files, Codex clean with no scope caveat, checks green, fix evidence
complete from the pushed state.
