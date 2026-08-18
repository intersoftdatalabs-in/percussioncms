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
 * Explorer Views catalog tree smoke (#3116) + Inbox leaf (#3240 / #3561).
 * Full Playwright / gap-matrix Present remains human QA (#3241 / #3117).
 *
 * <p>Do <strong>not</strong> soft-skip when the Views tree or Inbox leaf
 * is on {@code spa.jsp?entry=explorer}. Empty Inbox (HTTP 200) is
 * success. Missing chrome is a hard fail for this product-route
 * surface.</p>
 *
 *   npm run test:surface -- --path tests/explorer-views-catalog.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  shouldSkipViewsCatalogSurface,
  isViewsExecuteUrl,
} = require("./helpers/explorer-views-catalog");

test.describe("Explorer Views catalog tree (#3116)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Explorer left nav shows Views groups My/Community/All/Other @views @explorer-views @explorer @smoke", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      // Ignore fixture/network noise (missing static assets / 400 probes).
      if (/Failed to load resource: the server responded with a status of (404|400)/i.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId(TEST_IDS.nav)).toBeVisible({ timeout: 20_000 });
    const viewsTree = page.getByTestId(TEST_IDS.viewsTree);
    await expect(viewsTree).toBeVisible({
      timeout: 20_000,
    });
    expect(
      shouldSkipViewsCatalogSurface({ treeVisible: true }),
      "Views catalog must not soft-skip when the tree is on the product route (#3561)",
    ).toBe(false);
    await expect(page.getByTestId(TEST_IDS.viewsRoot)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.group(1))).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.group(2))).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.group(3))).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.group(4))).toBeVisible();

    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("Views → My Content shows Inbox leaf and running it is not a no-op @views @explorer-inbox @explorer @smoke", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (/Failed to load resource: the server responded with a status of (404|400)/i.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId(TEST_IDS.group(1))).toBeVisible({
      timeout: 20_000,
    });
    const inboxLeaf = page.getByTestId(TEST_IDS.inboxLeaf);
    await expect(inboxLeaf).toBeVisible();
    expect(
      shouldSkipViewsCatalogSurface({ leafVisible: true, treeVisible: true }),
      "Views catalog must not soft-skip when the Inbox leaf is on the product route (#3561)",
    ).toBe(false);
    await expect(page.getByTestId(TEST_IDS.inboxIcon)).toBeVisible();
    await expect(inboxLeaf).toHaveAttribute(
      "data-cx-path",
      "//Views//MyContent/Inbox",
    );

    const executeBodies = [];
    const executeStatuses = [];
    page.on("request", (req) => {
      if (req.method() !== "POST") return;
      if (!isViewsExecuteUrl(req.url())) return;
      executeBodies.push(req.postData() || "");
    });
    page.on("response", (res) => {
      if (res.request().method() !== "POST") return;
      if (!isViewsExecuteUrl(res.url())) return;
      executeStatuses.push(res.status());
    });

    await inboxLeaf.click();
    await expect(page.getByTestId(TEST_IDS.results)).toBeVisible({
      timeout: 20_000,
    });
    const loading = page.getByTestId(TEST_IDS.resultsLoading);
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }
    expect(executeBodies.length, "Inbox execute should POST").toBeGreaterThan(0);
    expect(
      executeStatuses.some((s) => s === 200),
      `Inbox execute must return 200 (empty list is success) (statuses=${executeStatuses.join(",")})`,
    ).toBe(true);
    for (const raw of executeBodies) {
      const parsed = JSON.parse(raw);
      expect(
        parsed.ViewExecuteRequest,
        "JAXB root ViewExecuteRequest required (#3318 / QA #3244)",
      ).toBeTruthy();
      expect(parsed.startIndex, "bare startIndex must not be the JSON root").toBeUndefined();
    }
    const unsupported = page.getByTestId(TEST_IDS.resultsError);
    if (await unsupported.count()) {
      await expect(unsupported).not.toContainText(
        /Custom URL views cannot be run/i,
      );
    }

    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
