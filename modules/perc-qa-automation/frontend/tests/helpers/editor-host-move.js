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
 * React Content Editor host — move the open item to another folder (#4774).
 *
 * <p>CMS URL paths use {@code /}. Reuses public REST
 * {@code POST /rest/folders/move/item}.</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  contentId: "editor-content-id",
  move: "editor-move",
  moveError: "editor-move-error",
  moveDone: "editor-move-done",
  destInput: "explorer-move-dest-input",
  destOk: "explorer-move-dest-ok",
  destCancel: "explorer-move-dest-cancel",
});

/**
 * Numeric content id from a CMS GUID {@code host-type-uuid} or a plain id.
 *
 * @param {string} id
 * @returns {string}
 */
function numericContentId(id) {
  const raw = String(id || "").trim();
  const guid = /^(\d+)-(\d+)-(\d+)$/.exec(raw);
  if (guid) {
    return guid[3];
  }
  return raw;
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isFoldersMoveItemUrl(url) {
  return /\/rest\/folders\/move\/item(?:\?|$)/.test(String(url || ""));
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  numericContentId,
  isFoldersMoveItemUrl,
};
