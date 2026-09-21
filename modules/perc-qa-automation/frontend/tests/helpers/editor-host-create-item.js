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
 * Helpers for React Content Editor create new item (#4646).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  host: "editor-host",
  panel: "editor-create-panel",
  type: "editor-create-type",
  folder: "editor-create-folder",
  submit: "editor-create-submit",
  error: "editor-create-error",
  contentId: "editor-content-id",
});

/**
 * @param {string} baseUrl
 * @param {string} [query]
 * @returns {string}
 */
function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(
    query.startsWith("?") ? query.slice(1) : query,
  );
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isItemCreateUrl(url) {
  const u = String(url || "");
  return u.includes("/itemmanagement/item/create");
}

module.exports = { TEST_IDS, editorSpaUrl, isItemCreateUrl };
