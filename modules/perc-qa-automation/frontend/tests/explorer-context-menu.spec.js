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
 * Playwright surface: #3629 / parent #3102 — Explorer row context menu
 * nested MENU (same catalog as the toolbar).
 *
 * <p>Right-click an enabled detail row on {@code spa.jsp?entry=explorer}.
 * Do not soft-skip when ≥1 enabled Sites/Assets row exists. GET
 * {@code /actions/find} must succeed; MENU parents render as nested
 * pivots ({@code aria-haspopup=menu}), not a flat label dump.</p>
 *
 * <p>Tags: {@code @explorer-context-menu} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-context-menu.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");
const { collectMenuParents } = require("./helpers/explorer-action-toolbar-catalog");

const DESKTOP_ONLY_NAME = "night-desktop-only-cx";

async function fetchActionsFind(page) {
  return page.evaluate(async () => {
    const paths = [
      "/Rhythmyx/services/actions/find",
      "/services/actions/find",
      "/Rhythmyx/rest/actions/find",
    ];
    for (const url of paths) {
      try {
        const res = await fetch(url, { credentials: "same-origin" });
        if (!res.ok) {
          return { url, status: res.status, payload: null };
        }
        return { url, status: res.status, payload: await res.json() };
      } catch {
        // try next path
      }
    }
    return { url: null, status: 0, payload: null };
  });
}

async function openFolderWithEnabledRows(page) {
  await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
    waitUntil: "networkidle",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator('[data-testid="detail-list"]')).toBeVisible({
    timeout: 15_000,
  });

  const enabledRows = page.locator(
    '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
  );

  async function clickTree(label) {
    const node = page
      .locator(
        `[data-testid="tree-node-/${label}/"], [data-testid="tree-node-/${label}"], [data-testid*="tree-node"][data-testid*="${label}"]`,
      )
      .first();
    if ((await node.count()) > 0) {
      await node.click({ timeout: 10_000 });
      await page.waitForLoadState("networkidle").catch(() => {});
    }
  }

  await clickTree("Sites");
  if ((await enabledRows.count()) > 0) {
    return enabledRows;
  }
  await clickTree("Assets");
  await expect(
    enabledRows.first(),
    "H2 Explorer must have ≥1 enabled Sites/Assets detail row; no soft-skip (#3629)",
  ).toBeVisible({ timeout: 15_000 });
  return enabledRows;
}

test.describe("modern React Content Explorer — nested row context MENU (#3629)", () => {
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("response", (res) => {
      if (res.status() < 500) return;
      const url = res.url();
      if (/\/actions\/find(\?|$)/i.test(url)) {
        pageErrors.push(`HTTP ${res.status()} ${url}`);
      }
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (
        /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
          text,
        )
      ) {
        return;
      }
      pageErrors.push(text);
    });
    await loginAsAdmin(page);
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "right-click enabled row mounts nested MENU (no skip when a row exists)",
    { tag: ["@explorer-context-menu", "@explorer", "@smoke"] },
    async ({ page }) => {
      const desktopOnlyWire = {
        id: 9_000_002,
        name: DESKTOP_ONLY_NAME,
        label: "Desktop CX only (night probe)",
        sortRank: 9999,
        menuType: "MENUITEM",
        url: "rxapp://launch-cx",
      };

      await page.route("**/actions/find**", async (route) => {
        const reqUrl = route.request().url();
        if (/\/actions\/find\/(types|templates)/i.test(reqUrl)) {
          return route.continue();
        }
        const response = await route.fetch();
        const contentType = response.headers()["content-type"] || "";
        if (!contentType.includes("application/json")) {
          return route.fulfill({ response });
        }
        let body;
        try {
          body = await response.json();
        } catch {
          return route.fulfill({ response });
        }
        if (Array.isArray(body?.ActionMenu)) {
          body = { ...body, ActionMenu: [...body.ActionMenu, desktopOnlyWire] };
        } else if (Array.isArray(body?.ActionMenuList)) {
          body = {
            ...body,
            ActionMenuList: [...body.ActionMenuList, desktopOnlyWire],
          };
        }
        return route.fulfill({
          status: response.status(),
          headers: {
            ...response.headers(),
            "content-type": "application/json",
          },
          body: JSON.stringify(body),
        });
      });

      const rows = await openFolderWithEnabledRows(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`)).toBeVisible();

      const catalog = await fetchActionsFind(page);
      expect(
        catalog.url,
        "GET /actions/find must succeed so nested context MENU is asserted (#3629)",
      ).toBeTruthy();
      expect(
        catalog.status,
        `GET /actions/find must be HTTP 200, got ${catalog.status} at ${catalog.url}`,
      ).toBe(200);

      const menuParents = collectMenuParents(catalog.payload);
      test.info().annotations.push({
        type: "note",
        description: `find() ${catalog.url} MENU parents=${menuParents.length}`,
      });
      expect(
        menuParents.length,
        "H2 catalog must include cascading MENU parents (Paste/Arrange/View/Create)",
      ).toBeGreaterThan(0);

      const requiredNames = ["Paste", "Arrange", "View", "Create"];
      const required = menuParents.filter((p) => requiredNames.includes(p.name));
      expect(
        required.map((p) => p.name).sort(),
        "static MENU parents must stay nested in GET /actions/find",
      ).toEqual([...requiredNames].sort());

      await rows.first().click({ button: "right" });
      const menu = page.locator(`[data-testid="${TEST_IDS.contextMenu}"]`);
      await expect(menu).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.contextMenuEmpty}"]`),
      ).toHaveCount(0);

      await expect(
        page.locator(`[data-testid="context-menu-item-${DESKTOP_ONLY_NAME}"]`),
      ).toHaveCount(0);

      const parents = menu.locator(
        '[data-testid^="context-menu-item-"][aria-haspopup="menu"]',
      );
      await expect
        .poll(async () => parents.count(), { timeout: 10_000 })
        .toBeGreaterThan(0);

      for (const parentMenu of required) {
        const parentItem = menu.locator(
          `[data-testid="context-menu-item-${parentMenu.name}"][aria-haspopup="menu"]`,
        );
        await expect(
          parentItem,
          `MENU parent "${parentMenu.name}" must be a nested context-menu pivot`,
        ).toBeVisible();
      }

      const closedNames = await menu
        .locator(':scope > ul > li > [data-testid^="context-menu-item-"]')
        .evaluateAll((els) =>
          els.map((el) => el.getAttribute("data-testid") || ""),
        );
      for (const parentMenu of menuParents) {
        for (const childName of parentMenu.childNames) {
          expect(
            closedNames.includes(`context-menu-item-${childName}`),
            `closed context menu dumped child "${childName}" of "${parentMenu.name}" as a top-level item`,
          ).toBe(false);
        }
      }

      const firstRequired = required[0].name;
      const parentItem = menu.locator(
        `[data-testid="context-menu-item-${firstRequired}"][aria-haspopup="menu"]`,
      );
      await parentItem.evaluate((el) => {
        el.scrollIntoView({ block: "center", inline: "nearest" });
        el.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      });
      await expect(
        page.locator(`[data-testid="context-menu-submenu-${firstRequired}"]`),
      ).toBeVisible({ timeout: 5_000 });
      await expect(
        page.locator(
          `[data-testid="context-menu-submenu-${firstRequired}"] [role="menuitem"]`,
        ).first(),
      ).toBeVisible();

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="context-menu"]',
      });
    },
  );
});
