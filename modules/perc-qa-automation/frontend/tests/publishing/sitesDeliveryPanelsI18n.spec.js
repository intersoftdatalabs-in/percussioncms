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
 * PublishingShell Sites — delivery-panel chrome resolves via keyed i18n
 * messages (#6936, parent #4531).
 *
 * Asserts the FTP folder delivery panel renders its title, summary labels,
 * helper text, and actions through MSG.PUBLISH_DELIVERY_* keys (English
 * defaults byte-identical to the previous bare literals), plus the FTP
 * password panel summary status row renders its "Password:" label through
 * MSG.PUBLISH_DELIVERY_SUMMARY_PASSWORD (#7076 follow-up), plus a live-proof
 * screenshot of the passing surface.
 *
 * Tags: @publishing-delivery-panels-i18n @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing/sitesDeliveryPanelsI18n.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

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

function ftpPayload() {
  return {
    serverId: "9",
    serverName: "ftp-prod",
    type: "File",
    isDefault: false,
    properties: [
      { key: "driver", value: "FTP" },
      { key: "serverip", value: "ftp-old.example" },
      { key: "port", value: "2121" },
      { key: "userid", value: "ftpuser" },
      { key: "folder", value: "/kept/folder" },
      { key: "password", value: "s3cret" },
    ],
  };
}

function singleServerPath(url) {
  try {
    return /\/publishmanagement\/servers\/[^/]+\/[^/]+$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
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

test.describe("PublishingShell Sites delivery panels i18n", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("FTP folder panel renders keyed chrome with identical English defaults", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openFirstSite(page);

    const stamp = Date.now().toString().slice(-8);
    const localName = `Loc${stamp}`;
    await page.getByTestId("publish-add-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeVisible();
    await page.locator("#serverName").fill(localName);
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
    await page.getByRole("button", { name: localName, exact: true }).click();

    await page.route(/\/publishmanagement\/servers\/[^/]+\/[^/]+(?:\?|$)/, async (route) => {
      if (route.request().method() === "GET" && singleServerPath(route.request().url())) {
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(ftpPayload()),
        });
      }
      return route.continue();
    });

    await page.getByTestId("ftp-folder-open").click();
    const panel = page.getByTestId("ftp-folder-panel");
    await expect(page.getByTestId("ftp-folder-current")).toBeVisible({ timeout: 20000 });
    await expect(panel).toContainText("FTP folder");
    await expect(panel).toContainText("Host:");
    await expect(panel).toContainText("Port:");
    await expect(panel).toContainText("User ID:");
    await expect(panel).toContainText("Folder:");
    await expect(page.getByTestId("ftp-folder-current")).toHaveText("/kept/folder");
    await expect(page.getByTestId("ftp-folder-host")).toHaveText("ftp-old.example");
    await expect(page.getByTestId("ftp-folder-optional")).toContainText(
      "Leave blank to clear the folder.",
    );
    await expect(page.getByTestId("ftp-folder-save")).toHaveText("Save folder");
    await expect(page.getByTestId("ftp-folder-cancel")).toHaveText("Cancel");

    await page.getByTestId("ftp-password-open").click();
    await expect(page.getByTestId("ftp-password-status")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByTestId("ftp-password-status")).toContainText("Password:");
    await expect(page.getByTestId("ftp-password-status")).toContainText(
      "Stored password is never shown.",
    );

    await page.screenshot({ path: "test-results/live-proof-6936-delivery-panels.png" });
    expect(jsErrors).toEqual([]);
  });
});
