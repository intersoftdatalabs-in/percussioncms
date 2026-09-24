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
  isFoldersRenameItemUrl,
  isRenameFolderItemRequest,
} = require("../helpers/editor-host-rename");

describe("editor-host-rename helpers (#4791)", () => {
  it("names the host controls", () => {
    assert.equal(TEST_IDS.rename, "editor-rename");
    assert.equal(TEST_IDS.renameError, "editor-rename-error");
  });

  it("recognizes the folders rename-item contract", () => {
    assert.equal(
      isFoldersRenameItemUrl("/Rhythmyx/rest/folders/rename/item"),
      true,
    );
    assert.equal(
      isFoldersRenameItemUrl("/Rhythmyx/rest/folders/move/item"),
      false,
    );
    assert.equal(
      isRenameFolderItemRequest({
        RenameFolderItemRequest: { itemPath: "//Assets/Home", newName: "Home2" },
      }),
      true,
    );
    assert.equal(isRenameFolderItemRequest({ newName: "Home2" }), false);
  });
});
