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
 * Playwright surface: #2430 / #2411 slice C — Explorer P-Trans UI.
 *
 * <p>Verifies the modern React Content Explorer shell exposes the Translations
 * panel chrome and loads item locale / variants from the public REST façade
 * ({@code GET /rest/content-explorer/translations/{itemId}}). Create-variant
 * is exercised when a content row is selectable (GUID last-segment ids such as
 * {@code 1-101-708} → {@code 708}, #3545 / parent #2649). The client must not
 * emit {@code Selected item does not have a numeric content id} for a
 * GUID-shaped row. Folders/sites keep the select-item hint. Chrome visibility
 * is the hard gate when no content row is listed.</p>
 *
 * <p>Tags: {@code @explorer-translations} {@code @p-trans} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-translations.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

/**
 * Open the first listed content row (page/asset), drilling one folder when
 * the current list is folders only. GUID-shaped {@code data-testid} values
 * are valid (#3545).
 */
async function selectFirstContentRow(page) {
  const list = page.locator('[data-testid="detail-list"]');
  const itemRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
  );
  if ((await itemRows.count()) > 0) {
    await itemRows.first().click({ force: true, timeout: 10_000 });
    return true;
  }
  const folderRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  if ((await folderRows.count()) === 0) {
    return false;
  }
  await folderRows.first().dblclick({ force: true, timeout: 10_000 }).catch(
    async () => {
      await folderRows.first().click({ force: true, timeout: 10_000 }).catch(
        () => {},
      );
    },
  );
  await listWaitReady(page);
  await page.waitForLoadState("networkidle").catch(() => {});
  if ((await itemRows.count()) > 0) {
    await itemRows.first().click({ force: true, timeout: 10_000 });
    return true;
  }
  return false;
}

