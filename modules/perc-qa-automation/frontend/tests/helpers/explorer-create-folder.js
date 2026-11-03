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
 * Public REST create folder (#4637).
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isFoldersCreateUrl(url) {
  return /\/rest\/folders\/create(?:\?|$)/i.test(String(url || ""));
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
 * Unique folder name for this slice (cleanup via recycle after).
 *
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueCreateFolderName(nowMs) {
  return uniqueQaFolderName("qa4637", nowMs, "");
}

/**
 * Unique Explorer folder name. Entropy avoids leftover-name HTTP 409 on
 * skip-image-build H2 cells when Date.now() is not enough (#5507).
 *
 * @param {string} prefix
 * @param {number} [nowMs]
 * @param {string} [entropy]
 * @returns {string}
 */
function uniqueQaFolderName(prefix, nowMs, entropy) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  const head = String(prefix || "qa").replace(/[^A-Za-z0-9_]/g, "") || "qa";
  const extra =
    entropy === undefined
      ? Math.random().toString(36).slice(2, 8)
      : String(entropy ?? "")
          .replace(/[^A-Za-z0-9]/g, "")
          .slice(0, 8);
  const name = extra ? `${head}_${ts}_${extra}` : `${head}_${ts}`;
  return name.length > 50 ? name.slice(0, 50) : name;
}

/**
 * {@code ?name=} on pathmanagement addNewFolder, or null if unparseable.
 *
 * @param {string} url
 * @returns {string | null}
 */
function pathmanagementAddNewFolderName(url) {
  try {
    return new URL(url, "http://localhost").searchParams.get("name");
  } catch {
    return null;
  }
}

/**
 * Exact folder name from a {@code CreateFolderRequest} JSON body.
 *
 * @param {string} raw
 * @returns {string}
 */
function createFolderJsonName(raw) {
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return "";
    }
    const inner = parsed.CreateFolderRequest || parsed;
    return String(inner.name || "");
  } catch {
    return "";
  }
}

/**
 * True when a create-folder request carries this exact folder name
 * (JSON {@code CreateFolderRequest.name} or pathmanagement {@code ?name=}).
 * Prefix matches are not enough: fail names {@code ${name}x409} must not
 * satisfy the success wait (#5507).
 *
 * @param {{ url?: () => string, postData?: () => string | null } | null | undefined} request
 * @param {string} name
 * @returns {boolean}
 */
function createFolderRequestCarriesName(request, name) {
  const wanted = String(name || "");
  if (!wanted || !request) {
    return false;
  }
  const url = typeof request.url === "function" ? String(request.url() || "") : "";
  if (isPathmanagementAddNewFolderUrl(url)) {
    return pathmanagementAddNewFolderName(url) === wanted;
  }
  const raw =
    typeof request.postData === "function" ? String(request.postData() || "") : "";
  return Boolean(raw) && createFolderJsonName(raw) === wanted;
}

/**
 * True when this response is a non-OPTIONS create-folder mutation for
 * {@code name} (public REST or pathmanagement).
 *
 * @param {{ url?: () => string, request?: () => { method?: () => string, url?: () => string, postData?: () => string | null } } | null | undefined} res
 * @param {string} name
 * @returns {boolean}
 */
function isCreateFolderMutationForName(res, name) {
  if (!res || typeof res.url !== "function" || typeof res.request !== "function") {
    return false;
  }
  const req = res.request();
  if (!req || typeof req.method !== "function") {
    return false;
  }
  if (req.method() === "OPTIONS") {
    return false;
  }
  const url = String(res.url() || "");
  if (!isFoldersCreateUrl(url) && !isPathmanagementAddNewFolderUrl(url)) {
    return false;
  }
  return createFolderRequestCarriesName(req, name);
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
  if (rec.Folder && typeof rec.Folder === "object") {
    return /** @type {object} */ (rec.Folder);
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
  isFoldersCreateUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  uniqueCreateFolderName,
  uniqueQaFolderName,
  createFolderRequestCarriesName,
  isCreateFolderMutationForName,
  unwrapCreatedPathItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
