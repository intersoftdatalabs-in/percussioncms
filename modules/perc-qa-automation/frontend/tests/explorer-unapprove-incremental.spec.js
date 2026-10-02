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
 * Playwright: #5057 / parent #4530 — Explorer unapprove on the incremental queue.
 *
 * Tags: @explorer-unapprove-incremental @explorer
 *
 * npm run test:surface -- --path tests/explorer-unapprove-incremental.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const TAGS = ["@explorer-unapprove-incremental", "@explorer"];

function explorerUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer`;
}

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (/favicon|404|409 \(Conflict\)/.test(text)) {
        return;
      }
      jsErrors.push(text);
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

test.describe("Explorer unapprove selected item on the incremental queue", () => {
  test("cancel, folders, and HTTP 409 do not claim unapproved", async ({ page }) => {
    const calls = [];
    await page.route("**/sitemanage/publish/incremental/explorer/**/approve", async (route) => {
      await route.fulfill({ status: 204, body: "" });
    });
    await page.route("**/sitemanage/publish/incremental/explorer/**/unapprove", async (route) => {
      calls.push(route.request().method());
      await route.fulfill({ status: 204, body: "" });
    });
    const jsErrors = await openExplorer(page);
    const approve = page.locator('[data-testid="action-toolbar-item-approve_incremental"]');
    const unapprove = page.locator('[data-testid="action-toolbar-item-unapprove_incremental"]');
    await expect(unapprove).toBeVisible({ timeout: 20_000 });

    await unapprove.click();
    await expect(page.locator('[data-testid="explorer-server-actions-error"]')).toContainText(
      /select a page or asset/i,
    );
    expect(calls).toEqual([]);

    const sites = page.locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
    );
    await expect(sites.first()).toBeVisible({ timeout: 30_000 });
    await sites.first().click({ force: true });
    const folderRow = page
      .locator(
        '[data-testid="detail-list"] tbody tr[data-row-kind="folder"]:not([aria-disabled="true"])',
      )
      .first();
    await expect(folderRow).toBeVisible({ timeout: 20_000 });
    await folderRow.locator("td").nth(2).click({ force: true });
    await unapprove.click();
    await expect(page.locator('[data-testid="explorer-server-actions-error"]')).toContainText(
      /folders are not unapproved/i,
    );
    expect(calls).toEqual([]);

    const list = page.locator('[data-testid="detail-list"]');
    let itemRow = list.locator('tbody tr[data-row-kind="item"]').first();
    for (let depth = 0; depth < 6 && (await itemRow.count()) === 0; depth += 1) {
      const icon = list.locator('[data-testid^="detail-folder-icon-"]').first();
      await expect(icon).toBeVisible({ timeout: 15_000 });
      await icon.click({ force: true });
      await expect(list).toBeVisible();
      itemRow = list.locator('tbody tr[data-row-kind="item"]').first();
    }
    await expect(itemRow).toBeVisible({ timeout: 20_000 });
    await itemRow.click({ force: true });
    page.once("dialog", (dialog) => dialog.accept());
    await approve.click();
    await expect(page.locator('[data-incremental-approved="true"]')).toHaveCount(1);

    page.once("dialog", (dialog) => dialog.dismiss());
    await unapprove.click();
    await expect(page.locator('[data-incremental-approved="true"]')).toHaveCount(1);
    expect(calls).toEqual([]);

    await page.unroute("**/sitemanage/publish/incremental/explorer/**/unapprove");
    await page.route("**/sitemanage/publish/incremental/explorer/**/unapprove", async (route) => {
      calls.push("409");
      await route.fulfill({ status: 409, contentType: "text/plain", body: "blocked" });
    });
    page.once("dialog", (dialog) => dialog.accept());
    await unapprove.click();
    await expect(page.locator('[data-testid="explorer-server-actions-error"]')).toContainText(
      /409/,
    );
    await expect(page.locator('[data-incremental-approved="true"]')).toHaveCount(1);

    calls.length = 0;
    await page.unroute("**/sitemanage/publish/incremental/explorer/**/unapprove");
    await page.route("**/sitemanage/publish/incremental/explorer/**/unapprove", async (route) => {
      calls.push("204");
      await route.fulfill({ status: 204, body: "" });
    });
    page.once("dialog", (dialog) => dialog.accept());
    await unapprove.click();
    await expect(page.locator('[data-incremental-approved="true"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="explorer-flush-cache-status"]')).toContainText(
      /unapproved on the incremental queue/i,
    );
    expect(calls).toEqual(["204"]);
    expect(jsErrors).toEqual([]);
  });
});

void TAGS;
