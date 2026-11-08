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
 * PublishingShell Sites — set the SFTP delivery-server passphrase (#5764 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerSftpPassphrase.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live SFTP connection, so the SFTP passphrase path overlays
 * GET of a created Local server. The Local-only path is live.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const HOST = "old.example";
const PORT = "22";
const USER = "sftp-user";
const FOLDER = "/kept/folder";
const PASSPHRASE = "sftp-passphrase";
const PRIVATE_KEY = "stored-private-key";
const NEW_PASSPHRASE = "new-sftp-passphrase";
const NEW_PASSPHRASE_B64 = Buffer.from(NEW_PASSPHRASE).toString("base64");
const SERVER_NAME = "sftp-prod";

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

function singleServerPath(url) {
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

function sftpPayload() {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "File",
    isDefault: false,
    properties: [
      { key: "driver", value: "SFTP" },
      { key: "serverip", value: HOST },
      { key: "port", value: PORT },
      { key: "userid", value: USER },
      { key: "folder", value: FOLDER },
      { key: "password", value: PASSPHRASE },
      { key: "privateKey", value: PRIVATE_KEY },
    ],
  };
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

test.describe("PublishingShell Sites SFTP delivery-server passphrase", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets an SFTP passphrase only and never renders the secret", async ({
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
    await page.getByTestId("sftp-passphrase-open").click();
    await expect(page.getByTestId("sftp-passphrase-not-sftp")).toBeVisible();
    await expect(page.getByTestId("sftp-passphrase-not-sftp")).toContainText(
      /SFTP delivery server/i,
    );
    await expect(page.getByTestId("sftp-passphrase-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PRIVATE_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PASSPHRASE,
    );

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(sftpPayload()),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("sftp-passphrase-open").click();
    await expect(page.getByTestId("sftp-passphrase-form")).toBeVisible();
    await expect(page.getByTestId("sftp-passphrase-host")).toHaveText(HOST);
    await expect(page.getByTestId("sftp-passphrase-port")).toHaveText(PORT);
    await expect(page.getByTestId("sftp-passphrase-user")).toHaveText(USER);
    await expect(page.getByTestId("sftp-passphrase-folder")).toHaveText(FOLDER);
    await expect(page.getByTestId("sftp-passphrase-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("sftp-passphrase-driver")).toHaveText("SFTP");
    await expect(page.getByTestId("sftp-passphrase-status")).toContainText(
      /never shown/i,
    );
    await expect(page.getByTestId("sftp-passphrase-input")).toHaveValue("");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PRIVATE_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PASSPHRASE,
    );

    await page.getByTestId("sftp-passphrase-input").fill(NEW_PASSPHRASE);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("sftp-passphrase-cancel").click();
    await expect(page.getByTestId("sftp-passphrase-form")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      NEW_PASSPHRASE,
    );
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("sftp-passphrase-open").click();
    await expect(page.getByTestId("sftp-passphrase-input")).toHaveValue("");
    await page.getByTestId("sftp-passphrase-input").fill("   ");
    await page.getByTestId("sftp-passphrase-save").click();
    await expect(page.getByRole("alert")).toContainText(/SFTP passphrase is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("sftp-passphrase-input").fill("p".repeat(256));
    await page.getByTestId("sftp-passphrase-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("sftp-passphrase-input").fill(NEW_PASSPHRASE);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(sftpPayload()),
        });
      }
      if (method !== "PUT") {
        return route.continue();
      }
      postedBody = route.request().postData() || "";
      await holdPut;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(sftpPayload()),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("sftp-passphrase-save").click();
    await expect.poll(() => postedBody).toContain(NEW_PASSPHRASE_B64);
    expect(postedBody).not.toContain(NEW_PASSPHRASE);
    expect(postedBody).not.toContain(PRIVATE_KEY);
    expect(postedBody).not.toContain(PASSPHRASE);
    expect(postedBody).not.toContain(HOST);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain(FOLDER);
    expect(postedBody).not.toContain("privateKey");
    expect(postedBody).not.toContain("serverip");
    await expect(page.getByTestId("sftp-passphrase-form")).toBeVisible();
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `sftp passphrase HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("sftp-passphrase-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("sftp-passphrase-input")).toHaveCount(0);
    await expect(page.getByTestId("sftp-passphrase-host")).toHaveText(HOST);
    await expect(page.getByTestId("sftp-passphrase-port")).toHaveText(PORT);
    await expect(page.getByTestId("sftp-passphrase-user")).toHaveText(USER);
    await expect(page.getByTestId("sftp-passphrase-folder")).toHaveText(FOLDER);
    await expect(page.getByTestId("sftp-passphrase-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("sftp-passphrase-driver")).toHaveText("SFTP");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PRIVATE_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      NEW_PASSPHRASE,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous SFTP passphrase", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `SFTs${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(sftpPayload()),
        });
      }
      return route.continue();
    });
    await page.getByTestId("sftp-passphrase-open").click();
    await expect(page.getByTestId("sftp-passphrase-host")).toHaveText(HOST);

    const cases = [
      [400, "SFTP passphrase is required.", /SFTP passphrase is required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publish server name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
        if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(sftpPayload()),
          });
        }
        if (route.request().method() !== "PUT") {
          return route.continue();
        }
        return route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ message }),
        });
      });
      await page.getByTestId("sftp-passphrase-input").fill(`phrase${status}`);
      await page.getByTestId("sftp-passphrase-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("sftp-passphrase-form")).toBeVisible();
      await expect(page.getByTestId("sftp-passphrase-host")).toHaveText(HOST);
      await expect(page.getByTestId("sftp-passphrase-port")).toHaveText(PORT);
      await expect(page.getByTestId("sftp-passphrase-user")).toHaveText(USER);
      await expect(page.getByTestId("sftp-passphrase-folder")).toHaveText(FOLDER);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("sftp-passphrase-cancel").click();
    await expect(page.getByTestId("sftp-passphrase-host")).toHaveText(HOST);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      PRIVATE_KEY,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
