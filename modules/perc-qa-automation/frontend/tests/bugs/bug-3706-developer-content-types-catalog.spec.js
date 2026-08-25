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
 * Developer → Content Types catalog must load so Object ACL can be opened (#3706).
 *
 * Jackson WRAP_ROOT_VALUE on GET /services/contenttypes must not throw into
 * DeveloperSectionErrorBoundary ("Unable to load Content Types…").
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/bugs/bug-3706-developer-content-types-catalog.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const {
  catalogRowSelector,
} = require("../helpers/developer-catalog-selectors");

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer Content Types catalog (#3706)", () => {
  test("catalog table loads and first row can open Object ACL", async ({
    page,
  }) => {
    test.setTimeout(120_000);
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

    await loginAsAdmin(page);
    await page.goto(developerSectionUrl("content-types"), {
      waitUntil: "networkidle",
    });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-content-types"]'),
    ).toBeVisible({ timeout: 15_000 });

    const sectionCrash = page.locator('[data-testid="developer-section-error"]');
    await expect(sectionCrash).toHaveCount(0);

    const error = page.locator('[data-testid="developer-ct-error"]');
    const panel = page.locator('[data-testid="developer-ct-panel"]');
    const empty = page.locator('[data-testid="developer-ct-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    expect(
      await sectionCrash.count(),
      "Content Types catalog must not hit DeveloperSectionErrorBoundary",
    ).toBe(0);
    expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Content types catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No content types in CMS — cannot open Object ACL");
      return;
    }

    const table = page.locator('[data-testid="developer-ct-table"]');
    await expect(table).toBeVisible({ timeout: 15_000 });

    const firstRow = page.locator(catalogRowSelector("developer-ct-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(
      openBtn,
      "first content-type row should expose Open when selectionKey is set",
    ).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-ct-acl-section"]'),
    ).toBeVisible({ timeout: 15_000 });

    const relatedConsole = consoleErrors.filter(
      (t) =>
        /content type|contenttypes|Unable to load Content Types/i.test(t) &&
        !/favicon|Download the React DevTools/i.test(t),
    );
    expect(
      relatedConsole,
      `console errors on Content Types path: ${relatedConsole.join(" | ")}`,
    ).toEqual([]);
    expect(pageErrors, `uncaught pageerror after open: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});
