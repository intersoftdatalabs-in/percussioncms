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
 * Architecture structure mutations smoke (#3096 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-mutations-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture (structure action bar when sites exist).
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

test.describe("Architecture nav structure mutations (#3096)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("structure action bar is present with tree or empty states @smoke @ui", async ({
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
      /Navigation/i,
    );
    await expect(page.getByTestId("architecture-toolbar")).toBeVisible({
      timeout: 15_000,
    });

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
      await expect(treePanel.or(emptyState)).toBeVisible({ timeout: 15_000 });
      if (await treePanel.isVisible().catch(() => false)) {
        await expect(page.getByTestId("architecture-nav-tree")).toBeVisible();
        await expect(
          page.getByTestId("architecture-structure-actions"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-create"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-rename"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move-up"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move-down"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-delete"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-structure-note"),
        ).toBeVisible();
        // Read-only note must be gone
        await expect(
          page.getByTestId("architecture-readonly-note"),
        ).toHaveCount(0);

        // #3350 / #3155: when a NavTree is present, Create section is enabled
        // (root or selected regular section). Dialog is modal; Escape closes
        // and returns focus to the opener. No forced save against the live site.
        const treeItems = page.locator(
          '[data-testid="architecture-nav-tree"] [role="treeitem"]',
        );
        const itemCount = await treeItems.count();
        const createBtn = page.getByTestId("architecture-action-create");
        const extBtn = page.getByTestId(
          "architecture-action-create-external-link",
        );
        const linkBtn = page.getByTestId(
          "architecture-action-create-section-link",
        );
        if (itemCount > 0) {
          await treeItems.first().click();
          await expect(createBtn).toBeEnabled();
          await expect(extBtn).toBeEnabled();
          await expect(linkBtn).toBeEnabled();
          await createBtn.click();
          const createDialog = page.getByTestId("architecture-create-dialog");
          await expect(createDialog).toBeVisible({ timeout: 10_000 });
          await expect(createDialog.locator('[role="dialog"]')).toHaveAttribute(
            "aria-modal",
            "true",
          );
          await page.keyboard.press("Escape");
          await expect(createDialog).toHaveCount(0);
          await expect(createBtn).toBeFocused();

          await extBtn.click();
          const extDialog = page.getByTestId(
            "architecture-external-link-dialog",
          );
          await expect(extDialog).toBeVisible({ timeout: 10_000 });
          await expect(extDialog).toHaveAttribute("role", "dialog");
          await page.keyboard.press("Escape");
          await expect(extDialog).toHaveCount(0);
          await expect(extBtn).toBeFocused();

          await linkBtn.click();
          const linkDialog = page.getByTestId(
            "architecture-section-link-dialog",
          );
          await expect(linkDialog).toBeVisible({ timeout: 10_000 });
          await expect(linkDialog).toHaveAttribute("role", "dialog");
          await page.keyboard.press("Escape");
          await expect(linkDialog).toHaveCount(0);
          await expect(linkBtn).toBeFocused();

          const renameBtn = page.getByTestId("architecture-action-rename");
          await expect(renameBtn).toBeEnabled();
          await renameBtn.click();
          const renameDialog = page.getByTestId("architecture-rename-dialog");
          await expect(renameDialog).toBeVisible({ timeout: 10_000 });
          await expect(renameDialog.locator('[role="dialog"]')).toHaveAttribute(
            "aria-modal",
            "true",
          );
          await page.keyboard.press("Escape");
          await expect(renameDialog).toHaveCount(0);
          await expect(renameBtn).toBeFocused();
        } else if (await createBtn.isEnabled().catch(() => false)) {
          await createBtn.click();
          await expect(
            page.getByTestId("architecture-create-dialog"),
          ).toBeVisible({ timeout: 10_000 });
          await page.keyboard.press("Escape");
          await expect(
            page.getByTestId("architecture-create-dialog"),
          ).toHaveCount(0);
        } else {
          test.info().annotations.push({
            type: "note",
            description:
              "No treeitems — Create stays disabled (empty NavTree). Dialog Escape covered by Vitest + cells with #3352 seed.",
          });
        }
      }
    }

    // Zero uncaught page errors; ignore common network 404 console noise
    // (favicon, optional assets) that is not feature-related.
    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            e,
          ),
      ),
    ).toEqual([]);
  });
});
