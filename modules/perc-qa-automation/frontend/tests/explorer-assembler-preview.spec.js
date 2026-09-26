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
 * Explorer assembler preview for the selected page (#4943 / parent #4530).
 *
 * <p>Injects a catalog item whose URL is legacy
 * {@code previewslotvariant.html}. A selected page opens
 * {@code /assembler/render} in a new window and keeps the same selection
 * after that window closes. A folder selection names the failure and does
 * not navigate.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-assembler-preview.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { TEST_IDS, explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const ACTION_NAME = "Qa_Assembler_Preview";
const NEEDS_PAGE = "Select a page to open assembler preview";
const UNAVAILABLE = "This action is not available in Content Explorer yet";

function attachPageErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = String(msg.text() || "");
    if (
      /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

async function installAssemblerRoutes(page) {
  const context = page.context();
  const previewAction = {
    id: 9_494_301,
    name: ACTION_NAME,
    label: "Assembler preview",
    sortRank: 1,
    menuType: "MENUITEM",
    url: "../sys_cxSupport/previewslotvariant.html",
  };
  await context.route("**/actions/find**", async (route) => {
    const reqUrl = route.request().url();
    if (/\/actions\/find\/templates\//i.test(reqUrl)) {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ActionMenu: [
            {
              name: "rffPgGeneric",
              label: "Generic",
              menuType: "MENUITEM",
              url: "../assembler/render?sys_template=7",
            },
          ],
        }),
      });
    }
    if (/\/actions\/find\/types/i.test(reqUrl)) {
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
      body = { ...body, ActionMenu: [...body.ActionMenu, previewAction] };
    } else if (Array.isArray(body?.ActionMenuList)) {
      body = {
        ...body,
        ActionMenuList: [...body.ActionMenuList, previewAction],
      };
    } else if (body && typeof body === "object") {
      body = { ...body, ActionMenu: [previewAction] };
    }
    return route.fulfill({
      status: response.status(),
      headers: { ...response.headers(), "content-type": "application/json" },
      body: JSON.stringify(body),
    });
  });
  await context.route("**/assembly/preview-location**", async (route) => {
    const url = new URL(route.request().url());
    const contentId = url.searchParams.get("contentId") || "0";
    const templateId = url.searchParams.get("templateId") || "7";
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        previewUrl: `/assembler/render?sys_contentid=${contentId}&sys_template=${templateId}&sys_context=0&sys_itemfilter=preview`,
        contentId: Number(contentId),
        templateId: Number(templateId),
        revision: 1,
      }),
    });
  });
  await context.route("**/assembler/render**", async (route) => {
    return route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<!DOCTYPE html><html><body>assembler-preview</body></html>",
    });
  });
}

test.describe("Explorer assembler preview (#4943)", () => {
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    pageErrors = attachPageErrors(page);
    await loginAsAdmin(page);
    await installAssemblerRoutes(page);
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "selected page opens assembler preview and close keeps the selection",
    { tag: ["@explorer-assembler-preview", "@explorer"] },
    async ({ page }) => {
      await page.goto(
        `${explorerSpaUrl(BASE_URL)}&path=/Sites/Corporate_Investments/Pages`,
        { waitUntil: "domcontentloaded" },
      );
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      const rows = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
      );
      await expect(rows.first()).toBeVisible({ timeout: 20_000 });
      await rows.first().click();
      const selectedBefore = await shell.getAttribute("data-selected-item-id");
      expect(selectedBefore, "a page or asset row must be selected").toBeTruthy();

      const button = page.locator(
        `[data-testid="action-toolbar-item-${ACTION_NAME}"]`,
      );
      await expect(button).toBeVisible({ timeout: 15_000 });
      const popupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      await button.click();
      const popup = await popupPromise;
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      expect(popup.url()).toContain("/assembler/render");
      await popup.close();
      await expect(shell).toHaveAttribute(
        "data-selected-item-id",
        selectedBefore,
      );
      await expect(page.getByText(UNAVAILABLE)).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );

  test(
    "folder selection names the failure and does not navigate",
    { tag: ["@explorer-assembler-preview", "@explorer"] },
    async ({ page }) => {
      await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
        waitUntil: "domcontentloaded",
      });
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      const folderRow = page
        .locator(
          '[data-testid="detail-list"] tbody tr[data-row-kind="folder"]',
        )
        .first();
      await expect(folderRow).toBeVisible({ timeout: 20_000 });
      await folderRow.click();
      const folderBefore = await shell.getAttribute("data-selected-folder-path");
      const button = page.locator(
        `[data-testid="action-toolbar-item-${ACTION_NAME}"]`,
      );
      await expect(button).toBeVisible({ timeout: 15_000 });
      let popupOpened = false;
      page.once("popup", () => {
        popupOpened = true;
      });
      await button.click();
      await expect(page.getByTestId("explorer-server-actions-error")).toHaveText(
        NEEDS_PAGE,
      );
      await expect(page.getByText(UNAVAILABLE)).toHaveCount(0);
      expect(popupOpened).toBe(false);
      await expect(shell).toHaveAttribute(
        "data-selected-folder-path",
        folderBefore || "",
      );
    },
  );
});
