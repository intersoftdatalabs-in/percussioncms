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
 * PublishingShell Sites — delivery-server connection test (#5886 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerConnectionTest.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PORT = "21";
const USER = "nightftp";
const PASSWORD = "night-ftp-conn-pw";
const FOLDER = "/night-ftp-conn";
const ORIGINAL_HOST = "ftp-conn-old.example";

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

function testConnectionPost(url, method) {
  if (method !== "POST") {
    return false;
  }
  try {
    return /\/publishmanagement\/servers\/testConnection\/[^/]+\/[^/]+$/.test(
      new URL(url).pathname,
    );
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

test.describe("PublishingShell Sites delivery-server connection test", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("tests the connection and shows success or failure without saving", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    const testPosts = [];
    page.on("request", (req) => {
      if (serverPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
      if (testConnectionPost(req.url(), req.method())) {
        testPosts.push(req.postData() || "");
      }
    });

    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const ftpName = `FtpConn${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeVisible();
    await page.locator("#serverName").fill(ftpName);
    await page.locator("#driver").selectOption("FTP");
    await page.locator("#drv-serverip").fill(ORIGINAL_HOST);
    await page.locator("#drv-port").fill(PORT);
    await page.locator("#drv-userid").fill(USER);
    await page.locator("#drv-password").fill(PASSWORD);
    await page.locator("#drv-folder").fill(FOLDER);
    await saveServer(page);
    await page.getByRole("button", { name: ftpName, exact: true }).click();
    await expect(page.getByTestId("delivery-connection-test-panel")).toBeVisible();

    const putsBefore = putBodies.length;
    const postsBefore = testPosts.length;

    // Failure path: the stored server cannot answer.
    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.route(
      /\/publishmanagement\/servers\/testConnection\/[^/]+\/[^/]+(?:\?|$)/,
      (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ connected: false }),
        }),
    );
    await page.getByTestId("delivery-connection-test-button").click();
    await expect(page.getByTestId("delivery-connection-test-notice")).toContainText(
      /failed/i,
      { timeout: 20000 },
    );
    expect(testPosts.length).toBe(postsBefore + 1);
    expect(testPosts[testPosts.length - 1]).not.toContain(PASSWORD);
    expect(testPosts[testPosts.length - 1]).not.toContain(ORIGINAL_HOST);
    expect(putBodies.length).toBe(putsBefore);

    // Success path: the stored server answers.
    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.route(
      /\/publishmanagement\/servers\/testConnection\/[^/]+\/[^/]+(?:\?|$)/,
      (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ connected: true }),
        }),
    );
    await page.getByTestId("delivery-connection-test-button").click();
    await expect(page.getByTestId("delivery-connection-test-notice")).toContainText(
      /succeeded/i,
      { timeout: 20000 },
    );
    expect(putBodies.length).toBe(putsBefore);

    // Secrets are never rendered by the test flow.
    const bodyText = await page.locator("body").innerText();
    expect(bodyText).not.toContain(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 map to the save-error message", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const ftpName = `FtpConnE${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(ftpName);
    await page.locator("#driver").selectOption("FTP");
    await page.locator("#drv-serverip").fill(ORIGINAL_HOST);
    await page.locator("#drv-port").fill(PORT);
    await page.locator("#drv-userid").fill(USER);
    await page.locator("#drv-password").fill(PASSWORD);
    await page.locator("#drv-folder").fill(FOLDER);
    await saveServer(page);
    await page.getByRole("button", { name: ftpName, exact: true }).click();
    await expect(page.getByTestId("delivery-connection-test-panel")).toBeVisible();

    const cases = [
      [400, "Connection test failed validation.", /error|400|failed/i],
      [403, "Only Admin or Designer users may test.", /admin|designer|403|forbidden/i],
      [409, "Publish server name already exists", /already exists|409|conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/publishmanagement\/servers\/testConnection\/[^/]+\/[^/]+(?:\?|$)/,
        (route) =>
          route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          }),
      );
      await page.getByTestId("delivery-connection-test-button").click();
      await expect(page.getByTestId("delivery-connection-test-error")).toContainText(
        pattern,
        { timeout: 20000 },
      );
      await expect(page.getByTestId("delivery-connection-test-notice")).toHaveCount(0);
    }
    const bodyText = await page.locator("body").innerText();
    expect(bodyText).not.toContain(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
