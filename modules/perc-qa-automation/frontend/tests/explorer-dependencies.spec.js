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
 * Playwright surface: #2768 / #3571 / parent #2400 — Explorer Dependency Viewer.
 *
 * <p>Verifies the modern React Content Explorer mounts View → Dependencies.
 * Empty selection shows the select-item hint. A selected non-folder content
 * row (numeric or GUID last-segment) must mount {@code explorer-dependencies-panel}
 * / {@code dependency-viewer} — do not treat the hint as a pass when a content
 * row exists (H2 QA / parent #2776).</p>
 *
 * <p>Tags: {@code @explorer-dependencies} {@code @p-adv} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-dependencies.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { paginatedFolderUrl } = require("./helpers/pathmanagement-url");
const {
  isKnownExplorerTransientNetworkConsoleNoise,
} = require("./helpers/explorer-console-clean");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

function attachConsoleCleanGate(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    // Network 4xx/5xx, CORS, mixed-content, and cert failures on thin H2
    // fixtures are console "error" but not uncaught JS (C5d). Auth/error
    // viewer states stay in-scope for #3571.
    if (isKnownExplorerTransientNetworkConsoleNoise(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return { pageErrors, consoleErrors };
}

test.describe("modern React Content Explorer — dependency viewer (#2768 / #3571)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts dependencies toggle and select-item hint",
    { tag: ["@explorer-dependencies", "@p-adv", "@smoke"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = attachConsoleCleanGate(page);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // T082b / WebUI AGENTS.md — a11y gate on product Explorer shell surface.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });

      // #2731 / #2768: dependency toggle lives under the View menu dropdown.
      await page.locator('[data-testid="explorer-menu-view"]').click();
      const toggle = page.locator(
        '[data-testid="explorer-toggle-dependencies"]',
      );
      await expect(toggle).toBeVisible();
      await expect(toggle).toHaveAttribute("aria-expanded", "false");

      await toggle.click();
      await expect(toggle).toHaveAttribute("aria-expanded", "true");

      // No content item selected yet → select-item hint (not the live panel).
      const hint = page.locator('[data-testid="explorer-dependencies-hint"]');
      await expect(hint).toBeVisible({ timeout: 5_000 });
      await expect(
        page.locator('[data-testid="dependency-viewer"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="explorer-dependencies-panel"]'),
      ).toHaveCount(0);
      expect(pageErrors, "uncaught pageerror on dependencies hint").toEqual([]);
      expect(consoleErrors, "console error on dependencies hint").toEqual([]);
    },
  );

  test(
    "selecting a content row mounts the dependency viewer (#3571)",
    { tag: ["@explorer-dependencies", "@p-adv"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const { pageErrors, consoleErrors } = attachConsoleCleanGate(page);
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });

      async function fetchChildren(folderPath) {
        const url = paginatedFolderUrl(BASE_URL, folderPath);
        const res = await page.request.get(url, {
          headers: { Accept: "application/json" },
        });
        if (!res.ok()) {
          return [];
        }
        const body = await res.json();
        return body?.PagedItemList?.childrenInPage ?? [];
      }

      function isContentChild(child) {
        const type = String(child?.type ?? "").trim().toLowerCase();
        const category = String(child?.category ?? "").trim().toLowerCase();
        if (
          type === "folder" ||
          type === "fsfolder" ||
          type === "site" ||
          category === "folder" ||
          category === "site" ||
          category === "section_folder"
        ) {
          return false;
        }
        if (
          category === "page" ||
          category === "asset" ||
          category === "landing_page"
        ) {
          return true;
        }
        return type.length > 0 && type !== "folder";
      }

      async function findFolderWithContent(startPath, depth) {
        const children = await fetchChildren(startPath);
        const items = children.filter(isContentChild);
        if (items.length > 0) {
          return { folderPath: startPath, items };
        }
        if (depth <= 0) {
          return null;
        }
        for (const child of children) {
          const next =
            child.folderPath ||
            child.path ||
            (child.name
              ? `${startPath.replace(/\/+$/, "")}/${child.name}`
              : null);
          if (!next) continue;
          const found = await findFolderWithContent(next, depth - 1);
          if (found) return found;
        }
        return null;
      }

      const found = await findFolderWithContent("/Sites", 4);
      expect(
        found,
        "H2 sample sites should list at least one page/asset under Sites",
      ).toBeTruthy();

      const folderPath = found.folderPath.replace(/\/+$/, "") || "/Sites";
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&path=${encodeURIComponent(folderPath)}&_=${Date.now()}`,
      );
      await page.waitForLoadState("networkidle");
      await listWaitReady(page);

      const list = page.locator('[data-testid="detail-list"]');
      await expect(list).toBeVisible({ timeout: 15_000 });

      const itemRow = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
      );

      if ((await itemRow.count()) === 0) {
        const first = found.items[0];
        const idKey = first.id ?? first.path;
        const byId = list.locator(`[data-testid="detail-row-${idKey}"]`);
        expect(
          await byId.count(),
          "content row from pathmanagement must appear in the detail list",
        ).toBeGreaterThan(0);
        await byId.first().click({ force: true, timeout: 10_000 });
      } else {
        await itemRow.first().click({ force: true, timeout: 10_000 });
      }

      await page.locator('[data-testid="explorer-menu-view"]').click();
      await page.locator('[data-testid="explorer-toggle-dependencies"]').click();

      const region = page.locator(
        '[data-testid="explorer-dependencies-panel"]',
      );
      const panel = page.locator('[data-testid="dependency-viewer"]');
      await expect(region).toBeVisible({ timeout: 15_000 });
      await expect(panel).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="explorer-dependencies-hint"]'),
      ).toHaveCount(0);
      await expect(panel).toHaveAttribute(
        "data-testid-state",
        /ok|loading|auth|error/,
      );
      await expect(panel).not.toHaveAttribute("data-testid-state", "loading", {
        timeout: 20_000,
      });
      expect(
        pageErrors,
        "uncaught pageerror on dependencies mount",
      ).toEqual([]);
      expect(
        consoleErrors,
        "console error on dependencies mount",
      ).toEqual([]);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );
});
