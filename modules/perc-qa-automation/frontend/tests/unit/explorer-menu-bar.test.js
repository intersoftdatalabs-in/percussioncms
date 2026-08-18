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
 * Unit tests for explorer-menu-bar helpers (#2731) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("../helpers/explorer-menu-bar");

describe("explorer-menu-bar helpers (#2731)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.menuBar, "explorer-menu-bar");
    assert.equal(TEST_IDS.menuContent, "explorer-menu-content");
    assert.equal(TEST_IDS.menuView, "explorer-menu-view");
    assert.equal(TEST_IDS.menuHelp, "explorer-menu-help");
    assert.equal(TEST_IDS.toggleSearch, "explorer-toggle-search");
    assert.equal(TEST_IDS.toggleClipboard, "explorer-toggle-clipboard");
    assert.equal(TEST_IDS.clipboardPanel, "explorer-clipboard-panel");
    assert.equal(TEST_IDS.contentSearch, "explorer-menu-content-search");
    assert.equal(TEST_IDS.actionToolbar, "action-toolbar");
    assert.equal(TEST_IDS.serverActions, "explorer-server-actions");
    // displayFormat is shell-chrome only (explorer-shell-chrome.test.js)
    assert.equal(TEST_IDS.displayFormat, undefined);
  });

  it("builds explorer SPA entry URL with cache buster", () => {
    const url = explorerSpaUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.ok(!url.includes("9992//"));
  });
});
