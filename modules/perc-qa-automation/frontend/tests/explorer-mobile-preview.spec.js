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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright: #5078 / parent #4530 — Content → Mobile preview.
 *
 * A selected page opens a window whose URL has percmobilepreview=true.
 * Folders, assets, and an empty selection do not open a window and do not
 * claim success. Toolbar Preview stays a desktop URL.
 *
 * The page-render and site-path responses are context-level test doubles
 * (popups are separate pages) so assembly does not have to succeed for the
 * window-URL proof.
 *
 * npm run test:surface -- --path tests/explorer-mobile-preview.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { pathItemRows } = require("./helpers/demo-sites");
const {
  encodeFolderListPath,
  openContentMenu,
  pathFolderServiceUrl,
} = require("./helpers/explorer-sites-list-create");
const {
  TEST_IDS,
  explorerMobilePreviewUrl,
  isDesktopPreviewWindowUrl,
  isKnownExplorerMobilePreviewConsoleNoise,
  isMobilePreviewWindowUrl,
} = require("./helpers/explorer-mobile-preview");

const TAGS = ["@explorer-mobile-preview", "@explorer"];

const PREVIEW_DOUBLE =
  "<!DOCTYPE html><html><body>explorer-preview-double</body></html>";

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !isKnownExplorerMobilePreviewConsoleNoise(msg.text())
    ) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await installPreviewDoubles(page);
  await page.goto(explorerMobilePreviewUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

async function installPreviewDoubles(page) {
  const fulfill = (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: PREVIEW_DOUBLE,
    });
  // page.route does not cover window.open popups. Context routes do.
  const context = page.context();
  await context.route("**/pagemanagement/render/page/**", fulfill);
  await context.route("**/assembler/render**", fulfill);
  await context.route("**/Sites/**", async (route) => {
    const url = route.request().url();
    if (!/percmobilepreview=/i.test(url)) {
      return route.continue();
    }
    return fulfill(route);
  });
}

async function listReady(page) {
  await page
    .locator(`[data-testid="${TEST_IDS.detailList}"]`)
    .waitFor({ timeout: 20_000 });
}

function rowPath(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  return String(item.path || item.folderPath || "")
    .trim()
    .replace(/\\/g, "/");
}

function isFolderRow(item) {
  const type = String(item.type || "").trim().toLowerCase();
  const category = String(item.category || "").trim().toLowerCase();
  const path = rowPath(item);
  return (
    type === "folder" ||
    type === "fsfolder" ||
    type === "site" ||
    category === "folder" ||
    category === "site" ||
    path.endsWith("/")
  );
}

/**
 * Same order as Explorer {@code resolvePublishKind}: asset token wins over
 * a Sites path, then page / landing page. H2 sample images live under
 * Sites, not the empty Assets library root.
 */
function publishKind(item) {
  if (!item || isFolderRow(item)) {
    return "none";
  }
  if (!String(item.id || "").trim()) {
    return "none";
  }
  const token = `${item.type || ""} ${item.category || ""}`.toLowerCase();
  const path = rowPath(item).toLowerCase();
  if (
    token.includes("asset") ||
    path === "/assets" ||
    path.startsWith("/assets/")
  ) {
    return "asset";
  }
  if (
    token.includes("page") ||
    path === "/sites" ||
    path.startsWith("/sites/")
  ) {
    return "page";
  }
  return "none";
}

