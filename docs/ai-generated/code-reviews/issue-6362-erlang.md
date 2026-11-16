# Erlang pre-push review record — issue #6362 (INCOMPLETE: LLM unavailable)

> Status: **BLOCKED — `erlang_llm_unavailable`**. The mkd-code-review CLI ran
> on the branch diff, but the LLM gate could not load on EITHER configured
> route, so this is NOT a clean review and no PR was opened. Machine findings
> are kept below; the human/host retry should re-run the CLI before any PR.

## Attempts (this run, 2026-11-16 UTC)

1. Configured route (`models.openrouter-free.toml`, `--require-llm`,
   `--gate advisory`, `--git-base origin/main`): `EXIT=0` but report carries
   `llm.error` — "required model configuration could not be loaded".
   A process key was sourced from the local mkd config (nonempty), so this
   is a model-configuration load failure, not a missing-key failure.
2. Free local failover (`models.ollama-dev-coder.toml`, same flags; precedent
   #6363 which was accepted via this route): identical `llm.error`.

Both sanitized reports are byte-identical; the sanitized output follows.

## Sanitized CLI report (ollama route; openrouter route identical)

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: required model configuration could not be loaded
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[rule] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Diff under review (for the retry)

- `modules/perc-qa-automation/frontend/tests/editor-codemirror-bundle.spec.js`
  (+49/−1): new static test pinning the `sys_TextArea` code-editor control in
  `system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl`
  to the npm CodeMirror bundle (`/sys_resources/codemirror/codemirror/`),
  asserting both bundle `FileLocation`s present and no legacy per-file or
  TinyMCE-hosted references. Reuses the proven `codemirrorLoaderLines` helper
  and regexes from the neighboring JSP-mirror test.
- Local evidence (no live CMS needed): 3/3 static tests pass; negative
  control (legacy path injected into the XSL) fails the new test as designed.
- `modules/perc-qa-automation` standalone `mvnw clean install`: BUILD SUCCESS
  (no Java tests in module).
