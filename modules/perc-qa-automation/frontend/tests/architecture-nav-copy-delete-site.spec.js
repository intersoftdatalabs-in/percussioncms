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
 * Navigation Copy Site + Delete Site chrome (#3303 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-copy-delete-site.spec.js
 *
 * Does not submit a live site copy or delete a demo site. Proves chrome,
 * wizard open/close, and cancel-confirm on Delete Site.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Navigation Copy Site + Delete Site (#3303)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Copy Site wizard and Delete Site confirm chrome @smoke @ui", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const copyBtn = page.getByTestId("architecture-action-copy-site");
    const deleteBtn = page.getByTestId("architecture-action-delete-site");
    await expect(copyBtn).toBeVisible();
    await expect(deleteBtn).toBeVisible();
    await expect(copyBtn).toContainText(/Copy Site/i);
    await expect(deleteBtn).toContainText(/Delete Site/i);

    const picker = page.getByTestId("architecture-site-select");
    const empty = page.getByTestId("architecture-sites-empty");
    await expect(picker.or(empty)).toBeVisible({ timeout: 15_000 });

    if (await empty.isVisible().catch(() => false)) {
      await expect(copyBtn).toBeDisabled();
      await expect(deleteBtn).toBeDisabled();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      return;
    }

    await expect(copyBtn).toBeEnabled();
    await expect(deleteBtn).toBeEnabled();

    await copyBtn.click();
    await expect(page.getByTestId("architecture-copy-site-panel")).toBeVisible();
    await expect(page.getByTestId("site-copy-wizard")).toBeVisible();
    const source = page.getByTestId("site-copy-source");
    await expect(source).toBeVisible();
    const sourceVal = (await source.inputValue()).trim();
    expect(sourceVal.length).toBeGreaterThan(0);
    await page.getByTestId("architecture-copy-site-close").click();
    await expect(page.getByTestId("architecture-copy-site-panel")).toHaveCount(
      0,
    );

    page.once("dialog", (dialog) => {
      expect(dialog.message()).toMatch(/Delete site/i);
      void dialog.dismiss();
    });
    await deleteBtn.click();
    await expect(deleteBtn).toBeVisible();
    await expect(picker).toBeVisible();
    expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  });
});
