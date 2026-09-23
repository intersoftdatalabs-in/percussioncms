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
 * Playwright surface: #4764 / #4784 / parent #4530 — Explorer rename site.
 * Successful rename must be HTTP 200 (publish-server CHAR(1) flag) and stay renamed.
 * Invalid names keep the panel open with a visible error.
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-site-rename.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
} = require("./helpers/explorer-site-rename");

function attachConsoleGate(page) {
  const errors = [];
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!/favicon|Failed to load resource/i.test(text)) {
        errors.push(text);
      }
    }
  });
  page._renameErrors = errors;
}

async function createTraditionalSite(page, name) {
  await openContentMenu(page);
  await page.getByTestId("explorer-content-create-site").click();
  await expect(page.getByTestId("site-create-wizard")).toBeVisible({
    timeout: 15_000,
  });
  await page.getByTestId("site-create-type-traditional").check();
  await page.getByTestId("site-create-next").click();
  await page.getByTestId("site-create-name").fill(name);
  await page.getByTestId("site-create-next").click();
  await expect(page.getByTestId("site-create-step-confirm")).toBeVisible({
    timeout: 15_000,
  });
  await page.getByTestId("site-create-next").click();
  const run = page.getByTestId("site-create-run");
  await expect(run).toBeEnabled({ timeout: 15_000 });
  await run.click();
  await expect(page.getByTestId("explorer-site-create-panel")).toHaveCount(0, {
    timeout: 90_000,
  });
}

async function openRename(page) {
  await openContentMenu(page);
  const menu = page.getByTestId("explorer-content-site-rename");
  await expect(menu).toBeEnabled({ timeout: 10_000 });
  await menu.click();
  await expect(page.getByTestId("explorer-site-rename-panel")).toBeVisible();
}

test.describe("Explorer rename site (#4764)", () => {
  test(
    "cancel leaves the name; rename persists; duplicate is 409",
    { tag: ["@explorer-site-rename", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      attachConsoleGate(page);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
      await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
        timeout: 20_000,
      });

      const stamp = Date.now().toString(36);
      const original = `QaRen${stamp}`.replace(/[^A-Za-z0-9]/g, "").slice(0, 18);
      const renamed = `QaRenX${stamp}`.replace(/[^A-Za-z0-9]/g, "").slice(0, 18);
      const other = `QaRenY${stamp}`.replace(/[^A-Za-z0-9]/g, "").slice(0, 18);

      await createTraditionalSite(page, original);

      await openRename(page);
      await page.getByTestId("site-rename-cancel").click();
      await expect(page.getByTestId("explorer-site-rename-panel")).toHaveCount(0);

      await openRename(page);
      await page.getByTestId("site-rename-name").fill("bad/name");
      await page.getByTestId("site-rename-submit").click();
      await expect(page.getByTestId("site-rename-error")).toBeVisible();

      await page.getByTestId("site-rename-name").fill(renamed);
      const renamedResponse = page.waitForResponse(
        (response) =>
          response.url().includes("/rename") && response.request().method() === "POST",
      );
      await page.getByTestId("site-rename-submit").click();
      expect((await renamedResponse).status()).toBe(200);
      await expect(page.getByTestId("site-rename-error")).toHaveCount(0);
      await expect(page.getByTestId("explorer-site-rename-panel")).toHaveCount(0, {
        timeout: 30_000,
      });
      await page.reload({ waitUntil: "networkidle" });
      await openRename(page);
      await expect(page.getByTestId("site-rename-name")).toHaveValue(renamed);
      await page.getByTestId("site-rename-cancel").click();

      await createTraditionalSite(page, other);
      await openRename(page);
      await expect(page.getByTestId("site-rename-name")).toHaveValue(other);
      await page.getByTestId("site-rename-name").fill(renamed);
      const conflict = page.waitForResponse(
        (response) =>
          response.url().includes("/rename") && response.request().method() === "POST",
      );
      await page.getByTestId("site-rename-submit").click();
      expect((await conflict).status()).toBe(409);
      await expect(page.getByTestId("site-rename-error")).toContainText(/already uses/i);
      await expect(page.getByTestId("site-rename-name")).toHaveValue(renamed);

      expect(page._renameErrors || []).toEqual([]);
    },
  );
});
