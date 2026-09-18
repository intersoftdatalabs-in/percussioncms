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
 * Helpers for React Content Editor host new copy / promotable (#4570).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  contentId: "editor-content-id",
  newCopy: "editor-new-copy",
  promotable: "editor-promotable-version",
  copyError: "editor-copy-error",
});

/**
 * @param {string} url
 * @returns {boolean}
 */
function isNewCopyUrl(url) {
  return /\/services\/itemmanagement\/item\/newCopy\/[^/?#]+/.test(String(url || ""));
}

/**
 * @param {string} url
 * @returns {boolean}
 */
function isPromotableUrl(url) {
  return /\/services\/itemmanagement\/item\/promotableVersion\/[^/?#]+/.test(
    String(url || ""),
  );
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isNewCopyUrl,
  isPromotableUrl,
};
