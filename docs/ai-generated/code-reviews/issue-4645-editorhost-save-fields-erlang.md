# Erlang review — #4645 EditorHost save field values

Independent pre-PR review of `feat/issue-4645-editorhost-save-fields` vs `origin/main`.

## Summary

EditorHost PUT of itemmanagement fields now carries CMS tip `revision`. A mismatch maps to HTTP 409 and a host banner. Client-side required/400 mapping is unchanged. Playwright stubs the PUT; JUnit covers stale vs matching revision.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None that block. Cross-platform path checklist: N/A (no filesystem I/O). Behavioral tests: JUnit `PSItemServiceSaveEditorFieldsTest`, Vitest `editorSave` + EditorHost 409, Playwright surface.

## Memory patterns hit

HTTP 403/409 must not be treated as success (EditorHost lock/save).
