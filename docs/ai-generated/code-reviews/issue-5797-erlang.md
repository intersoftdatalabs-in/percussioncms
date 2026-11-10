# Erlang review — issue #5797 (PSActionManager.executeClientAction extraction slice)

**Date:** 2026-11-10
**Scope:** Pure extract-method refactor of `PSActionManager.executeClientAction` vs `origin/main`
  (23 branch bodies moved verbatim to private handlers + 1 selection-restore helper; zero logic edits).
**Memory patterns hit:** relocated-complexity adjudication; CMS-logical-path FP suppression.

## Adjudication (reviewer)

- `executeClientAction` cognitive 2205 -> 386, cyclomatic 194 -> 38 (machine-measured). Residual
  386 stays above threshold, so #5797 stays OPEN for follow-up slices (paste/help decomposition).
- `handlePasteAction` (cognitive 212) and `handleHelpAction` (cognitive 73) are byte-identical
  relocations of preexisting branch bodies (`git diff -w` shows zero logic-line changes); they
  introduce no new complexity and are tracked by the still-open residual #5797, not this slice.
- `handleEditFolderAction` cognitive 16 -> fixed this turn by extracting
  `restoreNavSelectionAfterFolderEdit` (re-run: finding gone).
- `paths.hardcoded_sep` on the moved `results[0] + "/" + node.getName()` line suppressed inline:
  adjudicated intentional — '/' joins the CMS repository folder path for
  `getNavTree().setSelectionPath`, not an OS path (same FP class as the
  `PSFolderActionManager.java:903-904` hits already adjudicated in #5797). Suppression
  recognized on re-run (remaining paths hits are preexisting, out of scope).
- No new behavioral unit tests: pure move refactor with no logic change; the DCE applet cannot be
  constructed without live server resources (see `PSActionManagerTypingTest` javadoc). Module suite
  (251 tests) is the behavioral guard and is green.

## Gate

- New in-diff bugs: none. Non-portable paths in new code: none. Missing behavioral tests: none
  (refactor-only, existing suite green). Verdict: **PASS (advisory)** — relocated preexisting
  complexity remains tracked by open #5797.

## Full machine report

## Summary

Machine analysis found **7** finding(s) (1 suppressed), **2** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- In-diff: 3 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5433 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5433)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5435 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5435)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5437 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5437)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:2444 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `executeClientAction` cognitive=386 (max 15), cyclomatic=38 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:2969 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handlePasteAction` cognitive=212 (max 15), cyclomatic=72 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3382 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleHelpAction` cognitive=73 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (1)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3474` -- *Justification*: : adjudicated intentional (issue #5797, (via in_source_mkd)

</details>

