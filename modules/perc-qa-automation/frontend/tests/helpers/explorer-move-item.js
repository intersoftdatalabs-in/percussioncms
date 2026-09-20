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
 * Explorer Move selected non-folder item on the product route
 * (#4601 / parent #4530).
 *
 * <p>Default public REST {@code POST /rest/folders/move/item} on
 * {@code spa.jsp?entry=explorer} without {@code rxFolderMutations=1}.
 * Distinct from folder Move (#3655) which uses {@code /folders/move/folder}.
 * Must not move golden sample pages — seed a disposable asset under Assets.</p>
 *
 * @see tests/explorer-move-item.spec.js
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
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
  isCopyFolderItemRequestEnvelope,
} = require("./explorer-copy-item");
const {
  isMoveFolderItemEnvelope,
  isFoldersMoveFolderUrl,
  isFoldersMoveItemUrl,
} = require("./explorer-move-folder");

const ITEM_CREATE_PATH = "/Rhythmyx/services/itemmanagement/item/create";

const SURFACE_TAGS = Object.freeze([
  "explorer-move-item",
  "explorer",
  "item",
  "smoke",
]);

/**
 * Move item test ids share ReducedActions test ids with copy.
 *
 * @type {Readonly<{ shell: string, tree: string, detailList: string, reducedActions: string, actionMove: string }>}
 */
const MOVE_ITEM_TEST_IDS = Object.freeze({
  ...COPY_TEST_IDS,
  reducedActions: "reduced-actions",
  actionMove: "action-move",
});

/**
 * Product Explorer URL must not enable the RX folder-mutations dual-run flag.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerProductMoveItemUrl(baseUrl) {
  return explorerProductCopyItemUrl(baseUrl);
}

/**
 * @param {number} status
 * @returns {boolean}
 */
function isMoveItemSuccessStatus(status) {
  return status === 200 || status === 201 || status === 204;
}

module.exports = {
  MOVE_ITEM_TEST_IDS,
  SURFACE_TAGS,
  PREFERRED_CREATE_TYPE_NAMES: require("./explorer-copy-item")
    .PREFERRED_CREATE_TYPE_NAMES,
  ITEM_CREATE_PATH,
  explorerProductMoveItemUrl,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl,
  isFoldersMoveFolderUrl,
  isFoldersMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isMoveItemSuccessStatus,
  uniqueCopyItemName,
  isMoveFolderItemEnvelope,
  isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
