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
 * PublishingShell Sites — set an MSSQL delivery-server owner (#5556 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDatabaseOwner.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live MSSQL connection, so the MSSQL owner path overlays
 * GET of a created Local server. The Local-only path is live.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PASSWORD = "NightSqlPw1";
const HOST = "sql-old.example";
const PORT = "1433";
const USER = "cmsdb";
const DATABASE = "rxdb";
const ORIGINAL_OWNER = "dbo";
const UPDATED_OWNER = "sa";
const SERVER_NAME = "sql-prod";

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

function mssqlPayload(owner) {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "Database",
    isDefault: false,
    properties: [
      { key: "driver", value: "MSSQL" },
      { key: "server", value: HOST },
      { key: "port", value: PORT },
      { key: "userid", value: USER },
      { key: "database", value: DATABASE },
      { key: "owner", value: owner },
      { key: "password", value: PASSWORD },
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

test.describe("PublishingShell Sites MSSQL delivery-server owner", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets an MSSQL owner only and does not write a Local folder", async ({
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
    await page.getByTestId("db-owner-open").click();
    await expect(page.getByTestId("db-owner-not-mssql")).toBeVisible();
    await expect(page.getByTestId("db-owner-not-mssql")).toContainText(
      /MSSQL delivery server/i,
    );
    await expect(page.getByTestId("db-owner-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    let overlayOwner = ORIGINAL_OWNER;
    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mssqlPayload(overlayOwner)),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("db-owner-open").click();
    await expect(page.getByTestId("db-owner-form")).toBeVisible();
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);
    await expect(page.getByTestId("db-owner-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-owner-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-owner-user")).toHaveText(USER);
    await expect(page.getByTestId("db-owner-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("db-owner-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-owner-driver")).toHaveText("MSSQL");
    await expect(page.getByTestId("db-owner-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    await page.getByTestId("db-owner-input").fill(UPDATED_OWNER);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-owner-cancel").click();
    await expect(page.getByTestId("db-owner-form")).toHaveCount(0);
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-owner-open").click();
    await page.getByTestId("db-owner-input").fill("   ");
    await page.getByTestId("db-owner-save").click();
    await expect(page.getByRole("alert")).toContainText(/MSSQL owner is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);

    await page.getByTestId("db-owner-input").fill(UPDATED_OWNER);
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
          body: JSON.stringify(mssqlPayload(overlayOwner)),
        });
      }
      if (method !== "PUT") {
        return route.continue();
      }
      postedBody = route.request().postData() || "";
      await holdPut;
      overlayOwner = UPDATED_OWNER;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mssqlPayload(UPDATED_OWNER)),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("db-owner-save").click();
    await expect.poll(() => postedBody).toContain(UPDATED_OWNER);
    expect(postedBody).not.toContain(PASSWORD);
    expect(postedBody).not.toContain(HOST);
    expect(postedBody).not.toContain(PORT);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain(DATABASE);
    expect(postedBody).not.toContain(ORIGINAL_OWNER);
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("userid");
    expect(postedBody).not.toContain("database");
    expect(postedBody).not.toContain("sid");
    expect(postedBody).not.toContain("serverip");
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `mssql owner HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("db-owner-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("db-owner-current")).toHaveText(UPDATED_OWNER, {
      timeout: 20000,
    });
    await expect(page.getByTestId("db-owner-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-owner-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-owner-user")).toHaveText(USER);
    await expect(page.getByTestId("db-owner-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("db-owner-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-owner-driver")).toHaveText("MSSQL");
    await expect(page.getByTestId("db-owner-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous MSSQL owner", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `DbO${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mssqlPayload(ORIGINAL_OWNER)),
        });
      }
      return route.continue();
    });
    await page.getByTestId("db-owner-open").click();
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);

    const cases = [
      [400, "MSSQL owner is required.", /MSSQL owner is required|400|Bad Request/i],
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
            body: JSON.stringify(mssqlPayload(ORIGINAL_OWNER)),
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
      await page.getByTestId("db-owner-input").fill(`own${status}`);
      await page.getByTestId("db-owner-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("db-owner-form")).toBeVisible();
      await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);
      await expect(page.getByTestId("db-owner-host")).toHaveText(HOST);
      await expect(page.getByTestId("db-owner-port")).toHaveText(PORT);
      await expect(page.getByTestId("db-owner-user")).toHaveText(USER);
      await expect(page.getByTestId("db-owner-database")).toHaveText(DATABASE);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-owner-cancel").click();
    await expect(page.getByTestId("db-owner-current")).toHaveText(ORIGINAL_OWNER);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
