/**
 * Explorer product shell chrome helpers (#2850 / parent #2407 / grandparent #2400).
 *
 * <p>Stable testids for search toggle, display format, action toolbar, and
 * free-text SearchPanel wiring. Used by surface-filtered Playwright
 * {@code explorer-shell-chrome.spec.js}.</p>
 */

"use strict";

const {
  TEST_IDS: MENU_TEST_IDS,
  explorerSpaUrl,
  openViewMenu,
  openContentMenu,
} = require("./explorer-menu-bar");

/** Shell composition test ids (#2407 / #2850). */
const TEST_IDS = Object.freeze({
  ...MENU_TEST_IDS,
  displayFormat: "explorer-display-format",
  displayFormatError: "explorer-display-format-error",
  viewTools: "explorer-view-tools",
  viewToolSearch: "explorer-view-tool-search",
  viewToolSecurity: "explorer-view-tool-security",
  sidePanels: "explorer-side-panels",
  contentSearch: "explorer-menu-content-search",
  searchPanelHost: "explorer-search-panel",
  searchPanel: "search-panel",
  searchInput: "search-panel-input",
  searchSubmit: "search-panel-submit",
  tree: "explorer-tree",
  detailList: "detail-list",
  reducedActions: "reduced-actions",
});

/**
 * Soft-assert a locator is visible when present; do not fail if count is 0.
 * Used for optional chrome from sibling slices that may not be on the build yet.
 *
 * @param {import('@playwright/test').Locator} locator
 * @param {number} [timeoutMs]
 * @returns {Promise<boolean>} true when visible
 */
async function softVisible(locator, timeoutMs = 3_000) {
  try {
    const count = await locator.count();
    if (count === 0) return false;
    await locator.first().waitFor({ state: "visible", timeout: timeoutMs });
    return true;
  } catch {
    return false;
  }
}

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  openViewMenu,
  openContentMenu,
  softVisible,
};
