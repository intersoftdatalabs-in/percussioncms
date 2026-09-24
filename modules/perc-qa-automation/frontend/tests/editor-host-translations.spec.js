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
 * EditorHost opens an existing translation variant or creates one locale (#4816).
 *
 * <p>Tags: {@code @editor} {@code @editor-translations}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-translations.spec.js}</p>
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

function variantsFor(itemId) {
  if (String(itemId).endsWith("901")) {
    return {
      itemId: 901,
      locale: "de-de",
      variants: [{ contentId: 901, locale: "de-de", role: "translation" }],
    };
  }
  if (String(itemId).endsWith("900")) {
    return {
      itemId: 900,
      locale: "fr-fr",
      variants: [
        { contentId: 42, locale: "en-us", role: "source" },
        { contentId: 900, locale: "fr-fr", role: "translation" },
      ],
    };
  }
  return {
    itemId: 42,
    locale: "en-us",
    variants: [
      { contentId: 42, locale: "en-us", role: "source" },
      { contentId: 900, locale: "fr-fr", role: "translation" },
    ],
  };
}

async function stubApis(page, { variantsStatus = 200, createStatus = 200 } = {}) {
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
    const id = route.request().url().split("/").pop()?.split("?")[0] ?? "42";
    return route.fulfill({
      status: variantsStatus,
      contentType: "application/json",
      body:
        variantsStatus === 200
          ? JSON.stringify(variantsFor(decodeURIComponent(id)))
          : JSON.stringify({ message: "Not allowed" }),
    });
  });
  await page.route("**/rest/content-explorer/translations", (route) => {
    if (route.request().method() !== "POST") {
      return route.fallback();
    }
    return route.fulfill({
      status: createStatus,
      contentType: "application/json",
      body:
        createStatus === 200
          ? JSON.stringify({
              created: [{ contentId: 901, locale: "de-de", role: "translation" }],
            })
          : JSON.stringify({ message: "Translation already exists for locale" }),
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

test.describe("EditorHost translation variants @editor @editor-translations", () => {
  test("opens an existing locale copy", async ({ page }) => {
    const consoleErrors = watchConsole(page);
    await loginAsAdmin(page);
    await stubApis(page);
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
    await page.getByTestId(TEST_IDS.open900).click();
    await expect(page.getByTestId(TEST_IDS.contentId)).toContainText("900");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("creates one locale and opens it", async ({ page }) => {
    const consoleErrors = watchConsole(page);
    await loginAsAdmin(page);
    await stubApis(page);
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await page.getByTestId(TEST_IDS.localeDe).click();
    await page.getByTestId(TEST_IDS.create).click();
    await expect(page.getByTestId(TEST_IDS.contentId)).toContainText("901");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("403 and 409 stay on the host", async ({ page }) => {
    await loginAsAdmin(page);
    await stubApis(page, { variantsStatus: 403 });
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.panel)).toHaveAttribute(
      "data-testid-state",
      "auth",
    );

    await page.unroute("**/rest/content-explorer/translations/**");
    await page.unroute("**/rest/content-explorer/translations");
    await stubApis(page, { createStatus: 409 });
    await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
    await page.getByTestId(TEST_IDS.localeDe).click();
    await page.getByTestId(TEST_IDS.create).click();
    await expect(page.getByTestId(TEST_IDS.createError)).toBeVisible();
    await expect(page.getByTestId(TEST_IDS.host)).toBeVisible();
  });
});
