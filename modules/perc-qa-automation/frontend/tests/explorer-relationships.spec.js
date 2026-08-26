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
 * Playwright surface: #2769 / #3546 / #3811 / parent #2400 — Explorer IA
 * Relationships view.
 *
 * <p>Verifies the modern React Content Explorer shell exposes the Relationships
 * panel chrome (View → IA Relationships) and mounts {@code RelationshipsView}
 * for a selected page or asset (or shows the select-item hint when none).
 * Admin on an asset must not see a permission toast. Consumes public REST
 * {@code GET /rest/content-explorer/relationships/{id}/summary}.</p>
 *
 * <p>Tags: {@code @explorer-relationships} {@code @p-adv} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-relationships.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

function attachPageErrors(page) {
  const errors = [];
  page.on("pageerror", (err) => {
    errors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
  return errors;
}

function unexpectedJsErrors(errors) {
  return (errors || []).filter(
    (t) =>
      !/ResizeObserver/i.test(t) &&
      !/Download the React DevTools/i.test(t) &&
      !/favicon/i.test(t) &&
      !/Failed to load resource/i.test(t),
  );
}

/**
 * Walk the already-mounted Explorer list until a selectable content row
 * exists. Avoids spa.jsp?path= (error-bounds Explorer on this H2 cell).
 */
async function selectFirstContentItem(page, rootName, depth) {
  const root = page.locator(
    `[data-testid="explorer-tree"] [data-testid="tree-node-/${rootName}/"], [data-testid="explorer-tree"] [data-testid="tree-node-/${rootName}"]`,
  );
  await expect(root.first()).toBeVisible({ timeout: 15_000 });
  await root.first().click();
  await listWaitReady(page);

  for (let remaining = depth; remaining >= 0; remaining -= 1) {
    const itemRow = page.locator(
      '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
    );
    if ((await itemRow.count()) > 0) {
      await itemRow.first().click({ force: true, timeout: 10_000 });
      return true;
    }
    if (remaining === 0) {
      return false;
    }
    const folderRow = page.locator(
      '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
    );
    if ((await folderRow.count()) === 0) {
      return false;
    }
    await folderRow.first().dblclick({ force: true, timeout: 10_000 });
    await listWaitReady(page);
  }
  return false;
}

test.describe("modern React Content Explorer — IA relationships (#2769)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts relationships toggle and select-item hint",
    { tag: ["@explorer-relationships", "@p-adv", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // T082b / WebUI AGENTS.md — a11y gate on product Explorer shell surface.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });

      // #2731: relationships toggle lives under the View menu dropdown.
      await page.locator('[data-testid="explorer-menu-view"]').click();
      const toggle = page.locator(
        '[data-testid="explorer-toggle-relationships"]',
      );
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");

      // No content item selected yet → select-item hint (not the live panel).
      const hint = page.locator(
        '[data-testid="explorer-relationships-hint"]',
      );
      await expect(hint).toBeVisible({ timeout: 5_000 });
      await expect(
        page.locator('[data-testid="relationships-view"]'),
      ).toHaveCount(0);
    },
  );

  test(
    "selecting a content row mounts the relationships panel (#3546)",
    { tag: ["@explorer-relationships", "@p-adv"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      const opened = await selectFirstContentItem(page, "Sites", 4);
      expect(opened, "H2 sample sites should list at least one page/asset").toBe(
        true,
      );

      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page
        .locator('[data-testid="explorer-toggle-relationships"]')
        .click();

      const region = page.locator(
        '[data-testid="explorer-relationships-panel"]',
      );
      const panel = page.locator('[data-testid="relationships-view"]');
      await expect(region).toBeVisible({ timeout: 15_000 });
      await expect(panel).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="explorer-relationships-hint"]'),
      ).toHaveCount(0);
      await expect(panel).toHaveAttribute(
        "data-testid-state",
        /ok|loading/,
      );
      await expect(
        page.getByText("You do not have permission to perform this action"),
      ).toHaveCount(0);
    },
  );

  test(
    "Admin on a selected asset opens IA Relationships without a permission error (#3811)",
    { tag: ["@explorer-relationships", "@p-adv"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const jsErrors = attachPageErrors(page);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // H2 Assets library is often folders-only; sample-site pages/files
      // still exercise the same Admin IA Relationships path (#3811).
      const opened = await selectFirstContentItem(page, "Sites", 4);
      expect(
        opened,
        "H2 QA should list at least one selectable page or asset",
      ).toBe(true);

      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page
        .locator('[data-testid="explorer-toggle-relationships"]')
        .click();

      const region = page.locator(
        '[data-testid="explorer-relationships-panel"]',
      );
      const panel = page.locator('[data-testid="relationships-view"]');
      await expect(region).toBeVisible({ timeout: 15_000 });
      await expect(panel).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="explorer-relationships-hint"]'),
      ).toHaveCount(0);
      await expect(panel).toHaveAttribute("data-testid-state", /ok|loading/);
      await expect(
        page.getByText("You do not have permission to perform this action"),
      ).toHaveCount(0);
      expect(
        unexpectedJsErrors(jsErrors),
        `JS console errors: ${unexpectedJsErrors(jsErrors).join("; ")}`,
      ).toEqual([]);
    },
  );
});
