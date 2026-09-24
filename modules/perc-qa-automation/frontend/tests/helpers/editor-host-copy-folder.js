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
 * React Content Editor host — copy the open item into a chosen folder (#4793).
 *
 * <p>CMS URL paths use {@code /}. Reuses public REST
 * {@code POST /rest/folders/copy/item}.</p>
 */

"use strict";

const { editorSpaUrl, numericContentId } = require("./editor-host-move");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  contentId: "editor-content-id",
  copyToFolder: "editor-copy-to-folder",
  copyError: "editor-copy-error",
  copyDone: "editor-copy-to-folder-done",
  destInput: "explorer-copy-dest-input",
  destOk: "explorer-copy-dest-ok",
  destCancel: "explorer-copy-dest-cancel",
});

/**
 * @param {string} url
 * @returns {boolean}
 */
function isFoldersCopyItemUrl(url) {
  return /\/rest\/folders\/copy\/item(?:\?|$)/.test(String(url || ""));
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  numericContentId,
  isFoldersCopyItemUrl,
};
