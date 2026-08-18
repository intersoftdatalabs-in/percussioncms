/**
 * Explorer DCE-style top menu bar helpers (#2731 / parent #2400).
 *
 * <p>Pure helpers for surface-filtered Playwright: open modern Content
 * Explorer → assert menubar Content/View/Help → open View dropdown and
 * toggle search (legacy test ids live under the View menu).</p>
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuBar: "explorer-menu-bar",
  menuContent: "explorer-menu-content",
  menuView: "explorer-menu-view",
  menuHelp: "explorer-menu-help",
  viewDropdown: "explorer-menu-view-dropdown",
  helpDropdown: "explorer-menu-help-dropdown",
  toggleSearch: "explorer-toggle-search",
  /** View → Clipboard (#3544 / #3551) — panel toggle, enabled even when empty. */
  toggleClipboard: "explorer-toggle-clipboard",
  clipboardPanel: "explorer-clipboard-panel",
  /** Content → Search (#2850) — same panel as View → Search. */
  contentSearch: "explorer-menu-content-search",
  searchPanel: "explorer-search-panel",
  /** Server-driven ActionToolbar (#2730 nested MENU dropdowns). */
  actionToolbar: "action-toolbar",
  /** Shell region wrapping ActionToolbar under ExplorerMenuBar. */
  serverActions: "explorer-server-actions",
  /** Visible chrome label for the server-actions region (#2972). */
  serverActionsLabel: "explorer-server-actions-label",
  /** Non-fatal load error under the server-actions region (#2972). */
  serverActionsError: "explorer-server-actions-error",
  // display-format lives on shell chrome only — see explorer-shell-chrome.js
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
 * Open View menu (nested dropdown) so view-tool test ids are visible.
 * @param {import('@playwright/test').Page} page
 */
async function openViewMenu(page) {
  await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();
  await page
    .locator(`[data-testid="${TEST_IDS.viewDropdown}"]`)
    .waitFor({ state: "visible", timeout: 10_000 });
}

/**
 * Open Content menu dropdown.
 * @param {import('@playwright/test').Page} page
 */
async function openContentMenu(page) {
  await page.locator(`[data-testid="${TEST_IDS.menuContent}"]`).click();
  await page
    .locator(`[data-testid="explorer-menu-content-dropdown"]`)
    .waitFor({ state: "visible", timeout: 10_000 });
}

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  openViewMenu,
  openContentMenu,
};
