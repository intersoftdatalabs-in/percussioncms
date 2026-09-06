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
 * Developer Database Explorer catalog browse (#4343 / parent #1690).
 *
 * Admin opens Developer → Database Explorer, lists allow-listed datasources,
 * and drills into tables/views. Read-only — no SQL/DDL. Non-allow-listed
 * catalog ids return 400.
 *
 * Requires `databaseExplorer.allowListedDatasources` on the QA cell (e.g.
 * `cms=repository`). Empty catalog is a setup failure, not a skip.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 \
 *     TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-database-explorer-browse.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const {
  catalogOpenByExactName,
  catalogRowsSelector,
} = require("./helpers/developer-catalog-selectors");
const {
  TEST_IDS,
  SAFE_ID_RE,
  developerDatabaseExplorerUrl,
  developerDatabaseExplorerRestUrl,
  unwrapDatabaseExplorerDatasources,
  unexpectedConsoleErrors,
} = require("./helpers/developer-database-explorer-surface");

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

test.describe("Developer Database Explorer browse (#4343 / #1690)", () => {
  test(
    "REST: GET /services/databaseexplorer returns 2xx Admin catalog",
    { tag: ["@developer", "@database-explorer", "@browse"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = developerDatabaseExplorerRestUrl(BASE_URL);
      const res = await request.get(url, { headers });
      expect(
        res.status(),
        `GET ${url} must be 2xx (Database Explorer REST #4343; was ${res.status()})`,
      ).toBeGreaterThanOrEqual(200);
      expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

      const body = await res.json();
      const datasources = unwrapDatabaseExplorerDatasources(body);
      expect(
        datasources.length,
        "databaseExplorer.allowListedDatasources must list at least one datasource on this H2 cell",
      ).toBeGreaterThan(0);
      const first = datasources[0];
      expect(first.id).toMatch(SAFE_ID_RE);
      const raw = JSON.stringify(body);
      expect(raw.toLowerCase().includes("jdbc:"), "catalog must not echo JDBC URL").toBe(
        false,
      );
      expect(raw.toLowerCase().includes("password"), "catalog must not echo secrets").toBe(
        false,
      );
    },
  );

  test(
    "REST: non-allow-listed datasource id is 400",
    { tag: ["@developer", "@database-explorer", "@browse"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = `${developerDatabaseExplorerRestUrl(BASE_URL)}/not_allow_listed/tables`;
      const res = await request.get(url, { headers });
      expect(res.status(), `GET ${url} must be 400 for non-allow-listed id`).toBe(400);
      const text = await res.text();
      expect(text.toLowerCase().includes("jdbc:")).toBe(false);
      expect(text.toLowerCase().includes("password")).toBe(false);
    },
  );

  test(
    "Admin can browse Database Explorer datasources and tables (read-only)",
    { tag: ["@developer", "@database-explorer", "@browse"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await page.goto(developerDatabaseExplorerUrl(BASE_URL), {
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
          `Database Explorer catalog error: ${(await error.innerText()).trim()}`,
        );
      }
      if (await empty.isVisible()) {
        throw new Error(
          "Database Explorer catalog empty — set databaseExplorer.allowListedDatasources " +
            "(e.g. cms=repository) on the QA cell and restart Jetty",
        );
      }

      await expect(
        page.locator(`[data-testid="${TEST_IDS.datasourcesTable}"]`),
      ).toBeVisible();
      const dsRows = page.locator(catalogRowsSelector(TEST_IDS.datasourceRow));
      await expect(dsRows.first()).toBeVisible({ timeout: 15_000 });

      const openFirst = page.locator(`[data-testid="${TEST_IDS.openDatasource}"]`).first();
      await expect(openFirst).toBeVisible();
      const dsId = ((await openFirst.getAttribute("data-dbx-ds")) || "").trim();
      expect(dsId).toMatch(SAFE_ID_RE);
      const openExact = page.locator(
        catalogOpenByExactName(TEST_IDS.openDatasource, "data-dbx-ds", dsId),
      );

      const escapedDs = dsId.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
      const tablesRe = new RegExp(
        `/services/databaseexplorer/${escapedDs}/tables(?:[/?#]|$)`,
        "i",
      );
      const tablesGet = page.waitForResponse(
        (r) => tablesRe.test(r.url()) && r.request().method() === "GET",
        { timeout: 30_000 },
      );
      await openExact.click();
      const tablesResp = await tablesGet;
      expect(tablesResp.status(), `GET tables status ${tablesResp.status()}`).toBe(200);

      const browse = page.locator(`[data-testid="${TEST_IDS.browse}"]`);
      await expect(browse).toBeVisible({ timeout: 20_000 });
      await expect(browse).toHaveAttribute("data-dbx-ds", dsId);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.backDatasources}"]`),
      ).toBeVisible();

      const tablesError = page.locator(`[data-testid="${TEST_IDS.tablesError}"]`);
      if (await tablesError.isVisible()) {
        throw new Error(
          `Database Explorer tables error: ${(await tablesError.innerText()).trim()}`,
        );
      }

      const tablesEmpty = page.locator(`[data-testid="${TEST_IDS.tablesEmpty}"]`);
      const tablesTable = page.locator(`[data-testid="${TEST_IDS.tablesTable}"]`);
      await expect(tablesTable.or(tablesEmpty).first()).toBeVisible({
        timeout: 20_000,
      });

      await expect(page.locator('[data-testid="developer-dbx-sql"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-dbx-ddl"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-dbx-save"]')).toHaveCount(0);

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${unexpectedConsoleErrors(consoleErrors).join(" | ")}`,
      ).toEqual([]);
    },
  );
});
