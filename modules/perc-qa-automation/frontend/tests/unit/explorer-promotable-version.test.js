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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  isItemPromotableUrl,
  isPromotableHttpFailure,
  isPromotableSuccess,
} = require("../helpers/explorer-promotable-version");

describe("explorer promotable version helpers (#5007)", () => {
  it("uses the Create menu promotable-version test ids", () => {
    assert.equal(TEST_IDS.promotable, "action-toolbar-item-Edit_PromotableVersion");
    assert.equal(TEST_IDS.createMenu, "action-toolbar-item-Create");
    assert.equal(TEST_IDS.serverError, "explorer-server-actions-error");
  });

  it("classifies itemmanagement promotableVersion and not newCopy", () => {
    assert.equal(
      isItemPromotableUrl(
        "/Rhythmyx/services/itemmanagement/item/promotableVersion/42",
      ),
      true,
    );
    assert.equal(
      isItemPromotableUrl(
        "/Rhythmyx/services/itemmanagement/item/promotableVersion/16777215-101-551",
      ),
      true,
    );
    assert.equal(
      isItemPromotableUrl("/Rhythmyx/services/itemmanagement/item/newCopy/42"),
      false,
    );
  });

  it("treats a later list epoch with no error as success", () => {
    assert.equal(isPromotableSuccess("0", "1", ""), true);
    assert.equal(isPromotableSuccess("0", "0", ""), false);
    assert.equal(
      isPromotableSuccess("0", "1", "Could not create a promotable version (HTTP 400)"),
      false,
    );
  });

  it("recognizes HTTP 400, 403, and 409 banners", () => {
    assert.equal(
      isPromotableHttpFailure("Could not create a promotable version (HTTP 400)", 400),
      true,
    );
    assert.equal(
      isPromotableHttpFailure(
        "You are not allowed to create a promotable version (HTTP 403)",
        403,
      ),
      true,
    );
    assert.equal(
      isPromotableHttpFailure(
        "A promotable version conflicts with the item state (HTTP 409)",
        409,
      ),
      true,
    );
    assert.equal(isPromotableHttpFailure("Select a content item first", 400), false);
  });
});
