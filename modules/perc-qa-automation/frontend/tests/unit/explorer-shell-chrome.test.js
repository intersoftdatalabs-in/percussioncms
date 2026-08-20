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
 * Unit tests for explorer-shell-chrome helpers (#2850) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  explorerSpaUrl,
  softVisible,
} = require("../helpers/explorer-shell-chrome");

describe("explorer-shell-chrome helpers (#2850)", () => {
  it("exports stable shell composition test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.menuBar, "explorer-menu-bar");
    assert.equal(TEST_IDS.toggleSearch, "explorer-toggle-search");
    assert.equal(TEST_IDS.contentSearch, "explorer-menu-content-search");
    assert.equal(TEST_IDS.searchPanelHost, "explorer-search-panel");
    assert.equal(TEST_IDS.searchPanel, "search-panel");
    assert.equal(TEST_IDS.searchInput, "search-panel-input");
    assert.equal(TEST_IDS.searchSubmit, "search-panel-submit");
    assert.equal(TEST_IDS.displayFormat, "explorer-display-format");
    assert.equal(TEST_IDS.displayFormatError, "explorer-display-format-error");
    assert.equal(TEST_IDS.viewTools, "explorer-view-tools");
    assert.equal(TEST_IDS.viewToolSearch, "explorer-view-tool-search");
    assert.equal(TEST_IDS.viewToolSecurity, "explorer-toggle-security");
    assert.equal(TEST_IDS.sidePanels, "explorer-side-panels");
    assert.equal(TEST_IDS.actionToolbar, "action-toolbar");
    assert.equal(TEST_IDS.serverActions, "explorer-server-actions");
  });

  it("builds explorer SPA entry URL with cache buster", () => {
    const url = explorerSpaUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
  });

  it("softVisible returns false when locator count is 0", async () => {
    const locator = {
      count: async () => 0,
      first: () => ({
        waitFor: async () => {
          throw new Error("should not wait");
        },
      }),
    };
    assert.equal(await softVisible(locator), false);
  });

  it("softVisible returns true when first match becomes visible", async () => {
    const locator = {
      count: async () => 1,
      first: () => ({
        waitFor: async () => undefined,
      }),
    };
    assert.equal(await softVisible(locator), true);
  });
});
