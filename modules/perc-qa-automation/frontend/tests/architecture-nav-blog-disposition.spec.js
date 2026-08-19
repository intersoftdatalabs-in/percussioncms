/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
 * Architecture blog-navon disposition (#3351 / parent #3092).
 *
 * Signed support: Navigation is read-only for blog type (badge + note).
 * Create/edit blogs stay on the Home Blogs gadget / Home → Create.
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-blog-disposition.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
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

test.describe("Architecture blog navon disposition (#3351)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("signed blog note and no Navigation blog editor @smoke @ui", async ({
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

    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("architecture-shell-title")).toContainText(
      /Navigation/i,
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
      await expect(page.getByTestId("architecture-blog-note")).toBeVisible();
      await expect(page.getByTestId("architecture-blog-note")).toContainText(
        /blog/i,
      );
      await expect(
        page.getByTestId("architecture-action-create-blog"),
      ).toHaveCount(0);

      const blogItem = page.locator('[data-section-type="blog"]').first();
      if (await blogItem.isVisible().catch(() => false)) {
        await blogItem.click();
        await expect(
          page.locator('[data-testid^="nav-tree-badge-"]').filter({
            hasText: /blog/i,
          }),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-landing"),
        ).toBeDisabled();
        await expect(
          page.getByTestId("architecture-action-properties"),
        ).toBeDisabled();
        await expect(
          page.getByTestId("architecture-action-folder-acl"),
        ).toBeDisabled();
        await expect(
          page.getByTestId("architecture-action-rename"),
        ).toBeDisabled();
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
