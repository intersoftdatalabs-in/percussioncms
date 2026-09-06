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
 * Developer File Explorer browse (#4327 / parent #1690).
 *
 * Admin opens Developer → File Explorer, lists allow-listed roots, and
 * drills into children. Read-only — no write/upload.
 *
 * Requires `fileExplorer.allowListedRoots` on the QA cell (e.g.
 * `rx_resources=rx_resources`). Empty catalog is a setup failure, not a skip.
 *
 * Consumes REST/SPA tips #4325 / #4326 (PRs #4331 / #4332).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 \
 *     TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-file-explorer-browse.spec.js
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
  developerFileExplorerUrl,
  developerFileExplorerRestUrl,
  unwrapFileExplorerRoots,
  unexpectedConsoleErrors,
} = require("./helpers/developer-file-explorer-surface");

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

test.describe("Developer File Explorer browse (#4327 / #1690)", () => {
  test(
    "REST: GET /services/fileexplorer returns 2xx Admin catalog",
    { tag: ["@developer", "@file-explorer", "@browse"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = developerFileExplorerRestUrl(BASE_URL);
      const res = await request.get(url, { headers });
      expect(
        res.status(),
        `GET ${url} must be 2xx (File Explorer REST #4325; was ${res.status()})`,
      ).toBeGreaterThanOrEqual(200);
      expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

      const body = await res.json();
      const roots = unwrapFileExplorerRoots(body);
      expect(
        roots.length,
        "fileExplorer.allowListedRoots must list at least one root on this H2 cell",
      ).toBeGreaterThan(0);
      const first = roots[0];
      expect(first.id).toMatch(/^[A-Za-z][A-Za-z0-9_-]{0,63}$/);
      const raw = JSON.stringify(body);
      expect(raw.includes(".."), "catalog must not echo parent traversal").toBe(
        false,
      );
    },
  );

  test(
    "Admin can browse File Explorer roots and children (read-only)",
    { tag: ["@developer", "@file-explorer", "@browse"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await page.goto(developerFileExplorerUrl(BASE_URL), {
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
          `File Explorer catalog error: ${(await error.innerText()).trim()}`,
        );
      }
      if (await empty.isVisible()) {
        throw new Error(
          "File Explorer catalog empty — set fileExplorer.allowListedRoots " +
            "(e.g. rx_resources=rx_resources) on the QA cell and restart Jetty",
        );
      }

      await expect(page.locator(`[data-testid="${TEST_IDS.rootsTable}"]`)).toBeVisible();
      const rootRows = page.locator(catalogRowsSelector(TEST_IDS.rootRow));
      await expect(rootRows.first()).toBeVisible({ timeout: 15_000 });

      const openFirst = page.locator(`[data-testid="${TEST_IDS.openRoot}"]`).first();
      await expect(openFirst).toBeVisible();
      const rootId = ((await openFirst.getAttribute("data-fe-root")) || "").trim();
      expect(rootId).toMatch(/^[A-Za-z][A-Za-z0-9_-]{0,63}$/);
      const openExact = page.locator(
        catalogOpenByExactName(TEST_IDS.openRoot, "data-fe-root", rootId),
      );

      const escapedRoot = rootId.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
      const childrenRe = new RegExp(
        `/services/fileexplorer/${escapedRoot}/children(?:[/?#]|$)`,
        "i",
      );
      const childrenGet = page.waitForResponse(
        (r) => childrenRe.test(r.url()) && r.request().method() === "GET",
        { timeout: 30_000 },
      );
      await openExact.click();
      const childrenResp = await childrenGet;
      expect(
        childrenResp.status(),
        `GET children status ${childrenResp.status()}`,
      ).toBe(200);

      const browse = page.locator(`[data-testid="${TEST_IDS.browse}"]`);
      await expect(browse).toBeVisible({ timeout: 20_000 });
      await expect(browse).toHaveAttribute("data-fe-root", rootId);
      await expect(page.locator(`[data-testid="${TEST_IDS.breadcrumb}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.backRoots}"]`)).toBeVisible();

      const childrenError = page.locator(
        `[data-testid="${TEST_IDS.childrenError}"]`,
      );
      if (await childrenError.isVisible()) {
        throw new Error(
          `File Explorer children error: ${(await childrenError.innerText()).trim()}`,
        );
      }

      const childrenEmpty = page.locator(
        `[data-testid="${TEST_IDS.childrenEmpty}"]`,
      );
      const childrenTable = page.locator(
        `[data-testid="${TEST_IDS.childrenTable}"]`,
      );
      await expect(childrenTable.or(childrenEmpty).first()).toBeVisible({
        timeout: 20_000,
      });

      const openDir = page.locator(`[data-testid="${TEST_IDS.openDir}"]`).first();
      if (await openDir.isVisible()) {
        const dirPath = ((await openDir.getAttribute("data-fe-path")) || "").trim();
        expect(dirPath.includes("..")).toBe(false);
        expect(dirPath.includes("\\")).toBe(false);
        await openDir.click();
        await expect(page.locator(`[data-testid="${TEST_IDS.up}"]`)).toBeVisible({
          timeout: 15_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.up}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.up}"]`)).toHaveCount(0, {
          timeout: 15_000,
        });
        await expect(browse).toBeVisible();
        await expect(browse).toHaveAttribute("data-fe-root", rootId);
        await expect(
          page.locator(`[data-testid="${TEST_IDS.rootsTable}"]`),
        ).toHaveCount(0);
      }

      await expect(
        page.locator('[data-testid="developer-fe-upload"]'),
      ).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-fe-save"]')).toHaveCount(
        0,
      );

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${unexpectedConsoleErrors(consoleErrors).join(" | ")}`,
      ).toEqual([]);
    },
  );
});
