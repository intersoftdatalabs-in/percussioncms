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

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  DELETE_ITEM_TEST_IDS,
  explorerProductDeleteItemUrl,
  hasRxFolderMutationsQuery,
  isFoldersDeleteItemUrl,
  isPathmanagementDeleteFolderUrl,
  isDeleteItemSuccessStatus,
  uniqueDeleteItemName,
} = require("../helpers/explorer-delete-item");

describe("explorer-delete-item helpers (#4602)", () => {
  it("exports stable product test ids", () => {
    assert.equal(DELETE_ITEM_TEST_IDS.shell, "content-explorer-shell");
    assert.equal(DELETE_ITEM_TEST_IDS.actionDelete, "action-delete");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductDeleteItemUrl("http://127.0.0.1:9992/");
    assert.match(url, /entry=explorer/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
  });

  it("classifies DELETE folders/item vs path deleteFolder", () => {
    assert.equal(
      isFoldersDeleteItemUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/item/Assets/qa4602",
      ),
      true,
    );
    assert.equal(
      isFoldersDeleteItemUrl(
        "http://127.0.0.1/Rhythmyx/rest/folders/copy/item",
      ),
      false,
    );
    assert.equal(
      isPathmanagementDeleteFolderUrl(
        "http://127.0.0.1/Rhythmyx/services/pathmanagement/path/deleteFolder",
      ),
      true,
    );
    assert.equal(isDeleteItemSuccessStatus(200), true);
    assert.equal(isDeleteItemSuccessStatus(409), false);
  });

  it("unique names stay qa4602 prefixed", () => {
    assert.match(uniqueDeleteItemName("qa4602itm", 1), /^qa4602itm_/);
  });
});
