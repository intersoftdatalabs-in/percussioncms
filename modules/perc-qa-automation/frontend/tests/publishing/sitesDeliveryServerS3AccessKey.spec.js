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
 * PublishingShell Sites — set an S3 delivery-server access key (#5654 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerS3AccessKey.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 *
 * H2 QA cannot open a live Amazon S3 connection, so the S3 access-key path overlays
 * GET of a created Local server. The Local-only path is live.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const ACCESS_KEY = "AKIATESTACCESS";
const NEW_ACCESS_KEY = "AKIANEWACCESS12345";
const NEW_ACCESS_KEY_B64 = Buffer.from(NEW_ACCESS_KEY).toString("base64");
const SECRET_KEY = "NightS3Secret1";
const REGION = "us-west-2";
const BUCKET = "old-bucket";
const SERVER_NAME = "s3-prod";

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

function s3Payload() {
  return {
    serverId: "9",
    serverName: SERVER_NAME,
    type: "File",
    isDefault: false,
    properties: [
      { key: "driver", value: "AMAZONS3" },
      { key: "bucketlocation", value: BUCKET },
      { key: "region", value: REGION },
      { key: "accesskey", value: ACCESS_KEY },
      { key: "securitykey", value: SECRET_KEY },
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

test.describe("PublishingShell Sites S3 delivery-server access key", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets an S3 access key only and never renders the secret", async ({
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
    await page.getByTestId("s3-access-key-open").click();
    await expect(page.getByTestId("s3-access-key-not-s3")).toBeVisible();
    await expect(page.getByTestId("s3-access-key-not-s3")).toContainText(
      /Amazon S3 delivery server/i,
    );
    await expect(page.getByTestId("s3-access-key-save")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeLocal);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      SECRET_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      ACCESS_KEY,
    );

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      const method = route.request().method();
      if (method === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(s3Payload()),
        });
      }
      if (method === "PUT") {
        return route.continue();
      }
      return route.continue();
    });

    await page.getByTestId("s3-access-key-open").click();
    await expect(page.getByTestId("s3-access-key-form")).toBeVisible();
    await expect(page.getByTestId("s3-access-key-bucket")).toHaveText(BUCKET);
    await expect(page.getByTestId("s3-access-key-region")).toHaveText(REGION);
    await expect(page.getByTestId("s3-access-key-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("s3-access-key-driver")).toHaveText("AMAZONS3");
    await expect(page.getByTestId("s3-access-key-status")).toContainText(
      /never shown/i,
    );
    await expect(page.getByTestId("s3-access-key-input")).toHaveValue("");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      SECRET_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      ACCESS_KEY,
    );

    await page.getByTestId("s3-access-key-input").fill(NEW_ACCESS_KEY);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("s3-access-key-cancel").click();
    await expect(page.getByTestId("s3-access-key-form")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      NEW_ACCESS_KEY,
    );
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("s3-access-key-open").click();
    await page.getByTestId("s3-access-key-input").fill("   ");
    await page.getByTestId("s3-access-key-save").click();
    await expect(page.getByRole("alert")).toContainText(/S3 access key is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("s3-access-key-input").fill("k".repeat(256));
    await page.getByTestId("s3-access-key-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("s3-access-key-input").fill(NEW_ACCESS_KEY);
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
          body: JSON.stringify(s3Payload()),
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
        body: JSON.stringify(s3Payload()),
      });
    });
    const putResponse = page.waitForResponse(
      (res) => serverPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("s3-access-key-save").click();
    await expect.poll(() => postedBody).toContain(NEW_ACCESS_KEY_B64);
    expect(postedBody).not.toContain(NEW_ACCESS_KEY);
    expect(postedBody).not.toContain(SECRET_KEY);
    expect(postedBody).not.toContain(ACCESS_KEY);
    expect(postedBody).not.toContain(BUCKET);
    expect(postedBody).not.toContain(REGION);
    expect(postedBody).not.toContain("securitykey");
    expect(postedBody).not.toContain("bucketlocation");
    expect(postedBody).not.toContain("serverip");
    await expect(page.getByTestId("s3-access-key-form")).toBeVisible();
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `s3 access key HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("s3-access-key-form")).toHaveCount(0, { timeout: 20000 });
    await expect(page.getByTestId("s3-access-key-input")).toHaveCount(0);
    await expect(page.getByTestId("s3-access-key-bucket")).toHaveText(BUCKET);
    await expect(page.getByTestId("s3-access-key-region")).toHaveText(REGION);
    await expect(page.getByTestId("s3-access-key-name")).toHaveText(SERVER_NAME);
    await expect(page.getByTestId("s3-access-key-driver")).toHaveText("AMAZONS3");
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      SECRET_KEY,
    );
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      NEW_ACCESS_KEY,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous S3 access key", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);
    const stamp = Date.now().toString().slice(-8);
    const localName = `S3a${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await page.locator("#serverName").fill(localName);
    await saveServer(page);
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(s3Payload()),
        });
      }
      return route.continue();
    });
    await page.getByTestId("s3-access-key-open").click();
    await expect(page.getByTestId("s3-access-key-bucket")).toHaveText(BUCKET);

    const cases = [
      [400, "S3 access key is required.", /S3 access key is required|400|Bad Request/i],
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
            body: JSON.stringify(s3Payload()),
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
      await page.getByTestId("s3-access-key-input").fill(`key${status}`);
      await page.getByTestId("s3-access-key-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("s3-access-key-form")).toBeVisible();
      await expect(page.getByTestId("s3-access-key-bucket")).toHaveText(BUCKET);
      await expect(page.getByTestId("s3-access-key-region")).toHaveText(REGION);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("s3-access-key-cancel").click();
    await expect(page.getByTestId("s3-access-key-bucket")).toHaveText(BUCKET);
    await expect(page.getByTestId("publish-site-workspace")).not.toContainText(
      SECRET_KEY,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
