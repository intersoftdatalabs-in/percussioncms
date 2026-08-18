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
 * Unit tests for explorer-sites-assets-tree-list helpers (#3575) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  REQUIRED_TREE_ROOTS,
  explorerSpaUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  rootFolderUrl,
  pathItemNames,
  shouldSoftSkipSitesList,
  shouldSkipSitesAssetsTreeList,
  treeHasRoot,
  siteChildNamesFromTreeTestIds,
} = require("../helpers/explorer-sites-assets-tree-list");

describe("explorer-sites-assets-tree-list helpers (#3575)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.tree, "explorer-tree");
    assert.equal(TEST_IDS.detailList, "detail-list");
    assert.equal(TEST_IDS.detailEmpty, "detail-list-empty");
    assert.deepEqual(REQUIRED_TREE_ROOTS, ["Sites", "Assets"]);
  });

  it("builds explorer SPA and path/folder URLs without double slash", () => {
    const url = explorerSpaUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(
      rootFolderUrl("http://127.0.0.1:9992"),
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder/",
    );
    assert.equal(
      sitesFolderUrl("http://127.0.0.1:9992"),
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder/Sites",
    );
    assert.equal(
      assetsFolderUrl("http://127.0.0.1:9992/"),
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder/Assets",
    );
  });

  it("treeHasRoot matches exact Sites/Assets testids", () => {
    const ids = [
      "tree-node-/",
      "tree-node-/Sites/",
      "tree-node-/Assets",
      "tree-node-/Design/",
    ];
    assert.equal(treeHasRoot(ids, "Sites"), true);
    assert.equal(treeHasRoot(ids, "Assets"), true);
    assert.equal(treeHasRoot(ids, "Design"), true);
    assert.equal(treeHasRoot(ids, "Missing"), false);
    assert.equal(treeHasRoot(["tree-node-/SitesArchive/"], "Sites"), false);
  });

  it("shouldSkipSitesAssetsTreeList is false when roots or sample sites exist", () => {
    assert.equal(
      shouldSkipSitesAssetsTreeList({ sitesRootVisible: true }),
      false,
    );
    assert.equal(
      shouldSkipSitesAssetsTreeList({ assetsRootVisible: true }),
      false,
    );
    assert.equal(
      shouldSkipSitesAssetsTreeList({
        sitesChildNames: ["Corporate Investments"],
      }),
      false,
    );
    assert.equal(shouldSkipSitesAssetsTreeList({}), false);
  });

  it("shouldSoftSkipSitesList is false for H2 and non-empty REST", () => {
    assert.equal(
      shouldSoftSkipSitesList(["Corporate Investments"], {}),
      false,
    );
    assert.equal(shouldSoftSkipSitesList([], { TEST_DB_TYPE: "h2" }), false);
    assert.equal(shouldSoftSkipSitesList([], {}), true);
  });

  it("pathItemNames unwraps Jackson PathItem wrap used by H2 REST", () => {
    assert.deepEqual(
      pathItemNames({ PathItem: [{ name: "Sites" }, { name: "Assets" }] }),
      ["Sites", "Assets"],
    );
    assert.deepEqual(pathItemNames({ PathItem: { name: "Sites" } }), ["Sites"]);
  });

  it("siteChildNamesFromTreeTestIds extracts /Sites children", () => {
    assert.deepEqual(
      siteChildNamesFromTreeTestIds([
        "tree-node-/Sites/",
        "tree-node-/Sites/Corporate Investments",
        "tree-node-/Assets/",
      ]),
      ["Corporate Investments"],
    );
  });
});
