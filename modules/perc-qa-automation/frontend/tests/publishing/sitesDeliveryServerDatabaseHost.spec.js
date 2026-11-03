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
 * PublishingShell Sites — set a database delivery-server host (#5522 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDatabaseHost.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live MYSQL/MSSQL/Oracle connection, so the MYSQL host
 * path overlays GET of a created Local server. The Local-only path is live.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PASSWORD = "NightDbPw1";
const PORT = "3306";
const USER = "cmsdb";
const DATABASE = "rxdb";
const ORIGINAL_HOST = "db-old.example";
const UPDATED_HOST = "db-new.example";
const SERVER_NAME = "mysql-prod";

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

function mysqlPayload(host) {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "Database",
    isDefault: false,
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: host },
      { key: "port", value: PORT },
      { key: "userid", value: USER },
      { key: "database", value: DATABASE },
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

test.describe("PublishingShell Sites database delivery-server host", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets a database host only and does not write a Local folder", async ({
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
    await page.getByTestId("db-host-open").click();
    await expect(page.getByTestId("db-host-not-database")).toBeVisible();
    await expect(page.getByTestId("db-host-not-database")).toContainText(
      /database delivery server/i,
    );
    await expect(page.getByTestId("db-host-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    let overlayHost = ORIGINAL_HOST;
    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mysqlPayload(overlayHost)),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("db-host-open").click();
    await expect(page.getByTestId("db-host-form")).toBeVisible();
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);
    await expect(page.getByTestId("db-host-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-host-user")).toHaveText(USER);
    await expect(page.getByTestId("db-host-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("db-host-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-host-driver")).toHaveText("MYSQL");
    await expect(page.getByTestId("db-host-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    await page.getByTestId("db-host-input").fill(UPDATED_HOST);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-host-cancel").click();
    await expect(page.getByTestId("db-host-form")).toHaveCount(0);
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-host-open").click();
    await page.getByTestId("db-host-input").fill("   ");
    await page.getByTestId("db-host-save").click();
    await expect(page.getByRole("alert")).toContainText(/Database host is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);

    await page.getByTestId("db-host-input").fill("h".repeat(256));
    await page.getByTestId("db-host-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-host-input").fill(UPDATED_HOST);
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
          body: JSON.stringify(mysqlPayload(overlayHost)),
        });
      }
      if (method !== "PUT") {
        return route.continue();
      }
      postedBody = route.request().postData() || "";
      await holdPut;
      overlayHost = UPDATED_HOST;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(mysqlPayload(UPDATED_HOST)),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("db-host-save").click();
    await expect.poll(() => postedBody).toContain(UPDATED_HOST);
    expect(postedBody).not.toContain(PASSWORD);
    expect(postedBody).not.toContain(PORT);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain(DATABASE);
    expect(postedBody).not.toContain(ORIGINAL_HOST);
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("serverip");
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `database host HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("db-host-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("db-host-current")).toHaveText(UPDATED_HOST, {
      timeout: 20000,
    });
    await expect(page.getByTestId("db-host-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-host-user")).toHaveText(USER);
    await expect(page.getByTestId("db-host-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("db-host-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-host-driver")).toHaveText("MYSQL");
    await expect(page.getByTestId("db-host-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous database host", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `DbE${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mysqlPayload(ORIGINAL_HOST)),
        });
      }
      return route.continue();
    });
    await page.getByTestId("db-host-open").click();
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);

    const cases = [
      [400, "Database host is required.", /Database host is required|400|Bad Request/i],
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
            body: JSON.stringify(mysqlPayload(ORIGINAL_HOST)),
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
      await page.getByTestId("db-host-input").fill(`db.will-not-save.${status}.example`);
      await page.getByTestId("db-host-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("db-host-form")).toBeVisible();
      await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);
      await expect(page.getByTestId("db-host-port")).toHaveText(PORT);
      await expect(page.getByTestId("db-host-user")).toHaveText(USER);
      await expect(page.getByTestId("db-host-database")).toHaveText(DATABASE);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-host-cancel").click();
    await expect(page.getByTestId("db-host-current")).toHaveText(ORIGINAL_HOST);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
