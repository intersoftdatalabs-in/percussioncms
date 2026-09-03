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
 * Developer SE-03 Roles browse catalog (#4243 / #1690).
 *
 * Opens Developer → Roles and asserts the grouped catalog (community /
 * workflow / unassigned) loads without panel error. Read-only browse only.
 *
 * Entry: spa.jsp?entry=developer&section=roles
 * Refs #4243, #4242, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerRolesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "roles",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer SE-03 roles browse catalog (#4243)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Admin sees Roles catalog grouped by community/workflow/unassigned", async ({
    page,
  }) => {
    const consoleErrors = [];
    const pageErrors = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });

    await page.goto(developerRolesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    // DeveloperShell: data-testid={`tab-developer-${section}`} with section "roles"
    // in DEVELOPER_SECTIONS (allowlists.ts) → tab-developer-roles.
    await expect(page.locator('[data-testid="tab-developer-roles"]')).toBeVisible({
      timeout: 15_000,
    });

    const error = page.locator('[data-testid="developer-roles-error"]');
    const panel = page.locator('[data-testid="developer-roles-panel"]');
    const empty = page.locator('[data-testid="developer-roles-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Roles browse catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No roles in CMS — cannot exercise SE-03 groups");
      return;
    }

    await expect(page.locator('[data-testid="developer-roles-groups"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-roles-group-community"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-roles-group-workflow"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-roles-group-unassigned"]'),
    ).toBeVisible();

    await page.locator('[data-testid="developer-roles-filter-community"]').click();
    await expect(
      page.locator('[data-testid="developer-roles-group-community"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-roles-group-workflow"]'),
    ).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-filter-all"]').click();
    await expect(
      page.locator('[data-testid="developer-roles-group-unassigned"]'),
    ).toBeVisible();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});
