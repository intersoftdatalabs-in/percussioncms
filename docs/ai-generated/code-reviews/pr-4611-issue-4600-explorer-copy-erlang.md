# Erlang review — GitHub PR #4611 (issue #4600)

Independent pre-merge review. Do not post this to GitHub (operator request).

## Summary

PR #4611 replaces Explorer Copy’s native `prompt` with a destination path picker, remaps adaptor path-not-found to `FolderNotFoundException`, sets that exception’s JAX-RS status to 404, and rethrows 403/404 from `FoldersResource.copyFolderItem` instead of wrapping as 500. Intent is sound and companions (REST, adaptor, WebUI tests, Playwright, product-docs) are present. Two blocking issues remain: destination-missing is still not mapped to 404, and a new `NotFoundException` catch returns a JSON `Status` body under HTTP 200 (silent success).

## Scope

- Base: `main`
- Head: PR #4611 (`fix/issue-4600-explorer-copy-selected-item`, `7652cdd`)
- Files: 17 changed
- Prior report: `docs/ai-generated/code-reviews/issue-4600-explorer-copy-erlang.md` (in the PR; self-approve — not treated as independent memory)
- Memory patterns hit: `installer.false-green-exit` analogue (success payload when work failed); missing dest mapping vs documented contract; `tests.structural-only` not the main gap (behavioral tests exist for source-404/403)

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: **no**

Cross-platform path review: applied. CMS/finder paths (`/Assets/...`, `//Folders/$System$/...`) are repository URL/path forms, not OS filesystem joins. No hardcoded `File.separator` issues.

## Issues

### Issue 1 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java` (copyFolderItem; ~1461 in current main, catch hunk in the PR)
- Description: Only `folderHelper.findItem(correctedItemPath)` (source) is mapped from `PSPathNotFoundServiceException` / `PSNotFoundException` to `FolderNotFoundException`. The destination folder is passed to `contentService.newCopies` / `pageService.copy` without a `findFolder` (or equivalent) that is remapped the same way. Missing dest therefore still becomes `BackendException` / HTTP 500. Product-docs and the PR body claim missing source **or destination** is HTTP 404. Adaptor test only covers missing source.
- Suggestion: Resolve dest with `folderHelper.findFolder(correctedTargetPath)` (or the same helper used by `copyFolder`) and let `PSPathNotFoundServiceException` fall into the new 404 catch. Add `copyFolderItemMapsMissingDestToFolderNotFound` next to the source test.
- Status: open
- Pattern-id: incomplete dest 404 vs documented contract

### Issue 2 -- Severity: bug

- File: `rest/src/main/java/com/percussion/rest/folders/FoldersResource.java` (`copyFolderItem` catch)
- Description: New `catch (NotFoundException nfe) { return new Status(404, "Not Found"); }` returns the REST `Status` DTO from a `@POST` method, which JAX-RS serializes as **HTTP 200** with `statusCode: 404` in the body. That is the silent-200 the ticket forbids. `jakarta.ws.rs.NotFoundException` is a `WebApplicationException`; rethrow it (or throw `FolderNotFoundException`) so the mapper/WAE path yields HTTP 404. The existing `copyFolder` method has the same anti-pattern; do not copy it onto `copy/item`.
- Suggestion: Drop the return-Status catch. Rethrow `FolderNotFoundException` / `NotFoundException`. Add a resource test that asserts the method **throws** (not returns 200) when the adaptor or JAX-RS not-found path fires. `copyFolderItem_mapsFolderNotFound` should also assert `ex.getStatus() == NOT_FOUND`.
- Status: open
- Pattern-id: false-green HTTP 200 with error payload

### Issue 3 -- Severity: suggestion

- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java` (generic `Exception` catch)
- Description: `if (e instanceof NotAuthorizedException)` is dead after the dedicated `catch (NotAuthorizedException e)` above.
- Suggestion: Remove the instanceof branch.
- Status: open

### Issue 4 -- Severity: nit

- File: `rest/src/test/java/com/percussion/rest/errors/FolderNotFoundExceptionTest.java`
- Description: Cause constructor test does not assert `getStatus() == NOT_FOUND` after the mapper-critical change.
- Suggestion: Assert status on both constructors.
- Status: open

## Handoff

- Recommendation: **request-changes**. Verdict: **BLOCK**.
- Author should **not** merge until dest-404 is real and the HTTP-200 Status return is gone.
- Durable report: `docs/ai-generated/code-reviews/pr-4611-issue-4600-explorer-copy-erlang.md`
- Not posted to GitHub.
