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
 * Unit tests for explorer-move-folder helpers (#3655) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  MOVE_TEST_IDS,
  explorerProductMoveFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementMoveItemUrl,
  isRxContentExplorerFoldersUrl,
  isFoldersCopyFolderUrl,
  isMoveFolderSuccessStatus,
  uniqueMoveFolderName,
  unwrapPathItem,
  wrapMoveFolderItem,
  isMoveFolderItemEnvelope,
} = require("../helpers/explorer-move-folder");

describe("explorer-move-folder helpers (#3655)", () => {
  it("exports stable product test ids", () => {
    assert.equal(MOVE_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(MOVE_TEST_IDS.tree, "explorer-tree");
    assert.equal(MOVE_TEST_IDS.detailList, "detail-list");
    assert.equal(MOVE_TEST_IDS.actionMove, "action-move");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductMoveFolderUrl("http://127.0.0.1:9992/");
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

  it("classifies moveItem vs copy/folder vs RX folders URLs", () => {
    assert.equal(
      isPathmanagementMoveItemUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/moveItem",
      ),
      true,
    );
    assert.equal(
      isFoldersCopyFolderUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/folder",
      ),
      true,
    );
    assert.equal(
      isPathmanagementMoveItemUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/folder",
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

  it("wraps MoveFolderItem and rejects a bare sourcePath root", () => {
    const wrapped = wrapMoveFolderItem("/Assets/Src", "/Assets/Dst");
    assert.deepEqual(wrapped, {
      MoveFolderItem: {
        itemPath: "/Assets/Src",
        targetFolderPath: "/Assets/Dst",
      },
    });
    assert.equal(isMoveFolderItemEnvelope(wrapped), true);
    assert.deepEqual(wrapMoveFolderItem("Assets/Src", "Assets/Dst"), {
      MoveFolderItem: {
        itemPath: "/Assets/Src",
        targetFolderPath: "/Assets/Dst",
      },
    });
    assert.equal(
      isMoveFolderItemEnvelope({
        sourcePath: "/Assets/Src",
        targetPath: "/Assets/Dst",
      }),
      false,
    );
  });

  it("unwraps PathItem and treats 200/204 as success", () => {
    assert.equal(isMoveFolderSuccessStatus(200), true);
    assert.equal(isMoveFolderSuccessStatus(204), true);
    assert.equal(isMoveFolderSuccessStatus(500), false);
    assert.deepEqual(unwrapPathItem({ PathItem: { name: "A", path: "/A" } }), {
      name: "A",
      path: "/A",
    });
    assert.deepEqual(unwrapPathItem({ name: "flat" }), { name: "flat" });
    assert.deepEqual(unwrapPathItem(null), {});
  });

  it("uniqueMoveFolderName is stable for a given stamp", () => {
    assert.equal(uniqueMoveFolderName("qa3655src", 1), "qa3655src_1");
    assert.match(uniqueMoveFolderName(), /^qa3655_\d+$/);
  });
});
