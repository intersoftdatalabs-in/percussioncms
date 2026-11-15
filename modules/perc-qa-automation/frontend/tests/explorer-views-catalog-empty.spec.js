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
 * Explorer Views catalog loading + group-empty states (#6418 / parent #4530).
 *
 * <p>The catalog tree already covers load error + retry and the always-present
 * Inbox leaf (#3240). This surface proves the two remaining chrome states on
 * H2 with a stubbed catalog: the transient loading indicator and the
 * per-group empty row ({@code VIEWS_GROUP_EMPTY}) for an expanded group with
 * zero leaves.</p>
 *
 *   npm run test:surface -- --path tests/explorer-views-catalog-empty.spec.js
 */

"use strict";

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
} = require("./helpers/explorer-views-catalog");

const VIEWS_URL_PATTERN = "**/services/views";

function inboxOnlyCatalog() {
  return JSON.stringify([
    { name: "Inbox", label: "Inbox", customView: true, parentCategory: 1 },
  ]);
}

function collectConsoleErrors(page, sink) {
  page.on("pageerror", (err) => {
    sink.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") return;
    const text = msg.text();
    // Ignore fixture/network noise (missing static assets / 400 probes).
    if (
      /Failed to load resource: the server responded with a status of (404|400)/i.test(
        text,
      )
    ) {
      return;
    }
    sink.push(text);
  });
}

function expectCleanConsole(sink) {
  expect(
    sink.filter(
      (e) =>
        !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
          e,
        ),
    ),
  ).toEqual([]);
}

test.describe("Explorer Views catalog loading + group empty (#6418)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("delayed catalog shows loading, then group empty rows @views @explorer-views @explorer @smoke", async ({
    page,
  }) => {
    const consoleErrors = [];
    collectConsoleErrors(page, consoleErrors);

    await page.route(VIEWS_URL_PATTERN, async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      // Hold the catalog so the transient loading indicator is observable.
      await new Promise((resolve) => setTimeout(resolve, 1200));
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: inboxOnlyCatalog(),
      });
    });

    await page.goto(explorerEntryUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 20_000,
    });

    // Loading chrome is visible while the catalog fetch is held.
    await expect(page.getByTestId("explorer-views-loading")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("explorer-views-loading")).toContainText(
      /loading views/i,
    );

    // Catalog resolves: four groups, Inbox under My Content. The tree chrome
    // mounts regardless of the stubbed catalog payload, so a missing tree is
    // a hard product-route failure (#6418 dead-skip fix: no soft-skip here).
    await expect(page.getByTestId(TEST_IDS.group(2))).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("explorer-views-loading")).toHaveCount(0);
    await expect(page.getByTestId(TEST_IDS.inboxLeaf)).toBeVisible();

    // Community group has zero leaves: expanding shows the group empty row.
    await page.getByTestId(TEST_IDS.groupRow(2)).click();
    const groupEmpty = page.getByTestId("explorer-views-group-2-empty");
    await expect(groupEmpty).toBeVisible({ timeout: 20_000 });
    await expect(groupEmpty).toHaveAttribute("role", "status");
    await expect(groupEmpty).toContainText(/no views in this group/i);

    // My Content keeps the Inbox leaf: no empty row there.
    await expect(page.getByTestId("explorer-views-group-1-empty")).toHaveCount(
      0,
    );
    await expect(page.getByTestId(TEST_IDS.viewsTree)).toBeVisible();

    await page.screenshot({
      path: "test-results/live-proof-6418-views-catalog-empty.png",
    });

    expectCleanConsole(consoleErrors);
  });
});
