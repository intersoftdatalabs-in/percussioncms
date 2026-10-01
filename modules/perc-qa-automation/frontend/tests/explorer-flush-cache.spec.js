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
 * Explorer Flush Cache (Refresh Item) uses POST /services/assembly/flush-cache.
 *
 * <p>Tags: {@code @explorer-flush-cache} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-flush-cache.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

async function injectFlushCacheAction(page) {
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
    const flush = {
      name: "Flush_Cache",
      label: "Flush Cache",
      sortRank: 1,
      menuType: "MENUITEM",
      url: "../sys_uiSupport/flushcache.html",
    };
    const push = (list) => {
      const rows = Array.isArray(list) ? list : list != null ? [list] : [];
      if (rows.some((row) => row && row.name === "Flush_Cache")) {
        return rows;
      }
      return [...rows, flush];
    };
    if (Array.isArray(body?.ActionMenu) || (body?.ActionMenu && typeof body.ActionMenu === "object")) {
      body = { ...body, ActionMenu: push(body.ActionMenu) };
    } else if (Array.isArray(body?.ActionMenuList) || (body?.ActionMenuList && typeof body.ActionMenuList === "object")) {
      body = { ...body, ActionMenuList: push(body.ActionMenuList) };
    } else if (Array.isArray(body)) {
      body = push(body);
    }
    return route.fulfill({
      status: response.status(),
      headers: { ...response.headers(), "content-type": "application/json" },
      body: JSON.stringify(body),
    });
  });
}

test.describe("modern React Content Explorer — flush assembler cache", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "confirm flushes and shows success; cancel and HTTP 403 do not",
    { tag: ["@explorer-flush-cache", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      const legacy = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") {
          return;
        }
        const text = msg.text();
        const loc = msg.location()?.url || "";
        if (/favicon|Download the React DevTools/i.test(text)) {
          return;
        }
        // The 403 case is the stub under test. Unrelated 404s (not flush) are catalog noise.
        if (/status of 403/.test(text)) {
          return;
        }
        if (/status of 404/.test(text) && !/flush/i.test(`${loc} ${text}`)) {
          return;
        }
        consoleErrors.push(`${text} ${loc}`);
      });
      page.on("request", (req) => {
        const u = req.url();
        if (u.includes("flushcache.html") || u.includes("sys_uiSupport/flushcache")) {
          legacy.push(u);
        }
      });

      await injectFlushCacheAction(page);
      let flushPosts = 0;
      await page.route("**/assembly/flush-cache**", async (route) => {
        flushPosts += 1;
        if (flushPosts === 1) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({ ok: true, message: "Assembler cache flushed" }),
          });
          return;
        }
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "not allowed" }),
        });
      });

      await page.goto(explorerSpaUrl(BASE_URL));
      await expect(page.locator('[data-testid="explorer-server-actions"]')).toBeVisible({
        timeout: 20_000,
      });
      const viewMenu = page.locator('[data-testid="action-toolbar-item-View"]');
      await expect(viewMenu).toBeVisible({ timeout: 20_000 });
      const button = page.locator('[data-testid="action-toolbar-item-Flush_Cache"]');
      async function openFlush() {
        await viewMenu.click();
        await expect(button).toBeVisible({ timeout: 10_000 });
      }

      page.once("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const postsBeforeCancel = flushPosts;
      await openFlush();
      await button.click();
      await page.waitForTimeout(400);
      expect(flushPosts, "cancel must not POST flush-cache").toBe(postsBeforeCancel);
      await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveCount(0);

      page.once("dialog", (dialog) => {
        void dialog.accept();
      });
      await openFlush();
      await button.click();
      await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveText(
        /Assembler cache flushed/,
        { timeout: 15_000 },
      );
      expect(flushPosts).toBe(postsBeforeCancel + 1);
      expect(legacy, "legacy flushcache.html must not be requested").toEqual([]);

      page.once("dialog", (dialog) => {
        void dialog.accept();
      });
      await openFlush();
      await button.click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 15_000 });
      await expect(error).toContainText(/not allowed/);
      await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toHaveCount(0);
      await expect(error).not.toContainText(/Assembler cache flushed/);

      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      const noisy = consoleErrors.filter(
        (line) => !/favicon|Download the React DevTools/i.test(line),
      );
      expect(noisy, noisy.join("\n")).toEqual([]);
    },
  );
});
