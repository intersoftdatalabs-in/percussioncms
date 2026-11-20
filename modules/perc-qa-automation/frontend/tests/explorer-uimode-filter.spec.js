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
 * Playwright surface: SPA shell UI-mode claim (#6619 / parent #4530).
 *
 * <p>{@code GET /Rhythmyx/services/actions/find} accepts the shell claim
 * as {@code ?uiMode=} (mode id or mode name). Scoped menus survive when
 * the token matches, and menus scoped exclusively to other modes are
 * hidden (cascade). This spec locks the transport contract on the H2 QA
 * seed: the param is accepted (HTTP 200 JSON), and — because the seed
 * serves no uiContext-scoped menus — scoped and unscoped catalogs agree,
 * proving the filter is a no-op on unscoped data (no toolbar regression).
 * The enforcement logic itself is locked by Vitest
 * ({@code menuVisibility.test.ts}) and Surefire
 * ({@code ActionMenuUiModeFilterTest}).</p>
 *
 * <p>Tags: {@code @explorer-uimode} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-uimode-filter.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const FIND = `${BASE_URL}/Rhythmyx/services/actions/find`;

function asArray(payload) {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload === "object") {
    const raw =
      payload.ActionMenu ??
      payload.ActionMenuList ??
      payload.actionMenu ??
      payload.actionMenuList;
    if (raw == null) return [];
    return Array.isArray(raw) ? raw : [raw];
  }
  return [];
}

test.describe("Explorer action catalog UI-mode filter (#6619)", () => {
  /** Uncaught page errors + unexpected console errors for C5. */
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (
        /Failed to load resource: the server responded with a status of (404|400)/i.test(
          text,
        )
      ) {
        return;
      }
      pageErrors.push(text);
    });
    await loginAsAdmin(page);
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "uiMode claim is accepted and is a no-op on unscoped seed data",
    { tag: ["@explorer-uimode", "@explorer", "@smoke"] },
    async ({ page }) => {
      const unscoped = await page.request.get(FIND);
      expect(unscoped.status()).toBe(200);
      const plainMenus = asArray(await unscoped.json());

      const scoped = await page.request.get(
        `${FIND}?uiMode=${encodeURIComponent("Content Explorer")}`,
      );
      expect(scoped.status()).toBe(200);
      const claimedMenus = asArray(await scoped.json());

      expect(claimedMenus.map((m) => m.name).sort()).toEqual(
        plainMenus.map((m) => m.name).sort(),
      );
    },
  );

  test(
    "explorer toolbar mounts with the scoped catalog load",
    { tag: ["@explorer-uimode", "@explorer", "@smoke"] },
    async ({ page }) => {
      const {
        explorerSpaUrl,
        TEST_IDS,
      } = require("./helpers/explorer-menu-bar");
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 15_000,
      });
      // T082b a11y gate: scoped catalog + role="status" empty hint (#6619).
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      // C5 live proof (passing-surface screenshot).
      await page.screenshot({
        path: "test-results/live-proof-6619-uimode.png",
      });
    },
  );
});
