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
 * Explorer Delete folder on the product route (#3646 / parent #3102).
 *
 * <p>Default pathmanagement {@code POST …/path/deleteFolder} on
 * {@code spa.jsp?entry=explorer} without {@code rxFolderMutations=1}.
 * Must not soft-skip when a Sites or Assets parent exists. Only a folder
 * this surface created may be deleted — never sample pages.</p>
 *
 * @see tests/explorer-delete-folder.spec.js
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
const {
  cmsUrl,
  recycleFolder,
  PATH_DELETE_FOLDER,
  PATH_ADD_NEW_FOLDER,
} = require("./folder-recycle-smoke");

const DELETE_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  reducedActions: "reduced-actions",
  actionDelete: "action-delete",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-delete-folder",
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
function explorerProductDeleteFolderUrl(baseUrl) {
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
 * Product-default folder delete (pathmanagement). Dual-run RX
 * {@code DELETE /content-explorer/folders/by-id/…} is out of scope.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementDeleteFolderUrl(url) {
  return /\/pathmanagement\/path\/deleteFolder(?:\?|$)/i.test(String(url || ""));
}

/**
 * Legacy/incorrect SPA path that does not exist on {@code PSPathService}.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isLegacyPathDeleteItemUrl(url) {
  const raw = String(url || "");
  return (
    /\/pathmanagement\/path\/delete\//i.test(raw) &&
    !isPathmanagementDeleteFolderUrl(raw)
  );
}

/**
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isRxContentExplorerFoldersUrl(url) {
  return /\/content-explorer\/folders(?:\?|$|\/)/i.test(String(url || ""));
}

/**
 * HTTP success for folder recycle (pathmanagement returns item-count text).
 *
 * @param {number} status
 * @returns {boolean}
 */
function isDeleteFolderSuccessStatus(status) {
  return status === 200 || status === 204;
}

/**
 * Unique folder name for this slice. Only this name may be deleted.
 *
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueDeleteFolderName(nowMs) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  return `qa3646_${ts}`;
}

/**
 * Unwrap a pathmanagement PathItem (flat or Jackson wrap).
 *
 * @param {unknown} body
 * @returns {object}
 */
function unwrapPathItem(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  if (rec.PathItem && typeof rec.PathItem === "object" && !Array.isArray(rec.PathItem)) {
    return /** @type {object} */ (rec.PathItem);
  }
  return rec;
}

/**
 * JAXB / WRAP_ROOT_VALUE body for {@code POST …/deleteFolder}.
 *
 * @param {string} path
 * @param {{ guid?: string, shouldPurge?: boolean, skipItems?: string }} [opts]
 * @returns {{ DeleteFolderCriteria: { path: string, skipItems: string, shouldPurge: boolean, guid: string } }}
 */
function wrapDeleteFolderCriteria(path, opts = {}) {
  const raw = String(path || "").trim();
  const withSlash =
    !raw || raw === "/" ? raw : raw.endsWith("/") ? raw : `${raw}/`;
  const guid = String(opts.guid || "").trim();
  return {
    DeleteFolderCriteria: {
      path: withSlash,
      skipItems: String(opts.skipItems || "NO"),
      shouldPurge: Boolean(opts.shouldPurge),
      guid: /^\d+-\d+(-\d+)*$/.test(guid) ? guid : "",
    },
  };
}

/**
 * Seed an empty disposable {@code qa3646_*} folder under Assets or Sites.
 * Applies Jackson {@code RenameFolderItem} wrap when addNewFolder ignores
 * {@code ?name=}.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ parentPath: string, name: string }} opts
 * @returns {Promise<{ name: string, path: string, guid?: string }>}
 */
async function seedDisposableEmptyFolder(request, baseUrl, headers, opts) {
  const name = String(opts.name || uniqueDeleteFolderName());
  const parent = String(opts.parentPath || "Assets").replace(/^\/+|\/+$/g, "");
  const addUrl = cmsUrl(
    baseUrl,
    `${PATH_ADD_NEW_FOLDER}/${parent}?name=${encodeURIComponent(name)}`,
  );
  const addRes = await request.get(addUrl, { headers });
  if (!addRes.ok()) {
    const text = await addRes.text().catch(() => "");
    throw new Error(
      `seed addNewFolder ${name} under ${parent} failed status=${addRes.status()} body=${text.slice(0, 300)}`,
    );
  }
  const created = unwrapPathItem(await addRes.json().catch(() => ({})));
  let finalName = String(created.name || "");
  let livePath = String(created.path || `/${parent}/${finalName}`);
  let guid = String(created.id || created.guid || "");
  if (finalName !== name) {
    const withSlash = livePath.endsWith("/") ? livePath : `${livePath}/`;
    const renameRes = await request.post(
      cmsUrl(baseUrl, "/Rhythmyx/services/pathmanagement/path/renameFolder"),
      {
        headers: {
          ...headers,
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        data: {
          RenameFolderItem: {
            path: withSlash,
            name,
          },
        },
      },
    );
    if (!renameRes.ok()) {
      const text = await renameRes.text().catch(() => "");
      throw new Error(
        `seed rename ${name} failed status=${renameRes.status()} body=${text.slice(0, 300)}`,
      );
    }
    const item = unwrapPathItem(await renameRes.json().catch(() => ({})));
    finalName = String(item.name || name);
    livePath = String(item.path || livePath);
    guid = String(item.id || guid);
  }
  return {
    name: finalName,
    path: livePath.startsWith("/") ? livePath : `/${livePath}`,
    guid,
  };
}

module.exports = {
  DELETE_TEST_IDS,
  SURFACE_TAGS,
  TEST_IDS,
  explorerProductDeleteFolderUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementDeleteFolderUrl,
  isLegacyPathDeleteItemUrl,
  isRxContentExplorerFoldersUrl,
  isDeleteFolderSuccessStatus,
  uniqueDeleteFolderName,
  unwrapPathItem,
  wrapDeleteFolderCriteria,
  seedDisposableEmptyFolder,
  cmsUrl,
  recycleFolder,
  PATH_DELETE_FOLDER,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
