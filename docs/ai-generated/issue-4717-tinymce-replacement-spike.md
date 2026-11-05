# #4717 — Research spike: replace TinyMCE (7+ GPL incompatible with Apache 2.0)

**Date**: 2026-11-05
**Type**: Research spike (comparison + recommendation only — no replacement landed)
**Issue**: [#4717](https://github.com/intersoftdatalabs-in/percussioncms/issues/4717)
**Parent**: #4717 (standalone epic)
**Labels**: enhancement, tech-debt, licensing-blocker, accessibility, i18n, ui, p3

## TL;DR (recommendation)

1. **Recommended primary editor: TipTap 2.x (ProseMirror), MIT** — best fit for the React Content Editor host: MIT-licensed core + extensions, headless/React-first, no cloud dependency, full control over toolbar and i18n, and HTML output that matches what we already persist. Reuse the already-vendored **CodeMirror 6** (MIT) for the source view, as today.
2. **Recommended fallback: Lexical (Meta, MIT)** if TipTap/ProseMirror proves too heavy for the classic CM1 chrome or if we need Meta's plugin primitives; same MIT license, more integration work, less out-of-the-box toolbar.
3. **Explicit "do not use TinyMCE 7+":** GPL-2.0-or-later is incompatible with Apache 2.0 for distribution. Drop CKEditor 5's free/GPL build for the same reason (commercial license would be required and is out of scope per this spike's assumptions).
4. **Custom Percussion plugins must be rewritten** — `percadvimage`, `percadvlink`, `percglobalvariables`, `percmorelink`, `rxinline`, `rxinserthtml`, and the CodeMirror source plugin are TinyMCE-specific and have no direct equivalent in any candidate. Budget this as the largest single cost.
5. **Follow-on implementation issue (proposed):** "Replace TinyMCE 6.8.6 with TipTap/ProseMirror in the React Content Editor (html field) + classic CM1 dialogs" — tracked as a separate epic; this spike closes only the recommendation.

Until that epic lands, **stay on TinyMCE 6.8.6** (MIT) with CVE/support-horizon monitoring and the existing freeze in `modules/perc-tinymce/pom.xml` + root `pom.xml` (`tinymce.version`).

---

## 1. Inventory of current TinyMCE usage

### 1.1 Version + packaging

| Item | Value |
|------|-------|
| Editor version | TinyMCE **6.8.6** (MIT) |
| Version property | root `pom.xml` line 325: `<tinymce.version>6.8.6</tinymce.version>` |
| Codemirror version | root `pom.xml`: `<codemirror.version>6.65.7</codemirror.version>` (MIT) |
| Packaging module | `modules/perc-tinymce` (WebJAR + `rx_resources` overlay) |
| Freeze comment | `modules/perc-tinymce/pom.xml` license block: "TinyMCE 7+ switched to GPL-2.0-or-later. Do NOT upgrade past 6.8.x without legal review" |
| Dist copy | `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml` copies `perc-tinymce/target/classes/META-INF/resources/rx_resources` (guard: `TinyMceRxResourcesPackagingTest`) |
| Update scripts | `modules/perc-distribution-tree/scripts/update-tinymce.py`, `test_update_tinymce.py` |

### 1.2 React call site

- `WebUI/src/main/ts/editor/widgets/HtmlFieldWidget.tsx` — loads `/sys_resources/tinymce/js/tinymce/tinymce.min.js`, inits with `plugins: "lists link code"`, toolbar `undo redo | bold italic underline | bullist numlist | link | code`, `convert_urls: false`. Textarea fallback if script missing; read-only handling; `setMode`/view-only not fully wired (textarea fallback covers view).
- Classification: `WebUI/src/main/ts/editor/controlKinds.ts` maps any control containing `tinymce`/`editlive`/`html` or `dataType === "html"` to `"html"` widget kind.
- i18n: `EDITOR_MSG.HTML_LABEL` via `message(...)`.

### 1.3 Legacy / classic CM1 call sites

- `WebUI/src/main/webapp/cm/app/includes/template/editTemplate_js.jsp` (and `pages/` dual tree)
- `WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js` — selects `.tox-tinymce` / `.tinymce` nodes, adds `perc-tinymce-readonly` class for view mode.
- `WebUI/war/...` mirror of the above (dual-ship; must stay consistent per AGENTS.md).

