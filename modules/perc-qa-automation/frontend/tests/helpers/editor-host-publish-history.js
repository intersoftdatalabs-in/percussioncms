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
 * Helpers for React Content Editor host publish history (#4863).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  history: "editor-publishing-history",
  dialog: "explorer-publishing-history-dialog",
  close: "explorer-publishing-history-close",
  row: "item-history-row",
  empty: "item-history-empty",
  error: "item-history-error",
  publishNow: "editor-publish-now",
});

/**
 * Item publish history GET. Does not match demand-publish URLs.
 * @param {string} url
 * @returns {boolean}
 */
function isPubHistoryUrl(url) {
  return /\/services\/itemmanagement\/item\/pubhistory\/[^/?#]+/.test(
    String(url || ""),
  );
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isPubHistoryUrl,
};
