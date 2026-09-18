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
 * Helpers for React Content Editor host Publish now (#4540).
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

"use strict";

const { editorSpaUrl } = require("./editor-host-workflow");

const TEST_IDS = Object.freeze({
  host: "editor-host",
  form: "editor-form",
  publishNow: "editor-publish-now",
  publishDone: "editor-publish-done",
  publishError: "editor-publish-error",
});

/**
 * Demand-publish page GET (not staging).
 * @param {string} url
 * @returns {boolean}
 */
function isPagePublishUrl(url) {
  const raw = String(url || "");
  return (
    /\/services\/sitemanage\/publish\/page\/[^/?#]+/.test(raw) &&
    !/\/publish\/page\/staging\//.test(raw)
  );
}

/**
 * Demand-publish resource GET (not staging).
 * @param {string} url
 * @returns {boolean}
 */
function isResourcePublishUrl(url) {
  const raw = String(url || "");
  return (
    /\/services\/sitemanage\/publish\/resource\/[^/?#]+/.test(raw) &&
    !/\/publish\/resource\/staging\//.test(raw)
  );
}

/**
 * @param {string} url
 * @returns {{ kind: string, itemId: string }}
 */
function parsePublishUrl(url) {
  const match = String(url || "").match(
    /\/sitemanage\/publish\/(page|resource)\/([^/?#]+)/,
  );
  return {
    kind: match ? match[1] : "",
    itemId: match ? decodeURIComponent(match[2]) : "",
  };
}

module.exports = {
  TEST_IDS,
  editorSpaUrl,
  isPagePublishUrl,
  isResourcePublishUrl,
  parsePublishUrl,
};
