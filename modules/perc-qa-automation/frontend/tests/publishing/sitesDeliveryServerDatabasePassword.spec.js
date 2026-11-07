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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * PublishingShell Sites — set the database delivery-server password (#5691 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDatabasePassword.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live MYSQL/MSSQL/Oracle connection, so the MYSQL password
 * path overlays GET of a created Local server. The Local-only path is live.
 * The database password PUT goes live against H2 (no DB connection is opened,
 * only the stored property is replaced). The stored secret must never appear
 * in page text, GET responses, or the PUT body (base64-encoded in flight).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PASSWORD = "NightDbPw1";
const UPDATED_PASSWORD = "NightDbPw2!";
const HOST = "db-old.example";
const PORT = "3306";
const USER = "cmsdb";
const DATABASE = "rxdb";
const SERVER_NAME = "mysql-prod";
const MASK = "passwordEntry";

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

function mysqlPayload() {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "Database",
    isDefault: false,
    properties: [
      { key: "driver", value: "MYSQL" },
      { key: "server", value: HOST },
      { key: "port", value: PORT },
      { key: "userid", value: USER },
      { key: "database", value: DATABASE },
      { key: "password", value: MASK },
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

test.describe("PublishingShell Sites database delivery-server password", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets a database password only and never echoes the secret", async ({
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
    await page.getByTestId("db-password-open").click();
    await expect(page.getByTestId("db-password-not-database")).toBeVisible();
    await expect(page.getByTestId("db-password-not-database")).toContainText(
      /database delivery server/i,
    );
    await expect(page.getByTestId("db-password-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mysqlPayload()),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("db-password-open").click();
    await expect(page.getByTestId("db-password-form")).toBeVisible();
    await expect(page.getByTestId("db-password-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-password-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-password-user")).toHaveText(USER);
    await expect(page.getByTestId("db-password-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i);
    // The secret input opens empty; the stored secret is never rendered.
    await expect(page.getByTestId("db-password-input")).toHaveValue("");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(MASK);

    await page.getByTestId("db-password-input").fill(UPDATED_PASSWORD);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-password-cancel").click();
    await expect(page.getByTestId("db-password-form")).toHaveCount(0);
    await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      UPDATED_PASSWORD,
    );

    await page.getByTestId("db-password-open").click();
    await page.getByTestId("db-password-input").fill("   ");
    await page.getByTestId("db-password-save").click();
    await expect(page.getByRole("alert")).toContainText(/Database password is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-password-input").fill("p".repeat(256));
    await page.getByTestId("db-password-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-password-input").fill(UPDATED_PASSWORD);
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
          body: JSON.stringify(mysqlPayload()),
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
        body: JSON.stringify(mysqlPayload()),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("db-password-save").click();
    await expect.poll(() => postedBody.length).toBeGreaterThan(0);
    const parsed = JSON.parse(postedBody);
    expect(parsed.serverInfo.properties).toHaveLength(1);
    expect(parsed.serverInfo.properties[0].key).toBe("password");
    const sent = String(parsed.serverInfo.properties[0].value || "");
    expect(sent).not.toContain(UPDATED_PASSWORD);
    expect(Buffer.from(sent, "base64").toString("utf8")).toBe(UPDATED_PASSWORD);
    expect(postedBody).not.toContain(PASSWORD);
    expect(postedBody).not.toContain(UPDATED_PASSWORD);
    expect(postedBody).not.toContain(HOST);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain(DATABASE);
    expect(postedBody).not.toContain("serverip");
    expect(postedBody).not.toContain("userid");
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `database password HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("db-password-form")).toHaveCount(0, { timeout: 20000 });
    // The typed secret is cleared and never echoed back into the workspace.
    await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i, {
      timeout: 20000,
    });
    await expect(page.getByTestId("db-password-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-password-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-password-user")).toHaveText(USER);
    await expect(page.getByTestId("db-password-database")).toHaveText(DATABASE);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      UPDATED_PASSWORD,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous database password", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `DbP${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(mysqlPayload()),
        });
      }
      return route.continue();
    });
    await page.getByTestId("db-password-open").click();
    await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i);

    const cases = [
      [400, "Database password is required.", /Database password is required|400|Bad Request/i],
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
            body: JSON.stringify(mysqlPayload()),
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
      await page.getByTestId("db-password-input").fill(`pw-will-not-save-${status}`);
      await page.getByTestId("db-password-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("db-password-form")).toBeVisible();
      await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i);
      await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
        `pw-will-not-save-${status}`,
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-password-cancel").click();
    await expect(page.getByTestId("db-password-status")).toContainText(/never shown/i);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
