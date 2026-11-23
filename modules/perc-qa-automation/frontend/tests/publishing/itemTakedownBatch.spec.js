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
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * PublishingShell site-workspace takedown, multi-item batch (#6930, parent #4531).
 *
 * <p>Covers the batch flow the single-item spec does not: review lists
 * linked pages per item, the run records one report row per item, and a
 * failed item retries without re-running the items that already succeeded.
 * The batch confirm, report, and retry live in
 * {@code ItemTakedownPanel}; this spec proves the wiring through the
 * workspace with stubbed REST.</p>
 *
 * <p>Tags: {@code @publishing} {@code @publishing-takedown}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing/itemTakedownBatch.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function publishSitesUrl(siteId) {
  const root = String(BASE_URL || "").replace(/\/$/, "");
  const params = new URLSearchParams({
    entry: "publish",
    section: "sites",
    _: String(Date.now()),
  });
  if (siteId) {
    params.set("siteId", siteId);
  }
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function linkedBodyFor(id, linkedById) {
  const rows = linkedById[id] || [];
  return { ArrayList: rows };
}

async function stubPublishSiteApis(page, opts = {}) {
  const linkedById = opts.linkedById || {};
  const linkedStatusById = opts.linkedStatusById || {};
  const takedownHandler =
    opts.takedownHandler ||
    (async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ status: "Success" }),
      });
    });

  await page.route("**/sitemanage/site**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SiteSummary: [{ name: "Demo", siteId: "9", id: "9" }],
      }),
    });
  });
  await page.route("**/publishmanagement/servers/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });
  await page.route("**/itemmanagement/item/getitemdates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemDates: {
          itemId: "42",
          startDate: "",
          endDate: "",
          comments: "",
        },
      }),
    });
  });
  await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
    const url = route.request().url();
    const id = Object.keys(linkedById).find((candidate) =>
      url.includes(`/findLinkedItems/${candidate}`),
    );
    const status =
      (id != null && linkedStatusById[id]) || 200;
    await route.fulfill({
      status,
      contentType: "application/json",
      body:
        status === 200
          ? JSON.stringify(linkedBodyFor(id, linkedById))
          : JSON.stringify({ message: "lookup failed" }),
    });
  });
  await page.route("**/sitemanage/publish/takedown/**", takedownHandler);
}

async function openWorkspace(page, siteId) {
  await page.goto(publishSitesUrl(siteId));
  await expect(page.locator('[data-testid="publishing-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="publish-site-workspace"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="item-takedown"]')).toBeVisible();
}

async function reviewBatch(page, ids) {
  await page.locator('[data-testid="item-takedown-id"]').fill(ids);
  await page.locator('[data-testid="item-takedown-review"]').click();
  await expect(page.locator('[data-testid="item-takedown-confirm"]')).toBeVisible({
    timeout: 10_000,
  });
}

test.describe("PublishingShell site-workspace batch takedown", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "batch review confirms per-item linked pages and reports one row per item",
    { tag: ["@publishing", "@publishing-takedown"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        linkedById: {
          42: [{ pagePath: "/Sites/Demo/Home", id: "7" }],
          43: [],
        },
      });

      await openWorkspace(page, "9");
      await reviewBatch(page, "42,43");

      await expect(
        page.locator('[data-testid="item-takedown-batch-item-42"]'),
      ).toContainText("42");
      await expect(
        page.locator('[data-testid="item-takedown-batch-linked-42"]'),
      ).toContainText("/Sites/Demo/Home");
      await expect(
        page.locator('[data-testid="item-takedown-batch-item-43"]'),
      ).toContainText("43");
      await expect(
        page.locator('[data-testid="item-takedown-batch-linked-empty-43"]'),
      ).toBeVisible();

      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-success"]')).toBeVisible({
        timeout: 10_000,
      });

      await expect(
        page.locator('[data-testid="item-takedown-batch-report-item-42"]'),
      ).toContainText("42");
      await expect(
        page.locator('[data-testid="item-takedown-batch-report-item-43"]'),
      ).toContainText("43");
      await expect(page.locator('[data-testid="item-takedown-retry"]')).toHaveCount(0);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "a failed batch item retries without re-running the items that succeeded",
    { tag: ["@publishing", "@publishing-takedown"] },
    async ({ page }) => {
      const pageErrors = [];
      const takedownCalls = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        linkedById: { 42: [], 43: [] },
        takedownHandler: async (route) => {
          const url = route.request().url();
          takedownCalls.push(url);
          const fails =
            url.includes("/takedown/page/43") &&
            takedownCalls.filter((seen) => seen.includes("/takedown/page/43")).length === 1;
          await route.fulfill({
            status: fails ? 500 : 200,
            contentType: "application/json",
            body: fails
              ? JSON.stringify({ message: "takedown failed" })
              : JSON.stringify({ status: "Success" }),
          });
        },
      });

      await openWorkspace(page, "9");
      await reviewBatch(page, "42,43");

      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="item-takedown-success"]')).toHaveCount(0);
      await expect(
        page.locator('[data-testid="item-takedown-batch-report-item-42"]'),
      ).toContainText("42");
      await expect(
        page.locator('[data-testid="item-takedown-batch-report-item-43"]'),
      ).toContainText("43");

      const callsBeforeRetry = takedownCalls.filter((seen) =>
        seen.includes("/takedown/page/42"),
      ).length;
      await page.locator('[data-testid="item-takedown-retry"]').click();
      await expect(page.locator('[data-testid="item-takedown-success"]')).toBeVisible({
        timeout: 10_000,
      });
      const callsAfterRetry = takedownCalls.filter((seen) =>
        seen.includes("/takedown/page/42"),
      ).length;
      expect(callsAfterRetry, "retry re-ran an item that already succeeded").toEqual(
        callsBeforeRetry,
      );
      await expect(
        page.locator('[data-testid="item-takedown-batch-report-item-43"]'),
      ).toContainText("43");

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "a failed linked-page lookup still confirms with an empty list and retries",
    { tag: ["@publishing", "@publishing-takedown"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err));
      });
      await stubPublishSiteApis(page, {
        linkedById: { 42: [], 43: [] },
        linkedStatusById: { 43: 500 },
      });

      await openWorkspace(page, "9");
      await reviewBatch(page, "42,43");

      await expect(page.locator('[data-testid="item-takedown-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(
        page.locator('[data-testid="item-takedown-batch-item-43"]'),
      ).toContainText("43");
      await expect(
        page.locator('[data-testid="item-takedown-batch-linked-empty-43"]'),
      ).toBeVisible();

      await page.unroute("**/itemmanagement/item/findLinkedItems/**");
      await page.route("**/itemmanagement/item/findLinkedItems/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ArrayList: [] }),
        });
      });
      await page.locator('[data-testid="item-takedown-retry"]').click();
      await expect(page.locator('[data-testid="item-takedown-error"]')).toHaveCount(0);

      await page.locator('[data-testid="item-takedown-submit"]').click();
      await expect(page.locator('[data-testid="item-takedown-success"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
