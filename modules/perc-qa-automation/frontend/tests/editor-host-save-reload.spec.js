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
 * React Content Editor save-success reload failure (#5768, parent #4532
 * slice 2: edit + save increment).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-save-reload.spec.js}</p>
 *
 * <p>Logical CMS URL paths use {@code /} (not OS file separators).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-save-fields");

const TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
    ],
  },
};

function fieldsPayload(title, revision) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "displaytitle", value: title },
      ],
    },
  };
}

async function stubEditorApis(page, { failReloadTimes = 0, putStatus = 0 } = {}) {
  const calls = { puts: 0, gets: 0, reloadFails: 0 };
  // Registered first so later field/workflow stubs win. Keeps fake id 42
  // off the QA server log; the browser still ignores failed-resource noise.
  await page.route("**/services/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: [],
        },
      }),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, locale: "en-us", variants: [] }),
    }),
  );
  await page.route("**/services/assembly/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "[]" }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      calls.puts += 1;
      if (putStatus !== 0) {
        await route.fulfill({
          status: putStatus,
          contentType: "application/json",
          body: JSON.stringify({ Error: { message: "save rejected" } }),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(fieldsPayload("Updated", 5)),
      });
      return;
    }
    calls.gets += 1;
    // GET #1 is the initial load; later GETs are post-save reloads (or the
    // retry button). Fail the first `failReloadTimes` reloads only.
    if (calls.gets > 1 && calls.reloadFails < failReloadTimes) {
      calls.reloadFails += 1;
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ Error: { message: "refresh boom" } }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        fieldsPayload(calls.puts > 0 ? "Updated" : "Welcome", calls.puts > 0 ? 7 : 2),
      ),
    });
  });
  return calls;
}

function trackPageProblems(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (text.includes("Failed to load resource")) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("React Content Editor save-success reload failure", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "save success with a failed refresh keeps Saved, offers retry, and retry reloads",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const calls = await stubEditorApis(page, { failReloadTimes: 1 });
      const titleField = () => page.locator('[data-testid="editor-field-displaytitle"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(titleField()).toHaveValue("Welcome", { timeout: 20_000 });
      await titleField().fill("Updated");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => calls.puts).toBeGreaterThan(0);
      // The PUT succeeded: Saved stays truthful, no PUT failure is claimed,
      // and the save region reports the failed refresh with Retry.
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-reload-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="editor-save-reload-error"]')).toContainText(
        /could not be reloaded/i,
      );
      await expect(page.locator('[data-testid="editor-retry-save-reload"]')).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toHaveCount(0);

      // Retry only reloads: no second PUT, the notice clears, revision 7 lands.
      await page.locator('[data-testid="editor-retry-save-reload"]').click();
      await expect(page.locator('[data-testid="editor-save-reload-error"]')).toHaveCount(0);
      expect(calls.puts).toBe(1);
      await expect(page.locator('[data-testid="editor-revision"]')).toContainText(/Revision 7/);
      await expect(page.locator('[data-testid="editor-saved"]')).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });
    },
  );

  test(
    "a 400 PUT is not success and shows no reload notice",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = trackPageProblems(page);
      const calls = await stubEditorApis(page, { putStatus: 400 });
      const titleField = () => page.locator('[data-testid="editor-field-displaytitle"]');

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(titleField()).toHaveValue("Welcome", { timeout: 20_000 });
      await titleField().fill("Rejected headline");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect.poll(() => calls.puts).toBeGreaterThan(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.saveError}"]`)).toBeVisible();
      await expect(page.locator('[data-testid="editor-saved"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-save-reload-error"]')).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
