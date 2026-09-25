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
 * PublishingShell Sites — delete a delivery server (#4860 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerDelete.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function installServerRoutes(page, options) {
  const servers = [
    {
      serverId: 11,
      serverName: "KeepMe",
      serverType: "PRODUCTION",
      type: "File",
      isDefault: true,
    },
    {
      serverId: 22,
      serverName: "DropMe",
      serverType: "STAGING",
      type: "File",
      isDefault: false,
    },
  ];
  return page.route("**/publishmanagement/servers/**", async (route) => {
    const req = route.request();
    const parts = new URL(req.url()).pathname.split("/").filter(Boolean);
    const idx = parts.indexOf("servers");
    const rest = idx >= 0 ? parts.slice(idx + 1) : [];
    const head = rest[0] || "";
    if (
      head === "stopPublishing" ||
      head === "availableDrivers" ||
      head === "availableRegions" ||
      head === "isEC2Instance" ||
      head === "defaultFolderLocation" ||
      head === "availableDeliveryServers"
    ) {
      return route.continue();
    }
    if (rest.length === 1 && req.method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ PubServer: servers }),
      });
    }
    if (rest.length === 2 && req.method() === "GET") {
      const found = servers.find((s) => String(s.serverId) === rest[1]) || {};
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(found),
      });
    }
    if (rest.length === 2 && req.method() === "DELETE") {
      if (options.fail) {
        return route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({
            message:
              "The server is being used by other user and cannot be deleted.",
          }),
        });
      }
      const i = servers.findIndex((s) => String(s.serverId) === rest[1]);
      if (i >= 0) {
        servers.splice(i, 1);
      }
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ PubServer: servers }),
      });
    }
    return route.continue();
  });
}

async function openDropMeEditor(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  const cards = page.locator(
    "[data-testid='publish-section-sites'] [role='listitem']",
  );
  await expect(cards.first()).toBeVisible({ timeout: 30000 });
  await cards.first().click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
  await page.getByRole("button", { name: "DropMe" }).click();
  await page.getByTestId("publish-edit-server").click();
  await expect(page.getByTestId("publish-delete-server")).toBeVisible();
}

test.describe("PublishingShell Sites delete delivery server", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("confirm removes the server from the list", async ({ page }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });
    await installServerRoutes(page, { fail: false });
    page.once("dialog", (dialog) => dialog.accept());
    await openDropMeEditor(page);
    await page.getByTestId("publish-delete-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name: "DropMe" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: /KeepMe/ })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not delete", async ({ page }) => {
    let deleted = false;
    page.on("request", (req) => {
      if (req.method() === "DELETE" && req.url().includes("/servers/")) {
        deleted = true;
      }
    });
    await installServerRoutes(page, { fail: false });
    page.once("dialog", (dialog) => dialog.dismiss());
    await openDropMeEditor(page);
    await page.getByTestId("publish-delete-server").click();
    await expect(page.getByTestId("publish-server-editor")).toBeVisible();
    expect(deleted).toBe(false);
  });

  test("in-use failure stays visible and the list is unchanged", async ({
    page,
  }) => {
    await installServerRoutes(page, { fail: true });
    page.once("dialog", (dialog) => dialog.accept());
    await openDropMeEditor(page);
    await page.getByTestId("publish-delete-server").click();
    await expect(page.getByRole("alert")).toContainText(/being used/i);
    await page.getByTestId("publish-server-editor").getByRole("button", {
      name: /Back/i,
    }).click();
    await expect(page.getByRole("button", { name: "DropMe" })).toBeVisible();
  });
});
