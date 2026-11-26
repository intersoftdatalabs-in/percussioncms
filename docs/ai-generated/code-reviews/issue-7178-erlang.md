## Summary

Machine analysis found **9** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 46 analyzed
- In-diff: 0 finding(s); preexisting: 9
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercFinderView.js:1749 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1749)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercFinderView.js:1751 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1751)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/webapp/cm/views/PercFinderView.js:1749 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1749)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: WebUI/src/main/webapp/cm/views/PercFinderView.js:1751 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1751)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: WebUI/src/main/webapp/cm/widgets/perc_site_map.js:2335 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2335)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/plugins/perc_gadgets_search_criteria_dialog.js:45 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openSearchCriteriaDialog` cognitive=99 (max 15), cyclomatic=42 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: bug

- File: WebUI/src/main/webapp/cm/app/js/legacy/views/PercFinderView.js:1055 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addTransitionButtons` cognitive=73 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: bug

- File: WebUI/src/main/webapp/cm/plugins/perc_gadgets_search_criteria_dialog.js:45 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `openSearchCriteriaDialog` cognitive=99 (max 15), cyclomatic=42 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: bug

- File: WebUI/src/main/webapp/cm/views/PercFinderView.js:1055 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addTransitionButtons` cognitive=73 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Codex review

The autocomplete migration loses selected tags and validates list-editor selections before their values are inserted. It also adds an asset-resolution assertion for a deleted URL without generating its replacement.

Full review comments:

- [P2] Read the selected tag from ui.item.value — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/modules/perc-packages/src/main/resources/Packages/perc.Baseline/SupportFile-rx_resources/js/percTagListControl.js:66-66
  Selecting a tag suggestion now discards it: jQuery UI supplies the selection as `ui.item.value`, not `ui.value`. Consequently, this handler appends `undefined`, producing an empty tag when joined, and returning `false` prevents the widget from inserting the correct value. Use `ui.item.value` to preserve the selected tag.

- [P2] Update the input before validating an autocomplete selection — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/WebUI/src/main/webapp/cm/plugins/PercListEditorWidget.js:203-204
  When a user clicks a suggestion after typing a partial username, `autocompleteselect` fires before jQuery UI assigns the selected value. `updatePlusButton()` therefore validates the partial text and leaves the add button disabled; unlike the retired plugin's `result` event, this event runs before value insertion. Assign `ui.item.value` before updating the button, and apply the same fix to the mirrored copies.

- [P2] Reconcile the autocomplete URL assertion with generated assets — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/modules/perc-qa-automation/frontend/tests/classic-jquery-plugins-npm.spec.js:69-69
  This newly required URL cannot resolve after the patch: all vendored `jquery.autocomplete.js` copies are deleted, and neither `NPM_LIBRARY_MAPPINGS` nor `STANDALONE_NPM_COPIES` provides a replacement at that path. The asset-resolution test will therefore fail against a clean deployment, and `percTagListControl.xsl` still requests the missing script and stylesheet. Either generate the promised compatible assets or remove the obsolete descriptors and update the test to assert the actual jQuery UI URL.
**Disposition of the three [P2] findings (all actioned on the branch before
the PR opened):**

- P2 tag-select `ui.value` → fixed: `percTagListControl.js` now reads
  `ui.item.value`; the vitest multi-value case asserts the joined
  `"red, green, "` insertion end to end.
- P2 validate-before-insert → fixed: all three `PercListEditorWidget.js`
  mirrors assign `inputField.val(ui.item.value)` before
  `updatePlusButton()`; the vitest list-editor case types a partial value
  and asserts the full selection lands in the input.
- P2 autocomplete-URL-without-asset → resolved as suggested (descriptors
  removed): the tag-list XSL drops both autocomplete FileDescriptors and
  keeps only `jquery-ui.min.js`; the Playwright suite asserts 404 for both
  stable autocomplete URLs and the audit/build tests pin the absence.

**Tooltip redesign after the Codex/Codex-adjacent review:** live-cell
probing during QA proved `$.fn.tooltip` on classic pages is Bootstrap's,
not jQuery UI's — Bootstrap 5.3 re-registers its jQuery plugins on
`DOMContentLoaded`, after every synchronous script (including jQuery UI),
so a jQUI-tooltip port could never take effect there. The two tooltip call
sites therefore use the Bootstrap 5 native API (`new bootstrap.Tooltip`,
npm-tracked) instead; the Playwright probe pins `$.fn.tooltip` as
Bootstrap-owned so a future widget-shaped call fails loudly.

No [P0]/[P1] findings. Codex review ran read-only
(`codex review --base origin/main`, no prompt argument) from this checkout.
