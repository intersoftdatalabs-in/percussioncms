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
 * Architecture convert-to-folder + create-from-folder chrome smoke
 * (#3302 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-folder-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Architecture convert / create-from-folder (#3302)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("convert and create-from-folder actions are present @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("nav-architecture")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const treePanel = page.getByTestId("architecture-tree-panel");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty";
          if (await sitesError.isVisible().catch(() => false)) return "error";
          if (await treePanel.isVisible().catch(() => false)) return "tree";
          return "wait";
        },
        { timeout: 45_000 },
      )
      .not.toBe("wait");

    if (await treePanel.isVisible().catch(() => false)) {
      await expect(
        page.getByTestId("architecture-structure-actions"),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.getByTestId("architecture-action-create-from-folder"),
      ).toBeVisible();
      await expect(
        page.getByTestId("architecture-action-convert-to-folder"),
      ).toBeVisible();

      const createFromFolder = page.getByTestId(
        "architecture-action-create-from-folder",
      );
      if (await createFromFolder.isEnabled().catch(() => false)) {
        await createFromFolder.click();
        await expect(
          page.getByTestId("architecture-from-folder-dialog"),
        ).toBeVisible({ timeout: 10_000 });
        await expect(
          page.getByTestId("architecture-from-folder-browse-folder"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-from-folder-browse-page"),
        ).toBeVisible();
        await page.getByTestId("architecture-from-folder-cancel").click();
        await expect(
          page.getByTestId("architecture-from-folder-dialog"),
        ).toHaveCount(0);
      }
    }

    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            String(e),
          ),
      ),
    ).toEqual([]);
  });
});
