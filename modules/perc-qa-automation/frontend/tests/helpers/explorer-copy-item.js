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
 * Explorer Copy selected non-folder item on the product route
 * (#3656 / parent #3102).
 *
 * <p>Default public REST {@code POST /rest/folders/copy/item} on
 * {@code spa.jsp?entry=explorer} without {@code rxFolderMutations=1}.
 * Distinct from folder Copy (#3647) which uses {@code /folders/copy/folder}.
 * Must not copy golden sample pages — seed a disposable asset under Assets.</p>
 *
 * @see tests/explorer-copy-item.spec.js
 */

"use strict";

const {
  COPY_TEST_IDS,
  explorerProductCopyFolderUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus,
  unwrapPathItem,
  wrapCopyFolderItemRequest,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-copy-folder");
const { cmsUrl, listFolderChildren } = require("./empty-recycling");
const { recycleFolder } = require("./folder-recycle-smoke");

const ITEM_CREATE_PATH = "/Rhythmyx/services/itemmanagement/item/create";

const SURFACE_TAGS = Object.freeze([
  "explorer-copy-item",
  "explorer",
  "item",
  "smoke",
]);

/**
 * Types that Home/Explorer create with sys_title only. Prefer these so
 * live POST create is not a 500 validation page.
 */
const PREFERRED_CREATE_TYPE_NAMES = Object.freeze([
  "percSimpleTextAsset",
  "percRawHtmlAsset",
  "percRichTextAsset",
  "percFileAsset",
  "percFile",
  "rffFile",
]);

/**
 * Product Explorer URL must not enable the RX folder-mutations dual-run flag.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerProductCopyItemUrl(baseUrl) {
  return explorerProductCopyFolderUrl(baseUrl);
}

/**
 * Unique item names for this slice (source fixture).
 *
 * @param {string} [prefix]
 * @param {number} [nowMs]
 * @returns {string}
 */
function uniqueCopyItemName(prefix, nowMs) {
  const ts =
    nowMs != null && Number.isFinite(Number(nowMs))
      ? Number(nowMs)
      : Date.now();
  const p = String(prefix || "qa3656").replace(/[^A-Za-z0-9_]/g, "") || "qa3656";
  return `${p}_${ts}`;
}

/**
 * Same-parent unique copy name from {@code newCopies} (index 2).
 *
 * @param {string} sourceName
 * @returns {string}
 */
function expectedSameParentCopyName(sourceName) {
  return `${String(sourceName || "").trim()}-2`;
}

/**
 * Names to look for in the destination list after Copy.
 *
 * @param {string} sourceName
 * @returns {string[]}
 */
function expectedCopiedItemNames(sourceName) {
  const n = String(sourceName || "").trim();
  return n ? [n, expectedSameParentCopyName(n)] : [];
}

/**
 * Convert a finder path to the repository folder path item/create needs.
 * {@code /Assets/…} is {@code //Folders/$System$/Assets/…} — prefixing
 * {@code /} alone would create under a non-finder {@code //Assets} folder.
 *
 * @param {string | null | undefined} finderPath
 * @returns {string}
 */
function toItemCreateFolderPath(finderPath) {
  const raw = String(finderPath || "")
    .trim()
    .replace(/\\/g, "/");
  if (!raw) {
    return raw;
  }
  const hadRepo = raw.startsWith("//");
  let p = raw.replace(/\/+$/, "");
  if (hadRepo) {
    p = `//${p.replace(/^\/+/, "").replace(/\/{2,}/g, "/")}`;
  } else {
    p = p.replace(/\/{2,}/g, "/");
  }
  const noLead = p.replace(/^\/+/, "");
  if (/^Assets(\/|$)/i.test(noLead)) {
    const rest = noLead.replace(/^Assets/i, "");
    return `//Folders/$System$/Assets${rest}`;
  }
  if (hadRepo) {
    return p;
  }
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  return `/${p}`;
}

/**
 * Unwrap ItemCreateResult (flat or Jackson wrap).
 *
 * @param {unknown} body
 * @returns {Record<string, unknown>}
 */
function unwrapItemCreateResult(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  const nested = rec.ItemCreateResult || rec.itemCreateResult;
  if (nested && typeof nested === "object" && !Array.isArray(nested)) {
    return /** @type {Record<string, unknown>} */ (nested);
  }
  return rec;
}

/**
 * JAXB / WRAP_ROOT_VALUE body for {@code POST …/itemmanagement/item/create}.
 *
 * @param {string} contentType
 * @param {string} folderPath
 * @param {string} name
 * @returns {{ ItemCreateRequest: { contentType: string, folderPath: string, name: string } }}
 */
function wrapItemCreateRequest(contentType, folderPath, name) {
  return {
    ItemCreateRequest: {
      contentType: String(contentType || "").trim(),
      folderPath: String(folderPath || "").trim(),
      name: String(name || "").trim(),
    },
  };
}

/**
 * Recycle a folder and its children ({@code skipItems=NO}).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ path: string, guid?: string }} folder
 * @returns {Promise<void>}
 */
async function recycleFolderWithItems(request, baseUrl, headers, folder) {
  const PATH_DELETE_FOLDER =
    "/Rhythmyx/services/pathmanagement/path/deleteFolder";
  const deletePath = String(folder.path || "").startsWith("/")
    ? String(folder.path)
    : `/${folder.path}`;
  const pathForDelete = deletePath.endsWith("/") ? deletePath : `${deletePath}/`;
  const delRes = await request.post(cmsUrl(baseUrl, PATH_DELETE_FOLDER), {
    headers: {
      ...headers,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    data: {
      DeleteFolderCriteria: {
        path: pathForDelete,
        shouldPurge: false,
        skipItems: "NO",
        guid: folder.guid || "",
      },
    },
  });
  if (delRes.ok()) {
    return;
  }
  await recycleFolder(request, baseUrl, headers, folder);
}

/**
 * Seed a disposable asset under a parent folder via item/create.
 * Tries Home-create-safe types first. Does not copy golden sample pages.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ parentPath: string, name: string, contentTypes?: readonly string[] }} opts
 * @returns {Promise<{ name: string, path: string, guid?: string, contentType: string }>}
 */
async function seedDisposableAsset(request, baseUrl, headers, opts) {
  const parent = String(opts.parentPath || "/Assets").trim();
  const folderPath = toItemCreateFolderPath(parent);
  const name = String(opts.name || uniqueCopyItemName()).trim();
  const types = opts.contentTypes && opts.contentTypes.length
    ? opts.contentTypes
    : PREFERRED_CREATE_TYPE_NAMES;
  const createUrl = cmsUrl(baseUrl, ITEM_CREATE_PATH);
  /** @type {string[]} */
  const errors = [];
  for (const contentType of types) {
    const res = await request.post(createUrl, {
      headers: {
        ...headers,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      data: wrapItemCreateRequest(contentType, folderPath, name),
    });
    if (!res.ok()) {
      const text = await res.text().catch(() => "");
      errors.push(`${contentType}:${res.status()}:${text.slice(0, 120)}`);
      continue;
    }
    const created = unwrapItemCreateResult(await res.json().catch(() => ({})));
    const createdName = String(created.name || created.Name || name);
    const guid = String(created.itemId || created.ItemId || created.id || "");
    if (guid) {
      // New-copy clone requires a workflow stateId > 0. Check in the stub
      // so sys_wfPerformTransition does not 500 (#3656).
      const checkInUrl = cmsUrl(
        baseUrl,
        `/Rhythmyx/services/itemmanagement/workflow/checkIn/${encodeURIComponent(guid)}`,
      );
      await request.get(checkInUrl, { headers }).catch(() => undefined);
    }
    const listPath = String(parent || "Assets")
      .replace(/^\/+/, "")
      .replace(/\/+$/, "");
    const kids = await listFolderChildren(request, baseUrl, headers, listPath);
    const listed = kids.find((row) => {
      const n = String(row.name || row.Name || "").trim();
      const id = String(row.id || row.guid || row.itemId || "").trim();
      return n === createdName || (guid && id === guid);
    });
    if (!listed) {
      const names = kids
        .map((row) => String(row.name || row.Name || ""))
        .filter(Boolean)
        .join(",");
      throw new Error(
        `item/create ${contentType} ${createdName} not listed under ${listPath} (children=${names || "none"})`,
      );
    }
    const livePath = String(
      listed.path || listed.Path || `${parent.replace(/\/+$/, "")}/${createdName}`,
    );
    return {
      name: String(listed.name || createdName),
      path: livePath,
      guid: String(listed.id || guid),
      contentType: String(created.contentType || created.ContentType || contentType),
    };
  }
  throw new Error(
    `item/create under ${folderPath} failed for ${types.join(",")}: ${errors.join(" | ")}`,
  );
}

module.exports = {
  COPY_TEST_IDS,
  SURFACE_TAGS,
  PREFERRED_CREATE_TYPE_NAMES,
  ITEM_CREATE_PATH,
  explorerProductCopyItemUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus,
  uniqueCopyItemName,
  expectedSameParentCopyName,
  expectedCopiedItemNames,
  toItemCreateFolderPath,
  unwrapPathItem,
  unwrapItemCreateResult,
  wrapCopyFolderItemRequest,
  wrapItemCreateRequest,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  recycleFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
