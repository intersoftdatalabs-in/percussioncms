/**
 * Explorer Sites/Assets tree + detail-list helpers (#3575 / parent #3102).
 *
 * <p>Product-route proof on {@code spa.jsp?entry=explorer}: Sites and Assets
 * roots in {@code explorer-tree}, and a sample folder lists children in
 * {@code detail-list}. Must not soft-skip when H2 QA has sample sites.</p>
 *
 * @see tests/explorer-sites-assets-tree-list.spec.js
 * @see tests/explorer-sites-list-create.spec.js
 */

"use strict";

const {
  shouldSoftSkipSitesList,
  pathItemNames,
  hasAnyExpectedSampleSite,
  hasAllExpectedSampleSites,
  EXPECTED_SAMPLE_SITE_NAMES,
} = require("./demo-sites");
const {
  explorerSpaUrl,
  sitesFolderUrl,
  pathFolderServiceUrl,
  folderChildrenUrl,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  siteChildNamesFromTreeTestIds,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-sites-list-create");

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  treeError: "explorer-tree-error",
  detailList: "detail-list",
  detailEmpty: "detail-list-empty",
  detailRowPrefix: "detail-row-",
});

const REQUIRED_TREE_ROOTS = Object.freeze(["Sites", "Assets"]);

/**
 * Locator for a well-known Explorer tree root (exact path identity).
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} rootName
 */
function treeRootLocator(page, rootName) {
  const name = String(rootName || "").replace(/^\/+|\/+$/g, "");
  return page.locator(
    `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${name}/"], ` +
      `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${name}"]`,
  );
}

/**
 * @param {import("@playwright/test").Page} page
 */
function assetsTreeRootLocator(page) {
  return treeRootLocator(page, "Assets");
}

/**
 * Whether rendered tree testids include a named root.
 *
 * @param {readonly string[]} nodeTestIds
 * @param {string} rootName
 * @returns {boolean}
 */
function treeHasRoot(nodeTestIds, rootName) {
  const n = String(rootName || "");
  if (!n) {
    return false;
  }
  const ids = nodeTestIds || [];
  const exactSlash = `tree-node-/${n}/`;
  const exact = `tree-node-/${n}`;
  return ids.some((id) => {
    const v = String(id || "");
    return v === exactSlash || v === exact || v.startsWith(exactSlash);
  });
}

/**
 * Product-route Sites/Assets tree+list (#3575) must not soft-skip when
 * Sites/Assets roots are on the explorer tree or REST listed sample children.
 *
 * @param {{
 *   sitesRootVisible?: boolean,
 *   assetsRootVisible?: boolean,
 *   sitesChildNames?: readonly string[],
 * }} [detail]
 * @returns {boolean}
 */
function shouldSkipSitesAssetsTreeList(detail = {}) {
  if (detail.sitesRootVisible === true || detail.assetsRootVisible === true) {
    return false;
  }
  if ((detail.sitesChildNames || []).length > 0) {
    return false;
  }
  return false;
}

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function assetsFolderUrl(baseUrl) {
  return `${pathFolderServiceUrl(baseUrl)}/Assets`;
}

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function rootFolderUrl(baseUrl) {
  return `${pathFolderServiceUrl(baseUrl)}/`;
}

module.exports = {
  TEST_IDS,
  REQUIRED_TREE_ROOTS,
  EXPECTED_SAMPLE_SITE_NAMES,
  explorerSpaUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  rootFolderUrl,
  pathFolderServiceUrl,
  folderChildrenUrl,
  pathItemNames,
  hasAnyExpectedSampleSite,
  hasAllExpectedSampleSites,
  shouldSoftSkipSitesList,
  shouldSkipSitesAssetsTreeList,
  treeRootLocator,
  assetsTreeRootLocator,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  siteChildNamesFromTreeTestIds,
  treeHasRoot,
  isKnownExplorerSitesConsoleNoise,
};
