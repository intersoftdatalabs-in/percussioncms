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
 * PublishingShell Design — associate a content list with an edition (#5107 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionAssociateContentList.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const ASSOC_POST =
  /\/services\/sitemanage\/publishingdesign\/editions\/[^/]+\/contentlists$/;

async function openDesign(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
}

async function createContentList(page, name) {
  await page.getByRole("tab", { name: /Content lists/i }).click();
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  await page.getByTestId("contentlist-save").click();
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name })).toBeVisible({
    timeout: 20000,
  });
}

async function createAndOpenEdition(page, name) {
  await page.getByRole("tab", { name: /Editions/i }).click();
  await page.getByTestId("design-add-edition").click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await page.locator("#ed-name").fill(name);
  await page.getByTestId("edition-save").click();
  await expect(page.getByTestId("edition-editor")).toBeHidden({
    timeout: 20000,
  });
  await page.getByRole("button", { name }).click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await expect(page.getByTestId("edition-assoc-empty")).toBeVisible();
}

async function chooseFirstContext(page) {
  const context = page.getByTestId("edition-assoc-context");
  await expect
    .poll(
      async () =>
        context.locator("option").evaluateAll((opts) => {
          const found = opts.map((opt) => opt.value).find((id) => id);
          return found || "";
        }),
      { timeout: 20000 },
    )
    .not.toEqual("");
  const value = await context.locator("option").evaluateAll((opts) => {
    const found = opts.map((opt) => opt.value).find((id) => id);
    return found || "";
  });
  await context.selectOption(value);
}

test.describe("PublishingShell Design associate content list", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("lists the content list only after associate succeeds", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    const stamp = Date.now();
    const listName = `NightAssoc-${stamp}`;
    const editionName = `NightEdAssoc-${stamp}`;
    await openDesign(page);
    await createContentList(page, listName);
    await createAndOpenEdition(page, editionName);

    let releasePost;
    const postGate = new Promise((resolve) => {
      releasePost = resolve;
    });
    let postBody = "";
    let markPosted;
    const posted = new Promise((resolve) => {
      markPosted = resolve;
    });
    await page.route(ASSOC_POST, async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      postBody = route.request().postData() || "";
      markPosted();
      await postGate;
      await route.continue();
    });

    await page.getByTestId("edition-assoc-content-list").selectOption({
      label: listName,
    });
    await chooseFirstContext(page);
    await page.getByTestId("edition-associate").click();
    await posted;
    await expect(page.getByTestId("edition-assoc-empty")).toBeVisible();
    await expect(page.getByTestId("edition-assoc-list")).toHaveCount(0);
    expect(postBody).toContain("editionContentList");
    expect(postBody).toContain("contentListId");
    expect(postBody).toContain("deliveryContextId");
    releasePost();
    await expect(page.getByTestId("edition-assoc-list")).toContainText(listName, {
      timeout: 20000,
    });
    await expect(page.getByRole("alert")).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("does not post when content list and context are blank", async ({
    page,
  }) => {
    const editionName = `NightEdBlank-${Date.now()}`;
    let posts = 0;
    await page.route(ASSOC_POST, async (route) => {
      if (route.request().method() === "POST") {
        posts += 1;
      }
      await route.continue();
    });
    await openDesign(page);
    await createAndOpenEdition(page, editionName);
    await page.getByTestId("edition-associate").click();
    await expect(page.getByRole("alert")).toContainText(
      "Select content list and delivery context",
    );
    expect(posts).toBe(0);
    await expect(page.getByTestId("edition-assoc-empty")).toBeVisible();
  });

  test("HTTP 400, 403, and 409 do not list the content list", async ({
    page,
  }) => {
    const stamp = Date.now();
    const listName = `NightAssocErr-${stamp}`;
    const editionName = `NightEdErr-${stamp}`;
    await openDesign(page);
    await createContentList(page, listName);
    await createAndOpenEdition(page, editionName);

    const cases = [
      [400, "contentListId and deliveryContextId are required"],
      [403, "Admin or Designer role required to save a publish edition"],
      [409, "Content list is already associated with this edition"],
    ];
    for (const [status, message] of cases) {
      await page.unroute(ASSOC_POST);
      await page.route(ASSOC_POST, async (route) => {
        if (route.request().method() === "POST") {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
          return;
        }
        await route.continue();
      });
      await page.getByTestId("edition-assoc-content-list").selectOption({
        label: listName,
      });
      await chooseFirstContext(page);
      await page.getByTestId("edition-associate").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("edition-assoc-empty")).toBeVisible();
      await expect(page.getByTestId("edition-assoc-list")).toHaveCount(0);
    }
  });
});
