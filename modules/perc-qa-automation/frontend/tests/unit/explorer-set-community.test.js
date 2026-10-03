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
  folderListingPhase,
  listingNavigationSettled,
  isPaginatedFolderListingUrl,
} = require("../helpers/explorer-set-community");

describe("explorer set community listing (#5077)", () => {
  it("uses the set-community dialog test ids", () => {
    assert.equal(TEST_IDS.menuItem, "explorer-set-community");
    assert.equal(TEST_IDS.dialog, "explorer-set-community-dialog");
    assert.equal(TEST_IDS.status, "explorer-set-community-status");
  });

  it("treats a mounted list with no rows and no empty marker as loading", () => {
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 0, emptyVisible: false }), "loading");
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 0, emptyVisible: true }), "empty");
    assert.equal(folderListingPhase({ listVisible: true, rowCount: 2, emptyVisible: false }), "ready");
  });

  it("does not treat the idle empty marker as a finished folder navigation", () => {
    assert.equal(listingNavigationSettled("none", "empty", "empty", false), false);
    assert.equal(listingNavigationSettled("none", "empty", "empty", true), true);
    assert.equal(
      listingNavigationSettled("none", "ready", "rows:1:detail-row-9"),
      true,
    );
  });

  it("matches only GET paginatedFolder listings", () => {
    const url =
      "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/paginatedFolder/Sites?startIndex=0";
    assert.equal(isPaginatedFolderListingUrl(url, "GET"), true);
    assert.equal(isPaginatedFolderListingUrl(url, "POST"), false);
  });
});
