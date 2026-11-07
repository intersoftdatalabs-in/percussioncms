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
 * PublishingShell Sites — copy a delivery server (#4888 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDeliveryServerCopy.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const PASSTHROUGH_HEADS = new Set([
  "stopPublishing",
  "availableDrivers",
  "availableRegions",
  "isEC2Instance",
  "defaultFolderLocation",
  "availableDeliveryServers",
]);

function fulfillJson(route, status, payload) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(payload),
  });
}

function handleCreatePost(route, name, options, holder, servers) {
  const lastPost = route.request().postData() || "";
  if (holder) {
    holder.post = lastPost;
  }
  if (options.conflict) {
    return fulfillJson(route, 409, {
      message: "Publish server name already exists",
    });
  }
  if (options.postStatus && options.postStatus !== 200) {
    return fulfillJson(
      route,
      options.postStatus,
      options.postBody || { message: "Request failed" },
    );
  }
  servers.push({
    serverId: 99,
    serverName: name,
    serverType: "PRODUCTION",
    type: "File",
    isDefault: false,
    properties: [],
  });
  return fulfillJson(route, 200, { serverName: name, serverId: 99 });
}

function installServerRoutes(page, options, holder) {
  const servers = [
    {
      serverId: 11,
      serverName: "KeepMe",
      serverType: "PRODUCTION",
      type: "File",
      isDefault: true,
      properties: [
        { key: "driver", value: "FTP" },
        { key: "folder", value: "/keep" },
        { key: "password", value: "s3cr3t" },
      ],
    },
  ];
  return page.route("**/publishmanagement/servers/**", async (route) => {
    const req = route.request();
    const parts = new URL(req.url()).pathname.split("/").filter(Boolean);
    const idx = parts.indexOf("servers");
    const rest = idx >= 0 ? parts.slice(idx + 1) : [];
    const head = rest[0] || "";
    if (PASSTHROUGH_HEADS.has(head)) {
      return route.continue();
    }
    if (rest.length === 1 && req.method() === "GET") {
      return fulfillJson(route, 200, { PubServer: servers });
    }
    if (rest.length === 2 && req.method() === "GET") {
      const found = servers.find((s) => String(s.serverId) === rest[1]) || {};
      return fulfillJson(route, 200, found);
    }
    if (rest.length === 2 && req.method() === "POST") {
      return handleCreatePost(
        route,
        decodeURIComponent(rest[1]),
        options,
        holder,
        servers,
      );
    }
    return route.continue();
  });
}

async function openSites(page) {
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
  await page.getByRole("button", { name: /KeepMe/ }).click();
}

test.describe("PublishingShell Sites copy delivery server", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("copies driver settings without the password and lists the new name", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });
    const holder = { post: "" };
    await installServerRoutes(page, { conflict: false }, holder);
    page.once("dialog", (dialog) => dialog.accept("CopiedSrv"));
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("button", { name: /CopiedSrv/ })).toBeVisible({
      timeout: 20000,
    });
    expect(holder.post).not.toContain("s3cr3t");
    expect(holder.post).toContain("FTP");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("blank name is not success", async ({ page }) => {
    let posted = false;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/servers/")) {
        posted = true;
      }
    });
    await installServerRoutes(page, { conflict: false });
    page.once("dialog", (dialog) => dialog.accept("   "));
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("alert")).toContainText(/required/i);
    expect(posted).toBe(false);
    await expect(page.getByRole("button", { name: /CopiedSrv/ })).toHaveCount(0);
  });

  test("duplicate name stays an error", async ({ page }) => {
    await installServerRoutes(page, { conflict: true });
    page.once("dialog", (dialog) => dialog.accept("KeepMe"));
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("alert")).toContainText(/already exists/i);
    await expect(page.getByRole("button", { name: /^KeepMe/ })).toHaveCount(1);
  });

  test("cancel the name prompt posts nothing and keeps the list", async ({
    page,
  }) => {
    let posted = false;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/servers/")) {
        posted = true;
      }
    });
    await installServerRoutes(page, { conflict: false });
    page.once("dialog", (dialog) => dialog.dismiss());
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("button", { name: /^KeepMe/ })).toHaveCount(1);
    expect(posted).toBe(false);
    await expect(
      page.getByTestId("publish-action-message"),
    ).toHaveCount(0);
  });

  test("HTTP 400 keeps the list and shows the mapped error", async ({
    page,
  }) => {
    await installServerRoutes(page, {
      postStatus: 400,
      postBody: { message: "Server name is required" },
    });
    page.once("dialog", (dialog) => dialog.accept("Bad Name"));
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("alert")).toContainText(/required/i);
    await expect(
      page.getByRole("button", { name: /^Bad Name/ }),
    ).toHaveCount(0);
    await expect(page.getByRole("button", { name: /^KeepMe/ })).toHaveCount(1);
  });

  test("HTTP 403 keeps the list and shows the mapped error", async ({
    page,
  }) => {
    await installServerRoutes(page, {
      postStatus: 403,
      postBody: { message: "Publish Forbidden" },
    });
    page.once("dialog", (dialog) => dialog.accept("NopeSrv"));
    await openSites(page);
    await page.getByTestId("publish-copy-server").click();
    await expect(page.getByRole("alert")).toContainText(/forbidden/i);
    await expect(
      page.getByRole("button", { name: /^NopeSrv/ }),
    ).toHaveCount(0);
    await expect(page.getByRole("button", { name: /^KeepMe/ })).toHaveCount(1);
  });
});
