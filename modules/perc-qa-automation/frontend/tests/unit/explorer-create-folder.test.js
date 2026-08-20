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
 * Unit tests for explorer-create-folder helpers (#3640) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  CREATE_TEST_IDS,
  explorerProductCreateFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementAddNewFolderUrl,
  isPathmanagementRenameFolderUrl,
  isRxContentExplorerFoldersUrl,
  isCreateFolderSuccessStatus,
  shouldSkipCreateFolder,
  uniqueCreateFolderName,
  unwrapCreatedPathItem,
} = require("../helpers/explorer-create-folder");

describe("explorer-create-folder helpers (#3640)", () => {
  it("exports reduced-action create test ids", () => {
    assert.equal(CREATE_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(CREATE_TEST_IDS.tree, "explorer-tree");
    assert.equal(CREATE_TEST_IDS.detailList, "detail-list");
    assert.equal(CREATE_TEST_IDS.actionCreateFolder, "action-create-folder");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductCreateFolderUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
  });

  it("detects rxFolderMutations query values", () => {
    assert.equal(
      hasRxFolderMutationsQuery(
        "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=1",
      ),
      true,
    );
    assert.equal(
      hasRxFolderMutationsQuery(
        "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=0",
      ),
      false,
    );
    assert.equal(
      hasRxFolderMutationsQuery(
        "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer",
      ),
      false,
    );
  });

  it("matches pathmanagement addNewFolder and not RX folders REST", () => {
    assert.equal(
      isPathmanagementAddNewFolderUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/addNewFolder/Assets?name=qa",
      ),
      true,
    );
    assert.equal(
      isPathmanagementRenameFolderUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/addNewFolder/Assets",
      ),
      false,
    );
  });

  it("treats HTTP 200/201 as create success", () => {
    assert.equal(isCreateFolderSuccessStatus(200), true);
    assert.equal(isCreateFolderSuccessStatus(201), true);
    assert.equal(isCreateFolderSuccessStatus(500), false);
  });

  it("shouldSkipCreateFolder is false when a Sites/Assets parent exists", () => {
    assert.equal(
      shouldSkipCreateFolder({ sitesRootVisible: true }),
      false,
    );
    assert.equal(
      shouldSkipCreateFolder({ assetsRootVisible: true }),
      false,
    );
    assert.equal(shouldSkipCreateFolder({ restParentOk: true }), false);
    assert.equal(shouldSkipCreateFolder({ testDbType: "h2" }), false);
    assert.equal(shouldSkipCreateFolder({}), false);
  });

  it("uniqueCreateFolderName is stable for a given timestamp", () => {
    assert.equal(uniqueCreateFolderName(1700000000000), "qa3640_1700000000000");
  });

  it("unwrapCreatedPathItem reads Jackson PathItem wrap", () => {
    assert.deepEqual(
      unwrapCreatedPathItem({ PathItem: { name: "qa3640", path: "/Assets/qa3640" } }),
      { name: "qa3640", path: "/Assets/qa3640" },
    );
    assert.deepEqual(unwrapCreatedPathItem({ name: "flat" }), { name: "flat" });
  });
});
