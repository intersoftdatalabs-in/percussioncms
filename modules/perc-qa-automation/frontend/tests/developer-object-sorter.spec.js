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
 * Developer Object Sorter session organization (#4344 / parent #1690).
 *
 * Admin opens Developer → Object Sorter, sorts the current Content Types
 * catalog, and sees the order stick after reload (sessionStorage only).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 \
 *     TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-object-sorter.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowsSelector } = require("./helpers/developer-catalog-selectors");
const {
  TEST_IDS,
  developerObjectSorterUrl,
  unexpectedConsoleErrors,
} = require("./helpers/developer-object-sorter-surface");

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

test.describe("Developer Object Sorter (#4344 / #1690)", () => {
  test(
    "Admin can sort the current object list and see the order stick for the session",
    { tag: ["@developer", "@object-sorter", "@browse"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await page.goto(developerObjectSorterUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });

      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.tab}"]`)).toBeVisible({
        timeout: 15_000,
      });

      const error = page.locator(`[data-testid="${TEST_IDS.error}"]`);
      const panel = page.locator(`[data-testid="${TEST_IDS.panel}"]`);
      const empty = page.locator(`[data-testid="${TEST_IDS.empty}"]`);

      await expect(panel.or(empty).or(error).first()).toBeVisible({
        timeout: 30_000,
      });

      if (await error.isVisible()) {
        throw new Error(
          `Object Sorter catalog error: ${(await error.innerText()).trim()}`,
        );
      }
      if (await empty.isVisible()) {
        throw new Error(
          "Object Sorter catalog empty — Content Types catalog must list objects on this H2 cell",
        );
      }

      await expect(page.locator(`[data-testid="${TEST_IDS.table}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.sessionNote}"]`)).toBeVisible();
      const rows = page.locator(catalogRowsSelector(TEST_IDS.row));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      const rowCount = await rows.count();
      expect(rowCount, "need at least two content types to prove sort").toBeGreaterThan(1);

      const firstBefore = ((await rows.first().getAttribute("data-os-name")) || "").trim();
      expect(firstBefore.length).toBeGreaterThan(0);

      await page.locator(`[data-testid="${TEST_IDS.mode}"]`).selectOption("name-desc");
      await expect(page.locator(`[data-testid="${TEST_IDS.panel}"]`)).toHaveAttribute(
        "data-os-mode",
        "name-desc",
      );

      const firstAfter = ((await rows.first().getAttribute("data-os-name")) || "").trim();
      expect(firstAfter.length).toBeGreaterThan(0);

      const stored = await page.evaluate(
        (key) => window.sessionStorage.getItem(key),
        "perc.developer.objectSorter.v1",
      );
      expect(stored || "").toContain("name-desc");

      // Re-enter via spa.jsp (plain reload of the client path 404s without the
      // SPA fallback). Same tab keeps sessionStorage.
      await page.goto(developerObjectSorterUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.panel}"]`)).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.panel}"]`)).toHaveAttribute(
        "data-os-mode",
        "name-desc",
      );
      const firstReload = ((await rows.first().getAttribute("data-os-name")) || "").trim();
      expect(firstReload).toBe(firstAfter);

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${unexpectedConsoleErrors(consoleErrors).join(" | ")}`,
      ).toEqual([]);
    },
  );
});
