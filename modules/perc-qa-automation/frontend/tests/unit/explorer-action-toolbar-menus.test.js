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

/**
 * Unit tests for #2730 ActionToolbar surface test ids — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const { TEST_IDS } = require("../helpers/explorer-menu-bar");

describe("explorer-action-toolbar-menus helpers (#2730)", () => {
  it("exports action toolbar + server-actions region test ids", () => {
    assert.equal(TEST_IDS.actionToolbar, "action-toolbar");
    assert.equal(TEST_IDS.serverActions, "explorer-server-actions");
    assert.equal(
      TEST_IDS.serverActionsLabel,
      "explorer-server-actions-label",
    );
    assert.equal(
      TEST_IDS.serverActionsError,
      "explorer-server-actions-error",
    );
  });
});
