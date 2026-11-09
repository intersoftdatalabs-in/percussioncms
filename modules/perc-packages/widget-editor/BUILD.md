# Widget TipTap editor — build notes (issue #5901)

## What ships

Each of `perc.widget.form` and `perc.widget.commentForm` carries, under
`SupportFile-rx_resources/widgets/<form|commentsForm>/js/`:

| File | Origin | Role |
|---|---|---|
| `perc-tiptap-editor.bundle.min.js` | built by `npm run build` from `src/perc-tiptap-vendor.js` | prebuilt vanilla (no-React) TipTap vendor bundle, exposes `window.__PercTipTap` |
| `PercWidgetEditor.js` | byte-identical copy of `src/PercWidgetEditor.js` | toolbar UI, `TinymceConfig.js` translator, editor lifecycle, `tinymce.EditorManager` facade |
| `TinymceConfig.js` | hand-maintained per package (same contract) | site-customizable config, `tinymce.EditorManager.settings` shape |

## Pinned npm sources (MIT)

- `@tiptap/core` **3.33.0** (exact pin)
- `@tiptap/starter-kit` **3.33.0** (document, paragraph, text, bold, italic,
  strike, heading h1–h3, lists, history/undo-redo, …)
- `@tiptap/extension-underline` **3.33.0**
- `esbuild` **0.28.2** (dev, IIFE + minify, target es2019)

See `package.json` + `package-lock.json`. This matches the WebUI TipTap 3
line (`WebUI/src/main/ts/editor/tiptap/tiptapSetup.ts`), so the delivered-site
widgets and the CM1 editor share one engine version.

## Rebuild

```bash
cd modules/perc-packages/widget-editor
npm ci          # first time only (needs npm registry access)
npm run build   # esbuild → both SupportFile trees
```

The committed bundles are the build output; no Maven wiring is required to
consume them (the perc-packages build copies `SupportFile-*` trees verbatim).

## Translator contract (parity with TinyMCE widget behavior)

Input: `tinymce.EditorManager.settings` as assigned by `TinymceConfig.js`.

| Setting | Behavior |
|---|---|
| `toolbar` (string or string[]) | subset of `bold italic underline undo redo styleselect`; unknown tokens ignored |
| `styleselect` | block formats (Paragraph, H1–H3) + site classes harvested from `content_css` (PercCustomStylesService semantics; fetch-then-regex, fail-closed to built-ins) |
| `content_css` | injected once as `<link>` in host `<head>` (deduped); `javascript:`/`data:` rejected |
| `auto_focus` | focuses the editor when it equals the textarea id (`elm1`) |
| `setup(fn)` | called with `{ on("click"\|"change"\|"input"), getContent(), setContent() }`; throwing handlers cannot break init |
| `branding: false` | inherent (TipTap has no branding) |
| dirty tracking | every TipTap update syncs the textarea, sets `data-perc-dirty`, dispatches bubbling `input` + `change` |

## Compatibility notes

- `window.tinymce` facade is installed **only** when no real TinyMCE
  `EditorManager` exists; a real TinyMCE always wins.
- Settings assigned before `PercWidgetEditor.js` loads (plain-object
  bootstrap in customized site configs) are adopted at install.
- `mceAddEditor`/`mceRemoveEditor`/`get(id)` facade methods cover every
  historical widget call site; in-repo call sites now use
  `PercWidgetEditor.addEditor/removeEditor/get` directly with null guards,
  so a missing bundle degrades to the plain textarea instead of throwing.
- No textarea fallback is shipped as a *product* behavior (per #5901): the
  bundle is a package dependency of both widgets.
