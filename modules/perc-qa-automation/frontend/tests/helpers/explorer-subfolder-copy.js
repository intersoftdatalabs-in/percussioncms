/**
 * Explorer Subfolder Copy wizard helpers (#2792 / parent #2400).
 *
 * <p>Pure helpers for surface-filtered Playwright: open modern Content
 * Explorer → Content menu → Subfolder Copy when a folder path is in context.</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuContent: "explorer-menu-content",
  contentDropdown: "explorer-menu-content-dropdown",
  subfolderCopyMenu: "explorer-content-subfolder-copy",
  subfolderCopyPanel: "explorer-subfolder-copy-panel",
  subfolderCopyHint: "explorer-subfolder-copy-hint",
  wizard: "subfolder-copy-wizard",
  sourceInput: "subfolder-copy-source",
  cancel: "subfolder-copy-cancel",
  back: "subfolder-copy-back",
  next: "subfolder-copy-next",
  stepSource: "subfolder-copy-step-source",
  stepTarget: "subfolder-copy-step-target",
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
 * Deep-link Explorer into a folder when the product accepts path query.
 *
 * @param {string} baseUrl
 * @param {string} [path] CMS path e.g. /Sites/Demo/Home
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
