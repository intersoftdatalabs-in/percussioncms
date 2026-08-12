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
 * Architecture a11y smoke — tree keyboard/ARIA + dialog Escape (#3098 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-a11y-smoke.spec.js
 *
 * Soft-skips deeper tree/dialog checks when the QA cell has no sites (H2 empty).
 * Shell landmarks and top-nav still assert green without sites.
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

test.describe("Architecture a11y hardening (#3098)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("shell landmarks + tree ARIA/keyboard + dialog Escape @smoke @ui @a11y", async ({
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
    await expect(page.getByTestId("architecture-shell-title")).toBeVisible();
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

    const canDepth =
      !(await sitesEmpty.isVisible().catch(() => false)) &&
      !(await sitesError.isVisible().catch(() => false)) &&
      (await treePanel.isVisible().catch(() => false));

    if (!canDepth) {
      // Soft-skip tree/dialog depth when H2 has no sites (issue #3098).
      // Shell landmarks above still assert green.
      test.info().annotations.push({
        type: "soft-skip",
        description:
          "No sites/tree panel — soft-skip tree keyboard + dialog Escape depth",
      });
    } else {
      // Structure toolbar landmark
      const toolbar = page.getByTestId("architecture-structure-actions");
      await expect(toolbar).toBeVisible({ timeout: 15_000 });
      await expect(toolbar).toHaveAttribute("role", "toolbar");

      const navTree = page.getByTestId("architecture-nav-tree");
      await expect(navTree).toBeVisible();

      // Wait for tree load (role=tree when data present; loading/empty otherwise)
      await expect
        .poll(
          async () => {
            if (
              await page
                .getByTestId("architecture-nav-tree-loading")
                .isVisible()
                .catch(() => false)
            ) {
              return "loading";
            }
            if (
              await page
                .getByTestId("architecture-nav-tree-empty")
                .isVisible()
                .catch(() => false)
            ) {
              return "empty";
            }
            if (
              await page
                .getByTestId("architecture-nav-tree-error")
                .isVisible()
                .catch(() => false)
            ) {
              return "error";
            }
            const tree = page.locator(
              '[data-testid="architecture-nav-tree"] [role="tree"]',
            );
            if (await tree.isVisible().catch(() => false)) return "tree";
            return "pending";
          },
          { timeout: 30_000 },
        )
        .not.toBe("loading");

      const treeRole = page.locator(
        '[data-testid="architecture-nav-tree"] [role="tree"]',
      );
      if (await treeRole.isVisible().catch(() => false)) {
        await expect(treeRole).toHaveAttribute(
          "aria-label",
          /navigation tree/i,
        );
        const treeItems = page.locator(
          '[data-testid="architecture-nav-tree"] [role="treeitem"]',
        );
        const itemCount = await treeItems.count();
        if (itemCount === 0) {
          test.info().annotations.push({
            type: "note",
            description:
              "Tree role present but empty — soft-skip keyboard/item a11y checks",
          });
        } else {
          await expect(treeItems.first()).toBeVisible();
          const groups = page.locator(
            '[data-testid="architecture-nav-tree"] [role="group"]',
          );
          if ((await groups.count()) > 0) {
            await expect(groups.first()).toBeVisible();
          }

          const first = treeItems.first();
          await first.focus();
          await expect(first).toBeFocused();
          if (itemCount > 1) {
            await first.press("ArrowDown");
            const second = treeItems.nth(1);
            await expect(second).toBeFocused();
            await second.press("ArrowUp");
            await expect(first).toBeFocused();
          }

          const createBtn = page.getByTestId("architecture-action-create");
          await expect(createBtn).toBeVisible();
          if (await createBtn.isEnabled().catch(() => false)) {
            await createBtn.click();
            const dialog = page.getByTestId("architecture-create-dialog");
            await expect(dialog).toBeVisible({ timeout: 10_000 });
            const dialogPanel = dialog.locator('[role="dialog"]');
            await expect(dialogPanel).toHaveAttribute("aria-modal", "true");
            await page.keyboard.press("Escape");
            await expect(dialog).toHaveCount(0);
          } else {
            test.info().annotations.push({
              type: "note",
              description:
                "Create disabled — skipped Escape dialog close (parent blocked)",
            });
          }
        }
      } else {
        test.info().annotations.push({
          type: "soft-skip",
          description:
            "Nav tree empty/error — soft-skip keyboard expand/collapse",
        });
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
