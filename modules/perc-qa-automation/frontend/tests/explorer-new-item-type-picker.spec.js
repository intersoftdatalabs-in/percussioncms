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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer New Item content-type picker on the product route (#3628 / #3513).
 *
 * <p>Live H2 catalog — <strong>no stub</strong> of {@code GET /actions/find}
 * or {@code POST /itemmanagement/item/create}. Select a Sites/Assets folder,
 * open New, pick a real type, assert create HTTP 200 (or documented
 * empty-success). Do not skip when the folder exposes New. Do not request
 * leftover Data Flow CE HTML. Do not claim gap-matrix Present.</p>
 *
 * <p>Tags: {@code @explorer-new-item} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-new-item-type-picker.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { isKnownExplorerSitesConsoleNoise } = require("./helpers/explorer-sites-list-create");
const { treeRootLocator } = require("./helpers/explorer-sites-assets-tree-list");
const {
  TEST_IDS,
  NEW_ITEM_HOST_TEST_IDS,
  CREATE_MENU_TEST_ID,
  preferredContentTypeName,
  isFeatureUrl,
  isDataFlowCeHtmlUrl,
  isCreateSuccessStatus,
  isItemCreateUrl,
  parseContentTypeFromCreateBody,
  newItemMissingFailMessage,
  pickerEmptyFailMessage,
} = require("./helpers/explorer-new-item-type-picker");

function collectLiveErrors(page) {
  const pageErrors = [];
  const consoleErrors = [];
  const featureHttpErrors = [];
  const blocked = [];
  const createCalls = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (!isKnownExplorerSitesConsoleNoise(text)) {
      consoleErrors.push(text);
    }
  });
  page.on("request", (req) => {
    if (isDataFlowCeHtmlUrl(req.url())) {
      blocked.push(req.url());
    }
  });
  page.on("response", (res) => {
    const url = res.url();
    if (isItemCreateUrl(url) && res.request().method() === "POST") {
      createCalls.push({
        status: res.status(),
        body: res.request().postData() || "",
      });
    }
    if (res.status() >= 500 && isFeatureUrl(url)) {
      featureHttpErrors.push(`${res.status()} ${res.request().method()} ${url}`);
    }
  });
  page.on("popup", (popup) => {
    popup.close().catch(() => {});
  });
  return { pageErrors, consoleErrors, featureHttpErrors, blocked, createCalls };
}

/**
 * @param {import("@playwright/test").Page} page
 */
