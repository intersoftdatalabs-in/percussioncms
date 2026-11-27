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
 * Developer thin-read catalog empty states (#7346 / parent #1690).
 *
 * Content-type / locale / role catalogs expose a named empty state with
 * actionable copy plus a create CTA hook where the panel supports creation;
 * the template catalog (no create action) keeps a neutral named empty
 * message with no dead button. The live cell always seeds these catalogs,
 * so the specs stub the list endpoints to `[]` and assert the UI branch —
 * no server writes happen (CTA clicks only open create chrome).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-catalog-empty-state.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function stubEmptyCatalog(page) {
  return Promise.all([
    page.route("**/services/contenttypes", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      });
    }),
    page.route("**/services/locales", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      });
    }),
    page.route("**/services/roles/catalog*", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ RoleBrowseCatalog: { roles: [] } }),
      });
    }),
    page.route("**/services/templates", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      });
    }),
  ]);
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

async function openDeveloperSection(page, section) {
  await page.goto(developerSectionUrl(section), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer catalog empty states (#7346)", () => {
  test("content types empty state names New and the CTA opens create", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await stubEmptyCatalog(page);
    await openDeveloperSection(page, "content-types");

    const empty = page.locator('[data-testid="developer-ct-empty"]');
    await expect(empty).toBeVisible({ timeout: 30_000 });
    await expect(empty).toContainText(/no content types returned/i);
    const hint = page.locator('[data-testid="developer-ct-empty-hint"]');
    await expect(hint).toBeVisible();
    await expect(hint).toContainText(/use new to create one/i);
    await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible();

    await page.locator('[data-testid="developer-ct-empty-new"]').click();
    await expect(page.locator('[data-testid="developer-ct-create"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.screenshot({
      path: "test-results/live-proof-7346-catalog-empty-state.png",
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("locales empty state names New and the CTA opens create", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await stubEmptyCatalog(page);
    await openDeveloperSection(page, "locales");

    const empty = page.locator('[data-testid="developer-loc-empty"]');
    await expect(empty).toBeVisible({ timeout: 30_000 });
    await expect(empty).toContainText(/no locales returned/i);
    const hint = page.locator('[data-testid="developer-loc-empty-hint"]');
    await expect(hint).toBeVisible();
    await expect(hint).toContainText(/use new to create one/i);
    await expect(page.locator('[data-testid="developer-loc-new"]')).toBeVisible();

    await page.locator('[data-testid="developer-loc-empty-new"]').click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("roles empty state names Create role and the CTA opens create", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await stubEmptyCatalog(page);
    await openDeveloperSection(page, "roles");

    const empty = page.locator('[data-testid="developer-roles-empty"]');
    await expect(empty).toBeVisible({ timeout: 30_000 });
    await expect(empty).toContainText(/no roles returned/i);
    const hint = page.locator('[data-testid="developer-roles-empty-hint"]');
    await expect(hint).toBeVisible();
    await expect(hint).toContainText(/use create role to add one/i);
    await expect(
      page.locator('[data-testid="developer-roles-create"]'),
    ).toBeVisible();

    await page.locator('[data-testid="developer-roles-empty-new"]').click();
    await expect(
      page.locator('[data-testid="developer-roles-create-form"]'),
    ).toBeVisible({ timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("templates empty state stays neutral with no create button", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await stubEmptyCatalog(page);
    await openDeveloperSection(page, "templates");

    const empty = page.locator('[data-testid="developer-tpl-empty"]');
    await expect(empty).toBeVisible({ timeout: 30_000 });
    await expect(empty).toContainText(/no templates returned/i);
    await expect(page.locator('[data-testid="developer-tpl-empty-new"]')).toHaveCount(
      0,
    );
    await expect(page.locator('[data-testid="developer-tpl-new"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
