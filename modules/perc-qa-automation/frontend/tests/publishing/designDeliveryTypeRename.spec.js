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
 * PublishingShell Design — rename one delivery type (#5203 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designDeliveryTypeRename.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const BEAN = "sys_fileDeliveryHandler";

function deliveryTypeUpdatePut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/deliverytypes\/[^/]+$/.test(new URL(url).pathname);
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

async function openDeliveryTypes(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Delivery types/i }).click();
  await expect(page.getByTestId("delivery-types-panel")).toBeVisible();
}

async function createDeliveryType(page, name, description) {
  await page.getByTestId("design-add-delivery-type").click();
  await expect(page.getByTestId("delivery-type-editor")).toBeVisible();
  await page.locator("#dt-name").fill(name);
  await page.locator("#dt-bean").fill(BEAN);
  await page.locator("#dt-desc").fill(description);
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/deliverytypes(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("delivery-type-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create delivery type HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("delivery-type-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

function sourceRow(page, name) {
  return page
    .getByRole("listitem")
    .filter({ has: page.getByRole("button", { name, exact: true }) });
}

test.describe("PublishingShell Design rename delivery type", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one delivery type only after save and keeps the bean name", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (deliveryTypeUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openDeliveryTypes(page);
    const stamp = Date.now().toString().slice(-8);
    const original = `DtRen${stamp}`;
    const renamed = `DtNew${stamp}`;
    const description = `Night rename source ${stamp}`;
    await createDeliveryType(page, original, description);

    const row = sourceRow(page, original);
    await expect(row.locator("[data-testid^='delivery-type-bean-']")).toHaveText(BEAN);
    await row.getByTestId("delivery-type-rename").click();
    await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();
    await expect(page.getByTestId("delivery-type-rename-bean")).toHaveText(BEAN);
    await expect(page.getByTestId("delivery-type-rename-description")).toHaveText(
      description,
    );
    await expect(page.locator("#delivery-type-rename-name")).toHaveValue(original);

    await page.locator("#delivery-type-rename-name").fill(renamed);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-type-rename-cancel").click();
    await expect(page.getByTestId("delivery-type-rename-form")).toBeHidden();
    await expect(page.getByRole("button", { name: original, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: renamed, exact: true })).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await sourceRow(page, original).getByTestId("delivery-type-rename").click();
    await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();
    await page.locator("#delivery-type-rename-name").fill("   ");
    await page.getByTestId("delivery-type-rename-submit").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#delivery-type-rename-name").fill("n".repeat(51));
    await page.getByTestId("delivery-type-rename-submit").click();
    await expect(page.getByRole("alert")).toContainText(/50 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();

    await page.locator("#delivery-type-rename-name").fill(renamed);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/deliverytypes\/[^/]+(?:\?|$)/,
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
      (res) => deliveryTypeUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("delivery-type-rename-submit").click();
    await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();
    await expect.poll(() => postedBody).toContain(renamed);
    expect(postedBody).not.toContain(BEAN);
    expect(postedBody).not.toContain("description");
    expect(postedBody).not.toContain("unpublishingRequiresAssembly");
    await expect(page.getByRole("button", { name: renamed, exact: true })).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `rename delivery type HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("delivery-type-rename-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name: renamed, exact: true })).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name: original, exact: true })).toHaveCount(0);
    const renamedRow = sourceRow(page, renamed);
    await expect(renamedRow.locator("[data-testid^='delivery-type-bean-']")).toHaveText(
      BEAN,
    );

    await page.getByRole("button", { name: renamed, exact: true }).click();
    await expect(page.getByTestId("delivery-type-editor")).toBeVisible();
    await expect(page.locator("#dt-name")).toHaveValue(renamed);
    await expect(page.locator("#dt-bean")).toHaveValue(BEAN);
    await expect(page.locator("#dt-desc")).toHaveValue(description);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old name", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const source = `DtErr${Date.now().toString().slice(-8)}`;
    const description = "kept on error";
    await createDeliveryType(page, source, description);
    await sourceRow(page, source).getByTestId("delivery-type-rename").click();
    await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();

    const cases = [
      [400, "Delivery type name must be 50 characters or fewer", /50 characters or fewer|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Delivery type name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/deliverytypes\/[^/]+(?:\?|$)/,
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
      const requested = `WillNotRename${status}`;
      await page.locator("#delivery-type-rename-name").fill(requested);
      await page.getByTestId("delivery-type-rename-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("delivery-type-rename-form")).toBeVisible();
      await expect(page.getByTestId("delivery-type-rename-bean")).toHaveText(BEAN);
      await expect(page.getByTestId("delivery-type-rename-description")).toHaveText(
        description,
      );
      await expect(page.getByRole("button", { name: requested, exact: true })).toHaveCount(
        0,
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-type-rename-cancel").click();
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    await expect(sourceRow(page, source).locator("[data-testid^='delivery-type-bean-']")).toHaveText(
      BEAN,
    );
    await expect(page.getByRole("button", { name: /WillNotRename/ })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
