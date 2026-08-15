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
 * Preview-first Content Editor host — chrome-less field form.
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-content-editor.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

test.describe("modern React Content Editor — first slice", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "editor entry is chrome-less and does not open CM1 editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          u.includes("sys_ceSupport") ||
          /view=editor/.test(u)
        ) {
          blocked.push(u);
        }
      });

      await page.goto(editorSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="editor-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-overlay"]')).toBeVisible();
      await expect(page.locator('[data-testid="perc-spa-app"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-error"]')).toBeVisible();
      expect(blocked, `CM1 / Data Flow editor must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );

  test(
    "Explorer Edit does not open CM1 ?view=editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        if (/view=editor/.test(req.url()) || req.url().includes("checkoutedit.xml")) {
          blocked.push(req.url());
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const edit = page.locator('[data-testid="action-toolbar-item-Edit"]');
      if (await edit.isVisible()) {
        await edit.click();
      }
      expect(blocked, `CM1 editor must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  test(
    "Explorer New Item does not open leftover Content Editor HTML",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("rx_ce") ||
          u.includes("contenteditorurls.html") ||
          u.includes("checkoutedit.xml")
        ) {
          blocked.push(u);
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const neu = page.locator('[data-testid="action-toolbar-item-New"]');
      if (await neu.isVisible()) {
        await neu.click();
      }
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  async function openPercPageTemplatePicker(page, createBodies) {
    await page.route("**/services/contenttypes/**", async (route) => {
      const url = route.request().url();
      if (!/percPage|\/Page(?:\?|$)/i.test(url)) {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ContentTypeDetail: {
            name: "percPage",
            allowedTemplates: [
              { name: "t1", label: "Home", guid: { stringValue: "tpl-home" } },
              { name: "t2", label: "Interior", guid: { stringValue: "tpl-in" } },
            ],
          },
        }),
      });
    });
    await page.route("**/services/itemmanagement/item/create**", async (route) => {
      createBodies.push(route.request().postData() || "");
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemCreateResult: {
            itemId: "1-101-1",
            folderPath: "//Sites/Demo",
            name: "New-percPage.html",
            contentType: "percPage",
          },
        }),
      });
    });

    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
    await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
      timeout: 20_000,
    });

    const tree = page.locator('[data-testid="explorer-tree"]');
    if ((await tree.count()) > 0) {
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
      }
    }

    const neu = page.locator('[data-testid="action-toolbar-item-New"]');
    if ((await neu.count()) === 0 || !(await neu.isVisible())) {
      return { skipped: "New menu is not visible" };
    }
    await neu.click();
    const percPage = page.locator(
      '[data-testid="action-toolbar-item-percPage"], [data-testid="action-toolbar-item-Page"]',
    );
    if ((await percPage.count()) === 0) {
      return { skipped: "percPage is not listed under New" };
    }
    await percPage.first().click();
    const picker = page.locator('[data-testid="explorer-template-picker"]');
    await expect(picker).toBeVisible({ timeout: 10_000 });
    return { picker };
  }

  test(
    "Explorer New percPage offers a template picker and does not create on cancel",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-template-picker"]',
      });
      await page.locator('[data-testid="explorer-template-picker-cancel"]').click();
      await expect(opened.picker).toHaveCount(0);
      expect(createBodies, "Cancel must not POST create").toEqual([]);
    },
  );

  test(
    "Explorer New percPage posts the picked templateId",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await page.locator('[data-testid="explorer-template-picker-select"]').selectOption("tpl-in");
      await page.locator('[data-testid="explorer-template-picker-ok"]').click();
      await expect.poll(() => createBodies.length).toBe(1);
      expect(createBodies[0]).toMatch(/"templateId"\s*:\s*"tpl-in"/);
    },
  );
});
