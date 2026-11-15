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
 * Translation target list excludes system-only locales (#6387).
 *
 * <p>The create-variant target list is the content-usable slice of the locale
 * catalog: EditorHost loads {@code GET /services/locales?contentUsableOnly=true}
 * and {@code availableTargetLocales} additionally skips any
 * {@code contentUsable === false} row. A system-only locale must never render
 * a {@code translations-locale-option-*} checkbox, while usable and pre-flag
 * (absent flag) locales stay selectable.</p>
 *
 * <p>Tags: {@code @editor} {@code @editor-translations} {@code @content-usable}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-translation-targets.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { TEST_IDS, editorSpaUrl } = require("./helpers/editor-host-translations");

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

const VARIANTS = {
  itemId: 42,
  locale: "en-us",
  variants: [{ contentId: 42, locale: "en-us", role: "source" }],
};

/** Catalog with a usable, a pre-flag (absent flag), and a system-only locale. */
const CATALOG = [
  { languageString: "fr-fr", label: "French", contentUsable: true },
  { languageString: "de-de", label: "German" },
  { languageString: "sys-only", label: "System only", contentUsable: false },
];

async function stubApis(page, catalog = CATALOG) {
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
  await page.route("**/services/locales**", (route) => {
    if (route.request().method() !== "GET") {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(catalog),
    });
  });
  // Unrelated EditorHost chrome (breadcrumb, workflow widget, related canvas)
  // has no seed row for the stubbed item id on a bare H2 cell; stub empty so
  // the translation surface asserts on a clean console.
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ PathItem: {} }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ choices: [] }),
    }),
  );
  await page.route("**/services/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SlotCanvas: { slots: [] } }),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) => {
    if (route.request().method() !== "GET") {
      return route.fallback();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(VARIANTS),
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

test.describe("EditorHost translation targets exclude system-only locales @editor @editor-translations @content-usable", () => {
  test("system-only locale has no target option; usable and pre-flag do", async ({
    page,
  }) => {
    const consoleErrors = watchConsole(page);
    let localesUrl = "";
    await loginAsAdmin(page);
    await stubApis(page);
    page.on("request", (request) => {
      if (request.url().includes("/services/locales")) {
        localesUrl = request.url();
      }
    });
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();

    expect(localesUrl).toContain("contentUsableOnly=true");

    const options = page.getByTestId("translations-locale-options");
    await expect(options).toBeVisible();
    await expect(
      page.getByTestId("translations-locale-option-fr-fr"),
    ).toBeVisible();
    await expect(
      page.getByTestId("translations-locale-option-de-de"),
    ).toBeVisible();
    await expect(
      page.getByTestId("translations-locale-option-sys-only"),
    ).toHaveCount(0);

    await page.screenshot({
      path: "test-results/live-proof-6387-translation-targets.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("all-system-only catalog shows the no-targets empty state", async ({ page }) => {
    const consoleErrors = watchConsole(page);
    await loginAsAdmin(page);
    await stubApis(page, [{ languageString: "en-us", label: "English" }]);
    // Source locale en-us is taken, so the only catalog row is excluded.
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
    await expect(page.getByTestId("translations-create-no-targets")).toBeVisible();
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
