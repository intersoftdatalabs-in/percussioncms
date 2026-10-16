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
 * EditorHost refuses a NUL in the new item name (#5472 / #4532).
 *
 * <p>A keyboard cannot type U+0000, so the spec sets the name through the
 * input value setter and an {@code input} event (the same controlled
 * {@code onChange} path New item already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor} {@code @create}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-create-name-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemCreateUrl,
} = require("./helpers/editor-host-create-item");

const TYPES = {
  ContentTypeList: {
    ContentType: [{ name: "percImageAsset", label: "Image" }],
  },
};

const CREATED = {
  ItemCreateResult: {
    itemId: "77",
    folderPath: "/Assets",
    name: "Shot",
    contentType: "percImageAsset",
  },
};

const FIELDS = {
  ItemEditorFields: {
    contentId: "77",
    contentType: "percImageAsset",
    name: "Shot",
    checkoutUser: "admin",
    revision: 1,
    fields: [{ name: "sys_title", value: "Shot" }],
  },
};

const TYPE_DETAIL = {
  ContentTypeDetail: {
    name: "percImageAsset",
    fields: [
      { name: "sys_title", label: "Title", control: "sys_EditBox", required: true },
    ],
  },
};

const NUL_NAME = `bad${String.fromCharCode(0)}name`;

/**
 * Controlled input update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setInputValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("input value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, codes);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {string[]} pageErrors
 */
function consoleOn(page, pageErrors) {
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409|500)/.test(
        text,
      )
    ) {
      return;
    }
    if (/Failed to load resource:.*404/.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
}

/**
 * @param {string} raw
 * @returns {string | undefined}
 */
function postedName(raw) {
  const parsed = JSON.parse(raw || "{}");
  const body = parsed.ItemCreateRequest || parsed;
  return body.name;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ posts: string[], status: number }} store
 */
async function stubApis(page, store) {
  await page.route("**/services/contenttypes", (route) => {
    if (route.request().method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(TYPES),
      });
    }
    return route.continue();
  });
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE_DETAIL),
    }),
  );
  await page.route("**/services/itemmanagement/item/create**", (route) => {
    if (!isItemCreateUrl(route.request().url())) {
      return route.continue();
    }
    store.posts.push(route.request().postData() || "");
    return route.fulfill({
      status: store.status,
      contentType: "application/json",
      body: JSON.stringify(
        store.status === 200
          ? CREATED
          : { Error: { message: "rejected" } },
      ),
    });
  });
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    }),
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
      body: JSON.stringify({ ItemStateTransition: { transitionTriggers: [] } }),
    }),
  );
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function chooseTypeAndFolder(page) {
  await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
  await page.getByTestId(TEST_IDS.type).selectOption("percImageAsset");
  await page.getByTestId(TEST_IDS.folder).fill("/Assets");
}

test.describe("EditorHost new item name NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a NUL name, keeps type and folder, then a normal name creates",
    { tag: ["@explorer-content-editor", "@editor", "@create"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 200 };
      await stubApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL));
      await chooseTypeAndFolder(page);
      const name = page.getByTestId(TEST_IDS.name);
      await setInputValue(name, NUL_NAME);
      await expect.poll(() => name.evaluate((el) => el.value.includes("\u0000"))).toBe(
        true,
      );
      await page.getByTestId(TEST_IDS.submit).click();
      await expect(page.getByTestId(TEST_IDS.error)).toContainText(
        /name contains a character that cannot be saved/i,
      );
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.type)).toHaveValue("percImageAsset");
      await expect(page.getByTestId(TEST_IDS.folder)).toHaveValue("/Assets");
      await expect(page.getByTestId(TEST_IDS.contentId)).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-create-panel"]',
      });

      await name.fill("Shot");
      await page.getByTestId(TEST_IDS.submit).click();
      await expect(page.getByTestId(TEST_IDS.contentId)).toContainText("77");
      expect(store.posts.length).toBe(1);
      expect(postedName(store.posts[0])).toBe("Shot");
      expect(store.posts[0]).not.toContain("\u0000");
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "a blank name still creates without sending a name",
    { tag: ["@explorer-content-editor", "@editor", "@create"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 200 };
      await stubApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL));
      await chooseTypeAndFolder(page);
      await page.getByTestId(TEST_IDS.name).fill("   ");
      await page.getByTestId(TEST_IDS.submit).click();
      await expect(page.getByTestId(TEST_IDS.contentId)).toContainText("77");
      expect(store.posts.length).toBe(1);
      expect(postedName(store.posts[0])).toBeUndefined();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "closing the panel does not create",
    { tag: ["@explorer-content-editor", "@editor", "@create"] },
    async ({ page }) => {
      const store = { posts: [], status: 200 };
      await stubApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL));
      await chooseTypeAndFolder(page);
      await page.getByTestId(TEST_IDS.name).fill("Shot");
      await page.getByTestId(TEST_IDS.newItem).click();
      await expect(page.getByTestId(TEST_IDS.panel)).toHaveCount(0);
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId(TEST_IDS.contentId)).toHaveCount(0);
    },
  );

  test(
    "HTTP 400, 403, and 404 do not claim success",
    { tag: ["@explorer-content-editor", "@editor", "@create"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 400 };
      await stubApis(page, store);
      const cases = [
        [400, /not valid/i],
        [403, /not allowed/i],
        [404, /not found/i],
      ];
      for (const [status, pattern] of cases) {
        store.status = status;
        store.posts = [];
        await page.goto(editorSpaUrl(BASE_URL));
        await chooseTypeAndFolder(page);
        await page.getByTestId(TEST_IDS.name).fill("Shot");
        await page.getByTestId(TEST_IDS.submit).click();
        await expect(page.getByTestId(TEST_IDS.error)).toContainText(pattern);
        await expect(page.getByTestId(TEST_IDS.panel)).toBeVisible();
        await expect(page.getByTestId(TEST_IDS.contentId)).toHaveCount(0);
        expect(store.posts.length).toBe(1);
      }
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
