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
 * Explorer runs a custom URL saved search (#4929 / parent #4530).
 *
 * {@code npm run test:surface -- --path tests/explorer-custom-url-search.spec.js}
 */

const { test, expect } = require("@playwright/test");
const {
  BASE_URL,
  adminBasicAuthHeaders,
  loginAsAdmin,
} = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  searchesCatalogUrl,
  searchesExecuteUrl,
  postExecuteRegionSelector,
} = require("./helpers/explorer-saved-search");

function uniqueSearchName() {
  const suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
    .replace(/[^a-z0-9]/gi, "")
    .slice(-8);
  return `qa4929${suffix || "x"}`;
}

test.describe("Explorer custom URL saved search (#4929)", () => {
  test(
    "Run saved search on a custom URL search shows rows or an empty list",
    { tag: ["@explorer-custom-url-search", "@explorer"] },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        if (
          /Failed to load resource: the server responded with a status of (404|400|403)/i.test(
            text,
          )
        ) {
          return;
        }
        consoleErrors.push(text);
      });

      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      const name = uniqueSearchName();
      const root = String(BASE_URL || "").replace(/\/$/, "");
      const create = await request.post(`${root}/Rhythmyx/services/searches`, {
        headers,
        data: {
          SearchDef: {
            name,
            label: name,
            type: "CustomSearch",
            customSearch: true,
            url: "../sys_cxViews/inbox.xml",
          },
        },
      });
      const createText = await create.text();
      expect(create.status(), createText).toBe(200);

      const missing = await request.post(searchesExecuteUrl(BASE_URL, "qa4929-missing"), {
        headers,
        data: { SearchExecuteRequest: { startIndex: 1, maxResults: 5 } },
      });
      expect(missing.status(), await missing.text()).toBe(404);

      try {
        await loginAsAdmin(page);
        await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
        await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({ timeout: 30_000 });
        await page.getByTestId(TEST_IDS.menuView).click();
        await page.getByTestId(TEST_IDS.toggleSearch).click();
        await expect(page.getByTestId(TEST_IDS.searchPanel)).toBeVisible({
          timeout: 20_000,
        });
        const select = page.getByTestId(TEST_IDS.savedSelect);
        await expect(select).toBeVisible({ timeout: 20_000 });
        const runBtn = page.getByTestId(TEST_IDS.savedRun);
        await expect(runBtn).toBeDisabled();

        const executeWait = page.waitForResponse(
          (res) =>
            res.request().method() === "POST" &&
            res.url().includes(`/services/searches/${encodeURIComponent(name)}/execute`),
          { timeout: 30_000 },
        );
        await select.selectOption(name);
        await expect(runBtn).toBeEnabled();
        await runBtn.click();
        const executeResponse = await executeWait;
        const status = executeResponse.status();

        await expect(page.locator(postExecuteRegionSelector()).first()).toBeVisible({
          timeout: 30_000,
        });
        const empty = page.getByTestId(TEST_IDS.resultsEmpty);
        const error = page.getByTestId(TEST_IDS.resultsError);
        const list = page.getByTestId(TEST_IDS.resultsList);
        if (status >= 200 && status < 300) {
          await expect(error).toHaveCount(0);
          await expect(list.or(empty)).toHaveCount(1);
          await expect(page.getByTestId(TEST_IDS.searchPanel)).not.toContainText(
            /cannot be run from Explorer/i,
          );
        } else {
          await expect(error).toBeVisible();
          await expect(error).toContainText(new RegExp(`HTTP ${status}`));
          await expect(list).toHaveCount(0);
          await expect(empty).toHaveCount(0);
        }

        const listed = await request.get(searchesCatalogUrl(BASE_URL, { includeViews: true }), {
          headers,
        });
        expect(listed.status()).toBe(200);
      } finally {
        const del = await request.delete(`${root}/Rhythmyx/services/searches/${encodeURIComponent(name)}`, {
          headers,
        });
        expect([200, 204, 404]).toContain(del.status());
      }

      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );
});
