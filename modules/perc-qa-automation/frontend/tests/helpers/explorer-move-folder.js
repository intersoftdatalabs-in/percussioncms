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
 * Explorer Move folder on the product route (#3655 / parent #3102).
 *
 * <p>Default pathmanagement {@code POST …/path/moveItem} on
 * {@code spa.jsp?entry=explorer} without {@code rxFolderMutations=1}.
 * Must not soft-skip when a Sites or Assets parent exists (H2 qa-up
 * demo-sites). Distinct from Copy (#3647) and clipboard paste (#2408).</p>
 *
 * @see tests/explorer-move-folder.spec.js
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
const { cmsUrl, PATH_ADD_NEW_FOLDER } = require("./empty-recycling");

const PATH_RENAME_FOLDER =
  "/Rhythmyx/services/pathmanagement/path/renameFolder";

const MOVE_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  reducedActions: "reduced-actions",
  actionMove: "action-move",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-move-folder",
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
function explorerProductMoveFolderUrl(baseUrl) {
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
 * Pathmanagement moveItem.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isPathmanagementMoveItemUrl(url) {
  return /\/pathmanagement\/path\/moveItem(?:\?|$)/i.test(String(url || ""));
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
 * Public REST folder copy — Move must not POST this.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isFoldersCopyFolderUrl(url) {
  return /\/rest\/folders\/copy\/folder(?:\?|$)/i.test(String(url || ""));
}

/**
 * @param {number} status
 * @returns {boolean}
 */
function isMoveFolderSuccessStatus(status) {
  return status === 200 || status === 204;
}

/**
 * Unique folder names for this slice (source / dest fixtures).
 *
 * @param {string} [prefix]
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueMoveFolderName(prefix, nowMs) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  const p = String(prefix || "qa3655").replace(/[^A-Za-z0-9_]/g, "") || "qa3655";
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
  if (
    rec.PathItem &&
    typeof rec.PathItem === "object" &&
    !Array.isArray(rec.PathItem)
  ) {
    return /** @type {Record<string, unknown>} */ (rec.PathItem);
  }
  return rec;
}

/**
 * JAXB / WRAP_ROOT_VALUE body for {@code POST …/path/moveItem}.
 *
 * @param {string} itemPath
 * @param {string} targetFolderPath
 * @returns {{ MoveFolderItem: { itemPath: string, targetFolderPath: string } }}
 */
function wrapMoveFolderItem(itemPath, targetFolderPath) {
  const withSlash = (p) => {
    const t = String(p || "").trim().replace(/\\/g, "/");
    if (!t) return t;
    return t.startsWith("/") ? t : `/${t}`;
  };
  return {
    MoveFolderItem: {
      itemPath: withSlash(itemPath),
      targetFolderPath: withSlash(targetFolderPath),
    },
  };
}

/**
 * True when a JSON body is a wrapped MoveFolderItem (not a bare
 * {@code sourcePath} root).
 *
 * @param {unknown} body
 * @returns {boolean}
 */
function isMoveFolderItemEnvelope(body) {
  if (body == null || typeof body !== "object" || Array.isArray(body)) {
    return false;
  }
  const inner = /** @type {Record<string, unknown>} */ (body).MoveFolderItem;
  if (inner == null || typeof inner !== "object" || Array.isArray(inner)) {
    return false;
  }
  const rec = /** @type {Record<string, unknown>} */ (inner);
  return (
    typeof rec.itemPath === "string" &&
    typeof rec.targetFolderPath === "string" &&
    rec.sourcePath === undefined &&
    rec.copy === undefined
  );
}

/**
 * Seed an empty disposable folder under Sites or Assets (addNewFolder +
 * rename when the server ignores {@code ?name=}).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ parentPath: string, name: string }} opts
 * @returns {Promise<{ name: string, path: string, guid?: string }>}
 */
async function seedDisposableEmptyFolder(request, baseUrl, headers, opts) {
  const parent = String(opts.parentPath || "Assets").replace(/^\/+|\/+$/g, "");
  const name = String(opts.name || uniqueMoveFolderName()).trim();
  const addUrl = cmsUrl(
    baseUrl,
    `${PATH_ADD_NEW_FOLDER}/${parent}?name=${encodeURIComponent(name)}`,
  );
  let addRes = await request.get(addUrl, { headers });
  if (!addRes.ok()) {
    addRes = await request.get(
      cmsUrl(baseUrl, `${PATH_ADD_NEW_FOLDER}/${parent}`),
      { headers },
    );
  }
  if (!addRes.ok()) {
    const text = await addRes.text().catch(() => "");
    throw new Error(
      `addNewFolder under ${parent} failed status=${addRes.status()} body=${text.slice(0, 300)}`,
    );
  }
  const created = unwrapPathItem(await addRes.json().catch(() => ({})));
  const createdName = String(created.name || "New-Folder");
  let livePath = String(created.path || `/${parent}/${createdName}`);
  let finalName = createdName;
  const guid = String(created.id || created.guid || "");
  if (createdName !== name) {
    const withSlash = livePath.endsWith("/") ? livePath : `${livePath}/`;
    const renameRes = await request.post(cmsUrl(baseUrl, PATH_RENAME_FOLDER), {
      headers: {
        ...headers,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      data: {
        RenameFolderItem: { path: withSlash, name },
      },
    });
    if (renameRes.ok()) {
      const renamed = unwrapPathItem(await renameRes.json().catch(() => ({})));
      finalName = String(renamed.name || name);
      livePath = String(renamed.path || `/${parent}/${finalName}`);
    }
  }
  return { name: finalName, path: livePath, guid };
}

module.exports = {
  MOVE_TEST_IDS,
  SURFACE_TAGS,
  TEST_IDS,
  explorerProductMoveFolderUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isFoldersCopyFolderUrl,
  isMoveFolderSuccessStatus,
  uniqueMoveFolderName,
  unwrapPathItem,
  wrapMoveFolderItem,
  isMoveFolderItemEnvelope,
  seedDisposableEmptyFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
