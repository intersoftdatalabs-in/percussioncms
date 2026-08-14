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
 * Navigation tree keyboard a11y (#3354 / QA #3155 steps 3 and 6).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-keyboard-a11y.spec.js
 *
 * Intercepts site list + tree so keyboard steps do not depend on sibling
 * slice 4 (#3352) having seeded a live NavTree. A populated three-level
 * tree (including one login-required node) is always available.
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    site: "KeyboardSite",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

const KEYBOARD_TREE = {
  SectionNode: {
    id: "nav-root",
    title: "Keyboard Site",
    folderPath: "//Sites/KeyboardSite",
    sectionType: "section",
    requiresLogin: false,
    childNodes: [
      {
        id: "nav-about",
        title: "About",
        folderPath: "//Sites/KeyboardSite/About",
        sectionType: "section",
        requiresLogin: false,
        childNodes: [
          {
            id: "nav-team",
            title: "Team",
            folderPath: "//Sites/KeyboardSite/About/Team",
            sectionType: "section",
            requiresLogin: false,
            childNodes: [],
          },
        ],
      },
      {
        id: "nav-members",
        title: "Members",
        folderPath: "//Sites/KeyboardSite/Members",
        sectionType: "section",
        requiresLogin: true,
        childNodes: [],
      },
    ],
  },
};

async function installKeyboardTreeRoutes(page) {
  await page.route("**/sitemanage/site/**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SiteSummary: [{ name: "KeyboardSite" }],
      }),
    });
  });
  await page.route("**/sitemanage/section/tree/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(KEYBOARD_TREE),
    });
  });
}

function treeItems(page) {
  return page.locator(
    '[data-testid="architecture-nav-tree"] [role="treeitem"]',
  );
}

async function activeIsTreeitem(page) {
  return page.evaluate(() => {
    const el = document.activeElement;
    return (
      el != null &&
      el.getAttribute("role") === "treeitem" &&
      el.closest('[data-testid="architecture-nav-tree"]') != null
    );
  });
}

test.describe("Navigation tree keyboard a11y (#3354)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Tab/arrows/Home/End + secure i18n badge; no trap @smoke @ui @a11y", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await installKeyboardTreeRoutes(page);
    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("architecture-nav-tree")).toBeVisible({
      timeout: 20_000,
    });

    const treeRole = page.locator(
      '[data-testid="architecture-nav-tree"] [role="tree"]',
    );
    await expect(treeRole).toBeVisible({ timeout: 20_000 });
    await expect(treeRole).toHaveAttribute("aria-label", /navigation tree/i);

    const items = treeItems(page);
    await expect(items.first()).toBeVisible();
    await expect.poll(async () => items.count()).toBeGreaterThanOrEqual(2);

    // Tab into the tree from the site picker (or a later toolbar control).
    await page.getByTestId("architecture-site-select").focus();
    let landed = false;
    for (let i = 0; i < 24; i++) {
      await page.keyboard.press("Tab");
      if (await activeIsTreeitem(page)) {
        landed = true;
        break;
      }
    }
    expect(landed, "Tab must land on a navigation treeitem").toBe(true);

    const firstId = await items.first().getAttribute("data-testid");
    await items.first().focus();
    await expect(items.first()).toBeFocused();

    await items.first().press("ArrowDown");
    await expect(items.nth(1)).toBeFocused();
    await items.nth(1).press("ArrowUp");
    await expect(items.first()).toBeFocused();

    await items.first().press("End");
    const lastIndex = (await items.count()) - 1;
    await expect(items.nth(lastIndex)).toBeFocused();
    await items.nth(lastIndex).press("Home");
    await expect(items.first()).toBeFocused();

    // Collapse root (Arrow Left) then expand (Arrow Right) and enter first child.
    await items.first().press("ArrowLeft");
    await expect.poll(async () => items.count()).toBe(1);
    await items.first().press("ArrowRight");
    await expect.poll(async () => items.count()).toBeGreaterThan(1);
    await items.first().press("ArrowRight");
    await expect(items.nth(1)).toBeFocused();

    // Enter/Space select the focused node; Enter on a branch also toggles expand.
    const about = page.getByTestId("nav-tree-item-nav-about");
    await about.focus();
    await about.press("Enter");
    await expect(about).toHaveAttribute("aria-selected", "true");
    await expect(page.getByTestId("nav-tree-item-nav-team")).toBeVisible();
    await about.press(" ");
    await expect(about).toHaveAttribute("aria-selected", "true");
    await expect(page.getByTestId("nav-tree-item-nav-team")).toHaveCount(0);

    const members = page.getByTestId("nav-tree-item-nav-members");
    await members.focus();
    await members.press("Enter");
    await expect(members).toHaveAttribute("aria-selected", "true");
    await members.press("ArrowLeft");
    await expect(items.first()).toBeFocused();

    const secure = page.getByTestId("nav-tree-secure-nav-members");
    await expect(secure).toBeVisible();
    await expect(secure).toHaveAttribute("title", /requires login/i);
    await expect(secure).toHaveAttribute("aria-label", /requires login/i);
    await expect(secure).toHaveAttribute(
      "data-i18n-key",
      "perc.ui.architecture.modern@Requires login",
    );
    await expect(secure).toHaveAttribute(
      "data-i18n-badge-key",
      "perc.ui.architecture.modern@Secure",
    );

    await items.first().press("End");
    await expect(items.nth((await items.count()) - 1)).toBeFocused();
    await page.keyboard.press("Tab");
    expect(
      await activeIsTreeitem(page),
      "Tab from the last node must leave the tree (no trap)",
    ).toBe(false);

    // Restore a known first-item focus so firstId is still a treeitem.
    expect(firstId).toBeTruthy();

    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            String(e),
          ),
      ),
    ).toEqual([]);
  });
});
