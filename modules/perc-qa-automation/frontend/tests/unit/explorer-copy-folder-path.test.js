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
  isCopiedFolderPathStatus,
} = require("../helpers/explorer-copy-folder-path");

describe("explorer copy folder path helpers (#4911)", () => {
  it("uses stable test ids", () => {
    assert.equal(TEST_IDS.copyFolderPath, "explorer-copy-folder-path");
    assert.equal(TEST_IDS.status, "explorer-copy-folder-path-status");
  });

  it("accepts a non-root copied path only on success", () => {
    assert.equal(isCopiedFolderPathStatus("success", "/Sites"), true);
    assert.equal(isCopiedFolderPathStatus("success", "/"), false);
    assert.equal(isCopiedFolderPathStatus("error", "/Sites"), false);
    assert.equal(isCopiedFolderPathStatus("success", ""), false);
  });
});
