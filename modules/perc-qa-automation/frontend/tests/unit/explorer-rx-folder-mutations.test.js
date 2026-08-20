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
 * Unit tests for explorer-rx-folder-mutations helpers (#3654) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  RX_TEST_IDS,
  explorerRxFolderMutationsUrl,
  hasRxFolderMutationsQuery,
  rxFoldersRestBase,
  encodeRxFolderPath,
  rxFolderByPathUrl,
  isRxContentExplorerFoldersUrl,
  isRxFolderCreateUrl,
  isRxFolderByIdUrl,
  isPathmanagementFolderMutationUrl,
  isRxFolderMutationSuccessStatus,
  uniqueRxFolderName,
  unwrapRxFolder,
  wrapAddFolderRequest,
} = require("../helpers/explorer-rx-folder-mutations");

describe("explorer-rx-folder-mutations helpers (#3654)", () => {
  it("exports reduced-action test ids", () => {
    assert.equal(RX_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(RX_TEST_IDS.tree, "explorer-tree");
    assert.equal(RX_TEST_IDS.detailList, "detail-list");
    assert.equal(RX_TEST_IDS.actionCreateFolder, "action-create-folder");
    assert.equal(RX_TEST_IDS.actionRename, "action-rename");
    assert.equal(RX_TEST_IDS.actionDelete, "action-delete");
  });

  it("builds diagnostic explorer URL with rxFolderMutations=1", () => {
    const url = explorerRxFolderMutationsUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+&rxFolderMutations=1/,
    );
    assert.equal(hasRxFolderMutationsQuery(url), true);
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
        "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=true",
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

  it("builds REST façade URLs with encoded by-path segments", () => {
    assert.equal(
      rxFoldersRestBase("http://127.0.0.1:9992/"),
      "http://127.0.0.1:9992/Rhythmyx/rest/content-explorer/folders",
    );
    assert.equal(encodeRxFolderPath("/Folders"), "Folders");
    assert.equal(encodeRxFolderPath("//Sites"), "Sites");
    assert.equal(
      rxFolderByPathUrl("http://x/", "/Folders"),
      "http://x/Rhythmyx/rest/content-explorer/folders/by-path/Folders",
    );
  });

  it("matches RX create/by-id and not pathmanagement mutations", () => {
    assert.equal(
      isRxFolderCreateUrl("http://x/Rhythmyx/rest/content-explorer/folders"),
      true,
    );
    assert.equal(
      isRxFolderCreateUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders/by-id/1-101-9",
      ),
      false,
    );
    assert.equal(
      isRxFolderByIdUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders/by-id/1-101-9?purge=false",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders/by-path/Folders",
      ),
      true,
    );
    assert.equal(
      isPathmanagementFolderMutationUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/addNewFolder/Folders?name=qa",
      ),
      true,
    );
    assert.equal(
      isPathmanagementFolderMutationUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/renameFolder",
      ),
      true,
    );
    assert.equal(
      isPathmanagementFolderMutationUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/deleteFolder",
      ),
      true,
    );
    assert.equal(
      isPathmanagementFolderMutationUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/folder/Folders",
      ),
      false,
    );
    assert.equal(
      isPathmanagementFolderMutationUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders",
      ),
      false,
    );
  });

  it("treats HTTP 200/201/204 as mutation success", () => {
    assert.equal(isRxFolderMutationSuccessStatus(200), true);
    assert.equal(isRxFolderMutationSuccessStatus(201), true);
    assert.equal(isRxFolderMutationSuccessStatus(204), true);
    assert.equal(isRxFolderMutationSuccessStatus(404), false);
    assert.equal(isRxFolderMutationSuccessStatus(503), false);
  });

  it("uniqueRxFolderName is stable for a given timestamp", () => {
    assert.equal(uniqueRxFolderName("qa3654", 1700000000000), "qa3654_1700000000000");
  });

  it("unwrapRxFolder and wrapAddFolderRequest match the JAXB envelope", () => {
    assert.deepEqual(
      unwrapRxFolder({ RxFolder: { id: "1-101-9", name: "qa" } }),
      { id: "1-101-9", name: "qa" },
    );
    assert.deepEqual(unwrapRxFolder({ id: "flat" }), { id: "flat" });
    assert.deepEqual(wrapAddFolderRequest("qa3654", "/Folders"), {
      AddFolderRequest: { name: "qa3654", parentPath: "/Folders" },
    });
  });
});
