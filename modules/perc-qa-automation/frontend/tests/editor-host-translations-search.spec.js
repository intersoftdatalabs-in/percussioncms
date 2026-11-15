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
 * EditorHost translation-variant list search filter (#6414, parent #4532).
 *
 * <p>The shared Translations panel variants table gains a locale search input:
 * typing narrows rows by locale, role, or content id; a filter with no matches
 * shows a distinct no-match state instead of the table; clearing restores
 * every row. Tags: {@code @editor} {@code @editor-translations}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-translations-search.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-translations");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const TYPE_DETAIL = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function fields(id) {
  return {
    ItemEditorFields: {
      contentId: String(id),
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 1,
      fields: [{ name: "sys_title", value: `Title ${id}` }],
    },
  };
}

function variantsFor() {
  return {
    itemId: 42,
    locale: "en-us",
    variants: [
      { contentId: 42, locale: "en-us", role: "source" },
      { contentId: 900, locale: "fr-fr", role: "translation" },
      { contentId: 901, locale: "de-de", role: "translation" },
    ],
  };
}

async function stubApis(page) {
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE_DETAIL),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) => {
    const url = route.request().url();
    const id = url.match(/fields\/(\d+)/)?.[1] ?? "42";
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fields(id)),
    });
  });
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
      body: JSON.stringify({ ItemStateTransition: { transitionTriggers: [] } }),
    }),
  );
  // Background host chrome outside this slice: workflow picker, breadcrumb
  // path, and related-content canvas. Quiet 200s keep the console-clean gate
  // deterministic on a fresh-H2 cell where fake id 42 has no live rows.
  const quiet = { status: 200, contentType: "application/json", body: "{}" };
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: "Home", path: "//Sites/Demo/Home", type: "page" },
      }),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ownerId: 42, templateId: 7, slots: [] }),
    }),
  );
  await page.route("**/itemmanagement/item/siteimpact/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
    }),
  );
  // Template picker catalogs (site + read-only library): empty on this
  // fresh-H2 cell where the Demo site has no loadable templates.
  const emptyList = { status: 200, contentType: "application/json", body: "[]" };
  await page.route("**/sitemanage/sitetemplates/templates/**", (route) =>
    route.fulfill(emptyList),
  );
  await page.route("**/pagemanagement/template/summary/all/readonly**", (route) =>
    route.fulfill(emptyList),
  );
  await page.route("**/services/locales", (route) => {
    if (route.request().method() !== "GET") {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        { languageString: "en-us", label: "English" },
        { languageString: "fr-fr", label: "French" },
        { languageString: "de-de", label: "German" },
      ]),
    });
  });
  await page.route("**/rest/content-explorer/translations/**", (route) => {
    if (route.request().method() !== "GET") {
      return route.fallback();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(variantsFor()),
    });
  });
}

function watchConsole(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => consoleErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource:.*40[34]/.test(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return consoleErrors;
}

async function openPanel(page) {
  await loginAsAdmin(page);
  await stubApis(page);
  await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
  await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
  await expect(page.getByTestId(TEST_IDS.panel)).toHaveAttribute(
    "data-testid-state",
    "ok",
  );
}

test.describe("EditorHost translation variant search @editor @editor-translations", () => {
  test("filters the variant list by typed locale", async ({ page }) => {
    const consoleErrors = watchConsole(page);
    await openPanel(page);
    const search = page.getByTestId("translations-variants-search");
    await expect(search).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-42")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-900")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-901")).toBeVisible();
    await search.fill("fr");
    await expect(page.getByTestId("translations-variant-row-900")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-42")).toHaveCount(0);
    await expect(page.getByTestId("translations-variant-row-901")).toHaveCount(0);
    await expect(page.getByTestId("translations-variants-table")).toBeVisible();
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("matches role and content id, shows no-match, restores on clear", async ({
    page,
  }) => {
    const consoleErrors = watchConsole(page);
    await openPanel(page);
    const search = page.getByTestId("translations-variants-search");
    await search.fill("translation");
    await expect(page.getByTestId("translations-variant-row-42")).toHaveCount(0);
    await expect(page.getByTestId("translations-variant-row-900")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-901")).toBeVisible();
    await search.fill("901");
    await expect(page.getByTestId("translations-variant-row-901")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-900")).toHaveCount(0);
    await search.fill("xx-no-such-locale");
    await expect(page.getByTestId("translations-variants-no-match")).toBeVisible();
    await expect(page.getByTestId("translations-variants-table")).toHaveCount(0);
    await search.fill("");
    await expect(page.getByTestId("translations-variants-no-match")).toHaveCount(0);
    await expect(page.getByTestId("translations-variant-row-42")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-900")).toBeVisible();
    await expect(page.getByTestId("translations-variant-row-901")).toBeVisible();
    await page.screenshot({
      path: "test-results/live-proof-6414-translations-search.png",
    });
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="translations-panel"]',
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