### 1.4 Config JSON (classic)

Under `modules/perc-tinymce/.../sys_resources/tinymce/config/`:
- `default_config.json`, `rx_default_config.json`, `simple_config.json`, `customer_config_override.json`, `readme.md`.
- `rx_default_config.json` plugins: `codemirror, advlist, autosave, autolink, lists, link, charmap, preview, anchor, searchreplace, wordcount, visualblocks, visualchars, fullscreen, insertdatetime, media, nonbreaking, table, directionality, emoticons, save, codesample, noneditable, rxinline, rxinserthtml`.
- Toolbar: `restoredraft | undo redo | styleselect bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link unlink | image`.
- Menubar: `edit insert view format table tools`.

### 1.5 Custom Percussion plugins (TinyMCE-specific)

Under `modules/perc-tinymce/.../sys_resources/tinymce/plugins/`:
- `percadvimage`, `percadvlink` — advanced image/link dialogs
- `percglobalvariables` — global variables (with docs, css, i18n toc)
- `percmorelink` — "more link" / read-more insert (icons, css, docs)
- `rxinline` — inline links
- `rxinserthtml` — insert HTML
- `codemirror` — source view (wraps CodeMirror 6.65.7, MIT; lang packs cs_CZ, de, en, es_ES, fr_FR, nl, pt_BR, pt_PT, ru, uk, zh_TW)

Plus `icons/percCustIcon/icons.js` custom icon set and `tinymce_init.js` (dirty controller wiring, broken-link tooltips, spellcheck menu).

### 1.6 i18n floor

TinyMCE/CodeMirror lang packs shipped: cs, de, en, es, fr, nl, pt (BR/PT), ru, uk, zh_TW. Product locales come from TMX (`modules/perc-i18n`: `CmsUi.tmx`, `SystemResources.tmx`, etc.). Editor UI strings must follow the CMS session locale.

### 1.7 Sanitization / security

- Client-side: HTML containing script tags, inline event handlers, `javascript:` URLs blocked (per product-docs content-explorer.md).
- Server-side: `PUT /services/itemmanagement/item/fields/{id}`.
- CodeQL-sensitive sinks in `perc_page_edit_dialog.js` and classic dialogs.

### 1.8 Packaging / dist

- `perc-tinymce` jar ships WebJAR assets under `/sys_resources/tinymce/...` and `/rx_resources/tinymce/...`.
- Installer `rx_resources` copy + `qa-rebuild-tinymce` fail-fast (see `TinyMceRxResourcesPackagingTest`).

### 1.9 QA coverage that depends on TinyMCE

Playwright specs in `modules/perc-qa-automation/frontend/tests/`: `editor-host-html-*.spec.js`, `assembly-html-*.spec.js`, `editor-rich-controls.spec.js`, `bug-2243-inline-link-title-field.spec.js`. These assert editor DOM (`.tox-tinymce`, toolbars). Any replacement must update selectors.

---

## 2. License matrix (SPDX)

| Candidate | Editor SPDX | Required plugins SPDX | Cloud/SaaS-gated features | Can vendor in `modules/`? | Verdict |
|-----------|-------------|----------------------|---------------------------|---------------------------|---------|
| TinyMCE 6.8.6 (control) | MIT | MIT (CodeMirror MIT) | none for core | yes (today) | Keep as time-boxed mitigation |
| TinyMCE 7+ | GPL-2.0-or-later | GPL + commercial | advanced features commercial | no (GPL clash) | **OUT** |
| CKEditor 5 (free) | GPL-2.0-or-later | GPL | premium features commercial | no (GPL clash) | **OUT** unless legal buys commercial license |
| CKEditor 5 (commercial) | proprietary (paid) | proprietary | n/a | requires license purchase | Out of scope (spike assumes no purchase) |
| TipTap 2 / ProseMirror | MIT (TipTap), MIT (ProseMirror) | MIT (extensions) | none for core; optional pro extensions paid | yes | **Primary** |
| Lexical (Meta) | MIT | MIT (community) | none | yes | **Fallback** |
| Quill | BSD-3-Clause | BSD/MIT community | none | yes | Viable but lower parity |
| CodeMirror 6 | MIT | MIT | none | yes (already vendored) | Source view only, not WYSIWYG |
| Slate | MIT | MIT | none | yes | Possible but smaller ecosystem; not shortlisted |

