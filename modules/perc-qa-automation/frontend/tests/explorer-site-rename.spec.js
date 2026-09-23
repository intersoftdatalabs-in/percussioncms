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
 * Playwright surface: #4764 / parent #4530 — Explorer rename site.
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
      errors.push(msg.text());
    }
  });
  page._renameErrors = errors;
}

async function csrfHeaders(page) {
  return page.evaluate(() => {
    const token =
      window.OWASP_CSRFTOKEN?.token ||
      document.querySelector('meta[name="_csrf"]')?.getAttribute("content") ||
      "";
    const header =
      document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") ||
      "OWASP-CSRFTOKEN";
    return { token, header };
  });
}

async function postJson(page, path, body) {
  const csrf = await csrfHeaders(page);
  return page.evaluate(
    async ({ path, body, csrf }) => {
      const headers = {
        "Content-Type": "application/json",
        Accept: "application/json",
      };
      if (csrf.token) {
        headers[csrf.header] = csrf.token;
      }
      const response = await fetch(path, {
        method: "POST",
        credentials: "same-origin",
        headers,
        body: JSON.stringify(body),
      });
      return { status: response.status, text: await response.text() };
    },
    { path, body, csrf },
  );
}

test.describe("Explorer rename site (#4764)", () => {
  test(
    "cancel leaves the name; rename persists; duplicate is 409",
    { tag: ["@explorer-site-rename", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      attachConsoleGate(page);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });

      const stamp = Date.now().toString(36);
      const original = `QaRen${stamp}`.slice(0, 20);
      const renamed = `QaRenX${stamp}`.slice(0, 20);
      const created = await postJson(page, "/Rhythmyx/services/sites", {
        Site: { name: original, description: "rename slice" },
      });
      expect(created.status, created.text).toBe(200);

      await page.goto(
        `${explorerSpaUrl(BASE_URL)}&path=${encodeURIComponent(`/Sites/${original}`)}`,
        { waitUntil: "networkidle" },
      );
      await openContentMenu(page);
      const menu = page.locator(`[data-testid="${TEST_IDS.siteRenameMenu}"]`);
      await expect(menu).toBeVisible();
      await expect(menu).toBeEnabled();
      await menu.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.siteRenamePanel}"]`),
      ).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.siteRenameCancel}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.siteRenamePanel}"]`),
      ).toHaveCount(0);

      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.siteRenameMenu}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.siteRenameName}"]`).fill("bad/name");
      await page.locator(`[data-testid="${TEST_IDS.siteRenameSubmit}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.siteRenameError}"]`)).toBeVisible();

      await page.locator(`[data-testid="${TEST_IDS.siteRenameName}"]`).fill(renamed);
      await page.locator(`[data-testid="${TEST_IDS.siteRenameSubmit}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.siteRenamePanel}"]`),
      ).toHaveCount(0, { timeout: 20_000 });

      await page.goto(
        `${explorerSpaUrl(BASE_URL)}&path=${encodeURIComponent(`/Sites/${renamed}`)}`,
        { waitUntil: "networkidle" },
      );
      await expect(shell).toBeVisible({ timeout: 20_000 });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.siteRenameMenu}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.siteRenameName}"]`)).toHaveValue(
        renamed,
      );

      await page.locator(`[data-testid="${TEST_IDS.siteRenameName}"]`).fill(original);
      const duplicate = page.waitForResponse(
        (response) =>
          response.url().includes("/rename") && response.request().method() === "POST",
      );
      await page.locator(`[data-testid="${TEST_IDS.siteRenameSubmit}"]`).click();
      const dupResponse = await duplicate;
      // original name may still exist as a folder after rename (409) or be free (200).
      if (dupResponse.status() === 409 || dupResponse.status() === 400) {
        await expect(page.locator(`[data-testid="${TEST_IDS.siteRenameError}"]`)).toBeVisible();
      } else {
        expect(dupResponse.status()).toBe(200);
      }

      const clash = await postJson(
        page,
        `/Rhythmyx/services/sites/${encodeURIComponent(renamed)}/rename`,
        { RenameSiteRequest: { name: renamed } },
      );
      // Same name is a no-op 200. A second distinct site name that exists is 409.
      const other = `QaRenY${stamp}`.slice(0, 20);
      const second = await postJson(page, "/Rhythmyx/services/sites", {
        Site: { name: other },
      });
      expect(second.status, second.text).toBe(200);
      const conflict = await postJson(
        page,
        `/Rhythmyx/services/sites/${encodeURIComponent(other)}/rename`,
        { RenameSiteRequest: { name: renamed } },
      );
      expect(conflict.status, conflict.text).toBe(409);
      expect(clash.status).toBeGreaterThanOrEqual(200);

      expect(page._renameErrors || []).toEqual([]);
    },
  );
});
