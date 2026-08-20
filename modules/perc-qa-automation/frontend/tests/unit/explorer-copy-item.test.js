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
 * Unit tests for explorer-copy-item helpers (#3656) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  COPY_TEST_IDS,
  explorerProductCopyItemUrl,
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
  unwrapItemCreateResult,
  wrapItemCreateRequest,
  wrapCopyFolderItemRequest,
  isCopyFolderItemRequestEnvelope,
  PREFERRED_CREATE_TYPE_NAMES,
} = require("../helpers/explorer-copy-item");

describe("explorer-copy-item helpers (#3656)", () => {
  it("exports stable product test ids", () => {
    assert.equal(COPY_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(COPY_TEST_IDS.tree, "explorer-tree");
    assert.equal(COPY_TEST_IDS.detailList, "detail-list");
    assert.equal(COPY_TEST_IDS.actionCopy, "action-copy");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductCopyItemUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
  });

  it("classifies copy/item vs copy/folder vs moveItem", () => {
    assert.equal(
      isFoldersCopyItemUrl("http://127.0.0.1/Rhythmyx/rest/folders/copy/item"),
      true,
    );
    assert.equal(
      isFoldersCopyFolderUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/folder",
      ),
      true,
    );
    assert.equal(
      isFoldersCopyItemUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/folder",
      ),
      false,
    );
    assert.equal(
      isFoldersCopyFolderUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/item",
      ),
      false,
    );
    assert.equal(
      isPathmanagementMoveItemUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/moveItem",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://127.0.0.1/Rhythmyx/rest/content-explorer/folders",
      ),
      true,
    );
  });

  it("wraps CopyFolderItemRequest for item copy (same envelope, distinct URL)", () => {
    const wrapped = wrapCopyFolderItemRequest(
      "/Assets/qa3656_src",
      "/Assets/qa3656_dst",
    );
    assert.deepEqual(wrapped, {
      CopyFolderItemRequest: {
        itemPath: "/Assets/qa3656_src",
        targetFolderPath: "/Assets/qa3656_dst",
      },
    });
    assert.equal(isCopyFolderItemRequestEnvelope(wrapped), true);
    assert.equal(
      isCopyFolderItemRequestEnvelope({
        sourcePath: "/Assets/qa3656_src",
        targetPath: "/Assets/qa3656_dst",
      }),
      false,
    );
  });

  it("maps Assets finder paths to //Folders/$System$/Assets for item/create", () => {
    assert.equal(
      toItemCreateFolderPath("/Assets/qa3656src"),
      "//Folders/$System$/Assets/qa3656src",
    );
    assert.equal(
      toItemCreateFolderPath("Assets/qa3656src/"),
      "//Folders/$System$/Assets/qa3656src",
    );
    assert.equal(
      toItemCreateFolderPath("//Folders/$System$/Assets/qa3656src"),
      "//Folders/$System$/Assets/qa3656src",
    );
    assert.equal(toItemCreateFolderPath("/Sites/Help"), "//Sites/Help");
  });

  it("wraps ItemCreateRequest and unwraps ItemCreateResult", () => {
    assert.deepEqual(
      wrapItemCreateRequest("percSimpleTextAsset", "/Assets/Src", "qa3656"),
      {
        ItemCreateRequest: {
          contentType: "percSimpleTextAsset",
          folderPath: "/Assets/Src",
          name: "qa3656",
        },
      },
    );
    assert.deepEqual(
      unwrapItemCreateResult({
        ItemCreateResult: {
          name: "qa3656",
          folderPath: "/Assets/Src",
          itemId: "1-101-9",
        },
      }),
      {
        name: "qa3656",
        folderPath: "/Assets/Src",
        itemId: "1-101-9",
      },
    );
    assert.ok(PREFERRED_CREATE_TYPE_NAMES.includes("percSimpleTextAsset"));
  });

  it("uniqueCopyItemName and dest-list name candidates are stable", () => {
    assert.equal(uniqueCopyItemName("qa3656src", 1), "qa3656src_1");
    assert.match(uniqueCopyItemName(), /^qa3656_\d+$/);
    assert.equal(expectedSameParentCopyName("qa3656src"), "qa3656src-2");
    assert.deepEqual(expectedCopiedItemNames("qa3656src"), [
      "qa3656src",
      "qa3656src-1",
      "qa3656src-2",
    ]);
    assert.equal(isCopyFolderSuccessStatus(200), true);
    assert.equal(isCopyFolderSuccessStatus(400), false);
  });
});
