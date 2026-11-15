## Summary

Machine analysis found **2** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: modules/perc-packages/src/main/java/com/percussion/packages/gadgetxml/PSGadgetRegistryCompiler.java:173 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 173)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/java/com/percussion/webui/gadget/servlets/GadgetRegistry.java:361
- Rule: `llm`
- Tool: `llm`
- Description: getGadgetNameKey() calls loadGadgetNameKeyMap() on every invocation, which opens the classpath resource and re-parses the JSON each time. Callers resolving keys for many gadgets (e.g., rendering a dashboard of N gadgets) will perform N classpath reads plus N JSON parses, which is avoidable I/O for a static catalog.
- Suggestion: Cache the parsed id→nameKey map in a static AtomicReference (mirroring the existing type-map caching) and have getGadgetNameKey read from the cached map instead of re-loading per call.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