Notes:
- TipTap's "Pro" extensions (e.g. collaboration, AI) are commercial but **not required** for parity; core editing, lists, links, images, tables, code blocks are MIT.
- Lexical ships accessibility primitives (`@lexical/react` `LexicalComposer`, `ContentEditable` with ARIA) but rich toolbar must be built by us.
- CKEditor 5's GPL build is the same Apache clash as TinyMCE 7; do not adopt even though feature set is strong.

---

## 3. i18n assessment

| Requirement | TinyMCE 6.8 (today) | TipTap/ProseMirror | Lexical | Quill |
|-------------|----------------------|--------------------|---------|-------|
| Locale follows CMS session | TinyMCE `language` option + lang pack file | UI chrome is ours → use `message()`/TMX-driven labels; no built-in chrome strings | Same as TipTap | Theme CSS + our labels |
| Shipped locale floor (cs, de, en, es, fr, nl, pt, ru, uk, zh_TW) | lang packs per locale | We control; reuse existing TMX keys; CodeMirror lang packs already cover source view | Same | Same |
| RTL | TinyMCE `directionality` plugin | CSS `dir` + ProseMirror; handle in our toolbar | CSS `dir` | CSS `dir` |
| Locale-specific quotes | TinyMCE config | Our sanitization/paste normalization | Our normalization | Our normalization |

Key point: TipTap/ProseMirror is **headless** — it has no built-in English-only toolbar, so we cannot accidentally ship English chrome. All labels flow through our existing `message()`/TMX pipeline, which is actually an i18n improvement over TinyMCE's separate lang-pack files.

---

## 4. Accessibility assessment (WCAG 2.2 AA bar)

| Requirement | TinyMCE 6.8 | TipTap/ProseMirror | Lexical |
|-------------|-------------|--------------------|---------|
| Keyboard-operable toolbar | yes (tox) | yes, if we build toolbar with roving tabindex + shortcuts (ProseMirror keymap) | yes, via Lexical commands + our toolbar |
| Screen reader name/role/value on field | textarea host + ARIA | `ContentEditable` needs `role="textbox"`, `aria-multiline`, `aria-label` (we already set `aria-label` on textarea; carry over) | same |
| Focus order with check-out chrome | managed by TinyMCE | we own focus; must test tab order in `spa.jsp?entry=editor` | same |
| No focus trap / escape | TinyMCE modal dialogs trap (known issue) | our dialogs must implement focus restore; doable | same |
| Source view a11y | CodeMirror (keyboard + SR decent) | reuse CodeMirror 6 | reuse CodeMirror 6 |

Recommendation: whichever candidate, add Playwright + axe checks in `perc-qa-automation` for the HTML field (keyboard-only edit, SR attributes). Note published VPATs: TipTap/ProseMirror has no official VPAT; Lexical has no official VPAT; Quill has community a11y notes. We will need our own VPAT-style smoke. This is a gap to record, not a blocker, since we own the chrome.

---

## 5. Integration sketch

### 5.1 React host (`HtmlFieldWidget.tsx`)

Replace the TinyMCE script load + `tinymce.init` with a TipTap `useEditor` hook:
- Extensions: `StarterKit` (bold, italic, lists, headings, code block), `Link` (autolink), `Image`, `Table`, `CodeBlockLowlight` (or CodeMirror for source), `Placeholder`.
- `content: value`, `onUpdate: ({editor}) => onChange(editor.getHTML())`.
- Read-only: `editable: !readOnly`.
- Source toggle: swap WYSIWYG view for a CodeMirror 6 instance (already vendored) editing the same HTML string.
- Sanitization: keep client-side block of script/event/`javascript:` (DOMPurify or our existing sanitizer) before persist; server remains authoritative.
- Fallback: if module fails to load, render `<textarea>` (existing behavior) — preserve.

Keep `TINYMCE_SCRIPT_PATH`/script-loading abstraction generic or rename to editor loader; update `controlKinds.ts` classification (rename `tinymce` → `html`/`richtext` but keep `tinymce`/`editlive` aliases for legacy data).

