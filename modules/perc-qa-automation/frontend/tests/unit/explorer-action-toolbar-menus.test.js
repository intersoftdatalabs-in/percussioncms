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
const {
  collectMenuParents,
  unwrapFindPayload,
} = require("../helpers/explorer-action-toolbar-catalog");

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

  it("collectMenuParents finds nested MENU children and parentId links (#3379)", () => {
    const fromChildren = collectMenuParents({
      ActionMenu: [
        {
          id: 8,
          name: "file",
          menuType: "MENU",
          children: [{ name: "open" }, { name: "saveAs" }],
        },
      ],
    });
    assert.equal(fromChildren.length, 1);
    assert.equal(fromChildren[0].name, "file");
    assert.deepEqual(fromChildren[0].childNames, ["open", "saveAs"]);

    const fromParentId = collectMenuParents({
      ActionMenu: [
        { id: 8, name: "file", menuType: "MENU" },
        { id: 2, name: "open", menuType: "MENUITEM", parentId: 8 },
      ],
    });
    assert.equal(fromParentId.length, 1);
    assert.deepEqual(fromParentId[0].childNames, ["open"]);
    assert.deepEqual(unwrapFindPayload(null), []);
  });
});
