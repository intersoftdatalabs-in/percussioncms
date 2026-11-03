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
 * PublishingShell Sites — rename a delivery server (#5502 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerRename.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const UPDATED_SUFFIX = "Rn";

function serverPut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishmanagement\/servers\/[^/]+\/[^/]+$/.test(new URL(url).pathname);
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

async function openFirstSite(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-sites")).toBeVisible();
  const cards = page.locator(
    "[data-testid='publish-section-sites'] [role='listitem']",
  );
  await expect(cards.first()).toBeVisible({ timeout: 30000 });
  await cards.first().click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
}

async function saveServer(page) {
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishmanagement\/servers\//.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("publish-server-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create delivery server HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
    );
  }
  await expect(page.getByTestId("publish-server-editor")).toBeHidden({
    timeout: 20000,
  });
}

async function addLocalServer(page, name) {
  await page.getByTestId("publish-add-server").click();
  await expect(page.getByTestId("publish-server-editor")).toBeVisible();
  await page.locator("#serverName").fill(name);
  await saveServer(page);
}

test.describe("PublishingShell Sites delivery-server rename", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one server and does not write a blank or duplicate name", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (serverPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const originalName = `Loc${stamp}`;
    const siblingName = `Oth${stamp}`;
    const renamed = `${UPDATED_SUFFIX}${stamp}`;
    await addLocalServer(page, originalName);
    await addLocalServer(page, siblingName);
    await page.getByRole("button", { name: originalName, exact: true }).click();

    await page.getByTestId("delivery-server-rename-open").click();
    await expect(page.getByTestId("delivery-server-rename-form")).toBeVisible();
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );
    await expect(page.getByTestId("delivery-server-rename-driver")).toHaveText(
      /Local/i,
    );
    const folderBefore = await page
      .getByTestId("delivery-server-rename-folder")
      .innerText();
    const defaultBefore = await page
      .getByTestId("delivery-server-rename-default")
      .innerText();

    await page.getByTestId("delivery-server-rename-input").fill("will-not-save");
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-server-rename-cancel").click();
    await expect(page.getByTestId("delivery-server-rename-form")).toHaveCount(0);
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("delivery-server-rename-open").click();
    await page.getByTestId("delivery-server-rename-input").fill("   ");
    await page.getByTestId("delivery-server-rename-save").click();
    await expect(page.getByRole("alert")).toContainText(/required field/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );

    await page.getByTestId("delivery-server-rename-input").fill(siblingName);
    await page.getByTestId("delivery-server-rename-save").click();
    await expect(page.getByRole("alert")).toContainText(/already exists/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("delivery-server-rename-input").fill(renamed);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      if (route.request().method() !== "PUT") {
        return route.continue();
      }
      postedBody = route.request().postData() || "";
      await holdPut;
      return route.continue();
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("delivery-server-rename-save").click();
    await expect.poll(() => postedBody).toContain(renamed);
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("driver");
    expect(postedBody).not.toContain("isDefault");
    expect(postedBody).not.toContain("folder");
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `rename delivery server HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("delivery-server-rename-form")).toHaveCount(0, {
      timeout: 20000,
    });
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      renamed,
      { timeout: 20000 },
    );
    await expect(page.getByTestId("delivery-server-rename-driver")).toHaveText(
      /Local/i,
    );
    await expect(page.getByTestId("delivery-server-rename-folder")).toHaveText(
      folderBefore,
    );
    await expect(page.getByTestId("delivery-server-rename-default")).toHaveText(
      defaultBefore,
    );
    await expect(
      page.getByRole("button", { name: renamed, exact: true }),
    ).toBeVisible({ timeout: 20000 });
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous delivery-server name", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const originalName = `Err${stamp}`;
    await addLocalServer(page, originalName);
    await page.getByRole("button", { name: originalName, exact: true }).click();
    await page.getByTestId("delivery-server-rename-open").click();
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );

    const cases = [
      [400, "Name is a required field.", /required field|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
        if (route.request().method() !== "PUT") {
          return route.continue();
        }
        return route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ message }),
        });
      });
      await page.getByTestId("delivery-server-rename-input").fill(`NoSave${status}`);
      await page.getByTestId("delivery-server-rename-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("delivery-server-rename-form")).toBeVisible();
      await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
        originalName,
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-server-rename-cancel").click();
    await expect(page.getByTestId("delivery-server-rename-current")).toHaveText(
      originalName,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
