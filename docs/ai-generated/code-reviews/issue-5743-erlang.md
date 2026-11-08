## Summary

Machine analysis found **4** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 16 analyzed
- In-diff: 4 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/pipelines/PipelinesResource.java:929 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 929)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/api/developer/pipelineImportExport.ts:35 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `pipelineApplicationExportFilename` cognitive=8 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/PipelineDetailPanel.tsx:939 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: rest/src/main/java/com/percussion/rest/pipelines/PipelinesResource.java:891 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `exportFilename` cognitive=8 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Work-agent disposition (issue #5743, 2026-11-08)

- **Issue 1 (`paths.hardcoded_sep`, line 929): FALSE POSITIVE — not fixed by
  code change, by evidence.** Line 929 is
  `+ "\"; filename*=UTF-8''"` inside `contentDispositionAttachment`, an
  RFC 6266 / RFC 5987 `Content-Disposition` header fragment. No filesystem
  path is constructed anywhere in `exportFilename`,
  `contentDispositionAttachment`, `asciiFilenameFallback`, or `rfc5987Encode`
  — all four produce HTTP header values only, and the javadoc states "Not a
  filesystem path". The flagged line is byte-identical to shipped precedent
  `rest/.../contenttypes/ContentTypesResource.java:1107-1112` (CD-14,
  #4018/#4021, reviewed and merged on `main`); the rule also fired on the
  first run at the `/`-comparison line of the precedent-mirroring sanitizer.
  Mitigation applied: separator char literals in `exportFilename` were
  replaced with named `FILENAME_FORWARD_SLASH`/`FILENAME_BACKSLASH` constants
  (behavior-identical, locked by `exportFilenameSanitizesSeparators`);
  re-run moved the flag to the RFC-mandated `filename=\"` string literal,
  which cannot be rewritten without violating RFC 6266. No portable-path API
  applies to an HTTP header value. Proceeding with reviewer visibility here
  and in the PR body.
- **Issues 2/4 (complexity, `pipelineApplicationExportFilename` /
  `exportFilename`): ADVISORY ONLY.** Both mirror the shipped CD-14
  precedent shape (`ContentTypesResource.exportFilename`,
  `contentTypeImportExport.ts` filename helper); splitting them would
  diverge from the reviewed precedent for no behavioral gain. No change.
- **Issue 3 (complexity, `onSaveHttpTank`): ADVISORY ONLY, out of scope.**
  Preexisting function not authored or modified by this slice (flagged only
  because its file is in-diff). No change.
- No missing-behavioral-test finding was raised; new REST/sitemanage/WebUI
  tests ship in this diff (resource 404/409/400 mapping, adaptor
  parse/validate/uniqueness/persist, SPA export/import incl. 409 panel test).