### 5.2 Classic CM1 (`perc_page_edit_dialog.js`, `editTemplate_js.jsp`)

This is the expensive part. Classic dialogs embed TinyMCE via config JSON (`rx_default_config.json`) and load `/sys_resources/tinymce/...`. Options:
- (a) Replace with TipTap in an iframe (new editor page) — large.
- (b) Keep TinyMCE 6.8.6 only in classic CM1 until classic is retired (React editor is the strategic surface) — recommended as a phased approach: migrate React host first, leave classic on 6.8.6 with a deprecation note, then retire classic.

Given specs 995/996 (React Content Editor) is the strategic direction, recommend (b) for phase 1.

### 5.3 Packaging module

- Replace `modules/perc-tinymce` with `modules/perc-richtext` (or keep name and swap assets) shipping TipTap/ProseMirror bundle (npm-built, like `perc-common-ui-bundle` esbuild) + CodeMirror.
- Update `installDistributionFiles.xml` copy path and `TinyMceRxResourcesPackagingTest` (rename to `RichTextRxResourcesPackagingTest`).
- Keep `/sys_resources/...` URL stable or add redirect to avoid breaking stored content references? Assets are runtime, not stored; safe to change path but update all references.

### 5.4 i18n

- TipTap toolbar labels via our `message()` keys; add keys to `CmsUi.tmx` for each locale floor.
- CodeMirror lang packs already cover source view; reuse.

### 5.5 Sanitization

- No change to persisted HTML contract. TipTap output is HTML string; existing `PUT /services/itemmanagement/item/fields/{id}` unchanged. Ensure no new sink introduces CodeQL findings (sanitize before persist, keep server validation).

---

## 6. Effort estimate

| Surface | Effort | Notes |
|---------|--------|-------|
| React host HTML field (TipTap) | M | Hook swap, extensions, read-only, source toggle, fallback |
| Unit tests (Vitest) for HtmlFieldWidget | S | Update existing tests |
| Playwright specs update | M | Selector changes across ~20 html specs |
| Classic CM1 dialogs | L | Config JSON + iframe or keep 6.8.6 |
| Custom Percussion plugins rewrite | L | Largest; each plugin re-implemented as TipTap extension or React dialog |
| Packaging module + dist + installer | M | Rename/copy paths, tests |
| i18n (TMX keys + locale floor) | M | All locales in floor |
| a11y smoke + VPAT-style doc | M | Keyboard + SR |
| **Total (React-first phased)** | **L** | Classic deferred |

---

## 7. Risks / open questions

- TipTap's `StarterKit` HTML output may differ subtly from TinyMCE's (e.g. `<p>` wrapping, list markup). Need a normalization/round-trip test against existing stored content to avoid content drift. Mitigation: add a corpus-based round-trip test in the implementation epic.
- Paste-from-Word: TipTap's default paste handling is decent but not TinyMCE-grade; may need `@tiptap/extension-clipboard-text-serializer` or custom. Budget in plugin rewrite.
- Table editing: TipTap tables extension is MIT; verify keyboard a11y.
- No official VPAT for TipTap/Lexical; we own the a11y evidence.
- Legal: confirm Intersoft is comfortable vendoring TipTap/ProseMirror (MIT) — compatible with Apache 2.0. Yes, MIT is Apache-compatible.

---

## 8. Success criteria check (issue #4717)

1. Recommended editor + license SPDX → **TipTap 2 / ProseMirror, MIT** (fallback Lexical, MIT).
2. Gaps vs TinyMCE 6.8 functional/i18n/a11y/usability → sections 1, 3, 4, 5, 7.
3. Whether custom Percussion plugins must be rewritten → **Yes** (section 1.5, 6).
4. Follow-on implementation issue → proposed: "Replace TinyMCE 6.8.6 with TipTap/ProseMirror in the React Content Editor (html field) + classic CM1 dialogs" (new epic; classic phased).

---

## 9. Files touched by this spike

- `docs/ai-generated/issue-4717-tinymce-replacement-spike.md` (this report).

No source, test, product-docs, or packaging changes. No Maven build required (docs-only).
path: docs/ai-generated/issue-4717-tinymce-replacement-spike.md