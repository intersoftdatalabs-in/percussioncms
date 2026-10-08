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
 * PublishingShell Design — set one delivery type unpublish assembly flag (#5386 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designDeliveryTypeAssembly.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const ORIGINAL_BEAN = "sys_fileDeliveryHandler";

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
  await page.locator("#dt-bean").fill(ORIGINAL_BEAN);
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

test.describe("PublishingShell Design delivery type unpublish assembly", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("saves the flag only after reload and leaves name, description, and bean", async ({
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
    const name = `DtAsm${stamp}`;
    const description = `Kept notes ${stamp}`;
    await createDeliveryType(page, name, description);

    const row = sourceRow(page, name);
    await expect(row.locator("[data-testid^='delivery-type-assembly-']")).toHaveText("No");
    await expect(row.locator("[data-testid^='delivery-type-bean-']")).toHaveText(ORIGINAL_BEAN);
    await expect(row.locator("[data-testid^='delivery-type-description-']")).toHaveText(
      description,
    );
    await row.getByTestId("delivery-type-set-assembly").click();
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeVisible();
    await expect(page.getByTestId("delivery-type-assembly-form-name")).toHaveText(name);
    await expect(page.getByTestId("delivery-type-assembly-form-bean")).toHaveText(ORIGINAL_BEAN);
    await expect(page.getByTestId("delivery-type-assembly-form-description")).toHaveText(
      description,
    );
    await expect(page.getByTestId("delivery-type-assembly-flag")).not.toBeChecked();

    await page.getByTestId("delivery-type-assembly-flag").check();
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-type-assembly-cancel").click();
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeHidden();
    await expect(
      sourceRow(page, name).locator("[data-testid^='delivery-type-assembly-']"),
    ).toHaveText("No");
    expect(putBodies.length).toBe(putsBeforeCancel);

    await sourceRow(page, name).getByTestId("delivery-type-set-assembly").click();
    await page.getByTestId("delivery-type-assembly-flag").check();
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
    await page.getByTestId("delivery-type-assembly-submit").click();
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeVisible();
    await expect.poll(() => postedBody).toContain('"unpublishingRequiresAssembly":true');
    expect(postedBody).not.toContain(name);
    expect(postedBody).not.toContain(description);
    expect(postedBody).not.toContain(ORIGINAL_BEAN);
    expect(postedBody).not.toContain('"name"');
    expect(postedBody).not.toContain('"beanName"');
    expect(postedBody).not.toContain('"description"');
    await expect(sourceRow(page, name)).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `assembly delivery type HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeHidden({
      timeout: 20000,
    });
    const updatedRow = sourceRow(page, name);
    await expect(updatedRow.locator("[data-testid^='delivery-type-assembly-']")).toHaveText(
      "Yes",
      { timeout: 20000 },
    );
    await expect(updatedRow.locator("[data-testid^='delivery-type-bean-']")).toHaveText(
      ORIGINAL_BEAN,
    );
    await expect(updatedRow.locator("[data-testid^='delivery-type-description-']")).toHaveText(
      description,
    );
    await expect(updatedRow.getByRole("button", { name, exact: true })).toBeVisible();

    await updatedRow.getByTestId("delivery-type-set-assembly").click();
    await expect(page.getByTestId("delivery-type-assembly-flag")).toBeChecked();
    await page.getByTestId("delivery-type-assembly-flag").uncheck();
    const offResponse = page.waitForResponse(
      (res) => deliveryTypeUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("delivery-type-assembly-submit").click();
    const offPosted = await offResponse;
    const offBody = offPosted.request().postData() || "";
    expect(offBody).toContain('"unpublishingRequiresAssembly":false');
    expect(offBody).not.toContain(name);
    if (offPosted.status() !== 200 && offPosted.status() !== 201) {
      throw new Error(
        `clear assembly flag HTTP ${offPosted.status()} ${(await offPosted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      sourceRow(page, name).locator("[data-testid^='delivery-type-assembly-']"),
    ).toHaveText("No", { timeout: 20000 });
    await expect(sourceRow(page, name).locator("[data-testid^='delivery-type-bean-']")).toHaveText(
      ORIGINAL_BEAN,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old flag", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const name = `DtAerr${Date.now().toString().slice(-8)}`;
    const description = "kept on error";
    await createDeliveryType(page, name, description);
    await sourceRow(page, name).getByTestId("delivery-type-set-assembly").click();
    await expect(page.getByTestId("delivery-type-assembly-form")).toBeVisible();
    await expect(page.getByTestId("delivery-type-assembly-flag")).not.toBeChecked();

    const cases = [
      [400, "Bad Request", /Bad Request|400/i],
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
      await page.getByTestId("delivery-type-assembly-flag").check();
      await page.getByTestId("delivery-type-assembly-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("delivery-type-assembly-form")).toBeVisible();
      await expect(page.getByTestId("delivery-type-assembly-form-name")).toHaveText(name);
      await expect(page.getByTestId("delivery-type-assembly-form-bean")).toHaveText(ORIGINAL_BEAN);
      await expect(page.getByTestId("delivery-type-assembly-form-description")).toHaveText(
        description,
      );
      await page.getByTestId("delivery-type-assembly-flag").uncheck();
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-type-assembly-cancel").click();
    await expect(
      sourceRow(page, name).locator("[data-testid^='delivery-type-assembly-']"),
    ).toHaveText("No");
    await expect(sourceRow(page, name).locator("[data-testid^='delivery-type-bean-']")).toHaveText(
      ORIGINAL_BEAN,
    );
    await expect(
      sourceRow(page, name).locator("[data-testid^='delivery-type-description-']"),
    ).toHaveText(description);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
