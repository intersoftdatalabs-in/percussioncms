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
  copyListedOnlyAfterPost,
  isMultiNewCopyConfirm,
  isNewCopyHttpFailure,
  isNewCopySuccess,
  isPartialNewCopy,
  namesSkippedNewCopyFolder,
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

describe("explorer multi new copy helpers (#5247)", () => {
  it("recognizes the one-confirm multi copy prompt", () => {
    assert.equal(
      isMultiNewCopyConfirm(
        "Create a new copy of 2 selected items in the same folder? Folders in the selection are not copied.",
      ),
      true,
    );
    assert.equal(
      isMultiNewCopyConfirm("Create a new copy of this item in the same folder?"),
      false,
    );
  });

  it("names a skipped folder and a partial HTTP failure", () => {
    const text =
      "Folders are not copied: News Not every selected item got a new copy. About (HTTP 409)";
    assert.equal(namesSkippedNewCopyFolder(text, "News"), true);
    assert.equal(namesSkippedNewCopyFolder(text, "Blog"), false);
    assert.equal(isPartialNewCopy(text, "About", 409), true);
    assert.equal(isPartialNewCopy(text, "About", 400), false);
    assert.equal(isPartialNewCopy("New copy created", "About", 409), false);
  });

  it("does not list a copy before that item's post", () => {
    assert.equal(
      copyListedOnlyAfterPost([
        { type: "post", id: "42" },
        { type: "listed", id: "42" },
        { type: "post", id: "43" },
        { type: "listed", id: "43" },
      ]),
      true,
    );
    assert.equal(
      copyListedOnlyAfterPost([
        { type: "listed", id: "42" },
        { type: "post", id: "42" },
      ]),
      false,
    );
  });
});
