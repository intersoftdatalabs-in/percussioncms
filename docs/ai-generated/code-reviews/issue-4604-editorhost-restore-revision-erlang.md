# Erlang review — EditorHost restore prior revision (#4604)

Independent of implementer. Scope: EditorHost restore-prior-revision
slice (WebUI TypeScript + sitemanage REST + Playwright surface + product-docs).

## Verdict

**Pass** for commit/PR of this slice (verdict `lgtm`).

## Bugs

None. 403 (no / reader assignment) and 404 (unknown item or revision)
are mapped in the host and are not treated as success. The sitemanage
service now distinguishes `PSNotFoundException` and
`PSValidationException` from the catch-all 500 path — exactly the
mapping the React Editor requires for `editorRevisionErrorReason`.

## Tests

- Vitest: `editorRevisions.test.ts` (6 cases — mode gating, row
  summarizer, confirm body, revision-id parser, 403/404/500 reason).
- Vitest: `EditorHost.test.tsx` (4 new restore cases — happy path with
  reload, 403 forbidden, 404 not-found refresh-suppressed, view /
  promote toggle hidden).
- Vitest totals in the slice area: `editor` filter is 29 files / 168
  tests pass; the full SPA Vitest run is 477 files / 4570 tests pass.
- JUnit: `PSItemServiceRestoreRevisionTest` (4 cases — NONE → 403,
  READER → 403, missing ComponentSummary → 404, blank id → 403).
- Playwright: `editor-host-restore-revision.spec.js` (5 cases —
  happy path, 403 forbidden, 404 not-found, empty revisions list,
  revisions-load error; console-clean and resource-line filters
  match the slice 7 #4603 pattern).

## Paths

No new OS filesystem I/O. Playwright regex helpers use `/` only in
CMS URL paths. No hardcoded `/` or `\\` joins; REST URL builders use
`encodeURIComponent`. Cross-platform review not applicable.

## Companions

- `WebUI` (EditorHost host, editorRevisions helper, i18n, Vitest).
- `projects/sitemanage` (REST 403/404 mapping, JUnit for those
  mappings — module is the production consumer).
- `modules/perc-qa-automation` (Playwright surface + helpers).
- `product-docs/8.2/admin/content-explorer.md` (Restore-prior-revision
  row in the Actions table + Edit-mode paragraph).
- `product-docs/8.2/developer/rest.md` (Restore-prior-revision
  contract: 403 / 404 semantics, field-reload after success).
- `rest` module: no JAX-RS surface change required — adapter already
  re-exports `/itemmanagement/item/revisions/{id}` and `/…/restoreRevision/{id}`.

## Notes

- The slice reuses the existing `PromoteForm` + `itemRevisionsApi`
  helpers (`fetchItemRevisions`, `restoreItemRevision`,
  `buildRestoreRevisionId`) so the encode-decode pair stays in one
  place rather than being duplicated across PromoteForm and the new
  EditorHost panel.
- The EditorHost panel is hidden in **View** and **Promote** modes
  via `canRestoreFromEditor(mode)` — same shape used by
  `canCopyFromEditor` and `canUseEditorCheckoutActions`. The Playwright
  spec asserts the toggle testid is absent in both modes.
- The interactive confirm seam defaults to `window.confirm` and is
  testable through the `confirmRestore` prop; the same `dialog.once`
  pattern from slice 7 covers it.
- `restoreRevision = restoreItemRevision` defaults the host seam to the
  API helper, so production code calls `buildRestoreRevisionId` once
  per row and the GUID format is the same `{rev}-101-{id}` string
  already accepted by `@Path("restoreRevision/{id}")`.
- The `ps_keyboardescape / window.close` behaviour is intentionally not
  touched — restore succeeds in place and the operator decides whether
  to close the editor (this matches PromoteForm's behaviour, and
  check-in from this slice is unrelated).
