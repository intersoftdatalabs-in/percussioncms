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
 * PublishingShell Design — delete one unused delivery type (#5221 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designDeliveryTypeDelete.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");

const BEAN = "sys_fileDeliveryHandler";

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

function isDeliveryTypeDelete(url, method) {
  if (method !== "DELETE") {
    return false;
  }
  try {
    return /\/publishingdesign\/deliverytypes\/[^/]+$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
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

async function createDeliveryType(page, name) {
  await page.getByTestId("design-add-delivery-type").click();
  await expect(page.getByTestId("delivery-type-editor")).toBeVisible();
  await page.locator("#dt-name").fill(name);
  await page.locator("#dt-bean").fill(BEAN);
  await page.locator("#dt-desc").fill(`night-issue-prs #5221 ${name}`);
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

/**
 * A content list whose URL names this delivery type. Create posts a
 * {@code contentList} root; a flat body does not bind under UNWRAP_ROOT_VALUE.
 */
async function seedContentListUsingType(request, listName, deliveryTypeName) {
  const res = await request.post(
    `${BASE_URL}/Rhythmyx/services/sitemanage/publishingdesign/contentlists`,
    {
      headers: {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      data: {
        contentList: {
          name: listName,
          url: `/Rhythmyx/contentlist?sys_deliverytype=${encodeURIComponent(deliveryTypeName)}&sys_contentlist=${encodeURIComponent(listName)}`,
        },
      },
    },
  );
  if (res.status() !== 200 && res.status() !== 201) {
    throw new Error(
      `seed content list HTTP ${res.status()} ${(await res.text()).slice(0, 400)}`,
    );
  }
}

test.describe("PublishingShell Design delete a delivery type", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("deletes an unused delivery type only after confirm succeeds", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const name = `DtDel${Date.now().toString().slice(-8)}`;
    await createDeliveryType(page, name);
    const row = sourceRow(page, name);
    await expect(row.getByTestId("delivery-type-delete")).toBeVisible();

    let releaseDelete = () => undefined;
    const holdDelete = new Promise((resolve) => {
      releaseDelete = resolve;
    });
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/deliverytypes\/[^/]+(?:\?|$)/,
      async (route) => {
        if (route.request().method() !== "DELETE") {
          return route.continue();
        }
        await holdDelete;
        return route.continue();
      },
    );
    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(/Delete this design object/i);
      await dialog.accept();
    });
    const deleteResponse = page.waitForResponse(
      (res) => isDeliveryTypeDelete(res.url(), res.request().method()),
      { timeout: 30000 },
    );
    await row.getByTestId("delivery-type-delete").click();
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    releaseDelete();
    const deleted = await deleteResponse;
    if (deleted.status() !== 200 && deleted.status() !== 204) {
      throw new Error(
        `delete HTTP ${deleted.status()} ${deleted.url()} ${(await deleted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0);
    await expect(
      page.getByRole("button", { name: "filesystem", exact: true }),
    ).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not call the server", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const name = `DtKeep${Date.now().toString().slice(-8)}`;
    await createDeliveryType(page, name);
    let deletes = 0;
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/deliverytypes\/[^/]+(?:\?|$)/,
      (route) => {
        if (route.request().method() === "DELETE") {
          deletes += 1;
          return route.abort();
        }
        return route.continue();
      },
    );
    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await sourceRow(page, name).getByTestId("delivery-type-delete").click();
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    expect(deletes).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps a delivery type that a content list still names", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const name = `DtUse${Date.now().toString().slice(-8)}`;
    await createDeliveryType(page, name);
    await seedContentListUsingType(request, `Cl${name}`, name);

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.accept();
    });
    const deleteResponse = page.waitForResponse(
      (res) => isDeliveryTypeDelete(res.url(), res.request().method()),
      { timeout: 30000 },
    );
    await sourceRow(page, name).getByTestId("delivery-type-delete").click();
    const denied = await deleteResponse;
    expect(denied.status()).toBe(409);
    await expect(page.getByRole("alert")).toContainText(/in use/i);
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("surfaces HTTP 400, 403, and 409 without removing the delivery type", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDeliveryTypes(page);
    const name = `DtErr${Date.now().toString().slice(-8)}`;
    await createDeliveryType(page, name);

    async function stubDelete(status, message) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/deliverytypes\/[^/]+(?:\?|$)/,
        (route) => {
          if (route.request().method() !== "DELETE") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      page.once("dialog", (dialog) => dialog.accept());
      await sourceRow(page, name).getByTestId("delivery-type-delete").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    }

    await stubDelete(400, "deliveryTypeId is required");
    await stubDelete(403, "Admin or Designer role required");
    await stubDelete(409, "Delivery type is in use");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
