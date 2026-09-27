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
  isContentGuid,
  isCopiedItemGuidStatus,
} = require("../helpers/explorer-copy-item-guid");

describe("explorer copy item guid helpers (#4989)", () => {
  it("uses stable test ids", () => {
    assert.equal(TEST_IDS.copyItemGuid, "explorer-copy-item-guid");
    assert.equal(TEST_IDS.status, "explorer-copy-item-guid-status");
  });

  it("accepts a host-type-uuid only on success", () => {
    assert.equal(isContentGuid("16777215-101-551"), true);
    assert.equal(isContentGuid("551"), false);
    assert.equal(isCopiedItemGuidStatus("success", "16777215-101-551"), true);
    assert.equal(isCopiedItemGuidStatus("error", "16777215-101-551"), false);
    assert.equal(isCopiedItemGuidStatus("success", ""), false);
  });
});
