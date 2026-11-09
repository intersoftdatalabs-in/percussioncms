---
id: developer-widget-editor
title: Form widget rich-text editor
description: TipTap editor bundle shipped inside the form and comments-form widgets
version: "8.2"
order: 53
tags: [developer, widgets, form]
---

# Form widget rich-text editor

The **form** (`perc.widget.form`) and **comments form**
(`perc.widget.commentForm`) widgets edit their descriptive-text labels with a
rich-text editor. As of 8.2 this editor is a prebuilt vanilla (no-React)
**TipTap** bundle that ships *inside each widget package* — delivered sites
no longer fetch any CMS-served TinyMCE script.

## What ships where

Each widget package carries, under
`rx_resources/widgets/<form|commentsForm>/js/`:

- `perc-tiptap-editor.bundle.min.js` — prebuilt TipTap 3 vendor bundle
  (MIT `@tiptap/core`, `@tiptap/starter-kit`, `@tiptap/extension-underline`).
- `PercWidgetEditor.js` — toolbar UI, config translator, editor lifecycle.
- `TinymceConfig.js` — site-customizable configuration (see below).

The widget controls (`formWidgetControl`, `commentsFormWidgetControl`) load
these three scripts from `rx_resources`, which publishes with the site.
There is no `sys_resources/tinymce` dependency left in either package, so
published forms and comment forms keep working after the CMS TinyMCE bundle
is retired.

## Customizing per site (`TinymceConfig.js`)

Keep editing the same `TinymceConfig.js` assignment shape. The translator
reads `tinymce.EditorManager.settings` and honors:

- `toolbar` — any subset of
  `bold italic underline undo redo styleselect` (string or string array;
  unknown tokens are ignored).
- `content_css` — site stylesheet URL. It is applied to the editing surface
  **and** harvested for `styleselect` site-style entries (classes from your
  CSS appear under “Site styles”). Use a site-relative URL that resolves on
  the delivered site; CMS-hosted `/sys_resources/…` URLs no longer resolve.
- `auto_focus` — editor id to focus when the text editor opens (`elm1`).
- `setup(fn)` — called with an editor facade exposing
  `on("click" | "change" | "input")`, `getContent()`, and `setContent()`.
  A throwing handler cannot break editor startup.
- `branding: false` — inherent to the engine (TipTap ships no branding).

Every keystroke syncs the underlying field, marks it dirty, and fires
`input`/`change`, so save/submit flows behave as before. Block styles
(Paragraph, Heading 1–3) and Clear formatting are always offered; site
classes apply as inline `span` styles.

## Verifying a site

1. Publish a page with a form or comments-form widget.
2. Open browser devtools → Network, filter `tinymce`: expect **zero**
   requests.
3. Edit a descriptive-text label: toolbar, styles dropdown (including your
   site classes when `content_css` is set), and save must work with no
   console errors.

## Rebuilding the bundle

The committed bundles are built from
`modules/perc-packages/widget-editor` (`npm ci && npm run build`, esbuild).
Rebuild only when the pinned TipTap sources change; the output is committed
byte-identical into both widget packages.