function newItemHostLocator(page) {
  const toolbar = page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`);
  let loc = toolbar.getByRole("button", { name: /^New Item$/i });
  for (const id of NEW_ITEM_HOST_TEST_IDS) {
    loc = loc.or(toolbar.locator(`[data-testid="${id}"]`));
  }
  return loc.first();
}

/**
 * Select Assets then Sites so New Item has a folder path.
 *
 * @param {import("@playwright/test").Page} page
 */
async function openExplorerOnNewItemFolder(page) {
  await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
  ).toBeVisible({ timeout: 20_000 });

  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  await expect(tree).toBeVisible({ timeout: 20_000 });
  const roots = ["Assets", "Sites"];
  for (const root of roots) {
    const byRole = tree.getByRole("treeitem", { name: root });
    const node =
      (await byRole.count()) > 0 ? byRole.first() : treeRootLocator(page, root);
    if ((await node.count()) === 0) {
      continue;
    }
    await node.click({ timeout: 10_000 }).catch(() => {});
    try {
      await expect(newItemHostLocator(page)).toBeVisible({ timeout: 12_000 });
      return;
    } catch {
      const create = page.locator(`[data-testid="${CREATE_MENU_TEST_ID}"]`);
      if ((await create.count()) > 0) {
        await create.first().click().catch(() => {});
        try {
          await expect(newItemHostLocator(page)).toBeVisible({ timeout: 8_000 });
          return;
        } catch {
          /* try the next root */
        }
      }
    }
  }
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function clickNewItemHost(page) {
  const neu = newItemHostLocator(page);
  if ((await neu.count()) === 0 || !(await neu.isVisible())) {
    const create = page.locator(`[data-testid="${CREATE_MENU_TEST_ID}"]`);
    await expect(create.first(), newItemMissingFailMessage()).toBeVisible({
      timeout: 8_000,
    });
    await create.first().click();
  }
  await expect(neu, newItemMissingFailMessage()).toBeVisible({
    timeout: 15_000,
  });
  await neu.click();
}

test.describe("Explorer New Item content-type picker live (#3628)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "New Item host opens a live type picker instead of an error toast",
    { tag: ["@explorer-new-item", "@explorer"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors, featureHttpErrors, blocked, createCalls } =
        collectLiveErrors(page);
      await openExplorerOnNewItemFolder(page);
      await clickNewItemHost(page);

      const picker = page.locator(`[data-testid="${TEST_IDS.typePicker}"]`);
      await expect(
        picker,
        "live New must open Choose a content type (no /actions/find stub) (#3628)",
      ).toBeVisible({ timeout: 15_000 });
      const options = page.locator(
        `[data-testid="${TEST_IDS.typePickerSelect}"] option`,
      );
      await expect
        .poll(async () => options.count(), { timeout: 10_000 })
        .toBeGreaterThan(0);
      await expect(page.getByText("Choose a content type from New Item")).toHaveCount(
        0,
      );
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.typePicker}"]`,
      });
      await page.locator(`[data-testid="${TEST_IDS.typePickerCancel}"]`).click();
      await expect(picker).toHaveCount(0);
      expect(createCalls, "Cancel must not POST create").toEqual([]);
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
      expect(
        featureHttpErrors,
        `feature HTTP 5xx: ${featureHttpErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "New Item picker creates the selected live type and does not open leftover CE HTML",
    { tag: ["@explorer-new-item", "@explorer"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors, featureHttpErrors, blocked, createCalls } =
        collectLiveErrors(page);
      await openExplorerOnNewItemFolder(page);
      await clickNewItemHost(page);

      const picker = page.locator(`[data-testid="${TEST_IDS.typePicker}"]`);
      await expect(picker).toBeVisible({ timeout: 15_000 });
      const optionValues = await page
        .locator(`[data-testid="${TEST_IDS.typePickerSelect}"] option`)
        .evaluateAll((els) =>
          els.map((el) => String(el.value || "").trim()).filter(Boolean),
        );
      const typeName = preferredContentTypeName(optionValues);
      expect(typeName, pickerEmptyFailMessage()).toBeTruthy();
      await page
        .locator(`[data-testid="${TEST_IDS.typePickerSelect}"]`)
        .selectOption(typeName);
      await page.locator(`[data-testid="${TEST_IDS.typePickerOk}"]`).click();

      const templatePicker = page.locator(
        `[data-testid="${TEST_IDS.templatePicker}"]`,
      );
      if (await templatePicker.isVisible().catch(() => false)) {
        const tpl = page.locator(
          `[data-testid="${TEST_IDS.templatePickerSelect}"]`,
        );
        const tplVals = await tpl.locator("option").evaluateAll((els) =>
          els.map((el) => String(el.value || "").trim()).filter(Boolean),
        );
        expect(tplVals.length, "page type must list a live template").toBeGreaterThan(
          0,
        );
        await tpl.selectOption(tplVals[0]);
        await page.locator(`[data-testid="${TEST_IDS.templatePickerOk}"]`).click();
      }

      await expect
        .poll(() => createCalls.length, { timeout: 20_000 })
        .toBeGreaterThan(0);
      const last = createCalls[createCalls.length - 1];
      expect(
        isCreateSuccessStatus(last.status),
        `create must be 200/201/204, got ${last.status}`,
      ).toBe(true);
      const postedType = parseContentTypeFromCreateBody(last.body);
      if (postedType) {
        expect(postedType).toBe(typeName);
      }
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
      expect(
        featureHttpErrors,
        `feature HTTP 5xx: ${featureHttpErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
