/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Vendor entry for the delivered-site widget editor bundle (issue #5901).
 *
 * Bundled with esbuild (IIFE, minified) into
 * `perc-tiptap-editor.bundle.min.js` and committed under both
 * `perc.widget.form` and `perc.widget.commentForm`
 * (`SupportFile-rx_resources/widgets/<form|commentsForm>/js/`).
 *
 * Exposes the TipTap pieces the human-readable `PercWidgetEditor.js`
 * controller consumes via `window.__PercTipTap`. Vanilla only: no React,
 * no framework. MIT-licensed npm sources, see package.json pins.
 */
import { Editor, Mark } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";
import Underline from "@tiptap/extension-underline";

window.__PercTipTap = {
  Editor: Editor,
  Mark: Mark,
  StarterKit: StarterKit,
  Underline: Underline,
  version: "3.33.0",
  engine: "tiptap",
};
