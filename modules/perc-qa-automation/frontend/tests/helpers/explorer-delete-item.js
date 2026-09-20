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
 * Explorer Delete selected non-folder item on the product route (#4602).
 */

"use strict";

const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  recycleFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-copy-item");
const {
  isPathmanagementDeleteFolderUrl,
} = require("./explorer-delete-folder");

const DELETE_ITEM_TEST_IDS = Object.freeze({
  ...COPY_TEST_IDS,
  actionDelete: "action-delete",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-delete-item",
  "explorer",
  "item",
  "smoke",
]);

function explorerProductDeleteItemUrl(baseUrl) {
  return explorerProductCopyItemUrl(baseUrl);
}

function uniqueDeleteItemName(prefix, nowMs) {
  return uniqueCopyItemName(prefix || "qa4602", nowMs);
}

function isFoldersDeleteItemUrl(url) {
  const u = String(url || "");
  return /\/rest\/folders\/item\//.test(u) && !/\/copy\/|\/move\//.test(u);
}

function isDeleteItemSuccessStatus(status) {
  return Number(status) === 200;
}

module.exports = {
  DELETE_ITEM_TEST_IDS,
  SURFACE_TAGS,
  explorerProductDeleteItemUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersDeleteItemUrl,
  isFoldersCopyFolderUrl,
  isPathmanagementDeleteFolderUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isDeleteItemSuccessStatus,
  uniqueDeleteItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  recycleFolder,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
