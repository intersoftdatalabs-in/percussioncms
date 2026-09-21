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
 * Explorer paste clipboard into destination (#4638 / parent #4530).
 *
 * @see tests/explorer-paste-clipboard.spec.js
 */

"use strict";

const copyItem = require("./explorer-copy-item");

const PASTE_TEST_IDS = Object.freeze({
  ...copyItem.COPY_TEST_IDS,
  clipboardAdd: "explorer-clipboard-add",
  clipboardPanel: "explorer-clipboard-panel",
  clipboardPaste: "clipboard-paste",
  clipboardDest: "clipboard-paste-dest",
  menuContent: "explorer-menu-content",
  menuView: "explorer-menu-view",
  toggleClipboard: "explorer-toggle-clipboard",
});

const SURFACE_TAGS = Object.freeze([
  "explorer-paste-clipboard",
  "explorer",
  "item",
  "smoke",
]);

function explorerProductPasteUrl(baseUrl) {
  return copyItem.explorerProductCopyItemUrl(baseUrl);
}

function uniquePasteName(prefix, nowMs) {
  return copyItem.uniqueCopyItemName(prefix || "qa4638", nowMs);
}

module.exports = {
  PASTE_TEST_IDS,
  SURFACE_TAGS,
  explorerProductPasteUrl,
  uniquePasteName,
  COPY_TEST_IDS: copyItem.COPY_TEST_IDS,
  explorerProductCopyItemUrl: copyItem.explorerProductCopyItemUrl,
  assetsFolderUrl: copyItem.assetsFolderUrl,
  hasRxFolderMutationsQuery: copyItem.hasRxFolderMutationsQuery,
  isFoldersCopyFolderUrl: copyItem.isFoldersCopyFolderUrl,
  isFoldersCopyItemUrl: copyItem.isFoldersCopyItemUrl,
  isPathmanagementMoveItemUrl: copyItem.isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl: copyItem.isRxContentExplorerFoldersUrl,
  isCopyFolderSuccessStatus: copyItem.isCopyFolderSuccessStatus,
  isCopyFolderItemRequestEnvelope: copyItem.isCopyFolderItemRequestEnvelope,
  seedDisposableEmptyFolder: copyItem.seedDisposableEmptyFolder,
  seedDisposableAsset: copyItem.seedDisposableAsset,
  recycleFolderWithItems: copyItem.recycleFolderWithItems,
  treeRootLocator: copyItem.treeRootLocator,
  isKnownExplorerSitesConsoleNoise: copyItem.isKnownExplorerSitesConsoleNoise,
};
