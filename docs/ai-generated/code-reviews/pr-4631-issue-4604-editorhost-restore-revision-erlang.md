## Summary

PR 4631 adds EditorHost restore-prior-revision in Edit mode: list revisions, confirm, call existing `restoreRevision` REST, reload fields, and map HTTP 403/404 as failures. Sitemanage maps `PSValidationException` to 403 and `PSNotFoundException` to 404. Vertical slice includes Vitest, JUnit, Playwright, and product-docs. Independent review found no hard-gate bugs.

## Scope

- Base: `main`
- Head: PR 4631 `fix/issue-4604-editorhost-restore-revision` (`76d076a17312e4b77cb2d330c3e28503e2b230c2`)
- Files: 12 changed
- Prior report: none in this checkout (`issue-4604-editorhost-restore-revision-erlang.md` is in the PR, not on this worktree)
- Memory patterns hit: missing behavioral tests (covered); change-class closure (WebUI + Playwright + product-docs + sitemanage); paths (URL `/` only — not filesystem)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(none)

Cross-platform path review: no issues. REST and Playwright helpers use `/` on CMS URL paths, not OS filesystem joins.

## Notes (non-blocking)

- Playwright stubs workflow comments, and the PR body says the panel lists comments, but `EditorHost` only renders the revision `<select>`. Issue #4604 acceptance is revision list + restore + field refresh, so this is documentation drift, not a gate.
- Inner `catch (Exception)` around `prepareForRestore` / `promoteRevisions` still wraps not-found as a generic `WebApplicationException` (pre-existing). The new 404 mapping covers `validateItemRestorable` / `getComponentSummary` only.