function safeItemName(name) {
  return (
    typeof name === "string" &&
    name.trim().length > 0 &&
    !/[\r\n"\\]/.test(name)
  );
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {"page" | "asset"} kind
 * @returns {Promise<{ folder: string, name: string } | null>}
 */
async function findListedItem(page, kind) {
  const queue = kind === "asset" ? ["/Sites", "/Assets"] : ["/Sites"];
  const seen = new Set();
  while (queue.length > 0 && seen.size < 80) {
    const folder = queue.shift();
    if (!folder || seen.has(folder)) {
      continue;
    }
    seen.add(folder);
    const url = `${pathFolderServiceUrl(BASE_URL)}/${encodeFolderListPath(folder)}`;
    const response = await page.request.get(url);
    if (!response.ok()) {
      continue;
    }
    let body;
    try {
      body = await response.json();
    } catch {
      continue;
    }
    for (const row of pathItemRows(body)) {
      if (!row || typeof row !== "object") {
        continue;
      }
      const match = publishKind(row) === kind && safeItemName(row.name);
      if (match) {
        return { folder, name: String(row.name).trim() };
      }
      if (isFolderRow(row)) {
        const child = rowPath(row).replace(/\/+$/, "");
        const next =
          child ||
          `${folder.replace(/\/+$/, "")}/${String(row.name || "").trim()}`;
        if (next && !seen.has(next)) {
          queue.push(next);
        }
      }
    }
  }
  return null;
}

async function openFolderListing(page, folderPath) {
  const root = String(BASE_URL || "").replace(/\/$/, "");
  await page.goto(
    `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&path=${encodeURIComponent(folderPath)}&_=${Date.now()}`,
    { waitUntil: "domcontentloaded" },
  );
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  await listReady(page);
}

async function selectNamedItem(page, name) {
  const row = page.locator(
    `[data-testid="${TEST_IDS.detailList}"] tbody tr[data-row-kind="item"][data-item-name=${JSON.stringify(name)}]`,
  );
  await expect(row.first()).toBeVisible({ timeout: 20_000 });
  await row.first().click({ force: true });
}

async function openMobileMenuItem(page) {
  await openContentMenu(page);
  const item = page.locator(`[data-testid="${TEST_IDS.mobilePreview}"]`);
  await expect(item).toBeVisible();
  return item;
}

test.describe("Explorer mobile preview of the selected page (#5078 / #4530)", () => {
  test(
    "UI: a selected page opens mobile preview and desktop preview stays desktop",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(180_000);
      const jsErrors = await openExplorer(page);
      const listed = await findListedItem(page, "page");
      expect(listed, "H2 Explorer has no selectable page").toBeTruthy();
      await openFolderListing(page, listed.folder);
      await selectNamedItem(page, listed.name);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-item-kind", "page");
      const before = await shell.getAttribute("data-selected-item-id");
      expect(before, "selected page id").toBeTruthy();

      const desktopButton = page.locator(
        `[data-testid="${TEST_IDS.desktopPreview}"]`,
      );
      await expect(desktopButton).toBeEnabled();
      const desktopPopupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      await desktopButton.click();
      const desktopPopup = await desktopPopupPromise;
      await desktopPopup.waitForLoadState("domcontentloaded");
      expect(isDesktopPreviewWindowUrl(desktopPopup.url())).toBe(true);
      expect(desktopPopup.url()).not.toContain("percmobilepreview=true");
      await desktopPopup.close();

      const mobileItem = await openMobileMenuItem(page);
      const mobilePopupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      await mobileItem.click();
      const mobilePopup = await mobilePopupPromise;
      await mobilePopup.waitForLoadState("domcontentloaded");
      expect(isMobilePreviewWindowUrl(mobilePopup.url())).toBe(true);
      expect(mobilePopup.url()).toContain(encodeURIComponent(before));
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "opened");
      await expect(status).toHaveAttribute("data-reason", "");
      const recorded = await status.getAttribute("data-preview-url");
      expect(isMobilePreviewWindowUrl(recorded)).toBe(true);
      await expect(status).toContainText("Mobile preview opened");
      await mobilePopup.close();
      await expect(shell).toHaveAttribute("data-selected-item-id", before);
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: no selection does not open a preview window",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-item-id", "");
      const menuItem = await openMobileMenuItem(page);
      const popupPromise = page
        .waitForEvent("popup", { timeout: 2_000 })
        .catch(() => null);
      await menuItem.click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "none");
      await expect(status).toContainText("Select a page to open mobile preview");
      expect(await popupPromise).toBeNull();
      await expect(shell).toHaveAttribute("data-selected-item-id", "");
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a folder does not open a preview window",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      await root.first().click();
      await listReady(page);
      const folderRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
      );
      await expect(folderRow.first()).toBeVisible({ timeout: 20_000 });
      await folderRow.first().click({ force: true });
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      const folderPath = await shell.getAttribute("data-selected-folder-path");
      const menuItem = await openMobileMenuItem(page);
      const popupPromise = page
        .waitForEvent("popup", { timeout: 2_000 })
        .catch(() => null);
      await menuItem.click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "folder");
      await expect(status).toContainText("Folders do not open a mobile preview:");
      const statusText = (await status.textContent()) || "";
      const named = statusText.split(":").slice(1).join(":").trim();
      expect(named.length, "folder failure names the folder").toBeGreaterThan(0);
      expect(statusText).not.toContain("Mobile preview opened");
      expect(await popupPromise).toBeNull();
      await expect(shell).toHaveAttribute(
        "data-selected-folder-path",
        folderPath || "",
      );
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: an asset does not open a preview window",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(180_000);
      const jsErrors = await openExplorer(page);
      const listed = await findListedItem(page, "asset");
      expect(listed, "H2 Explorer has no selectable asset").toBeTruthy();
      await openFolderListing(page, listed.folder);
      await selectNamedItem(page, listed.name);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-item-kind", "asset");
      const before = await shell.getAttribute("data-selected-item-id");
      expect(before, "selected asset id").toBeTruthy();
      const menuItem = await openMobileMenuItem(page);
      const popupPromise = page
        .waitForEvent("popup", { timeout: 2_000 })
        .catch(() => null);
      await menuItem.click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "not-page");
      await expect(status).toContainText("Mobile preview is only available for a page");
      expect(await popupPromise).toBeNull();
      await expect(shell).toHaveAttribute("data-selected-item-id", before);
      await expect(shell).toHaveAttribute("data-selected-item-kind", "asset");
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
