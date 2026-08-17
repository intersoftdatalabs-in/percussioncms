/**
 * Explorer Sites list + Create Site helpers (#3003 / parent #2989).
 *
 * <p>Pure helpers for surface-filtered Playwright: REST path/folder/Sites
 * children, modern Explorer Sites tree expansion, and Content → Create Site
 * wizard chrome (traditional repository site).</p>
 *
 * @see tests/explorer-sites-list-create.spec.js
 * @see tests/bugs/bug-1750-demo-sites-sample-site.spec.js
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  detailList: "detail-list",
  menuContent: "explorer-menu-content",
  contentDropdown: "explorer-menu-content-dropdown",
  /** Content → Create Site menuitem (#3002). */
  createSiteMenu: "explorer-content-create-site",
  createSitePanel: "explorer-site-create-panel",
  wizard: "site-create-wizard",
  stepType: "site-create-step-type",
  stepDetails: "site-create-step-details",
  stepTemplate: "site-create-step-template",
  stepConfirm: "site-create-step-confirm",
  stepProgress: "site-create-step-progress",
  typeTraditional: "site-create-type-traditional",
  typePage: "site-create-type-page",
  typeVirtual: "site-create-type-virtual",
  siteName: "site-create-name",
  description: "site-create-description",
  templateName: "site-create-template-name",
  baseTemplate: "site-create-base-template",
  confirmSummary: "site-create-confirm-summary",
  next: "site-create-next",
  back: "site-create-back",
  run: "site-create-run",
  cancel: "site-create-cancel",
  traditionalNote: "site-create-traditional-note",
  virtualNote: "site-create-virtual-note",
  virtualRoot: "site-create-virtual-root",
  virtualSourceNote: "site-create-virtual-source-note",
  managedNav: "site-create-managed-nav",
  managedNavHelp: "site-create-managed-nav-help",
  confirmManagedNav: "site-create-confirm-managed-nav",
});

const PRODUCT_ISSUES = Object.freeze({
  parent: 2989,
  slice1SitesList: 3001,
  slice2CreateSite: 3002,
  slice3PlaywrightDocs: 3003,
  repo: "https://github.com/intersoftdatalabs-in/percussioncms/issues",
});

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function explorerSpaUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

/**
 * Pathmanagement folder service base (no trailing path segment).
 * @param {string} baseUrl
 * @returns {string}
 */
function pathFolderServiceUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/services/pathmanagement/path/folder`;
}

/**
 * @param {string} baseUrl
 * @returns {string}
 */
function sitesFolderUrl(baseUrl) {
  return `${pathFolderServiceUrl(baseUrl)}/Sites`;
}

/**
 * Encode a CMS finder path as a path/folder URL suffix (no leading slash).
 * @param {string} cmsPath
 * @returns {string}
 */
function encodeFolderListPath(cmsPath) {
  return String(cmsPath || "")
    .trim()
    .replace(/\\/g, "/")
    .replace(/^[A-Za-z]:/, "")
    .replace(/^\/+/, "")
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}

/**
 * Prefer PathItem.folderPath (FOLDER_ROOT) over finder name path (#3326).
 * @param {{ path?: string, folderPath?: string } | null | undefined} item
 * @returns {string}
 */
function siteChildListPath(item) {
  const folder = firstFolderPath(item);
  if (folder.length > 0) {
    return folder;
  }
  return item && item.path ? String(item.path) : "";
}

/**
 * PathItem.folderPath, or the first folderPaths[] entry (Jackson list bind).
 * @param {{ folderPath?: string, folderPaths?: unknown } | null | undefined} item
 * @returns {string}
 */
function firstFolderPath(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  const direct = item.folderPath ? String(item.folderPath).trim() : "";
  if (direct.length > 0) {
    return direct;
  }
  const list = item.folderPaths;
  if (Array.isArray(list) && list.length > 0) {
    const first = String(list[0] || "").trim();
    if (first.length > 0) {
      return first;
    }
  }
  return "";
}

/**
 * Candidate CMS paths to list children of a Sites PathItem (#3410 / #3326).
 * Order: FOLDER_ROOT, finder path, /Sites/&lt;name&gt;.
 * @param {{ path?: string, name?: string, folderPath?: string, folderPaths?: unknown } | null | undefined} item
 * @returns {string[]}
 */
function siteChildListCandidates(item) {
  const out = [];
  const seen = new Set();
  const push = (raw) => {
    const v = String(raw || "").trim();
    if (!v) {
      return;
    }
    const key = v.replace(/\\/g, "/").replace(/\/+$/, "").toLowerCase();
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    out.push(v);
  };
  push(firstFolderPath(item));
  push(item && item.path);
  const name = item && typeof item.name === "string" ? item.name.trim() : "";
  if (name) {
    push(`/Sites/${name.replace(/\s+/g, "")}`);
    push(`/Sites/${name.replace(/\s+/g, "_")}`);
  }
  return out;
}

/**
 * @param {string} baseUrl
 * @param {string} cmsPath
 * @returns {string}
 */
function folderChildrenUrl(baseUrl, cmsPath) {
  const suffix = encodeFolderListPath(cmsPath);
  const root = pathFolderServiceUrl(baseUrl);
  return suffix ? `${root}/${suffix}` : `${root}/`;
}

/**
 * Open Content menu dropdown.
 * @param {import('@playwright/test').Page} page
 */
async function openContentMenu(page) {
  await page.locator(`[data-testid="${TEST_IDS.menuContent}"]`).click();
  await page
    .locator(`[data-testid="${TEST_IDS.contentDropdown}"]`)
    .waitFor({ state: "visible", timeout: 10_000 });
}

/**
 * Locator for the Explorer tree root Sites node (exact path identity).
 * @param {import('@playwright/test').Page} page
 */
function sitesTreeRootLocator(page) {
  return page.locator(
    `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], ` +
      `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
  );
}

