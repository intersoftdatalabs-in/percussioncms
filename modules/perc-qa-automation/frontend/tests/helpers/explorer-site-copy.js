/**
 * Explorer Site Copy wizard helpers (#2767 / parent #2400).
 *
 * <p>Pure helpers for surface-filtered Playwright: open modern Content
 * Explorer → Content menu → Site Copy when under a site path.</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuContent: "explorer-menu-content",
  contentDropdown: "explorer-menu-content-dropdown",
  siteCopyMenu: "explorer-content-site-copy",
  siteCopyPanel: "explorer-site-copy-panel",
  siteCopyHint: "explorer-site-copy-hint",
  wizard: "site-copy-wizard",
  sourceInput: "site-copy-source",
  tree: "explorer-tree",
  detailList: "detail-list",
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
 * Deep-link Explorer into a site folder when the product accepts path query.
 * Falls back to SPA entry only when path is empty.
 *
 * @param {string} baseUrl
 * @param {string} [path] CMS path e.g. /Sites/Demo
 * @returns {string}
 */
function explorerSpaUrlWithPath(baseUrl, path) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const p = path ? `&path=${encodeURIComponent(path)}` : "";
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer${p}&_=${Date.now()}`;
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

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  explorerSpaUrlWithPath,
  openContentMenu,
};
