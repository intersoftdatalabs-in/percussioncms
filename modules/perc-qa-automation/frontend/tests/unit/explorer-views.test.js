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
 * Unit tests for Explorer Views V3 pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend (standalone, not listed in
 * package.json test:unit — avoids thrash with #3252 views-catalog unit files):
 *
 *   node --test tests/unit/explorer-views.test.js
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  PATH_VIEWS,
  PARENT_MY_CONTENT,
  explorerEntryUrl,
  viewsCatalogUrl,
  viewsExecuteUrl,
  unwrapViewDefs,
  viewDefKey,
  viewDefLabel,
  isCustomUrlView,
  isInboxView,
  viewParentCategory,
  pickRunnableStandardView,
  noViewsChromeSkipMessage,
  noRunnableViewSkipMessage,
  postExecuteRegionSelector,
  viewsChromeSelector,
  isIgnorableConsoleError,
} = require("../helpers/explorer-views");

describe("explorer-views helpers (#3117)", () => {
  it("exports stable test ids used by ViewsCatalogTree / ViewResultsPanel", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.viewsTree, "explorer-views-tree");
    assert.equal(TEST_IDS.viewsRoot, "explorer-views-root");
    assert.equal(TEST_IDS.group(1), "explorer-views-group-1");
    assert.equal(TEST_IDS.groupRow(1), "explorer-views-group-1-row");
    assert.equal(TEST_IDS.leaf("MyPages"), "explorer-views-leaf-MyPages");
    assert.equal(TEST_IDS.inboxLeaf, "explorer-views-leaf-Inbox");
    assert.equal(TEST_IDS.results, "explorer-view-results");
    assert.equal(TEST_IDS.resultsList, "explorer-view-results-list");
    assert.equal(PATH_VIEWS, "/Rhythmyx/services/views");
    assert.equal(PARENT_MY_CONTENT, 1);
  });

  it("explorerEntryUrl builds spa.jsp explorer entry with cache-buster", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    assert.equal(
      explorerEntryUrl("http://localhost:9992/", { cacheBuster: "a b" }),
      "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=a%20b",
    );
  });

  it("viewsCatalogUrl and viewsExecuteUrl encode idOrName", () => {
    assert.equal(
      viewsCatalogUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/Rhythmyx/services/views",
    );
    assert.equal(
      viewsExecuteUrl("http://cms.example", "All Content"),
      "http://cms.example/Rhythmyx/services/views/All%20Content/execute",
    );
  });

  it("unwrapViewDefs handles arrays and Jackson wrappers", () => {
    assert.deepEqual(unwrapViewDefs(null), []);
    assert.deepEqual(unwrapViewDefs([{ name: "A" }]), [{ name: "A" }]);
    assert.deepEqual(unwrapViewDefs({ ViewDef: { name: "B" } }), [
      { name: "B" },
    ]);
    assert.deepEqual(
      unwrapViewDefs({ viewDef: [{ name: "C" }, { name: "D" }] }),
      [{ name: "C" }, { name: "D" }],
    );
  });

  it("viewDefKey prefers name then id", () => {
    assert.equal(viewDefKey(null), "");
    assert.equal(viewDefKey({ name: "My Pages" }), "My Pages");
    assert.equal(viewDefKey({ id: "42" }), "42");
    assert.equal(viewDefKey({ name: "  n  ", id: "1" }), "n");
  });

  it("viewDefLabel prefers label/displayName then key", () => {
    assert.equal(viewDefLabel({ name: "x", label: "Label X" }), "Label X");
    assert.equal(
      viewDefLabel({ name: "x", displayName: "Display X" }),
      "Display X",
    );
    assert.equal(viewDefLabel({ name: "fallback" }), "fallback");
  });

  it("isCustomUrlView and isInboxView detect flags / names", () => {
    assert.equal(isCustomUrlView({ customView: true }), true);
    assert.equal(isCustomUrlView({ customView: "true" }), true);
    assert.equal(isCustomUrlView({ standardView: false }), false);
    assert.equal(isCustomUrlView({ customView: false }), false);
    assert.equal(isCustomUrlView({}), false);
    assert.equal(isInboxView({ name: "Inbox" }), true);
    assert.equal(isInboxView({ label: "Inbox" }), true);
    assert.equal(isInboxView({ name: "My Pages" }), false);
  });

  it("viewParentCategory parses numbers and strings", () => {
    assert.equal(viewParentCategory({ parentCategory: 1 }), 1);
    assert.equal(viewParentCategory({ parentCategory: "2" }), 2);
    assert.equal(viewParentCategory({}), 0);
    assert.equal(viewParentCategory(null), 0);
  });

  it("pickRunnableStandardView prefers My Content and skips custom/Inbox", () => {
    assert.equal(pickRunnableStandardView([]), null);
    assert.equal(
      pickRunnableStandardView([{ name: "Inbox", customView: true }]),
      null,
    );
    const picked = pickRunnableStandardView([
      { name: "Inbox", customView: true, parentCategory: 1 },
      { name: "Community Pages", parentCategory: 2 },
      { name: "My Pages", label: "My Pages", parentCategory: 1 },
    ]);
    assert.ok(picked);
    assert.equal(picked.key, "My Pages");
    assert.equal(picked.parentCategory, 1);
  });

  it("soft-skip messages mention #3117 and reasons", () => {
    const chrome = noViewsChromeSkipMessage();
    assert.match(chrome, /#3117/);
    assert.match(chrome, /soft-skip/i);
    assert.match(chrome, /explorer-views-tree/);
    const msg = noRunnableViewSkipMessage({ empty: true, restStatus: 200 });
    assert.match(msg, /#3117/);
    assert.match(msg, /soft skip/i);
    assert.match(msg, /empty/i);
    assert.match(msg, /200/);
  });

  it("region selectors include key test ids", () => {
    assert.match(postExecuteRegionSelector(), /explorer-view-results/);
    assert.match(postExecuteRegionSelector(), /explorer-view-results-empty/);
    assert.match(viewsChromeSelector(), /explorer-views-tree/);
    assert.match(viewsChromeSelector(), /explorer-views-root/);
  });

  it("isIgnorableConsoleError filters 404/400 resource noise", () => {
    assert.equal(
      isIgnorableConsoleError(
        "Failed to load resource: the server responded with a status of 404 ()",
      ),
      true,
    );
    assert.equal(isIgnorableConsoleError("Uncaught TypeError: boom"), false);
  });
});
