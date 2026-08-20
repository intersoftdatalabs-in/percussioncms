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
 * Explorer dual-run RX folder mutations (#3654 / parent #3102).
 *
 * <p>{@code spa.jsp?entry=explorer&rxFolderMutations=1} Create / Rename /
 * Delete under Folders or Sites must hit content-explorer folders REST
 * (HTTP 200) and must not post pathmanagement. Product default stays flag
 * off — this helper is diagnostic only.</p>
 *
 * @see tests/explorer-rx-folder-mutations.spec.js
 */

"use strict";

const {
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  expandExplorerTreeNode,
} = require("./explorer-sites-list-create");
const {
  TEST_IDS,
  assetsFolderUrl,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-sites-assets-tree-list");

const RX_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  reducedActions: "reduced-actions",
  actionCreateFolder: "action-create-folder",
  actionRename: "action-rename",
  actionDelete: "action-delete",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-rx-folder-mutations",
  "explorer",
  "folder",
  "smoke",
]);

const RX_FOLDER_MUTATIONS_QUERY = "rxFolderMutations";

/**
 * Diagnostic Explorer URL with dual-run flag on. Operators do not use this.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerRxFolderMutationsUrl(baseUrl) {
  const product = explorerSpaUrl(baseUrl);
  const glue = product.includes("?") ? "&" : "?";
  return `${product}${glue}${RX_FOLDER_MUTATIONS_QUERY}=1`;
}

/**
 * True when the URL enables {@code rxFolderMutations}.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function hasRxFolderMutationsQuery(url) {
  const raw = String(url || "");
  try {
    const parsed = new URL(raw, "http://localhost");
    const v = String(parsed.searchParams.get(RX_FOLDER_MUTATIONS_QUERY) || "")
      .trim()
      .toLowerCase();
    return v === "1" || v === "true" || v === "on";
  } catch {
    return /[?&]rxFolderMutations=(1|true|on)(?:&|$)/i.test(raw);
  }
}

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function rxFoldersRestBase(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/rest/content-explorer/folders`;
}

/**
 * Encode an RX / finder path for {@code /by-path/{path:.+}}.
 *
 * @param {string | null | undefined} cmsPath
 * @returns {string}
 */
function encodeRxFolderPath(cmsPath) {
  let p = String(cmsPath || "")
    .trim()
    .replace(/\\/g, "/");
  while (p.startsWith("/")) {
    p = p.slice(1);
  }
  return p
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}

/**
 * @param {string} baseUrl
 * @param {string} cmsPath
 * @returns {string}
 */
function rxFolderByPathUrl(baseUrl, cmsPath) {
  const suffix = encodeRxFolderPath(cmsPath);
  return `${rxFoldersRestBase(baseUrl)}/by-path/${suffix}`;
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isRxContentExplorerFoldersUrl(url) {
  return /\/content-explorer\/folders(?:\?|$|\/)/i.test(String(url || ""));
}

/**
 * POST create (not by-id / by-path / tree / move-children).
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isRxFolderCreateUrl(url) {
  return /\/content-explorer\/folders\/?(?:\?.*)?$/i.test(String(url || ""));
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isRxFolderByIdUrl(url) {
  return /\/content-explorer\/folders\/by-id\//i.test(String(url || ""));
}

/**
 * Pathmanagement mutation endpoints that flag-on Create/Rename/Delete must
 * not hit under Folders/Sites. Browse {@code /path/folder} stays allowed.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementFolderMutationUrl(url) {
  return /\/pathmanagement\/path\/(?:addNewFolder|renameFolder|deleteFolder|moveItem|delete\/)/i.test(
    String(url || ""),
  );
}

/**
 * HTTP success for RX create / rename / delete.
 *
 * @param {number} status
 * @returns {boolean}
 */
function isRxFolderMutationSuccessStatus(status) {
  return status === 200 || status === 201 || status === 204;
}

/**
 * Unique disposable folder name for this slice ({@code qa3654_*}).
 *
 * @param {string} [prefix]
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueRxFolderName(prefix, nowMs) {
  const p = String(prefix || "qa3654").replace(/[^A-Za-z0-9_]/g, "") || "qa3654";
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  return `${p}_${ts}`;
}

/**
 * Unwrap RxFolder (flat or {@code RxFolder} wrap).
 *
 * @param {unknown} body
 * @returns {object}
 */
function unwrapRxFolder(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  if (rec.RxFolder && typeof rec.RxFolder === "object" && !Array.isArray(rec.RxFolder)) {
    return /** @type {object} */ (rec.RxFolder);
  }
  return rec;
}

/**
 * JAXB / WRAP_ROOT_VALUE body for {@code POST /content-explorer/folders}.
 *
 * @param {string} name
 * @param {string} parentPath
 * @returns {{ AddFolderRequest: { name: string, parentPath: string } }}
 */
function wrapAddFolderRequest(name, parentPath) {
  return {
    AddFolderRequest: {
      name: String(name || ""),
      parentPath: String(parentPath || ""),
    },
  };
}

module.exports = {
  RX_TEST_IDS,
  SURFACE_TAGS,
  TEST_IDS,
  RX_FOLDER_MUTATIONS_QUERY,
  explorerRxFolderMutationsUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  rxFoldersRestBase,
  encodeRxFolderPath,
  rxFolderByPathUrl,
  isRxContentExplorerFoldersUrl,
  isRxFolderCreateUrl,
  isRxFolderByIdUrl,
  isPathmanagementFolderMutationUrl,
  isRxFolderMutationSuccessStatus,
  uniqueRxFolderName,
  unwrapRxFolder,
  wrapAddFolderRequest,
  treeRootLocator,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  isKnownExplorerSitesConsoleNoise,
};
