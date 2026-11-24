## Summary

Machine analysis found **1** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/controlKinds.ts:70 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `classifyEditorControl` cognitive=25 (max 15), cyclomatic=41 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Codex review (LLM reviewer, after static gate clean)

_Mitigation applied before PR: [P1] fixed in this branch — `addTextCleanupExtension` now falls back to the legacy `sys_tinymce` ControlMeta when the renamed control has none, and the test fake is name-aware (returns metadata only for the legacy alias) so the golden itemDef test proves the fallback. No [P0]/[P2]/[P3] findings._

The renamed control is emitted without corresponding server metadata, breaking widget generation on installations where the previous control lookup worked.

Review comment:

- [P1] Supply server metadata for the renamed rich-text control — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/projects/sitemanage/src/main/java/com/percussion/widgetbuilder/utils/xform/PSContentTypeFileTransformer.java:71-71
  On an existing installation that still provides `sys_tinymce` metadata, creating a widget with a `RICH_TEXT` field now fails: `addTextCleanupExtension()` passes this new name to `PSSystemControlManager`, which finds no matching `ControlMeta`, then dereferences null via `ctrlMeta.getDependencies()`. Neither the React mapping nor `htmlControls` participates in that lookup. The test fake masks this by returning the same metadata for every name. Supply the renamed server metadata, or remove this metadata dependency while preserving cleanup behavior, before emitting the new name.
