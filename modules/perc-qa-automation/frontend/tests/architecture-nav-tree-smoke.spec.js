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
 * Architecture read-only nav tree smoke (#3095 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-tree-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture (site picker + tree when sites exist).
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

test.describe("Architecture read-only nav tree (#3095)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("shell loads site picker and tree or empty/error states @smoke @ui", async ({
    page,
  }) => {
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
      /Navigation/i,
    );

    // Toolbar always present once shell mounts
    await expect(page.getByTestId("architecture-toolbar")).toBeVisible({
      timeout: 15_000,
    });

    // Either no sites, site list error, or picker + tree panel
    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const picker = page.getByTestId("architecture-site-picker");
    const treePanel = page.getByTestId("architecture-tree-panel");
    const emptyState = page.getByTestId("architecture-empty-state");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty-sites";
          if (await sitesError.isVisible().catch(() => false)) return "sites-error";
          if (await picker.isVisible().catch(() => false)) return "picker";
          if (await emptyState.isVisible().catch(() => false)) return "no-site";
          return "pending";
        },
        { timeout: 25_000 },
      )
      .not.toBe("pending");

    if (await picker.isVisible().catch(() => false)) {
      await expect(page.getByTestId("architecture-site-select")).toBeVisible();
      // When a site is auto-selected, tree panel (loading/ready/error) appears
      await expect(treePanel.or(emptyState)).toBeVisible({ timeout: 15_000 });
      if (await treePanel.isVisible().catch(() => false)) {
        await expect(page.getByTestId("architecture-nav-tree")).toBeVisible();
        // Structure note / actions (Slice D) or legacy readonly note if older build
        const structureNote = page.getByTestId("architecture-structure-note");
        const readonlyNote = page.getByTestId("architecture-readonly-note");
        const actions = page.getByTestId("architecture-structure-actions");
        await expect
          .poll(async () => {
            if (await structureNote.isVisible().catch(() => false))
              return "structure";
            if (await actions.isVisible().catch(() => false)) return "actions";
            if (await readonlyNote.isVisible().catch(() => false))
              return "readonly";
            return "pending";
          })
          .not.toBe("pending");
      }
    }

    // Top-nav Architecture remains SPA NavLink
    const href = await page
      .getByTestId("nav-architecture")
      .getAttribute("href");
    expect(href || "").toMatch(/architecture/);
    expect(href || "").not.toMatch(/view=arch/);
  });
});
