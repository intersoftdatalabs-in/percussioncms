/**
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
 * Entry point for the npm-managed CodeMirror 5 bundle (issue #5784).
 *
 * This module pulls the exact CodeMirror files the classic template editor
 * (`editTemplate_js.jsp`) and the `sys_TextArea` code-editor control
 * (`sys_Templates.xsl`) need out of the `codemirror` npm package so esbuild
 * can ship them as a single `codemirror.bundle.js` + `codemirror.bundle.css`
 * pair served from `/sys_resources/codemirror/codemirror/` — outside the
 * `sys_resources/tinymce` tree, so the bundle survives the `perc-tinymce`
 * packaging removal (#5734). The file set mirrors the previous
 * `codemirror-unpack` Ant allow-list 1:1, so every runtime consumer keeps
 * working without API changes.
 *
 * CodeMirror 5 is imported for its side effects: the core exposes the global
 * `CodeMirror` object and each mode/addon self-registers on it. The bundle
 * is built with esbuild `format: "iife"` and no `globalName` (see
 * `bundle-codemirror.mjs`): the entry assigns `window.CodeMirror` itself,
 * so `window.CodeMirror` keeps working for the legacy consumers.
 */

import * as CodeMirrorModule from "codemirror/lib/codemirror.js";
import "codemirror/mode/xml/xml.js";
import "codemirror/mode/javascript/javascript.js";
import "codemirror/mode/css/css.js";
import "codemirror/mode/htmlmixed/htmlmixed.js";
import "codemirror/mode/velocity/velocity.js";
import "codemirror/addon/edit/matchbrackets.js";
import "codemirror/addon/dialog/dialog.js";
import "codemirror/addon/search/searchcursor.js";
import "codemirror/addon/search/search.js";
import "codemirror/addon/selection/active-line.js";
import "codemirror/addon/display/fullscreen.js";
import "codemirror/lib/codemirror.css";
import "codemirror/addon/dialog/dialog.css";
import "codemirror/addon/display/fullscreen.css";
import { unwrapCodeMirror } from "./codemirror-helpers.mjs";

// Legacy consumers (editTemplate_js.jsp, sys_Templates.xsl code editors)
// read the global `CodeMirror` constructor.
globalThis.CodeMirror = unwrapCodeMirror(CodeMirrorModule);
