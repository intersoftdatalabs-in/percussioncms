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
 * Playwright surface: #2730 residual — server action toolbar nested menus.
 *
 * <p>Coordinates with #2731 ExplorerMenuBar: this surface asserts the
 * server-driven {@code action-toolbar} mounts under the menubar and, when
 * the live CMS returns MENU parents with children, those parents render as
 * nested dropdowns ({@code aria-haspopup=menu}) rather than dumping every
 * child as a top-level button while closed.</p>
 *
 * <p>Tags: {@code @explorer-action-toolbar} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-action-toolbar-menus.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./helpers/explorer-menu-bar");
const { collectMenuParents } = require("./helpers/explorer-action-toolbar-catalog");
const {
  expectNoSeriousA11yViolations,
} = require("./helpers/a11y");

test.describe("modern React Content Explorer — nested ActionToolbar menus (#2730)", () => {
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
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "server actions region + action toolbar mount under menu bar",
    { tag: ["@explorer-action-toolbar", "@explorer", "@smoke"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.menuBar}"]`),
      ).toBeVisible();
      // #2972: labeled Server actions chrome is always visible product chrome.
      await expect(
        page.locator('[data-testid="explorer-server-actions"]'),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="explorer-server-actions-label"]'),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible();
      // Either at least one server action button OR the empty-state placeholder.
      const items = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"]`,
      );
      const empty = page.locator('[data-testid="action-toolbar-empty"]');
      const loadError = page.locator(
        '[data-testid="explorer-server-actions-error"]',
      );
      await expect
        .poll(async () => {
          const n = await items.count();
          const e = await empty.count();
          const err = await loadError.count();
          return n > 0 || e > 0 || err > 0;
        })
        .toBe(true);
    },
  );

  test(
    "MENU parents with children open nested dropdowns (do not skip when catalog has MENU parents)",
    { tag: ["@explorer-action-toolbar", "@explorer"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const catalog = await page.evaluate(async () => {
        const paths = [
          "/Rhythmyx/services/actions/find",
          "/services/actions/find",
        ];
        for (const url of paths) {
          try {
            const res = await fetch(url, { credentials: "same-origin" });
            if (!res.ok) continue;
            return { url, payload: await res.json() };
          } catch {
            // try next path
          }
        }
        return { url: null, payload: null };
      });

      expect(
        catalog.url,
        "GET /actions/find must succeed so nested chrome is asserted (#3560)",
      ).toBeTruthy();

      const menuParents = collectMenuParents(catalog.payload);
      test.info().annotations.push({
        type: "note",
        description: `find() ${catalog.url || "unreached"} MENU parents=${menuParents.length}`,
      });
      expect(
        menuParents.length,
        "H2 catalog must include cascading MENU parents (Paste/Arrange/View/Create)",
      ).toBeGreaterThan(0);

      const requiredNames = ["Paste", "Arrange", "View", "Create"];
      const required = menuParents.filter((p) => requiredNames.includes(p.name));
      expect(
        required.map((p) => p.name).sort(),
        "static MENU parents must stay nested in GET /actions/find",
      ).toEqual([...requiredNames].sort());

      const parents = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"][aria-haspopup="menu"]`,
      );

      // Live catalog has cascading MENU parents — must render dropdowns
      // (#3379 / #3560). Do not soft-skip.
      await expect
        .poll(async () => parents.count(), { timeout: 15_000 })
        .toBeGreaterThan(0);

      // #6927: snapshot rendered toolbar controls once so a missing MENU
      // parent fails with catalog-vs-rendered state (seed gap like #6886
      // vs render regression) instead of a bare locator timeout.
      const renderedToolbarControls = await page
        .locator(
          `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"]`,
        )
        .evaluateAll((els) =>
          els.map(
            (el) =>
              `${el.getAttribute("data-testid")}|haspopup=${el.getAttribute("aria-haspopup")}`,
          ),
        );
      const catalogMenuParentNames = menuParents.map((p) => p.name).join(",");

      for (const parentMenu of required) {
        const parentBtn = page.locator(
          `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid="action-toolbar-item-${parentMenu.name}"][aria-haspopup="menu"]`,
        );
        await expect(
          parentBtn,
          `MENU parent "${parentMenu.name}" must be one aria-haspopup=menu control ` +
            `(catalog MENU parents=[${catalogMenuParentNames}]; ` +
            `rendered=[${renderedToolbarControls.join(",")}])`,
        ).toBeVisible();
      }

      // Closed: children of every MENU parent must not appear as extra
      // top-level toolbar buttons (#3379 / #3560).
      const closedTopLevel = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] > button[data-testid^="action-toolbar-item-"], [data-testid="${TEST_IDS.actionToolbar}"] > div > button[data-testid^="action-toolbar-item-"]`,
      );
      const closedNames = await closedTopLevel.evaluateAll((els) =>
        els.map((el) => el.getAttribute("data-testid") || ""),
      );
      for (const parentMenu of menuParents) {
        for (const childName of parentMenu.childNames) {
          expect(
            closedNames.includes(`action-toolbar-item-${childName}`),
            `closed toolbar dumped child "${childName}" of "${parentMenu.name}" as a top-level button`,
          ).toBe(false);
        }
      }

      // #6886: Arrange is the static MENU parent this spec guards — open it
      // explicitly (not just required[0]) so the dropdown, its catalog
      // children, and keyboard return-focus are covered in the default
      // explorer state.
      const arrange = required.find((p) => p.name === "Arrange");
      expect(arrange, "Arrange must be among the static MENU parents").toBeDefined();
      const arrangeChildNames =
        (arrange && arrange.childNames) || [];
      expect(
        arrangeChildNames.length,
        "Arrange must carry children in GET /actions/find",
      ).toBeGreaterThan(0);
      await page
        .locator(
          `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid="action-toolbar-item-Arrange"]`,
        )
        .click();
      await expect(
        page.locator(`[data-testid="action-toolbar-menu-Arrange"]`),
      ).toBeVisible({ timeout: 5_000 });
      const arrangeItems = page.locator(
        `[data-testid="action-toolbar-menu-Arrange"] [role="menuitem"]`,
      );
      await expect(arrangeItems.first()).toBeVisible();
      const arrangeLabels = await arrangeItems.evaluateAll((els) =>
        els.map((el) => el.getAttribute("data-testid") ?? ""),
      );      for (const childName of arrangeChildNames) {
        expect(
          arrangeLabels.includes(`action-toolbar-item-${childName}`),
          `Arrange child "${childName}" must render as a menuitem`,
        ).toBe(true);
      }
      // #6915 (DCE parity): with no Active Assembly slot snippet selected,
      // Arrange children render aria-disabled with a tooltip naming the
      // missing slot context instead of dispatching a status hint.
      // Roving tabindex still lists each item as one tab stop.
      const disabledFlags = await arrangeItems.evaluateAll((els) =>
        els.map((el) => ({
          testId: el.getAttribute("data-testid") ?? "",
          disabled: el.getAttribute("aria-disabled"),
          title: el.getAttribute("title") ?? "",
        })),
      );
      expect(
        disabledFlags.length,
        "Arrange menu must list its catalog children",
      ).toBeGreaterThan(0);
      for (const flag of disabledFlags) {
        expect(
          flag.disabled,
          `slot-less Arrange child "${flag.testId}" must be aria-disabled`,
        ).toBe("true");
        expect(
          flag.title,
          `slot-less Arrange child "${flag.testId}" tooltip must name the missing slot context`,
        ).toMatch(/slot/i);
      }
      await expect(arrangeItems.first()).toHaveAttribute("tabindex", "0");
      await expect(
        arrangeItems.nth(1),
        "roving tabindex keeps one tab stop while disabled items stay focusable",
      ).toHaveAttribute("tabindex", "-1");
      // Keyboard: ArrowDown lands on (not skips) the disabled second item…
      await arrangeItems.first().focus();
      await page.keyboard.press("ArrowDown");
      await expect(arrangeItems.nth(1)).toBeFocused();
      // …and Enter on it is a no-op: no server call, no error, menu open.
      await page.keyboard.press("Enter");
      await expect(
        page.locator(`[data-testid="action-toolbar-menu-Arrange"]`),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);
      // Changed surface keeps the axe gate green while the disabled
      // Arrange menu is open (#6915 / T082b).
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="action-toolbar-menu-Arrange"]',
      });
      // Live Proof (#6886): pass-run capture of the restored Arrange menu.
      await page.screenshot({
        path: "test-results/live-proof-6886-arrange-dropdown.png",
      });
      // Keyboard: Escape closes the Arrange menu and returns focus to the
      // Arrange parent button (#6824).
      await page.keyboard.press("Escape");
      await expect(
        page.locator(`[data-testid="action-toolbar-menu-Arrange"]`),
      ).toBeHidden();
      await expect(
        page.locator(
          `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid="action-toolbar-item-Arrange"]`,
        ),
      ).toBeFocused();
    },
  );

  test(
    "keyboard traversal across an open toolbar dropdown (#6827)",
    { tag: ["@explorer-action-toolbar", "@explorer"] },
    async ({ page }) => {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const parents = page.locator(
        `[data-testid="${TEST_IDS.actionToolbar}"] [data-testid^="action-toolbar-item-"][aria-haspopup="menu"]`,
      );
      await expect
        .poll(async () => parents.count(), { timeout: 15_000 })
        .toBeGreaterThan(0);
      const firstParent = parents.first();
      const parentTestId = await firstParent.getAttribute("data-testid");
      const parentName = String(parentTestId).replace(
        "action-toolbar-item-",
        "",
      );
      const menu = page.locator(
        `[data-testid="action-toolbar-menu-${parentName}"]`,
      );
      const activeTestId = () =>
        page.evaluate(
          () => document.activeElement?.getAttribute("data-testid") ?? "",
        );

      // ArrowDown on the parent opens the menu on the first item (#6827).
      await firstParent.focus();
      await page.keyboard.press("ArrowDown");
      await expect(menu).toBeVisible({ timeout: 5_000 });
      const items = menu.locator('[role="menuitem"]');
      const count = await items.count();
      expect(count, "open dropdown must have traversable items").toBeGreaterThan(
        1,
      );
      expect(await activeTestId()).toBe(
        await items.nth(0).getAttribute("data-testid"),
      );
      // Roving tabindex: exactly one tab stop while open.
      await expect(menu.locator('[tabindex="0"]')).toHaveCount(1);

      // ArrowDown walks forward; End/Home jump; Escape returns to parent.
      await page.keyboard.press("ArrowDown");
      expect(await activeTestId()).toBe(
        await items.nth(1).getAttribute("data-testid"),
      );
      await page.keyboard.press("End");
      expect(await activeTestId()).toBe(
        await items.nth(count - 1).getAttribute("data-testid"),
      );
      await page.keyboard.press("Home");
      expect(await activeTestId()).toBe(
        await items.nth(0).getAttribute("data-testid"),
      );

      // Changed surface keeps the axe gate green while the menu is open.
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="action-toolbar-menu-${parentName}"]`,
      });

      await page.keyboard.press("Escape");
      await expect(menu).toBeHidden();
      expect(await activeTestId()).toBe(parentTestId);

      // Live Proof (#6827): pass-run capture of the keyboard-operated menu.
      await firstParent.focus();
      await page.keyboard.press("ArrowDown");
      await expect(menu).toBeVisible({ timeout: 5_000 });
      await page.screenshot({
        path: "test-results/live-proof-6827-toolbar-keyboard.png",
      });
    },
  );
});
