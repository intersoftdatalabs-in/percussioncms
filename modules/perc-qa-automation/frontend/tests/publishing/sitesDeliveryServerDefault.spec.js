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
 * PublishingShell Sites — mark a delivery server as default (#5503 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDefault.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

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

test.describe("PublishingShell Sites delivery-server default", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("marks one server as default and does not write cancel or already-default", async ({
    page,
  }) => {
    test.setTimeout(90000);
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (serverPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const targetName = `Loc${stamp}`;
    const siblingName = `Oth${stamp}`;
    await addLocalServer(page, targetName);
    await addLocalServer(page, siblingName);
    await page.getByRole("button", { name: targetName, exact: true }).click();

    await page.getByTestId("delivery-server-default-open").click();
    await expect(page.getByTestId("delivery-server-default-form")).toBeVisible();
    await expect(page.getByTestId("delivery-server-default-name")).toHaveText(
      targetName,
    );
    await expect(page.getByTestId("delivery-server-default-driver")).toHaveText(
      /Local/i,
    );
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "No",
    );
    const folderBefore = await page
      .getByTestId("delivery-server-default-folder")
      .innerText();
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-server-default-cancel").click();
    await expect(page.getByTestId("delivery-server-default-form")).toHaveCount(0);
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "No",
    );
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("delivery-server-default-open").click();
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
    await page.getByTestId("delivery-server-default-save").click();
    await expect.poll(() => postedBody).toContain("isDefault");
    expect(postedBody).toContain("true");
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("driver");
    expect(postedBody).not.toContain("serverName");
    expect(postedBody).not.toContain("folder");
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "No",
    );
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `mark default delivery server HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("delivery-server-default-form")).toHaveCount(0, {
      timeout: 20000,
    });
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "Yes",
      { timeout: 20000 },
    );
    await expect(page.getByTestId("delivery-server-default-name")).toHaveText(
      targetName,
    );
    await expect(page.getByTestId("delivery-server-default-driver")).toHaveText(
      /Local/i,
    );
    await expect(page.getByTestId("delivery-server-default-folder")).toHaveText(
      folderBefore,
    );
    await expect(
      page.getByRole("button", { name: targetName }).locator(
        "[data-testid='publish-server-default-badge']",
      ),
    ).toBeVisible({ timeout: 20000 });
    await expect(
      page.getByRole("button", { name: siblingName, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: siblingName, exact: true }).locator(
        "[data-testid='publish-server-default-badge']",
      ),
    ).toHaveCount(0);
    await expect(page.getByTestId("publish-server-default-badge")).toHaveCount(1);

    const putsAfterSuccess = putBodies.length;
    await expect(page.getByTestId("delivery-server-default-open")).toBeDisabled();
    expect(putBodies.length).toBe(putsAfterSuccess);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous delivery-server default", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const originalName = `Err${stamp}`;
    await addLocalServer(page, originalName);
    await page.getByRole("button", { name: originalName, exact: true }).click();
    await page.getByTestId("delivery-server-default-open").click();
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "No",
    );

    const cases = [
      [
        400,
        "Default flag must be true to mark a delivery server as default.",
        /must be true|400|Bad Request/i,
      ],
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
      await page.getByTestId("delivery-server-default-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("delivery-server-default-form")).toBeVisible();
      await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
        "No",
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("delivery-server-default-cancel").click();
    await expect(page.getByTestId("delivery-server-default-flag")).toHaveText(
      "No",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
