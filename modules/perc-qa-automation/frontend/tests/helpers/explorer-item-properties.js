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
 * Explorer item properties save (#4701 / parent #4530).
 *
 * @see tests/explorer-item-properties.spec.js
 */

"use strict";

const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-copy-item");

const SURFACE_TAGS = Object.freeze([
  "explorer-item-properties",
  "explorer",
  "item",
  "smoke",
]);

const ITEM_PROPS_TEST_IDS = Object.freeze({
  ...COPY_TEST_IDS,
  toggle: "explorer-toggle-item-properties",
  panel: "item-properties-panel",
  hint: "explorer-item-properties-hint",
  name: "item-properties-name",
  displayTitle: "item-properties-display-title",
  save: "item-properties-save",
  readonly: "item-properties-readonly",
});

function explorerProductItemPropertiesUrl(baseUrl) {
  return explorerProductCopyItemUrl(baseUrl);
}

function itemPropertiesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/Rhythmyx/rest/folders/item-properties`;
}

function isItemPropertiesSaveUrl(url) {
  const u = String(url || "");
  return (
    u.includes("/rest/folders/item-properties") &&
    !u.includes("/item-properties/")
  );
}

function wrapItemPropertiesRequest(itemPath, name, displayTitle) {
  return {
    ItemPropertiesRequest: {
      itemPath,
      name,
      displayTitle,
    },
  };
}

module.exports = {
  SURFACE_TAGS,
  ITEM_PROPS_TEST_IDS,
  explorerProductItemPropertiesUrl,
  assetsFolderUrl,
  itemPropertiesUrl,
  isItemPropertiesSaveUrl,
  wrapItemPropertiesRequest,
  hasRxFolderMutationsQuery,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
