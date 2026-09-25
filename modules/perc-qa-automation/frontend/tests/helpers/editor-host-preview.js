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
 * Helpers for React Content Editor host assembled preview (#4568).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");
const { isEditorHostPreviewUrl } = require("./explorer-preview-view");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  preview: "editor-preview",
  previewDone: "editor-preview-done",
  previewError: "editor-preview-error",
  previewPanel: "editor-preview-panel",
  previewTemplate: "editor-preview-template",
  previewFrame: "editor-preview-frame",
  fieldTitle: "editor-field-sys_title",
});

/**
 * Page Management render used by Explorer / EditorHost page preview.
 * @param {string} url
 * @returns {boolean}
 */
function isPageRenderPreviewUrl(url) {
  return /\/pagemanagement\/render\/page\/[^/?#]+/.test(String(url || ""));
}

/**
 * Asset view-url service (plain-text body, then window.open).
 * @param {string} url
 * @returns {boolean}
 */
function isAssetViewUrlRequest(url) {
  return /\/assetmanagement\/asset\/assetViewUrl\/[^/?#]+/.test(
    String(url || ""),
  );
}

/**
 * @param {string} url
 * @returns {{ kind: string, itemId: string }}
 */
function parseEditorPreviewUrl(url) {
  const raw = String(url || "");
  const page = raw.match(/\/pagemanagement\/render\/page\/([^/?#]+)/);
  if (page) {
    return { kind: "page", itemId: decodeURIComponent(page[1]) };
  }
  const asset = raw.match(/\/assetmanagement\/asset\/assetViewUrl\/([^/?#]+)/);
  if (asset) {
    return { kind: "asset", itemId: decodeURIComponent(asset[1]) };
  }
  return { kind: "", itemId: "" };
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isEditorHostPreviewUrl,
  isPageRenderPreviewUrl,
  isAssetViewUrlRequest,
  parseEditorPreviewUrl,
};
