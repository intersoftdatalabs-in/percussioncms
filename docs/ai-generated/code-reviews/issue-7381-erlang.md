## Summary

Machine analysis found **2** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 22 analyzed
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

### Issue 2 -- Severity: bug

- File: WebUI/src/main/webapp/cm/models/PercPageModel.js:674 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 674)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Codex review (final, after P2 mitigation)

No actionable regressions were identified in the diff. Both focused frontend test files passed (116 tests); full Maven and live-CMS verification were not performed.

### Codex review (round 1 — mitigated on-branch)

- [P2] Widget-control descriptors still loading the retired URL
  (`imageAutoListWidgetControl.xsl`, `pageAutoListWidgetControl.xsl`,
  `categoryListWidgetControl.xsl`, `resourceAutoListWidgetControl.xsl`,
  `tagListWidgetControl.xsl` + `sys__UserDependency` twins,
  `perc.widget.jqueryUI` `component-package.json` defaultValue,
  `sys_Templates.xsl` sys_CalendarSimple descriptor, `contentEdit.xsl`
  link, `percSystem.xml` `perc-jquery-ui-style` path,
  `perc.baseWidgets` web_resources payload copy,
  `war/models/PercPageModel.js` mirror). The patch deleted the WAR file
  those descriptors resolve, so editors would 404 the theme.
- Mitigation (same branch, before PR): all nine widget XSL FileDescriptors
  + names retargeted to `themes/smoothness/jquery-ui.css`; the
  web_resources payload copy replaced with the byte-identical npm 1.14
  dist at the corrected filename (+ 11 Baseline images refreshed to the
  npm renders); `psx_archiveInfo.xml` / `psx_archiveManifest.xml` keys,
  paths and display names renamed; guard-test attribution entry retargeted
  (`OpenJS Foundation`); `sys_Templates.xsl`, `contentEdit.xsl`,
  `percSystem.xml`, `component-package.json`, war mirror and dead manual
  testing pages retargeted; new `WidgetThemeUrlParityTest` pins the
  payload tree (no lie filename, npm banner, image parity, archive
  consistency). Round-2 Codex: no actionable regressions.