test.describe("modern React Content Explorer — translations (P-Trans #2430)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts translations toggle and select-item hint",
    { tag: ["@explorer-translations", "@p-trans", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // #2731: translations toggle lives under the View menu dropdown.
      await page.locator('[data-testid="explorer-menu-view"]').click();
      const toggle = page.locator('[data-testid="explorer-toggle-translations"]');
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");

      // No content item selected yet → select-item hint (not the live panel).
      const hint = page.locator('[data-testid="explorer-translations-hint"]');
      await expect(hint).toBeVisible({ timeout: 5_000 });
      await expect(
        page.locator('[data-testid="translations-panel"]'),
      ).toHaveCount(0);
    },
  );

  test(
    "selecting a list row opens translations panel or select-item hint",
    { tag: ["@explorer-translations", "@p-trans"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // Navigate into a structural root so the list has selectable children.
      const tree = page.locator('[data-testid="explorer-tree"]');
      await expect(tree).toBeVisible({ timeout: 15_000 });
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
        await listWaitReady(page);
        await page.waitForLoadState("networkidle").catch(() => {});
      }

      const list = page.locator('[data-testid="detail-list"]');
      await expect(list).toBeVisible({ timeout: 15_000 });

      // Prefer an enabled row; force-click to survive list re-renders (H2 QA).
      const enabledRows = list.locator(
        'tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
      );
      const anyRows = list.locator('tbody tr[data-testid^="detail-row-"]');
      const enabledCount = await enabledRows.count();
      const anyCount = await anyRows.count();
      if (enabledCount === 0 && anyCount === 0) {
        await page.locator('[data-testid="explorer-menu-view"]').click();
        await page.locator('[data-testid="explorer-toggle-translations"]').click();
        await expect(
          page.locator('[data-testid="explorer-translations-hint"]'),
        ).toBeVisible({ timeout: 5_000 });
        return;
      }

      const selectedContent = await selectFirstContentRow(page);
      if (!selectedContent) {
        const target = enabledCount > 0 ? enabledRows.first() : anyRows.first();
        await target.click({ force: true, timeout: 10_000 }).catch(async () => {
          // Re-query after detach from folder refresh.
          const again = list
            .locator('tbody tr[data-testid^="detail-row-"]')
            .first();
          await again.click({ force: true, timeout: 10_000 }).catch(() => {});
        });
      }

      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page.locator('[data-testid="explorer-toggle-translations"]').click();

      // Content row (including GUID 1-101-708) → panel; folder/site → hint.
      const panel = page.locator('[data-testid="translations-panel"]');
      const hint = page.locator('[data-testid="explorer-translations-hint"]');
      await expect(panel.or(hint)).toBeVisible({ timeout: 15_000 });

      if ((await panel.count()) > 0) {
        await expect(panel).toHaveAttribute(
          "data-testid-state",
          /ok|loading|auth|error/,
        );
        await expect(panel).not.toHaveAttribute("data-testid-state", "loading", {
          timeout: 20_000,
        });
        const state = await panel.getAttribute("data-testid-state");
        if (state === "ok") {
          await expect(
            page.locator('[data-testid="translations-current-locale"]'),
          ).toBeVisible();
          await expect(
            page.locator('[data-testid="translations-create"]'),
          ).toBeVisible();
          await expect(
            page.locator('[data-testid="translations-inflight-note"]'),
          ).toBeVisible();
          await expectNoSeriousA11yViolations(page, {
            scope: '[data-testid="translations-panel"]',
          });
        }
      } else {
        await expect(hint).toBeVisible();
      }
    },
  );

  test(
    "create-variant on a content row does not emit numeric-id client error (#3545)",
    { tag: ["@explorer-translations", "@p-trans"] },
    async ({ page }) => {
      test.setTimeout(75_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      const tree = page.locator('[data-testid="explorer-tree"]');
      await expect(tree).toBeVisible({ timeout: 15_000 });
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
        await listWaitReady(page);
        await page.waitForLoadState("networkidle").catch(() => {});
      }

      const selectedContent = await selectFirstContentRow(page);
      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page.locator('[data-testid="explorer-toggle-translations"]').click();

      const panel = page.locator('[data-testid="translations-panel"]');
      const hint = page.locator('[data-testid="explorer-translations-hint"]');
      await expect(panel.or(hint)).toBeVisible({ timeout: 15_000 });

      if (!selectedContent || (await panel.count()) === 0) {
        await expect(hint).toBeVisible();
        expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
          [],
        );
        return;
      }

      await expect(panel).not.toHaveAttribute("data-testid-state", "loading", {
        timeout: 20_000,
      });
      const state = await panel.getAttribute("data-testid-state");
      if (state !== "ok") {
        expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
          [],
        );
        return;
      }

      await expect(
        page.locator('[data-testid="translations-current-locale"]'),
      ).toBeVisible();

      const localeOptions = page.locator(
        '[data-testid^="translations-locale-option-"]',
      );
      if ((await localeOptions.count()) === 0) {
        expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
          [],
        );
        return;
      }

      /** @type {{ itemIds?: number[] } | null} */
      let posted = null;
      await page.route("**/rest/content-explorer/translations", async (route) => {
        if (route.request().method() !== "POST") {
          await route.continue();
          return;
        }
        try {
          posted = route.request().postDataJSON();
        } catch {
          posted = {};
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            created: [
              {
                contentId: 901,
                locale: "xx-test",
                role: "translation",
              },
            ],
          }),
        });
      });

      await localeOptions.first().click();
      await page.locator('[data-testid="translations-create-submit"]').click();

      await expect(
        page.locator('[data-testid="translations-create-success"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="translations-create-error"]'),
      ).toHaveCount(0);
      await expect(
        page.getByText(/does not have a numeric content id/i),
      ).toHaveCount(0);

      expect(posted, "create-variant POST should have been sent").not.toBeNull();
      const ids = posted && Array.isArray(posted.itemIds) ? posted.itemIds : [];
      expect(ids.length).toBeGreaterThan(0);
      expect(Number(ids[0])).toBeGreaterThan(0);
      expect(Number.isFinite(Number(ids[0]))).toBe(true);

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
