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
 * PublishingShell Sites — set an Oracle delivery-server SID (#5555 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDatabaseSid.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live Oracle connection, so the Oracle SID path overlays
 * GET of a created Local server. The Local-only path is live.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PASSWORD = "NightOraPw1";
const HOST = "ora-old.example";
const PORT = "1521";
const USER = "cmsora";
const ORIGINAL_SID = "ORCL";
const UPDATED_SID = "XE";
const SERVER_NAME = "ora-prod";

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

function oraclePayload(sid) {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "Database",
    isDefault: false,
    properties: [
      { key: "driver", value: "Oracle" },
      { key: "server", value: HOST },
      { key: "port", value: PORT },
      { key: "userid", value: USER },
      { key: "sid", value: sid },
      { key: "schema", value: "SYSTEM" },
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

test.describe("PublishingShell Sites Oracle delivery-server SID", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets an Oracle SID only and does not write a Local folder", async ({
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
    await page.getByTestId("db-sid-open").click();
    await expect(page.getByTestId("db-sid-not-oracle")).toBeVisible();
    await expect(page.getByTestId("db-sid-not-oracle")).toContainText(
      /Oracle delivery server/i,
    );
    await expect(page.getByTestId("db-sid-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    let overlaySid = ORIGINAL_SID;
    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(oraclePayload(overlaySid)),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("db-sid-open").click();
    await expect(page.getByTestId("db-sid-form")).toBeVisible();
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);
    await expect(page.getByTestId("db-sid-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-sid-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-sid-user")).toHaveText(USER);
    await expect(page.getByTestId("db-sid-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-sid-driver")).toHaveText("Oracle");
    await expect(page.getByTestId("db-sid-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);

    await page.getByTestId("db-sid-input").fill(UPDATED_SID);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-sid-cancel").click();
    await expect(page.getByTestId("db-sid-form")).toHaveCount(0);
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("db-sid-open").click();
    await page.getByTestId("db-sid-input").fill("   ");
    await page.getByTestId("db-sid-save").click();
    await expect(page.getByRole("alert")).toContainText(/Oracle SID is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);

    await page.getByTestId("db-sid-input").fill(UPDATED_SID);
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
          body: JSON.stringify(oraclePayload(overlaySid)),
        });
      }
      if (method !== "PUT") {
        return route.continue();
      }
      postedBody = route.request().postData() || "";
      await holdPut;
      overlaySid = UPDATED_SID;
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(oraclePayload(UPDATED_SID)),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("db-sid-save").click();
    await expect.poll(() => postedBody).toContain(UPDATED_SID);
    expect(postedBody).not.toContain(PASSWORD);
    expect(postedBody).not.toContain(HOST);
    expect(postedBody).not.toContain(PORT);
    expect(postedBody).not.toContain(USER);
    expect(postedBody).not.toContain(ORIGINAL_SID);
    expect(postedBody).not.toContain("password");
    expect(postedBody).not.toContain("userid");
    expect(postedBody).not.toContain("schema");
    expect(postedBody).not.toContain("database");
    expect(postedBody).not.toContain("serverip");
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `oracle SID HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("db-sid-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("db-sid-current")).toHaveText(UPDATED_SID, {
      timeout: 20000,
    });
    await expect(page.getByTestId("db-sid-host")).toHaveText(HOST);
    await expect(page.getByTestId("db-sid-port")).toHaveText(PORT);
    await expect(page.getByTestId("db-sid-user")).toHaveText(USER);
    await expect(page.getByTestId("db-sid-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("db-sid-driver")).toHaveText("Oracle");
    await expect(page.getByTestId("db-sid-default")).toHaveText("no");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous Oracle SID", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `DbS${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(oraclePayload(ORIGINAL_SID)),
        });
      }
      return route.continue();
    });
    await page.getByTestId("db-sid-open").click();
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);

    const cases = [
      [400, "Oracle SID is required.", /Oracle SID is required|400|Bad Request/i],
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
            body: JSON.stringify(oraclePayload(ORIGINAL_SID)),
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
      await page.getByTestId("db-sid-input").fill(`SID${status}`);
      await page.getByTestId("db-sid-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("db-sid-form")).toBeVisible();
      await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);
      await expect(page.getByTestId("db-sid-host")).toHaveText(HOST);
      await expect(page.getByTestId("db-sid-port")).toHaveText(PORT);
      await expect(page.getByTestId("db-sid-user")).toHaveText(USER);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("db-sid-cancel").click();
    await expect(page.getByTestId("db-sid-current")).toHaveText(ORIGINAL_SID);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(PASSWORD);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
