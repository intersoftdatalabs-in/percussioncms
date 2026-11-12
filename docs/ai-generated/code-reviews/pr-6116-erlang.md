# Erlang review — PR #6116 (issue #6070)

- Status: **LGTM — approved, squash-merged** (after #6114; mergeable re-checked)
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 15 files (system + sitemanage + WebUI +
  qa-automation + product-docs)
- Verdict: `lgtm_merged`, merged: true, bugs_found: [] (in-diff genuine)

## Gate

- In-diff genuine bugs: **0**. Recommendation: approve.

## Machine findings (independent re-run + PR-embedded report)

- PR-embedded: 3 findings, 1 in-diff bug (`paths.hardcoded_sep` on the new
  parity test) — adjudicated FALSE POSITIVE: the flagged line is the H2 JDBC
  URL (`jdbc:h2:mem:…`), not an OS path. Verified **suppressed inline** with
  `// mkd-code-review[paths.hardcoded_sep]` citing #6070 + precedent, on the
  sink lines. Per persona spec, documented suppressions are gate-exempt.
- Independent re-run: **2 findings, 0 blocking** (suppression filtered the FP):
  - `putResultPage` cognitive 31 (preexisting) → already tracked by #6115
    (dedupe hit).
  - `onSaveResultPage` cognitive 15/16 (preexisting, suggestion) → no prior
    issue → new p8 residual #6123 filed.
- 2 preexisting rows; out-of-diff; none block.

## Manual verification (security-sensitive core)

- `normalizeLegacyStylesheetUri`: `file:` values funnel through
  `tryImportedFileUri` → `tryImportedStylesheetUri` → `requireSafeStylesheetUri`;
  every parse failure falls back to the fail-closed validator. Absolute /
  traversal / cloud values still 400.
- Execute-time containment verified in `PSPipelineXslMerge.readSandboxFile`
  (`appDir.startsWith(root)`, `target.startsWith(appDir)` post-normalize).
- `getStoreBaseDir()` is a `default` interface method returning null
  (source/binary compatible); sole implementor overrides; null root keeps
  bundled-only behavior.
- Byte-identical parity test asserts every field value incl. `& < > " '`,
  CJK, nulls in rendered HTML; missing sheets still fail closed.
- C1 evidence present (system 3151/0, sitemanage 4231/0, rest prerequisite
  rebuild 1826/0, tsc + 71 vitest, docs smoke 52 pages); C5 present (H2 cell,
  legacy-bind + regression specs pass, 0 console errors, 0 server.log ERROR).
- product-docs checkbox: done. Checks: all green. No unresolved threads.
- Overlap with #6114 (shared hot files, disjoint regions per body) — merged
  after #6114 with mergeable re-check; conflicts would have stopped the merge
  for follow-up rebase.

## Residuals

- #6123 (p8, `onSaveResultPage` complexity decomposition).
