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
 * Developer shell catalog search/filter (#5995 / parent #1690).
 *
 * Thin read catalogs (communities, slots) expose a client-side filter box:
 * typing narrows catalog rows, a no-match query shows a named empty state,
 * and clearing restores the full server list without a refetch.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-catalog-search-filter.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowsSelector } = require("./helpers/developer-catalog-selectors");

/**
 * @type {{
 *   section: string,
 *   panel: string,
 *   table: string,
 *   rowBase: string,
 *   empty: string,
 *   search: string,
 *   searchEmpty: string,
 *   nameAttr: string,
 * }[]}
 */
const CATALOGS = [
  {
    section: "communities",
    panel: "developer-comm-panel",
    table: "developer-comm-table",
    rowBase: "developer-comm-row",
    empty: "developer-comm-empty",
    search: "developer-comm-search",
    searchEmpty: "developer-comm-search-empty",
    nameAttr: "data-comm-name",
  },
  {
    section: "slots",
    panel: "developer-slot-panel",
    table: "developer-slot-table",
    rowBase: "developer-slot-row",
    empty: "developer-slot-empty",
    search: "developer-slot-search",
    searchEmpty: "developer-slot-search-empty",
    nameAttr: "data-slot-name",
  },
];

function developerUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

test.describe("Developer catalog search/filter (#5995)", () => {
  for (const catalog of CATALOGS) {
    test(`${catalog.section} filter narrows rows, names the empty state, and restores`, async ({
      page,
    }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await page.goto(developerUrl(catalog.section), {
        waitUntil: "networkidle",
      });

      const panel = page.locator(`[data-testid="${catalog.panel}"]`);
      const empty = page.locator(`[data-testid="${catalog.empty}"]`);
      const listError = page.locator(
        `[data-testid="${catalog.panel.replace("-panel", "-error")}"]`,
      );
      await expect(panel.or(empty).first()).toBeVisible({
        timeout: 30_000,
      });
      if (await listError.isVisible()) {
        throw new Error(
          `Developer ${catalog.section} catalog error: ${(
            await listError.innerText()
          ).trim()}`,
        );
      }
      // Empty seed catalog: nothing to filter; filter box stays hidden.
      if (await empty.isVisible()) {
        expect(await page.locator(`[data-testid="${catalog.search}"]`).count()).toBe(0);
        assertConsoleClean(pageErrors, consoleErrors);
        return;
      }

      const table = page.locator(`[data-testid="${catalog.table}"]`);
      await expect(table).toBeVisible();
      const search = page.locator(`[data-testid="${catalog.search}"]`);
      await expect(search).toBeVisible();

      const rows = catalogRowsSelector(catalog.rowBase);
      const before = await page.locator(rows).count();
      expect(before).toBeGreaterThan(0);

      const firstName = await page
        .locator(`[${catalog.nameAttr}]`)
        .first()
        .getAttribute(catalog.nameAttr);
      expect(firstName && firstName.trim().length).toBeGreaterThan(0);
      const needle = firstName.trim().slice(0, 4);

      await search.fill(needle);
      await expect(async () => {
        const count = await page.locator(rows).count();
        expect(count).toBeGreaterThan(0);
        expect(count).toBeLessThanOrEqual(before);
      }).toPass({ timeout: 10_000 });
      // The seed row that supplied the needle is still listed.
      await expect(table).toContainText(firstName.trim());
      await expect(
        page.locator(`[data-testid="${catalog.searchEmpty}"]`),
      ).toBeHidden();

      await search.fill("zzz-no-such-catalog-row");
      await expect(
        page.locator(`[data-testid="${catalog.searchEmpty}"]`),
      ).toBeVisible({ timeout: 10_000 });
      expect(await page.locator(rows).count()).toBe(0);

      await search.fill("");
      await expect(async () => {
        expect(await page.locator(rows).count()).toBe(before);
      }).toPass({ timeout: 10_000 });

      assertConsoleClean(pageErrors, consoleErrors);
    });
  }
});
