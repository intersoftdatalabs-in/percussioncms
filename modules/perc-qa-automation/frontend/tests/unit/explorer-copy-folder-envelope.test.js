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
  isBareSourcePathRoot,
  isMoveFolderItemEnvelope,
  isCopyFolderItemRequestEnvelope,
} = require("../helpers/explorer-copy-folder-envelope");

describe("explorer-copy-folder-envelope (#3362)", () => {
  it("flags the HTTP 400 bare sourcePath root", () => {
    assert.equal(
      isBareSourcePathRoot({
        sourcePath: "/Folders/A",
        targetPath: "/Folders/B",
        copy: true,
      }),
      true,
    );
    assert.equal(
      isBareSourcePathRoot({
        MoveFolderItem: {
          itemPath: "/Folders/A",
          targetFolderPath: "/Folders/B",
        },
      }),
      false,
    );
  });

  it("accepts MoveFolderItem wrap without sourcePath or copy", () => {
    assert.equal(
      isMoveFolderItemEnvelope({
        MoveFolderItem: {
          itemPath: "/Folders/A",
          targetFolderPath: "/Folders/B",
        },
      }),
      true,
    );
    assert.equal(
      isMoveFolderItemEnvelope({
        MoveFolderItem: {
          itemPath: "/Folders/A",
          targetFolderPath: "/Folders/B",
          copy: true,
        },
      }),
      false,
    );
  });

  it("accepts CopyFolderItemRequest wrap", () => {
    assert.equal(
      isCopyFolderItemRequestEnvelope({
        CopyFolderItemRequest: {
          itemPath: "/Folders/A",
          targetFolderPath: "/Folders/B",
        },
      }),
      true,
    );
    assert.equal(
      isCopyFolderItemRequestEnvelope({
        sourcePath: "/Folders/A",
        targetPath: "/Folders/B",
      }),
      false,
    );
  });
});
