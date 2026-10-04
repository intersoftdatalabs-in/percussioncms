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
  isKnownExplorerSetFolderWorkflowConsoleNoise,
} = require("../helpers/explorer-set-folder-workflow");

describe("explorer set folder workflow helpers (#5104)", () => {
  it("uses the folder workflow dialog test ids", () => {
    assert.equal(TEST_IDS.menuItem, "explorer-set-folder-workflow");
    assert.equal(TEST_IDS.dialog, "explorer-set-folder-workflow-dialog");
    assert.equal(TEST_IDS.multi, "explorer-set-folder-workflow-multi");
    assert.equal(TEST_IDS.status, "explorer-set-folder-workflow-status");
  });

  it("ignores known browser noise and not workflow errors", () => {
    assert.equal(isKnownExplorerSetFolderWorkflowConsoleNoise("favicon.ico"), true);
    assert.equal(
      isKnownExplorerSetFolderWorkflowConsoleNoise("Could not set the folder workflow"),
      false,
    );
  });
});
