/**
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
 * Explorer Rename folder on the product route (#3645 / parent #3102).
 *
 * <p>Default pathmanagement rename on {@code spa.jsp?entry=explorer}
 * without {@code rxFolderMutations=1}. Must not soft-skip when a Sites or
 * Assets folder exists (H2 qa-up demo-sites).</p>
 *
 * @see tests/explorer-rename-folder.spec.js
 */

"use strict";

const {
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
} = require("./explorer-sites-list-create");
const {
  TEST_IDS,
  assetsFolderUrl,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-sites-assets-tree-list");

const RENAME_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  reducedActions: "reduced-actions",
  actionRename: "action-rename",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-rename-folder",
  "explorer",
  "folder",
  "smoke",
]);

/**
 * Product Explorer URL must not enable the RX folder-mutations dual-run flag.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerProductRenameFolderUrl(baseUrl) {
  return explorerSpaUrl(baseUrl);
}

/**
 * True when the URL enables {@code rxFolderMutations} (the dual-run flag
 * this slice must not require).
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function hasRxFolderMutationsQuery(url) {
  const raw = String(url || "");
  try {
    const parsed = new URL(raw, "http://localhost");
    const v = String(parsed.searchParams.get("rxFolderMutations") || "")
      .trim()
      .toLowerCase();
    return v === "1" || v === "true" || v === "on";
  } catch {
    return /[?&]rxFolderMutations=(1|true|on)(?:&|$)/i.test(raw);
  }
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementRenameFolderUrl(url) {
  return /\/pathmanagement\/path\/renameFolder(?:\?|$)/i.test(String(url || ""));
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementAddNewFolderUrl(url) {
  return /\/pathmanagement\/path\/addNewFolder(?:\/|\?|$)/i.test(
    String(url || ""),
  );
}

/**
 * Dual-run RX façade — product route with flag off must not hit this.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isRxContentExplorerFoldersUrl(url) {
  return /\/content-explorer\/folders(?:\?|$|\/)/i.test(String(url || ""));
}

/**
 * @param {number} status
 * @returns {boolean}
 */
function isRenameFolderSuccessStatus(status) {
  return status === 200 || status === 201;
}

/**
 * Unique folder names for this slice (seed then UI rename).
 *
 * @param {string} [prefix]
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueRenameFolderName(prefix, nowMs) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  const p = String(prefix || "qa3645").replace(/[^A-Za-z0-9_]/g, "") || "qa3645";
  return `${p}_${ts}`;
}

/**
 * Unwrap a pathmanagement PathItem (flat or Jackson wrap).
 *
 * @param {unknown} body
 * @returns {Record<string, unknown>}
 */
function unwrapPathItem(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  if (rec.PathItem && typeof rec.PathItem === "object" && !Array.isArray(rec.PathItem)) {
    return /** @type {Record<string, unknown>} */ (rec.PathItem);
  }
  return rec;
}

/**
 * JAXB / WRAP_ROOT_VALUE body for {@code POST …/renameFolder}.
 *
 * @param {string} path
 * @param {string} name
 * @returns {{ RenameFolderItem: { path: string, name: string } }}
 */
function wrapRenameFolderItem(path, name) {
  const raw = String(path || "").trim();
  const withSlash =
    !raw || raw === "/" ? raw : raw.endsWith("/") ? raw : `${raw}/`;
  return {
    RenameFolderItem: {
      path: withSlash,
      name: String(name || "").trim(),
    },
  };
}

module.exports = {
  RENAME_TEST_IDS,
  SURFACE_TAGS,
  TEST_IDS,
  explorerProductRenameFolderUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementRenameFolderUrl,
  isPathmanagementAddNewFolderUrl,
  isRxContentExplorerFoldersUrl,
  isRenameFolderSuccessStatus,
  uniqueRenameFolderName,
  unwrapPathItem,
  wrapRenameFolderItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
