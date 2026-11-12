# Erlang review — issue #6021 (work sub-agent adjudication addendum)

Tool gate: **advisory** — Blocking bugs: 9 (all in-diff rows adjudicated below), **May commit/push: yes**.

## Work-agent adjudication of the 9 in-diff rows (2026-11-11)

All 9 in-diff rows share rule `paths.hardcoded_sep`. Each flagged line was
inspected verbatim; every one is an exception-message string literal (escaped
quotes only, no `/` or `\\` filesystem joins) or a Java annotation line.
No filesystem path is constructed on any flagged line. The one genuine
path join the tool surfaced in-diff (PSBeanProperties default file location)
was fixed in this branch via NIO `Path.resolve().toFile()`.

Flagged lines (branch tip):
- `PSConfigUtils.java:185`: `+ "\" property must be a \"Collection\" or a String type.");` — FP, message/annotation text only.
- `PSContentListSetter.java:131`: `"The type of property \"" + EXPANDER_PARAMS + "\" must be a Map.");` — FP, message/annotation text only.
- `PSContentListSetter.java:142`: `throw new PSConfigException("The type of property \"" + GEN_PARAMS + "\" must be a Map.");` — FP, message/annotation text only.
- `PSContentTypeSetter.java:364`: `"The type of \"" + FIELDS + "\" property (in PSContentTypeSetter) must be Map");` — FP, message/annotation text only.
- `PSLocationSchemeSetter.java:202`: `"\"" + GENERATOR_PARAMS + "\" property must be defined by pvalues, as a list of pairs.");` — FP, message/annotation text only.
- `PSSimplePropertySetter.java:343`: `throw new PSConfigException("The type of property \"" + propName + "\" must be Map.");` — FP, message/annotation text only.
- `PSSiteSetter.java:235`: `"The type of property \"" + VARIABLES + "\" of the Site Variable Setter must be List.");` — FP, message/annotation text only.
- `PSTemplateSetter.java:245`: `throw new PSConfigException("\"" + BINDING_SEQ + "\" property must be \"List\" type.");` — FP, message/annotation text only.
- `PSConfigMapper.java:343`: `@SuppressWarnings({"unchecked", "rawtypes"})` — FP, message/annotation text only.

## Full tool report (mkd-code-review, advisory)

```markdown
## Summary

Machine analysis found **105** finding(s), **9** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 33 analyzed
- In-diff: 9 finding(s); preexisting: 95
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 9
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:78 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 78)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:81 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 81)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:84 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 84)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:125 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 125)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:139 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 139)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:140 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 140)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:145 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 145)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:184 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 184)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:583 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 583)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMerger.java:196 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 196)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigService.java:316 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 316)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigService.java:558 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 558)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 13 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:185 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 185)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 14 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:269 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 269)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 15 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:287 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 287)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 16 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:299 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 299)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 17 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:376 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 376)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 18 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:378 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 378)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 19 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigUtils.java:556 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 556)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 20 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentListSetter.java:131 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 131)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 21 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentListSetter.java:142 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 142)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 22 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentListSetter.java:155 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 155)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 23 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:223 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 223)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 24 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:229 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 229)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 25 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:231 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 231)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 26 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:237 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 237)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 27 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:239 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 239)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 28 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:258 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 258)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 29 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:262 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 262)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 30 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:282 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 282)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 31 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:452 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 452)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 32 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:454 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 454)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 33 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:510 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 510)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 34 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:539 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 539)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 35 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeFieldSetter.java:577 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 577)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 36 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeSetter.java:364 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 364)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 37 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSContentTypeSetter.java:406 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 406)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 38 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:86 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 86)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 39 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:383 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 383)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 40 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:385 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 385)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 41 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:405 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 405)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 42 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:407 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 407)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 43 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:409 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 409)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 44 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:418 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 418)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 45 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:420 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 420)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 46 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:422 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 422)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 47 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:498 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 498)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 48 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:500 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 500)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 49 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:502 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 502)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 50 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:504 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 504)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 51 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:506 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 506)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 52 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:512 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 512)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 53 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:514 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 514)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 54 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:516 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 516)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 55 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:518 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 518)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 56 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:520 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 520)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 57 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:538 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 538)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 58 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:540 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 540)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 59 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:545 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 545)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 60 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:547 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 547)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 61 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:553 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 553)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 62 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:555 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 555)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 63 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:557 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 557)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 64 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:587 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 587)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 65 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:589 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 589)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 66 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:594 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 594)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 67 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeConfigHandler.java:596 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 596)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 68 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:68 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 68)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 69 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:70 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 70)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 70 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:72 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 72)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 71 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:97 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 97)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 72 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:99 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 99)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 73 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:101 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 101)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 74 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:107 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 107)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 75 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:109 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 109)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 76 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:111 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 111)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 77 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:118 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 118)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 78 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSLocationSchemeSetter.java:202 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 202)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 79 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSRelationshipConfigEffectSetter.java:131 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 131)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 80 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSRelationshipConfigEffectSetter.java:133 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 133)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 81 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:67 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 67)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 82 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:90 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 90)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 83 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:246 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 246)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 84 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:272 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 272)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 85 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:343 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 343)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 86 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:441 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 441)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 87 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:520 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 520)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 88 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:522 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 522)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 89 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:527 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 527)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 90 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:529 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 529)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 91 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSiteSetter.java:140 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 140)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 92 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSiteSetter.java:235 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 235)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 93 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSiteSetter.java:334 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 334)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 94 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSiteSetter.java:338 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 338)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 95 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSTemplateSetter.java:156 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 156)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 96 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSTemplateSetter.java:245 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 245)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 97 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSTemplateSlotSetter.java:190 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 190)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 98 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSTemplateSlotSetter.java:216 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 216)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 99 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSTemplateSlotSetter.java:218 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 218)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 100 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigDeNormalizer.java:159 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleProperties` cognitive=42 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 101 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: endpoint http://localhost:11434/v1 unavailable (health: error sending request for url (http://localhost:11434/v1/models)); is Ollama serving?
- Status: open

### Issue 102 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSActionSetter.java:214 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `setVisibility` cognitive=19 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 103 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigMapper.java:343 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `replaceCollectionValues` cognitive=17 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 104 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSEditionSetter.java:157 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `createTask` cognitive=22 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 105 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSSimplePropertySetter.java:334 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addPropertyDefsForMap` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

```


---

# Slice 2 — rx-config deprecation cleanup (2026-11-12, branch fix/issue-6021-rx-config-deployer-warnings)

Tool gate: **advisory** — 0 blocking bugs, 0 in-diff findings. **May commit/push: yes.**

The 2 flagged rows are both preexisting `paths.hardcoded_sep` FPs (exception/log message string literals with escaped quotes, no path joins) at `PSConfigService.java:315` and `:557`, untouched by this diff.

## Full tool report (mkd-code-review, advisory)

```markdown
## Summary

Machine analysis found **2** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigService.java:315 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 315)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: deployer/src/main/java/com/percussion/rx/config/impl/PSConfigService.java:557 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 557)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

```
