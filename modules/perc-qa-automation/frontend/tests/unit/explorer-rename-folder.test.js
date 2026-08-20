/*
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
 * Unit tests for explorer-rename-folder helpers (#3645) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  RENAME_TEST_IDS,
  explorerProductRenameFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementRenameFolderUrl,
  isPathmanagementAddNewFolderUrl,
  isRxContentExplorerFoldersUrl,
  isRenameFolderSuccessStatus,
  uniqueRenameFolderName,
  unwrapPathItem,
  wrapRenameFolderItem,
} = require("../helpers/explorer-rename-folder");

describe("explorer-rename-folder helpers (#3645)", () => {
  it("exports stable product test ids", () => {
    assert.equal(RENAME_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(RENAME_TEST_IDS.tree, "explorer-tree");
    assert.equal(RENAME_TEST_IDS.detailList, "detail-list");
    assert.equal(RENAME_TEST_IDS.actionRename, "action-rename");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductRenameFolderUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
    assert.equal(
      hasRxFolderMutationsQuery(
        "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=1",
      ),
      true,
    );
    assert.equal(hasRxFolderMutationsQuery("http://x/?rxFolderMutations=0"), false);
  });

  it("classifies pathmanagement rename vs RX folders URLs", () => {
    assert.equal(
      isPathmanagementRenameFolderUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      true,
    );
    assert.equal(
      isPathmanagementAddNewFolderUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/addNewFolder/Assets?name=x",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://127.0.0.1/Rhythmyx/rest/content-explorer/folders",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      false,
    );
  });

  it("wraps RenameFolderItem with trailing slash and server field name", () => {
    assert.deepEqual(wrapRenameFolderItem("/Assets/Old", "New"), {
      RenameFolderItem: { path: "/Assets/Old/", name: "New" },
    });
    assert.deepEqual(wrapRenameFolderItem("/Assets/Old/", "New"), {
      RenameFolderItem: { path: "/Assets/Old/", name: "New" },
    });
  });

  it("unwraps PathItem and treats 200/201 as success", () => {
    assert.equal(isRenameFolderSuccessStatus(200), true);
    assert.equal(isRenameFolderSuccessStatus(201), true);
    assert.equal(isRenameFolderSuccessStatus(400), false);
    assert.deepEqual(unwrapPathItem({ PathItem: { name: "A", path: "/A" } }), {
      name: "A",
      path: "/A",
    });
    assert.deepEqual(unwrapPathItem({ name: "flat" }), { name: "flat" });
    assert.deepEqual(unwrapPathItem(null), {});
  });

  it("uniqueRenameFolderName is stable for a given timestamp", () => {
    assert.equal(uniqueRenameFolderName("qa3645", 1), "qa3645_1");
    assert.match(uniqueRenameFolderName(), /^qa3645_\d+$/);
  });
});
