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
 * React Content Editor host — autosave draft recovery notice after reload (#6076).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-autosave-draft.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { editorSpaUrl } = require("./helpers/editor-host-workflow");

const STALE_TITLE = "Stale draft";
const SERVER_TITLE = "Home";
const AUTOSAVE_KEY = "perc-editor-autosave-42";

function fieldsBody(title) {
  return {
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 1,
      fields: [{ name: "sys_title", value: title }],
    },
  };
}

function stubEditorApis(page, store) {
  return (async () => {
    await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
    );
    await page.route("**/rest/editor/items/**/checkout", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
    );
    await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
      if (route.request().method() === "PUT") {
        store.puts.push(route.request().postData() || "");
        try {
          const parsed = JSON.parse(route.request().postData() || "{}");
          const rows = parsed.fields || parsed.Fields || [];
          const title = rows.find((row) => (row.name || row.Name) === "sys_title");
          if (title) {
            store.title = String(title.value ?? title.Value ?? store.title);
          }
        } catch {
          // Keep the previous title when the body cannot be read.
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(fieldsBody(store.title)),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(fieldsBody(store.title)),
      });
    });
    await page.route("**/services/contenttypes/**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ContentTypeDetail: {
            name: "percPage",
            fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
          },
        }),
      }),
    );
    await page.route("**/services/itemmanagement/workflow/**", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemStateTransition: {
            itemId: "42",
            stateName: "Draft",
            transitionTriggers: [],
          },
          ItemWorkflowChoices: { workflows: [] },
        }),
      }),
    );
  })();
}

function seedAutosave(page, draft) {
  // One-shot seed (not addInitScript): re-navigating to the entry URL
  // re-boots the SPA with persisted localStorage, which is the reload
  // equivalent. An init script would re-seed on every navigation and
  // corrupt the post-save/post-discard assertions below.
  return page.evaluate(
    ({ key, value }) => {
      window.localStorage.setItem(key, value);
    },
    {
      key: AUTOSAVE_KEY,
      value: JSON.stringify({
        version: 1,
        contentId: "42",
        savedAt: new Date().toISOString(),
        revision: 1,
        draft,
      }),
    },
  );
}

function editorEntryUrl() {
  return editorSpaUrl(BASE_URL, "contentId=42&mode=edit");
}

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
    pageErrors.push(text);
  });
}

test.describe("React Content Editor autosave draft recovery", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "restore fills the draft without PUT; explicit save then writes and reload stays silent",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { title: SERVER_TITLE, puts: [] };
      await stubEditorApis(page, store);
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-field-sys_title")).toBeVisible({
        timeout: 20_000,
      });
      await seedAutosave(page, { sys_title: STALE_TITLE });
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-autosave-notice")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        SERVER_TITLE,
      );
      await page.getByTestId("editor-autosave-restore").click();
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        STALE_TITLE,
      );
      expect(store.puts).toEqual([]);
      await expect(page.getByTestId("editor-autosave-notice")).toHaveCount(0);
      await page.getByTestId("editor-save").click();
      await expect(page.getByTestId("editor-saved")).toBeVisible();
      expect(store.puts.length).toBe(1);
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        STALE_TITLE,
        { timeout: 20_000 },
      );
      await expect(page.getByTestId("editor-autosave-notice")).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "discard drops the stored draft and a reload stays silent",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { title: SERVER_TITLE, puts: [] };
      await stubEditorApis(page, store);
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-field-sys_title")).toBeVisible({
        timeout: 20_000,
      });
      await seedAutosave(page, { sys_title: STALE_TITLE });
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-autosave-notice")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-autosave-discard").click();
      await expect(page.getByTestId("editor-autosave-notice")).toHaveCount(0);
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        SERVER_TITLE,
      );
      expect(store.puts).toEqual([]);
      const stored = await page.evaluate((key) => window.localStorage.getItem(key), AUTOSAVE_KEY);
      expect(stored).toBeNull();
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        SERVER_TITLE,
        { timeout: 20_000 },
      );
      await expect(page.getByTestId("editor-autosave-notice")).toHaveCount(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "typing then reloading surfaces the recovery banner",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const pageErrors = [];
      consoleOn(page, pageErrors);
      const store = { title: SERVER_TITLE, puts: [] };
      await stubEditorApis(page, store);
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-field-sys_title")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("editor-autosave-notice")).toHaveCount(0);
      await page.getByTestId("editor-field-sys_title").fill("Typed draft");
      await page.goto(editorEntryUrl());
      await expect(page.getByTestId("editor-autosave-notice")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("editor-autosave-restore").click();
      await expect(page.getByTestId("editor-field-sys_title")).toHaveValue(
        "Typed draft",
      );
      expect(store.puts).toEqual([]);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );
});
