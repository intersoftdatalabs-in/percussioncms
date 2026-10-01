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
  TEST_IDS,
  isItemNewCopyUrl,
  isNewCopyHttpFailure,
  isNewCopySuccess,
  pickContentFolderIndex,
} = require("../helpers/explorer-new-copy");

describe("explorer new copy helpers (#5006)", () => {
  it("uses the Create menu new-copy test ids", () => {
    assert.equal(TEST_IDS.newCopy, "action-toolbar-item-Workflow_NewVersion");
    assert.equal(TEST_IDS.createMenu, "action-toolbar-item-Create");
    assert.equal(TEST_IDS.serverError, "explorer-server-actions-error");
  });

  it("classifies itemmanagement newCopy and not promotable or copy/item", () => {
    assert.equal(
      isItemNewCopyUrl("/Rhythmyx/services/itemmanagement/item/newCopy/42"),
      true,
    );
    assert.equal(
      isItemNewCopyUrl(
        "/Rhythmyx/services/itemmanagement/item/newCopy/16777215-101-551",
      ),
      true,
    );
    assert.equal(
      isItemNewCopyUrl("/Rhythmyx/services/itemmanagement/item/promotableVersion/42"),
      false,
    );
    assert.equal(isItemNewCopyUrl("/Rhythmyx/rest/folders/copy/item"), false);
  });

  it("treats a later list epoch with no error as success", () => {
    assert.equal(isNewCopySuccess("0", "1", ""), true);
    assert.equal(isNewCopySuccess("0", "0", ""), false);
    assert.equal(isNewCopySuccess("0", "1", "Could not create a new copy (HTTP 400)"), false);
  });

  it("recognizes HTTP 400, 403, and 409 banners", () => {
    assert.equal(
      isNewCopyHttpFailure("Could not create a new copy (HTTP 400)", 400),
      true,
    );
    assert.equal(
      isNewCopyHttpFailure("You are not allowed to create a new copy (HTTP 403)", 403),
      true,
    );
    assert.equal(
      isNewCopyHttpFailure("A new copy conflicts with the item state (HTTP 409)", 409),
      true,
    );
    assert.equal(isNewCopyHttpFailure("Select a content item first", 400), false);
  });

  it("opens an unvisited Pages folder before a path that only mentions Pages", () => {
    const rows = [
      { id: "site", name: "Corporate" },
      { id: "decoy", name: "Notes" },
      { id: "pages", name: "Pages" },
    ];
    assert.equal(pickContentFolderIndex(rows, "page", []), 2);
    assert.equal(pickContentFolderIndex(rows, "page", ["pages"]), 0);
    assert.equal(pickContentFolderIndex(rows, "asset", []), 0);
    assert.equal(pickContentFolderIndex(rows, "page", ["site", "decoy", "pages"]), -1);
    assert.equal(pickContentFolderIndex([{ id: "", name: "Pages" }], "page", []), -1);
  });
});
