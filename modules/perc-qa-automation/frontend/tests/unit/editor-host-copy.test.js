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
 * Unit tests for EditorHost copy URL helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  isNewCopyUrl,
  isPromotableUrl,
} = require("../helpers/editor-host-copy");

describe("editor-host-copy helpers (#4570)", () => {
  it("exports stable test ids", () => {
    assert.equal(TEST_IDS.host, "editor-host");
    assert.equal(TEST_IDS.newCopy, "editor-new-copy");
    assert.equal(TEST_IDS.promotable, "editor-promotable-version");
    assert.equal(TEST_IDS.copyError, "editor-copy-error");
  });

  it("classifies newCopy and promotableVersion URLs", () => {
    assert.equal(
      isNewCopyUrl("/Rhythmyx/services/itemmanagement/item/newCopy/42"),
      true,
    );
    assert.equal(
      isPromotableUrl(
        "/Rhythmyx/services/itemmanagement/item/promotableVersion/42",
      ),
      true,
    );
    assert.equal(isNewCopyUrl("/Rhythmyx/services/itemmanagement/item/create"), false);
    assert.equal(isPromotableUrl("/Rhythmyx/services/itemmanagement/item/newCopy/42"), false);
  });
});
