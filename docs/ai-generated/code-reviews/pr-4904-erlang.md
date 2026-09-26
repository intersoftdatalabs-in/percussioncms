<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# PR 4904 Erlang review

Status: reviewed
Persona: erlang 0.1.1
Persona source: /home/nate/.local/share/mkd/agents/erlang
Recommendation: approve
In-diff blocking bugs: 0

Disposition: in-diff `paths.hardcoded_sep` on `ContentTypeAdaptorCopyTest` line 164 is an editor URL (`../psx_ce…/*.html`), not a filesystem join. Not a portability defect. Cognitive complexity on `rewriteEmbeddedTypeName` (16 vs max 15) is a non-blocking suggestion. Preexisting `paths.hardcoded_sep` rows are out of gate. `github-advanced-security` CodeQL failure is the Code Quality app; required CodeQL Advanced check is success. Branch ruleset has no required status checks.

## Summary

Machine analysis found **11** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 2 finding(s); preexisting: 9
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/test/java/com/percussion/apibridge/ContentTypeAdaptorCopyTest.java:164 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 164)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/contenttypes/ContentTypesResource.java:1110 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1110)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:315 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 315)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:316 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 316)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:317 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 317)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:318 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 318)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:338 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 338)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:339 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 339)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:340 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 340)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: rest/src/test/java/com/percussion/rest/contenttypes/ContentTypesResourceDetailTest.java:341 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 341)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/ContentTypeAdaptor.java:514 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `rewriteEmbeddedTypeName` cognitive=16 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang gate (human read of the machine report)

- Issue 1 is a false positive: `getEditorUrl()` returns a content-editor URL, and `/` is correct for URL paths. Not a Windows filesystem join.
- Issues 2–10 are preexisting and do not block.
- Issue 11 is a suggestion (cognitive 16). `rewriteEmbeddedTypeName` is an intentional single-pass substring rewrite of design tokens that embed the source type name. Not a defect.
- Copy closure is present: adaptor + resource + both test adaptors, behavioral unit tests (distinct type, 409, 400, Folder), Vitest wire tests, product-docs, Playwright spec. Live Playwright copy did not complete because new content-type save fails in the H2 cell on stock jars as well (`Specified defs not found`); not a regression introduced by this diff.
- Recommendation: approve. May merge: yes.
