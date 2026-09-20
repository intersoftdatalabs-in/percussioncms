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
 * Explorer Rename selected non-folder item on the product route
 * (#4636 / parent #4530).
 *
 * @see tests/explorer-rename-item.spec.js
 */

"use strict";

const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-copy-item");

const SURFACE_TAGS = Object.freeze([
  "explorer-rename-item",
  "explorer",
  "item",
  "smoke",
]);

const RENAME_ITEM_TEST_IDS = Object.freeze({
  ...COPY_TEST_IDS,
  reducedActions: "reduced-actions",
  actionRename: "action-rename",
});

function explorerProductRenameItemUrl(baseUrl) {
  return explorerProductCopyItemUrl(baseUrl);
}

function isFoldersRenameItemUrl(url) {
  return String(url || "").includes("/rest/folders/rename/item");
}

function isPathmanagementRenameFolderUrl(url) {
  return String(url || "").includes("/pathmanagement/path/renameFolder");
}

function isRenameItemSuccessStatus(status) {
  return status === 200 || status === 201 || status === 204;
}

function isRenameFolderItemRequestEnvelope(body) {
  if (!body || typeof body !== "object") {
    return false;
  }
  const nested = body.RenameFolderItemRequest;
  if (!nested || typeof nested !== "object") {
    return false;
  }
  return Boolean(nested.itemPath) && Boolean(nested.newName);
}

module.exports = {
  RENAME_ITEM_TEST_IDS,
  SURFACE_TAGS,
  explorerProductRenameItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersRenameItemUrl,
  isPathmanagementRenameFolderUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isRenameItemSuccessStatus,
  isRenameFolderItemRequestEnvelope,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
