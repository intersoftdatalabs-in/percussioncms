/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
 * Unit tests for explorer-rename-item helpers (#4636 / #5480) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  RENAME_ITEM_TEST_IDS,
  explorerProductRenameItemUrl,
  hasRxFolderMutationsQuery,
  isFoldersRenameItemUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isRenameItemSuccessStatus,
  isRenameFolderItemRequestEnvelope,
  renameFolderItemRequestNewName,
  isRenameFolderItemRequestForName,
  uniqueRenameItemName,
} = require("../helpers/explorer-rename-item");

describe("explorer-rename-item helpers (#4636 / #5480)", () => {
  it("exports reduced-action rename test ids", () => {
    assert.equal(RENAME_ITEM_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(RENAME_ITEM_TEST_IDS.tree, "explorer-tree");
    assert.equal(RENAME_ITEM_TEST_IDS.detailList, "detail-list");
    assert.equal(RENAME_ITEM_TEST_IDS.actionRename, "action-rename");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductRenameItemUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
  });

  it("classifies rename/item vs folder rename URLs", () => {
    assert.equal(
      isFoldersRenameItemUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/rename/item",
      ),
      true,
    );
    assert.equal(
      isPathmanagementRenameFolderUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      true,
    );
    assert.equal(
      isFoldersRenameItemUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      false,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://127.0.0.1/Rhythmyx/rest/content-explorer/folders",
      ),
      true,
    );
  });

  it("treats 200/201/204 as rename success", () => {
    assert.equal(isRenameItemSuccessStatus(200), true);
    assert.equal(isRenameItemSuccessStatus(201), true);
    assert.equal(isRenameItemSuccessStatus(204), true);
    assert.equal(isRenameItemSuccessStatus(400), false);
    assert.equal(isRenameItemSuccessStatus(403), false);
    assert.equal(isRenameItemSuccessStatus(409), false);
  });

  it("matches RenameFolderItemRequest by exact newName", () => {
    const body = {
      RenameFolderItemRequest: {
        itemPath: "/Assets/qa5480fld/qa5480itm",
        newName: "qa5480ren",
      },
    };
    assert.equal(isRenameFolderItemRequestEnvelope(body), true);
    assert.equal(renameFolderItemRequestNewName(body), "qa5480ren");
    assert.equal(isRenameFolderItemRequestForName(body, "qa5480ren"), true);
    assert.equal(
      isRenameFolderItemRequestForName(body, "qa5480renx409"),
      false,
    );
    assert.equal(isRenameFolderItemRequestForName({}, "qa5480ren"), false);
  });

  it("mints unique rename names", () => {
    const a = uniqueRenameItemName("qa5480", 1);
    const b = uniqueRenameItemName("qa5480", 2);
    assert.equal(a, "qa5480_1");
    assert.equal(b, "qa5480_2");
  });
});