/**
 * Expand a tree node via the toggle (row click only selects; #3410).
 * @param {import('@playwright/test').Locator} node
 */
async function expandExplorerTreeNode(node) {
  const toggle = node.locator('[aria-hidden="true"]').first();
  await toggle.click();
}

/**
 * Tree nodes under /Sites (all descendants currently rendered, not only
 * immediate children). Selector matches any {@code tree-node-/Sites/...}
 * except the exact root {@code tree-node-/Sites/}. Name extraction for
 * immediate site folders is handled separately by
 * {@link siteChildNamesFromTreeTestIds}.
 *
 * @param {import('@playwright/test').Page} page
 */
function sitesTreeDescendantsLocator(page) {
  return page.locator(
    `[data-testid="${TEST_IDS.tree}"] [data-testid^="tree-node-/Sites/"]:not([data-testid="tree-node-/Sites/"])`,
  );
}

/**
 * Extract site folder name segments from explorer tree data-testid values.
 * Captures the first path segment under /Sites for each id
 * ({@code tree-node-/Sites/<name>[/...]}), including deeper descendants
 * (callers should treat the list as a multiset of site folder names).
 *
 * @param {readonly string[]} nodeTestIds
 * @returns {string[]}
 */
function siteChildNamesFromTreeTestIds(nodeTestIds) {
  const out = [];
  for (const id of nodeTestIds || []) {
    const m = /^tree-node-\/Sites\/([^/]+)/.exec(String(id || ""));
    if (m && m[1]) {
      out.push(m[1]);
    }
  }
  return out;
}

/**
 * Unique site name for create tests (avoids collisions on re-runs).
 * @param {string} [prefix]
 * @returns {string}
 */
function uniqueQaSiteName(prefix = "QaSite") {
  const safe = String(prefix || "QaSite").replace(/[^A-Za-z0-9]/g, "");
  const stamp = Date.now().toString(36).slice(-6);
  return `${safe || "QaSite"}${stamp}`;
}

/**
 * Skip reason when Create Site affordance is not deployed (#3002 not in image).
 * @returns {string}
 */
function createSiteMissingSkipReason() {
  const { slice2CreateSite, parent, repo } = PRODUCT_ISSUES;
  return (
    `BUG: Content → Create Site not present in Explorer under test ` +
    `(parent #${parent} slice 2 #${slice2CreateSite}). Requires WebUI Create ` +
    `Site affordance in the image: ${repo}/${slice2CreateSite}`
  );
}

/**
 * Soft-skip annotation description when Sites list is empty but create path
 * coverage still applies (#3003 acceptance).
 * @returns {string}
 */
/**
 * Known browser console noise on Explorer Sites (missing FF nav types 313–315
 * still 404/400 some nav/icon/preview URLs — related #3326). Uncaught
 * pageerror must not be filtered.
 * @param {string} text
 * @returns {boolean}
 */
function isKnownExplorerSitesConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

function emptySitesSoftSkipNote() {
  const { slice1SitesList, parent, repo } = PRODUCT_ISSUES;
  return (
    `Sites list empty under fixture (parent #${parent} / seed #${slice1SitesList}). ` +
    `List assertions soft-skipped; Create Site path remains in scope. ` +
    `${repo}/${slice1SitesList}`
  );
}

module.exports = {
  TEST_IDS,
  PRODUCT_ISSUES,
  explorerSpaUrl,
  pathFolderServiceUrl,
  sitesFolderUrl,
  encodeFolderListPath,
  siteChildListPath,
  siteChildListCandidates,
  folderChildrenUrl,
  openContentMenu,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  siteChildNamesFromTreeTestIds,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  emptySitesSoftSkipNote,
  isKnownExplorerSitesConsoleNoise,
};
