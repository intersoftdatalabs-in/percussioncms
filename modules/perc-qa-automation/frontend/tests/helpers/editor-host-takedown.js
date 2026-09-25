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
 * Helpers for React Content Editor host Take down (#4862).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  takedown: "editor-takedown",
  takedownDone: "editor-takedown-done",
  takedownError: "editor-takedown-error",
});

/**
 * Live-site takedown page URL (not staging).
 * @param {string} url
 * @returns {boolean}
 */
function isPageTakedownUrl(url) {
  const raw = String(url || "");
  return (
    /\/services\/sitemanage\/publish\/takedown\/page\/[^/?#]+/.test(raw) &&
    !/\/takedown\/page\/staging\//.test(raw)
  );
}

/**
 * Live-site takedown resource URL (not staging).
 * @param {string} url
 * @returns {boolean}
 */
function isResourceTakedownUrl(url) {
  const raw = String(url || "");
  return (
    /\/services\/sitemanage\/publish\/takedown\/resource\/[^/?#]+/.test(raw) &&
    !/\/takedown\/resource\/staging\//.test(raw)
  );
}

/**
 * @param {string} url
 * @returns {{ kind: string, itemId: string }}
 */
function parseTakedownUrl(url) {
  const match = String(url || "").match(
    /\/sitemanage\/publish\/takedown\/(page|resource)\/([^/?#]+)/,
  );
  return {
    kind: match ? match[1] : "",
    itemId: match ? decodeURIComponent(match[2]) : "",
  };
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isPageTakedownUrl,
  isResourceTakedownUrl,
  parseTakedownUrl,
};
