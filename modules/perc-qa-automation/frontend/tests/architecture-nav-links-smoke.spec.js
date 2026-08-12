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
 * Architecture landing + section-link chrome smoke (#3097 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-links-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture (link / landing actions when sites exist).
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

test.describe("Architecture nav landing & links (#3097)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("landing and link actions are present with tree panel @smoke @ui", async ({
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
    await expect(page.getByTestId("architecture-shell-title")).toContainText(
      /Architecture/i,
    );

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
        page.getByTestId("architecture-action-create-section-link"),
      ).toBeVisible();
      await expect(
        page.getByTestId("architecture-action-create-external-link"),
      ).toBeVisible();
      await expect(page.getByTestId("architecture-action-landing")).toBeVisible();
      await expect(
        page.getByTestId("architecture-action-edit-link"),
      ).toBeVisible();
      await expect(page.getByTestId("architecture-blog-note")).toBeVisible();

      // Open external link dialog (create parent is root when nothing selected)
      const createExt = page.getByTestId(
        "architecture-action-create-external-link",
      );
      if (await createExt.isEnabled().catch(() => false)) {
        await createExt.click();
        await expect(
          page.getByTestId("architecture-external-link-dialog"),
        ).toBeVisible({ timeout: 10_000 });
        await page.getByTestId("architecture-external-link-cancel").click();
        await expect(
          page.getByTestId("architecture-external-link-dialog"),
        ).toHaveCount(0);
      }

      const createLink = page.getByTestId(
        "architecture-action-create-section-link",
      );
      if (await createLink.isEnabled().catch(() => false)) {
        await createLink.click();
        await expect(
          page.getByTestId("architecture-section-link-dialog"),
        ).toBeVisible({ timeout: 10_000 });
        await page.getByTestId("architecture-section-link-cancel").click();
        await expect(
          page.getByTestId("architecture-section-link-dialog"),
        ).toHaveCount(0);
      }
    }

    // Zero uncaught page errors; ignore common network 404 console noise
    // (favicon, optional assets) that is not feature-related.
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
