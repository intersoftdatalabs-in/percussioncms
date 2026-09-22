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
  ITEM_PROPS_TEST_IDS,
  isItemPropertiesSaveUrl,
  wrapItemPropertiesRequest,
} = require("../helpers/explorer-item-properties");

describe("explorer-item-properties helpers (#4701)", () => {
  it("exports panel test ids", () => {
    assert.equal(ITEM_PROPS_TEST_IDS.panel, "item-properties-panel");
    assert.equal(ITEM_PROPS_TEST_IDS.save, "item-properties-save");
  });

  it("matches POST save URL not GET path", () => {
    assert.equal(
      isItemPropertiesSaveUrl(
        "http://localhost/Rhythmyx/rest/folders/item-properties",
      ),
      true,
    );
    assert.equal(
      isItemPropertiesSaveUrl(
        "http://localhost/Rhythmyx/rest/folders/item-properties/Assets/x",
      ),
      false,
    );
  });

  it("wraps ItemPropertiesRequest", () => {
    assert.deepEqual(wrapItemPropertiesRequest("/Assets/a", "n", "t"), {
      ItemPropertiesRequest: {
        itemPath: "/Assets/a",
        name: "n",
        displayTitle: "t",
      },
    });
  });
});
