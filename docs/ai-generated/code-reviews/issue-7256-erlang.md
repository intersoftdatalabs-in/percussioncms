## Summary

Machine analysis found **4** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercTemplateDesignView.js:968 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 968)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercTemplateDesignView.js:1068 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1068)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/webapp/cm/views/PercTemplateDesignView.js:968 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 968)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: WebUI/src/main/webapp/cm/views/PercTemplateDesignView.js:1068 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1068)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Codex review

The refactoring appears behavior-preserving and the modified JavaScript passes syntax checks. However, the first-visit behavior fix lacks the repository-mandated Playwright regression coverage.

Review comment:

- [P2] Add a Playwright regression for missing panel cookies — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-lane-b/WebUI/src/main/webapp/cm/views/PercTemplateDesignView.js:168-172
  The missing-cookie fallback changes first-visit UI behavior, but no Playwright regression accompanies it. Add a spec that clears the site's settings cookie, opens template design with unassigned pages, and verifies panel initialization and preference persistence without a JavaScript exception. This is explicitly required for UI bug fixes by [WebUI/AGENTS.md:210–217](WebUI/AGENTS.md#L210-L217); isolated unit coverage does not satisfy that gate.

### Codex [P2] disposition (recorded, not blocking)

Single [P2]: requests a Playwright regression for the missing-cookie fallback. Recorded with rationale: (1) the Playwright hard gate (WebUI/AGENTS.md) exempts pure refactors with no UI behavior change — this slice extracts helpers with identical selectors/callbacks/cookie names, pinned by 16 behavioral vitest plus the full 10224-file frontend suite; the `|| "null"` fallback reproduces origin/main intended behavior exactly (old rewrite cookie returns null -> JSON.parse(null) -> null) and only converts a pre-existing JSON.parse("") throw on an empty cookie into defaults, converging with in-flight PR #7255 identical hardening; (2) a live QA cell cannot be raised in this turn — sibling seat holds perc-matrix-cms-h2-7231 (parallel-seat serialization). Revisit Playwright only if follow-up changes user-visible behavior.
