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
 * Helpers for React Content Editor host Remove from staging (#4916).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  unstage: "editor-unstage-item",
  unstageDone: "editor-unstage-done",
  unstageError: "editor-unstage-error",
});

/**
 * Page staging takedown GET, not live stage and not live takedown.
 * @param {string} url
 * @returns {boolean}
 */
function isPageStagingTakedownUrl(url) {
  return /\/services\/sitemanage\/publish\/takedown\/page\/staging\/[^/?#]+/.test(
    String(url || ""),
  );
}

/**
 * Asset staging takedown GET.
 * @param {string} url
 * @returns {boolean}
 */
function isResourceStagingTakedownUrl(url) {
  return /\/services\/sitemanage\/publish\/takedown\/resource\/staging\/[^/?#]+/.test(
    String(url || ""),
  );
}

/**
 * @param {string} url
 * @returns {{ kind: string, itemId: string }}
 */
function parseStagingTakedownUrl(url) {
  const match = String(url || "").match(
    /\/sitemanage\/publish\/takedown\/(page|resource)\/staging\/([^/?#]+)/,
  );
  return {
    kind: match ? match[1] : "",
    itemId: match ? decodeURIComponent(match[2]) : "",
  };
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isPageStagingTakedownUrl,
  isResourceStagingTakedownUrl,
  parseStagingTakedownUrl,
};
