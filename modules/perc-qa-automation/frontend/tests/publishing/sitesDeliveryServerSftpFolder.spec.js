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
 * PublishingShell Sites — set the SFTP delivery-server folder (#5763 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerSftpFolder.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const HOST = "sftp.example";
const PORT = "2121";
const USER = "sftpseed";
const FOLDER = "/night-sftp";
const UPDATED_FOLDER = "/remote-new";
const OVERLONG_FOLDER = "/".repeat(256);

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

test.describe("PublishingShell Sites SFTP delivery-server folder", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets an SFTP folder only and does not write a Local folder", async ({
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
    const localName = `Loc${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeVisible();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();
    const putsBeforeLocal = putBodies.length;
    await page.getByTestId("sftp-folder-open").click();
    await expect(page.getByTestId("sftp-folder-not-sftp")).toBeVisible();
    await expect(page.getByTestId("sftp-folder-not-sftp")).toContainText(/SFTP delivery server/i);
    await expect(page.getByTestId("sftp-folder-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);

    const sftpName = `Sftp${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeVisible();
    await page.locator("#serverName").fill(sftpName);
    await page.locator("#driver").selectOption("SFTP");
    await page.locator("#drv-serverip").fill(HOST);
    await page.locator("#drv-port").fill(PORT);
    await page.locator("#drv-userid").fill(USER);
    await page.locator("#drv-folder").fill(FOLDER);
    await saveServer(page);

    await page.getByRole("button", { name: sftpName, exact: true }).click();
    await page.getByTestId("sftp-folder-open").click();
    await expect(page.getByTestId("sftp-folder-form")).toBeVisible();
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);
    await expect(page.getByTestId("sftp-folder-host")).toHaveText(HOST);
    await expect(page.getByTestId("sftp-folder-port")).toHaveText(PORT);
    await expect(page.getByTestId("sftp-folder-user")).toHaveText(USER);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText("secret-passphrase");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText("stored-private-key");

    await page.getByTestId("sftp-folder-input").fill(UPDATED_FOLDER);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("sftp-folder-cancel").click();
    await expect(page.getByTestId("sftp-folder-form")).toHaveCount(0);
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("sftp-folder-open").click();
    await page.getByTestId("sftp-folder-input").fill(OVERLONG_FOLDER);
    await page.getByTestId("sftp-folder-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);

    await page.getByTestId("sftp-folder-input").fill(`  ${UPDATED_FOLDER}  `);
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
    await page.getByTestId("sftp-folder-save").click();
    await expect.poll(() => postedBody).toContain(UPDATED_FOLDER);
    expect(postedBody).not.toContain(FOLDER);
    expect(postedBody).not.toContain(HOST);
    expect(postedBody).not.toContain(PORT);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("privateKey");
    expect(postedBody).not.toContain("serverip");
    expect(postedBody).not.toContain("userid");
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `sftp folder HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("sftp-folder-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(UPDATED_FOLDER, {
      timeout: 20000,
    });
    await expect(page.getByTestId("sftp-folder-host")).toHaveText(HOST);
    await expect(page.getByTestId("sftp-folder-port")).toHaveText(PORT);
    await expect(page.getByTestId("sftp-folder-user")).toHaveText(USER);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText("secret-passphrase");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText("stored-private-key");

    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.getByTestId("sftp-folder-open").click();
    await page.getByTestId("sftp-folder-input").fill("   ");
    const clearResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("sftp-folder-save").click();
    const cleared = await clearResponse;
    const clearedBody = cleared.request().postData() || "";
    expect(clearedBody).toContain('"folder"');
    expect(clearedBody).not.toContain(UPDATED_FOLDER);
    expect(clearedBody).not.toContain(HOST);
    expect(clearedBody).not.toContain(USER);
    if (cleared.status() !== 200 && cleared.status() !== 201) {
      throw new Error(
        `clear sftp folder HTTP ${cleared.status()} ${(await cleared.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("sftp-folder-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("sftp-folder-current")).toHaveText("—", { timeout: 20000 });
    await expect(page.getByTestId("sftp-folder-host")).toHaveText(HOST);
    await expect(page.getByTestId("sftp-folder-port")).toHaveText(PORT);
    await expect(page.getByTestId("sftp-folder-user")).toHaveText(USER);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous SFTP folder", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const sftpName = `SftpE${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(sftpName);
    await page.locator("#driver").selectOption("SFTP");
    await page.locator("#drv-serverip").fill(HOST);
    await page.locator("#drv-port").fill(PORT);
    await page.locator("#drv-userid").fill(USER);
    await page.locator("#drv-folder").fill(FOLDER);
    await saveServer(page);
    await page.getByRole("button", { name: sftpName, exact: true }).click();
    await page.getByTestId("sftp-folder-open").click();
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);

    const cases = [
      [400, "SFTP folder must be 255 characters or fewer.", /255 characters or fewer|400|Bad Request/i],
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
      await page.getByTestId("sftp-folder-input").fill(`/will-not-save-${status}`);
      await page.getByTestId("sftp-folder-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("sftp-folder-form")).toBeVisible();
      await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);
      await expect(page.getByTestId("sftp-folder-host")).toHaveText(HOST);
      await expect(page.getByTestId("sftp-folder-port")).toHaveText(PORT);
      await expect(page.getByTestId("sftp-folder-user")).toHaveText(USER);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("sftp-folder-cancel").click();
    await expect(page.getByTestId("sftp-folder-current")).toHaveText(FOLDER);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
