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
  isKnownExplorerSetFolderAllowedSitesConsoleNoise,
} = require("../helpers/explorer-set-folder-allowed-sites");

describe("explorer set folder allowed sites helpers (#5132)", () => {
  it("uses the allowed publish sites dialog test ids", () => {
    assert.equal(TEST_IDS.menuItem, "explorer-set-folder-allowed-sites");
    assert.equal(TEST_IDS.dialog, "explorer-set-folder-allowed-sites-dialog");
    assert.equal(TEST_IDS.status, "explorer-set-folder-allowed-sites-status");
  });

  it("ignores known browser noise and not allowed-sites errors", () => {
    assert.equal(isKnownExplorerSetFolderAllowedSitesConsoleNoise("favicon.ico"), true);
    assert.equal(
      isKnownExplorerSetFolderAllowedSitesConsoleNoise("Could not set allowed publish sites"),
      false,
    );
  });
});
