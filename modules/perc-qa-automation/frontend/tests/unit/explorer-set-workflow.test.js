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

/**
 * Pure helpers for Explorer set-workflow listing waits (#5086 / #5076).
 *
 * Run: npm run test:unit (from frontend/)
 */

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  folderListingPhase,
  listingNavigationSettled,
  listingAlreadySelectable,
  unchangedListingUsable,
  isPaginatedFolderListingUrl,
} = require("../helpers/explorer-set-workflow");

describe("explorer set workflow listing (#5086)", () => {
  it("uses the set-workflow dialog test ids", () => {
    assert.equal(TEST_IDS.menuItem, "explorer-set-workflow");
    assert.equal(TEST_IDS.dialog, "explorer-set-workflow-dialog");
    assert.equal(TEST_IDS.detailList, "detail-list");
  });

  it("treats a mounted list with no rows and no empty marker as loading", () => {
    assert.equal(folderListingPhase({ listVisible: false, rowCount: 0, emptyVisible: false }), "loading");
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 0, emptyVisible: false }), "loading");
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 0, emptyVisible: true }), "empty");
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 3, emptyVisible: false }), "ready");
  });

  it("does not settle on the pre-click signature while the list is loading", () => {
    assert.equal(listingNavigationSettled("none", "loading", "none"), false);
    assert.equal(listingNavigationSettled("rows:2:detail-row-1", "ready", "rows:2:detail-row-1"), false);
    assert.equal(listingNavigationSettled("none", "ready", "none"), false);
    assert.equal(
      listingNavigationSettled("none", "ready", "rows:2:detail-row-9|detail-row-10"),
      true,
    );
    assert.equal(listingNavigationSettled("rows:1:detail-row-1", "empty", "empty"), false);
    assert.equal(listingNavigationSettled("rows:1:detail-row-1", "empty", "empty", true), true);
  });

  it("does not treat the idle empty marker as a finished folder navigation (#5089)", () => {
    assert.equal(listingNavigationSettled("none", "empty", "empty", false), false);
    assert.equal(listingNavigationSettled("none", "empty", "empty", true), true);
    assert.equal(listingNavigationSettled("empty", "empty", "empty", true), false);
  });

  it("treats an already-open row listing as selectable (#5096)", () => {
    assert.equal(listingAlreadySelectable("ready", "rows:2:detail-row-1|detail-row-2"), true);
    assert.equal(listingAlreadySelectable("empty", "empty"), false);
    assert.equal(listingAlreadySelectable("loading", "none"), false);
    assert.equal(listingAlreadySelectable("ready", "none"), false);
    assert.equal(listingAlreadySelectable("ready", ""), false);
  });

  it("keeps an unchanged ready listing only when this click started no GET (#5096)", () => {
    const rows = "rows:2:detail-row-1|detail-row-2";
    assert.equal(unchangedListingUsable(false, "ready", rows, rows), true);
    assert.equal(unchangedListingUsable(true, "ready", rows, rows), false);
    assert.equal(unchangedListingUsable(false, "empty", "empty", "empty"), false);
    assert.equal(
      unchangedListingUsable(false, "ready", "rows:1:detail-row-9", rows),
      false,
    );
  });

  it("matches only GET paginatedFolder listings", () => {
    const url =
      "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/paginatedFolder/Sites?startIndex=0";
    assert.equal(isPaginatedFolderListingUrl(url, "GET"), true);
    assert.equal(isPaginatedFolderListingUrl(url, "POST"), false);
    assert.equal(
      isPaginatedFolderListingUrl(
        "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/folder/Sites",
        "GET",
      ),
      false,
    );
  });
});
