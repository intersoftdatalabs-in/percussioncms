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
 * Unit tests for explorer-sites-list-create helpers (#3003) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  PRODUCT_ISSUES,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  siteChildNamesFromTreeTestIds,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  emptySitesSoftSkipNote,
} = require("../helpers/explorer-sites-list-create");

describe("explorer-sites-list-create helpers (#3003)", () => {
  it("exports stable product test ids for Create Site + shell", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.tree, "explorer-tree");
    assert.equal(TEST_IDS.createSiteMenu, "explorer-content-create-site");
    assert.equal(TEST_IDS.createSitePanel, "explorer-site-create-panel");
    assert.equal(TEST_IDS.wizard, "site-create-wizard");
    assert.equal(TEST_IDS.siteName, "site-create-name");
    assert.equal(TEST_IDS.run, "site-create-run");
  });

  it("tracks parent epic and slice issue numbers", () => {
    assert.equal(PRODUCT_ISSUES.parent, 2989);
    assert.equal(PRODUCT_ISSUES.slice3PlaywrightDocs, 3003);
    assert.equal(PRODUCT_ISSUES.slice2CreateSite, 3002);
  });

  it("builds explorer SPA entry URL with cache buster", () => {
    const url = explorerSpaUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.ok(!url.includes("9992//"));
  });

  it("builds pathmanagement Sites folder URL without double slash", () => {
    assert.equal(
      pathFolderServiceUrl("http://127.0.0.1:9992/"),
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder",
    );
    assert.equal(
      sitesFolderUrl("http://127.0.0.1:9992"),
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder/Sites",
    );
  });

  it("extracts /Sites site-name segments (including deeper descendants)", () => {
    const names = siteChildNamesFromTreeTestIds([
      "tree-node-/",
      "tree-node-/Sites/",
      "tree-node-/Sites/Corporate Investments",
      "tree-node-/Sites/Enterprise Investments/",
      "tree-node-/Sites/Corporate Investments/folder",
      "tree-node-/Assets/",
    ]);
    // Deeper nodes re-emit the first segment (multiset of site folder names).
    assert.deepEqual(names, [
      "Corporate Investments",
      "Enterprise Investments",
      "Corporate Investments",
    ]);
  });

  it("returns empty array for empty/null tree test ids", () => {
    assert.deepEqual(siteChildNamesFromTreeTestIds([]), []);
    assert.deepEqual(siteChildNamesFromTreeTestIds(null), []);
  });

  it("uniqueQaSiteName is alphanumeric-safe and non-empty", () => {
    const a = uniqueQaSiteName("Qa Create!");
    const b = uniqueQaSiteName("Qa Create!");
    assert.match(a, /^QaCreate[a-z0-9]+$/i);
    assert.match(b, /^QaCreate[a-z0-9]+$/i);
    // Same prefix, different stamp almost always; allow equal if same ms.
    assert.ok(a.length >= 8);
  });

  it("skip reasons cite durable issue URLs", () => {
    const missing = createSiteMissingSkipReason();
    assert.match(missing, /BUG:/);
    assert.match(missing, /3002/);
    assert.match(missing, /issues\/3002/);

    const empty = emptySitesSoftSkipNote();
    assert.match(empty, /3001/);
    assert.match(empty, /2989/);
  });
});
