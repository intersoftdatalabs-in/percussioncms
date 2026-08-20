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
 * Explorer Create Folder on the product route (#3640 / parent #3102).
 *
 * <p>Default pathmanagement New Folder on {@code spa.jsp?entry=explorer}
 * without {@code rxFolderMutations=1}. Must not soft-skip when a Sites or
 * Assets parent exists (H2 qa-up demo-sites).</p>
 *
 * @see tests/explorer-create-folder.spec.js
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

const CREATE_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  reducedActions: "reduced-actions",
  actionCreateFolder: "action-create-folder",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-create-folder",
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
function explorerProductCreateFolderUrl(baseUrl) {
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
 * Product-default create-folder request (pathmanagement). Dual-run RX
 * {@code POST /content-explorer/folders} is out of scope for this slice.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementAddNewFolderUrl(url) {
  return /\/pathmanagement\/path\/addNewFolder(?:\/|\?|$)/i.test(
    String(url || ""),
  );
}

/**
 * Pathmanagement rename used after addNewFolder ignores {@code ?name=}.
 *
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
function isRxContentExplorerFoldersUrl(url) {
  return /\/content-explorer\/folders(?:\?|$|\/)/i.test(String(url || ""));
}

/**
 * HTTP success for create-folder (pathmanagement historically GET; RX POST).
 *
 * @param {number} status
 * @returns {boolean}
 */
function isCreateFolderSuccessStatus(status) {
  return status === 200 || status === 201;
}

/**
 * Product-route Create Folder must not soft-skip when a Sites/Assets parent
 * is on the tree or REST-listed, or when the cell is H2.
 *
 * @param {{
 *   sitesRootVisible?: boolean,
 *   assetsRootVisible?: boolean,
 *   restParentOk?: boolean,
 *   testDbType?: string,
 * }} [detail]
 * @returns {boolean}
 */
function shouldSkipCreateFolder(detail = {}) {
  if (
    detail.sitesRootVisible === true ||
    detail.assetsRootVisible === true ||
    detail.restParentOk === true
  ) {
    return false;
  }
  const db = String(
    detail.testDbType || process.env.TEST_DB_TYPE || "",
  ).toLowerCase();
  if (db === "h2") {
    return false;
  }
  return false;
}

/**
 * Unique folder name for this slice (cleanup via recycle after).
 *
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueCreateFolderName(nowMs) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  return `qa3640_${ts}`;
}

/**
 * Unwrap a pathmanagement PathItem (flat or Jackson wrap).
 *
 * @param {unknown} body
 * @returns {object}
 */
function unwrapCreatedPathItem(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  if (rec.PathItem && typeof rec.PathItem === "object") {
    return /** @type {object} */ (rec.PathItem);
  }
  return rec;
}

module.exports = {
  CREATE_TEST_IDS,
  SURFACE_TAGS,
  TEST_IDS,
  explorerProductCreateFolderUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementAddNewFolderUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  shouldSkipCreateFolder,
  uniqueCreateFolderName,
  unwrapCreatedPathItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
