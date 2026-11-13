## Summary

Machine analysis found **33** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 50 analyzed
- In-diff: 1 finding(s); preexisting: 32
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:185 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 185)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:269 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 269)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:287 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 287)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:299 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 299)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:376 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 376)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:378 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 378)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:555 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 555)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:223 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 223)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:229 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 229)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:231 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 231)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:237 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 237)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:239 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 239)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 13 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:258 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 258)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 14 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:262 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 262)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 15 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:282 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 282)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 16 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:452 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 452)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 17 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:454 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 454)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 18 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:510 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 510)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 19 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:539 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 539)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 20 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:577 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 577)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 21 -- Severity: bug

- File: system/src/main/java/com/percussion/cms/handlers/PSContentEditorHandler.java:1241 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1241)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 22 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSField.java:940 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 940)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 23 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/PSField.java:942 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 942)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 24 -- Severity: bug

- File: system/src/main/java/com/percussion/install/PSUpgradePluginRelationship.java:495 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 495)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 25 -- Severity: bug

- File: system/src/main/java/com/percussion/system/utils/PSCms.java:453 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 453)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 26 -- Severity: bug

- File: system/src/test/java/com/percussion/design/objectstore/PSControlDependencyMapTest.java:302 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 302)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 27 -- Severity: bug

- File: system/src/main/java/com/percussion/design/objectstore/server/PSServerXmlObjectStore.java:2414 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getContentEditorSharedDef` cognitive=71 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 28 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentRelationDependencyHandler.java:274 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=21 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 29 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSContentTypeDependencyHandler.java:916 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveItemDef` cognitive=26 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 30 -- Severity: suggestion

- File: system/src/main/java/com/percussion/design/objectstore/PSDisplayMapper.java:511 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `mergeMapper` cognitive=20 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 31 -- Severity: suggestion

- File: system/src/main/java/com/percussion/design/objectstore/PSDisplayMapper.java:924 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `demergeMapper` cognitive=19 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 32 -- Severity: suggestion

- File: system/src/main/java/com/percussion/design/objectstore/server/PSServerXmlObjectStore.java:2652 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveContentEditorSharedDefFile` cognitive=27 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 33 -- Severity: suggestion

- File: system/src/test/java/com/percussion/data/PSDataComparisonTest.java:97 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `testNumericComparisons` cognitive=16 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

