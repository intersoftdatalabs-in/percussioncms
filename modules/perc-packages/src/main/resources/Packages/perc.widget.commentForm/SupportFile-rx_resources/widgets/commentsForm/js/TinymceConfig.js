/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
 * Rich-text editor configuration for the comments form widget (issue #5901).
 *
 * The editor engine is now the vanilla TipTap bundle (`PercWidgetEditor.js`
 * + `perc-tiptap-editor.bundle.min.js`, no CMS-served TinyMCE). This file
 * keeps the exact `tinymce.EditorManager.settings` assignment shape that
 * shipped for years, so per-site customizations keep working: the widget
 * editor translates the settings below (translator: `PercWidgetEditor`
 * in `PercWidgetEditor.js`).
 *
 * Honored keys: `toolbar` (any subset of
 * `bold italic underline undo redo styleselect`, string or string array),
 * `content_css` (site stylesheet URL applied to the editing surface AND
 * harvested for `styleselect` site-style entries, PercCustomStylesService
 * semantics), `auto_focus` (editor id to focus on open), `setup`
 * (called with an editor facade exposing `on("click"|"change"|"input")`,
 * `getContent()` and `setContent()`). Unknown keys/tokens are ignored.
 * `branding: false` is inherent to the engine (no branding exists).
 */
window.tinymce = window.tinymce || {};
window.tinymce.EditorManager = window.tinymce.EditorManager || {};
tinymce.EditorManager.settings = {
  selector: "textarea#elm1",
  // General options
  theme: "silver",
  plugins: "-example", // - tells TinyMCE to skip the loading of the plugin
  branding: false,
  // when the editor is initialized, setup event handlers to detect changes and set dirty flag
  // Theme options
  menubar: "null",
  toolbar: ["bold italic underline undo redo styleselect "],
  autosave_ask_before_unload: false,
  // Site stylesheet for the editing surface. Set this to your site CSS so
  // the editor renders content with site styles and `styleselect` offers
  // your site classes. Relative URLs resolve against the delivered site;
  // CMS-hosted /sys_resources/... URLs no longer work here (the CMS
  // TinyMCE bundle was retired, issue #5734).
  content_css: "",
  auto_focus: "elm1",
  setup: function (editor) {
    editor.on("click", function () {
      console.log("Editor was clicked");
    });
  },
};
