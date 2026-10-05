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
 * PublishingShell Design — set or clear one content list description (#5249 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListDescription.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const GENERATOR = "sys_Search";

function contentListUpdatePut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/contentlists\/[^/]+$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
}

function trackJsErrors(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      text.includes("Failed to load resource") ||
      text.includes("status of 400") ||
      text.includes("status of 403") ||
      text.includes("status of 409")
    ) {
      return;
    }
    jsErrors.push(text);
  });
  return jsErrors;
}

async function openContentLists(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Content lists/i }).click();
}

function sourceRow(page, name) {
  return page
    .getByRole("listitem")
    .filter({ has: page.getByRole("button", { name, exact: true }) });
}

async function chooseFilter(page) {
  const select = page.locator("#cl-item-filter");
  await expect(select).toBeEnabled({ timeout: 20000 });
  const chosen = await select.evaluate((el) => {
    const option = Array.from(el.options).find((opt) => opt.value);
    if (!option) {
      return null;
    }
    return { value: option.value, label: option.label };
  });
  expect(chosen, "H2 catalog has no item filter to select").toBeTruthy();
  await select.selectOption(chosen.value);
  return chosen;
}

async function createModernList(page, name, description) {
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  await page.locator("#cl-desc").fill(description);
  await page.locator("#cl-gen").fill(GENERATOR);
  const filter = await chooseFilter(page);
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/contentlists(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("contentlist-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create content list HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
  return filter;
}

async function createLegacyList(page, name, description, url) {
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  await page.locator("#cl-desc").fill(description);
  await page.locator("#cl-type").selectOption("legacy");
  await page.locator("#cl-url").fill(url);
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/contentlists(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("contentlist-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create legacy content list HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

test.describe("PublishingShell Design content list description", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("saves a description only after reload and can clear it", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (contentListUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openContentLists(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `ClDsc${stamp}`;
    const original = `Night description ${stamp}`;
    const updated = `Updated description ${stamp}`;
    const filter = await createModernList(page, name, original);

    const row = sourceRow(page, name);
    await expect(row.locator("[data-testid^='design-content-list-type-']")).toHaveText(
      "modern",
    );
    await expect(row.locator("[data-testid^='design-content-list-source-']")).toHaveText(
      GENERATOR,
    );
    await expect(
      row.locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(original);
    await expect(row.locator("[data-testid^='design-content-list-filter-']")).toHaveText(
      filter.label,
    );

    await row.locator("[data-testid^='design-content-list-describe-']").click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
    await expect(page.getByTestId("contentlist-description-name")).toHaveText(name);
    await expect(page.getByTestId("contentlist-description-type")).toHaveText("modern");
    await expect(page.getByTestId("contentlist-description-source")).toHaveText(GENERATOR);
    await expect(page.getByTestId("contentlist-description-filter")).toHaveText(filter.label);
    await expect(page.locator("#contentlist-description-input")).toHaveValue(original);

    await page.locator("#contentlist-description-input").fill(updated);
    const putsBeforeCancel = putBodies.length;
    await page.getByTestId("contentlist-description-cancel").click();
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden();
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(original);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await sourceRow(page, name)
      .locator("[data-testid^='design-content-list-describe-']")
      .click();
    await page.locator("#contentlist-description-input").fill("d".repeat(256));
    await page.getByTestId("contentlist-description-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#contentlist-description-input").fill(`  ${updated}  `);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/contentlists\/[^/]+(?:\?|$)/,
      async (route) => {
        if (route.request().method() !== "PUT") {
          return route.continue();
        }
        postedBody = route.request().postData() || "";
        await holdPut;
        return route.continue();
      },
    );
    const putResponse = page.waitForResponse(
      (res) => contentListUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("contentlist-description-save").click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
    await expect.poll(() => postedBody).toContain(updated);
    expect(postedBody).not.toContain(name);
    expect(postedBody).not.toContain(GENERATOR);
    expect(postedBody).not.toContain("itemFilterId");
    expect(postedBody).not.toContain("listType");
    expect(postedBody).not.toContain('"generator"');
    await expect(sourceRow(page, name)).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `describe content list HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden({
      timeout: 20000,
    });
    const updatedRow = sourceRow(page, name);
    await expect(
      updatedRow.locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(updated, { timeout: 20000 });
    await expect(updatedRow.locator("[data-testid^='design-content-list-type-']")).toHaveText(
      "modern",
    );
    await expect(updatedRow.locator("[data-testid^='design-content-list-source-']")).toHaveText(
      GENERATOR,
    );
    await expect(updatedRow.locator("[data-testid^='design-content-list-filter-']")).toHaveText(
      filter.label,
    );

    await sourceRow(page, name)
      .locator("[data-testid^='design-content-list-describe-']")
      .click();
    await page.locator("#contentlist-description-input").fill("   ");
    const clearResponse = page.waitForResponse(
      (res) => contentListUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("contentlist-description-save").click();
    const cleared = await clearResponse;
    expect(cleared.request().postData() || "").toContain('"description":""');
    expect(cleared.request().postData() || "").not.toContain(GENERATOR);
    expect(cleared.request().postData() || "").not.toContain("itemFilterId");
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText("");
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-source-']"),
    ).toHaveText(GENERATOR);
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-filter-']"),
    ).toHaveText(filter.label);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("legacy URL stays when only the description is saved", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContentLists(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `ClLeg${stamp}`;
    const url = `/Rhythmyx/night-legacy-${stamp}`;
    const original = `Legacy description ${stamp}`;
    const updated = `Legacy updated ${stamp}`;
    await createLegacyList(page, name, original, url);

    const row = sourceRow(page, name);
    await expect(row.locator("[data-testid^='design-content-list-type-']")).toHaveText(
      "legacy",
    );
    await expect(row.locator("[data-testid^='design-content-list-source-']")).toHaveText(url);
    await row.locator("[data-testid^='design-content-list-describe-']").click();
    await expect(page.getByTestId("contentlist-description-source")).toHaveText(url);
    await page.locator("#contentlist-description-input").fill(updated);
    const putResponse = page.waitForResponse(
      (res) => contentListUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("contentlist-description-save").click();
    const posted = await putResponse;
    const body = posted.request().postData() || "";
    expect(body).toContain(updated);
    expect(body).not.toContain(name);
    expect(body).not.toContain(url);
    expect(body).not.toContain('"url"');
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(updated, { timeout: 20000 });
    await expect(sourceRow(page, name).locator("[data-testid^='design-content-list-source-']")).toHaveText(
      url,
    );
    await expect(sourceRow(page, name).locator("[data-testid^='design-content-list-type-']")).toHaveText(
      "legacy",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old description", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContentLists(page);
    const name = `ClErr${Date.now().toString().slice(-8)}`;
    const description = "kept on error";
    const filter = await createModernList(page, name, description);
    await sourceRow(page, name)
      .locator("[data-testid^='design-content-list-describe-']")
      .click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();

    const cases = [
      [
        400,
        "Content list description must be 255 characters or fewer",
        /255 characters or fewer|400|Bad Request/i,
      ],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Content list name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/contentlists\/[^/]+(?:\?|$)/,
        (route) => {
          if (route.request().method() !== "PUT") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await page.locator("#contentlist-description-input").fill(`WillNotSave${status}`);
      await page.getByTestId("contentlist-description-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
      await expect(page.getByTestId("contentlist-description-name")).toHaveText(name);
      await expect(page.getByTestId("contentlist-description-source")).toHaveText(GENERATOR);
      await expect(page.getByTestId("contentlist-description-filter")).toHaveText(filter.label);
    }
    await page.getByTestId("contentlist-description-cancel").click();
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(description);
    await expect(sourceRow(page, name).locator("[data-testid^='design-content-list-source-']")).toHaveText(
      GENERATOR,
    );
    await expect(sourceRow(page, name).locator("[data-testid^='design-content-list-filter-']")).toHaveText(
      filter.label,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
