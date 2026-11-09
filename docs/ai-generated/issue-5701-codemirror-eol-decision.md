# #5701 — CodeMirror 5 npm bundle end-of-life decision record

> Decision/verification slice (2026-11-09). Not a deletion: the bundle +
> plugin-dir + npm-wiring deletion rides the `perc-tinymce` packaging removal
> (#5734, owned by #5622). Detach-after-landing is #5746 (gated, filed).

## Decision: retire with the packaging slice

- **Retire-with-#5734** for the `modules/perc-tinymce` legacy copy
  (`sys_resources/tinymce/plugins/codemirror/codemirror/`,
  `codemirror.bundle.*`), the TinyMCE `codemirror` plugin dir (`source.html`),
  and the `codemirror@5.65.21` npm wiring in `modules/perc-tinymce/package.json`.
  Deletion happens **in the same PR as the TinyMCE 6.8.6 / `perc-tinymce`
  retirement** — no orphaned bundle ships.
- **No CodeMirror 6 migration.** The remaining half of #5626 as written
  (migrate to `@codemirror/*` in `WebUI/package.json`) is **superseded** by
  #5652 / PR #5688 (TipTap-native source view, zero `@codemirror` imports).
  Migrating classic dialogs to CM6 would be churn on surfaces slated for
  replacement + deletion.
- **Canonical bundle until classic EOL.** The CodeMirror 5 bundle now also
  lives in `modules/perc-common-ui-bundle` (TinyMCE-independent path
  `/sys_resources/codemirror/`, surviving #5734) serving classic template /
  code-editor surfaces while those live (classic migration: #5784). The
  `perc-tinymce` copy is a legacy fallback until #5734 lands.

## Verification evidence (this slice, on `origin/main` @ 2026-11-09)

- Zero React/TipTap need re-confirmed: `WebUI` vitest guard
  `src/test/ts/editor/tiptap/noCodeMirrorImports.test.ts` → **2 passed**
  (no `codemirror`/`@codemirror/*` module import or bundle reference in
  `src/main/ts`; no `codemirror` dep in SPA manifests). Original proof:
  #5745 / PR #5747 (merged 2026-11-08).
- Remaining in-`WebUI/src` CodeMirror references are classic-only or prose:
  `webapp/cm/**/editTemplate_js.jsp` (+ `war/` mirror) template dialogs,
  `perc_template_metadata_dialog.js` (classic dialog JS), and comments in
  `TipTapPercussionBar.tsx` / `tiptapPercussion.ts` / `templateSourceViewer.ts`
  (no module imports; allowed by the guard).
- `npm audit --omit=dev` → **found 0 vulnerabilities** in both
  `modules/perc-tinymce` and `modules/perc-common-ui-bundle`
  (`codemirror@5.65.21`). Dependabot/`npm audit` visibility stays on until
  deletion; CM5 CVEs assessed on the TinyMCE 6.8.6 freeze cadence.

## In-repo anchor

- `modules/perc-tinymce/build-scripts/bundle-codemirror.mjs` doc block now
  carries the EOL pointer (retire with #5734, no CM6 migration, canonical copy
  in `perc-common-ui-bundle`) so nobody re-invests in this wiring.

## Still open (not this slice)

- #5734 — packaging removal incl. CM5 legacy copy + plugin dir + product-docs.
- #5746 — detach CM5 bundle after #5733/#5734 land (gated, filed).
- #5784 — classic-surface migration (seated blocker, #5622 epic).
