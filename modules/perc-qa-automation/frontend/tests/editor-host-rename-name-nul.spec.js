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
 * EditorHost refuses a NUL in the rename listing name (#5473 / #4532).
 *
 * <p>A keyboard cannot type U+0000, so the spec sets the name through the
 * input value setter and an {@code input} event (the same controlled
 * {@code onChange} path Rename already uses).</p>
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-rename-name-nul.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isFoldersRenameItemUrl,
  isRenameFolderItemRequest,
} = require("./helpers/editor-host-rename");

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

const NUL_NAME = `Home${String.fromCharCode(0)}Next`;

/**
 * @param {string} name
 * @returns {string}
 */
function fieldsBody(name) {
  return JSON.stringify({
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name,
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: name }],
    },
  });
}

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
 * @param {import("@playwright/test").Page} page
 * @param {{ posts: { url: string, body: object }[], status: number, listed: string }} store
 */
async function stubApis(page, store) {
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: fieldsBody(store.listed),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_TYPE),
    }),
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
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: store.listed, path: `//Assets/${store.listed}` },
      }),
    }),
  );
  await page.route("**/rest/folders/rename/item", async (route) => {
    const raw = route.request().postData() || "";
    let body = {};
    try {
      body = JSON.parse(raw);
    } catch {
      body = {};
    }
    store.posts.push({ url: route.request().url(), body });
    if (store.status === 200) {
      const posted = body.RenameFolderItemRequest || {};
      if (posted.newName) {
        store.listed = String(posted.newName);
      }
    }
    await route.fulfill({
      status: store.status,
      contentType: "application/json",
      body:
        store.status === 200
          ? JSON.stringify({ Status: { code: 200 } })
          : JSON.stringify({ Error: { message: "rejected" } }),
    });
  });
}

test.describe("EditorHost rename listing name NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "refuses a NUL name, keeps the listing name, then a normal name renames",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 200, listed: "Home" };
      await stubApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      const name = page.getByTestId(TEST_IDS.renameName);
      await expect(name).toBeVisible({ timeout: 20_000 });
      await expect(name).toHaveValue("Home");
      await setInputValue(name, NUL_NAME);
      await expect.poll(() => name.evaluate((el) => el.value.includes("\u0000"))).toBe(
        true,
      );
      await page.getByTestId(TEST_IDS.rename).click();
      await expect(page.getByTestId(TEST_IDS.renameError)).toContainText(
        /listing name contains a character that cannot be saved/i,
      );
      expect(store.posts).toEqual([]);
      await expect(page.getByTestId(TEST_IDS.renamed)).toHaveCount(0);
      await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home");
      await expect.poll(() => name.evaluate((el) => el.value.includes("\u0000"))).toBe(
        true,
      );
      // Translations "Item not found" contrast is outside this rename slice.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
        exclude: ['[data-testid="translations-panel"]'],
      });

      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home", {
        timeout: 20_000,
      });
      await expect(page.getByTestId(TEST_IDS.renameName)).toHaveValue("Home");
      await expect(page.getByTestId(TEST_IDS.renamed)).toHaveCount(0);
      expect(store.posts).toEqual([]);

      await page.getByTestId(TEST_IDS.renameName).fill("Home2");
      await page.getByTestId(TEST_IDS.rename).click();
      await expect(page.getByTestId(TEST_IDS.renamed)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home2");
      expect(store.posts.length).toBe(1);
      expect(isFoldersRenameItemUrl(store.posts[0].url)).toBe(true);
      expect(isRenameFolderItemRequest(store.posts[0].body)).toBe(true);
      expect(store.posts[0].body.RenameFolderItemRequest.newName).toBe("Home2");
      expect(JSON.stringify(store.posts[0].body)).not.toContain("\\u0000");
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home2", {
        timeout: 20_000,
      });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "blank, slash, and backslash still refuse before POST",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 200, listed: "Home" };
      await stubApis(page, store);
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.getByTestId(TEST_IDS.rename)).toBeVisible({ timeout: 20_000 });
      for (const value of ["   ", "a/b", "a\\b"]) {
        await page.getByTestId(TEST_IDS.renameName).fill(value);
        await page.getByTestId(TEST_IDS.rename).click();
        await expect(page.getByTestId(TEST_IDS.renameError)).toContainText(/slash/i);
        await expect(page.getByTestId(TEST_IDS.renamed)).toHaveCount(0);
        await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home");
      }
      expect(store.posts).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "HTTP 400, 403, and 404 do not claim success",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { posts: [], status: 400, listed: "Home" };
      await stubApis(page, store);
      const cases = [
        [400, /slash/i],
        [403, /not allowed/i],
        [404, /not found/i],
      ];
      for (const [status, pattern] of cases) {
        store.status = status;
        store.posts = [];
        store.listed = "Home";
        await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
        await expect(page.getByTestId(TEST_IDS.renameName)).toBeVisible({
          timeout: 20_000,
        });
        await page.getByTestId(TEST_IDS.renameName).fill("Other");
        await page.getByTestId(TEST_IDS.rename).click();
        await expect(page.getByTestId(TEST_IDS.renameError)).toContainText(pattern);
        await expect(page.getByTestId(TEST_IDS.renamed)).toHaveCount(0);
        await expect(page.getByTestId(TEST_IDS.title)).toHaveValue("Home");
        expect(store.posts.length).toBe(1);
        expect(store.listed).toBe("Home");
      }
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
