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
 * Design SPA create template without Widget XML (#3305 / parent #2631).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/design-template-create.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=design&section=templates
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function designTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "design",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Design template create (#3305)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("creates a modern template and refreshes the list @smoke @ui", async ({
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

    const name = `qa.create.tpl.${Date.now()}`;

    await page.goto(designTemplatesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="perc-design-shell"]')).toBeVisible({
      timeout: 20_000,
    });

    const error = page.locator('[data-testid="design-tpl-error"]');
    const createBtn = page.locator('[data-testid="design-tpl-create"]');
    await expect(createBtn).toBeVisible({ timeout: 30_000 });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    await createBtn.click();
    await expect(
      page.locator('[data-testid="design-tpl-create-dialog"]'),
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator('[data-testid="design-tpl-create-dialog"]'),
    ).toHaveAttribute("aria-describedby", "design-tpl-create-hint");
    await page.locator('[data-testid="design-tpl-create-name"]').fill(name);
    await page.locator('[data-testid="design-tpl-create-submit"]').click();

    await expect(
      page.locator('[data-testid="design-tpl-create-dialog"]'),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(page.locator('[data-testid="design-tpl-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="design-tpl-table"]')).toContainText(
      name,
    );

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
