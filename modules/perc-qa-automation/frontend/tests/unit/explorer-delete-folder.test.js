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
 * Unit tests for explorer-delete-folder helpers (#3646) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  DELETE_TEST_IDS,
  explorerProductDeleteFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementDeleteFolderUrl,
  isLegacyPathDeleteItemUrl,
  isRxContentExplorerFoldersUrl,
  isDeleteFolderSuccessStatus,
  uniqueDeleteFolderName,
  wrapDeleteFolderCriteria,
} = require("../helpers/explorer-delete-folder");

describe("explorer-delete-folder helpers (#3646)", () => {
  it("exports reduced-action delete test ids", () => {
    assert.equal(DELETE_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(DELETE_TEST_IDS.tree, "explorer-tree");
    assert.equal(DELETE_TEST_IDS.detailList, "detail-list");
    assert.equal(DELETE_TEST_IDS.actionDelete, "action-delete");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductDeleteFolderUrl("http://127.0.0.1:9992/");
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

  it("matches pathmanagement deleteFolder and not the missing /path/delete/{path}", () => {
    assert.equal(
      isPathmanagementDeleteFolderUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/deleteFolder",
      ),
      true,
    );
    assert.equal(
      isLegacyPathDeleteItemUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/delete/Sites/Foo",
      ),
      true,
    );
    assert.equal(
      isLegacyPathDeleteItemUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/deleteFolder",
      ),
      false,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://x/Rhythmyx/rest/content-explorer/folders/by-id/1",
      ),
      true,
    );
    assert.equal(
      isRxContentExplorerFoldersUrl(
        "http://x/Rhythmyx/services/pathmanagement/path/deleteFolder",
      ),
      false,
    );
  });

  it("treats HTTP 200/204 as delete success", () => {
    assert.equal(isDeleteFolderSuccessStatus(200), true);
    assert.equal(isDeleteFolderSuccessStatus(204), true);
    assert.equal(isDeleteFolderSuccessStatus(500), false);
  });

  it("uniqueDeleteFolderName is stable for a given timestamp", () => {
    assert.equal(uniqueDeleteFolderName(1700000000000), "qa3646_1700000000000");
  });

  it("wrapDeleteFolderCriteria sends trailing slash and empty guid", () => {
    assert.deepEqual(wrapDeleteFolderCriteria("/Assets/qa3646", { guid: "1-2-3" }), {
      DeleteFolderCriteria: {
        path: "/Assets/qa3646/",
        skipItems: "NO",
        shouldPurge: false,
        guid: "1-2-3",
      },
    });
    assert.equal(wrapDeleteFolderCriteria("/Assets/x").DeleteFolderCriteria.guid, "");
    assert.equal(
      wrapDeleteFolderCriteria("/Assets/x", { guid: "n-3646" }).DeleteFolderCriteria
        .guid,
      "",
    );
  });
});
