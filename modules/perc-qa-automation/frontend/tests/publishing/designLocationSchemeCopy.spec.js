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
 * PublishingShell Design — copy a location scheme (#5109 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designLocationSchemeCopy.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const GENERATOR =
  "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";

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

async function openContexts(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Contexts/i }).click();
  await expect(page.getByTestId("contexts-panel")).toBeVisible();
}

async function createScheme(page, name) {
  await page.getByTestId("design-add-location-scheme").click();
  await expect(page.getByTestId("scheme-editor")).toBeVisible();
  await page.locator("#sch-name").fill(name);
  await page.locator("#sch-gen").fill(GENERATOR);
  const stamp = Date.now().toString().slice(-8);
  await page.locator("#sch-ctype").fill(stamp);
  await page.locator("#sch-tpl").fill(String(Number(stamp) + 1));
  await page.locator('input[placeholder="name"]').fill("path");
  await page.locator('input[placeholder="value"]').fill("$sys.site.path");
  await page.getByRole("button", { name: "Add param" }).click();
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("location-scheme-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create scheme HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("scheme-editor")).toBeHidden({ timeout: 20000 });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

test.describe("PublishingShell Design copy location scheme", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("copies a scheme under a new name and lists it only after success", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const stamp = Date.now().toString().slice(-8);
    const source = `SchSrc${stamp}`;
    const copied = `SchDst${stamp}`;
    await createScheme(page, source);

    const sourceRow = page
      .getByRole("listitem")
      .filter({ has: page.getByRole("button", { name: source, exact: true }) });
    await sourceRow.getByTestId("location-scheme-copy").click();
    await expect(page.getByTestId("scheme-copy")).toBeVisible();
    await expect(page.getByRole("button", { name: copied, exact: true })).toHaveCount(0);

    await page.locator("#scheme-copy-name").fill(copied);
    let releaseCopy = () => undefined;
    const holdCopy = new Promise((resolve) => {
      releaseCopy = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/,
      async (route) => {
        if (route.request().method() !== "POST") {
          return route.continue();
        }
        postedBody = route.request().postData() || "";
        await holdCopy;
        return route.continue();
      },
    );
    const copyResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        /\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(res.url()),
      { timeout: 30000 },
    );
    await page.getByTestId("location-scheme-copy-submit").click();
    await expect(page.getByTestId("scheme-copy")).toBeVisible();
    await expect.poll(() => postedBody).toContain(copied);
    expect(postedBody).toContain(GENERATOR);
    await expect(page.getByRole("button", { name: copied, exact: true })).toHaveCount(0);
    releaseCopy();
    const posted = await copyResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `copy scheme HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("scheme-copy")).toBeHidden({ timeout: 20000 });
    await expect(page.getByRole("button", { name: copied, exact: true })).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("rejects a blank or overlong name and cancel does not call the server", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const source = `SchVal${Date.now().toString().slice(-8)}`;
    await createScheme(page, source);

    const posts = [];
    const schemeGets = [];
    page.on("request", (req) => {
      const url = req.url();
      if (
        req.method() === "POST" &&
        /\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(url)
      ) {
        posts.push(url);
      }
      if (
        req.method() === "GET" &&
        /\/publishingdesign\/schemes\/[^/]+(?:\?|$)/.test(url)
      ) {
        schemeGets.push(url);
      }
    });

    const sourceRow = page
      .getByRole("listitem")
      .filter({ has: page.getByRole("button", { name: source, exact: true }) });
    await sourceRow.getByTestId("location-scheme-copy").click();
    await expect(page.getByTestId("scheme-copy")).toBeVisible();
    await page.locator("#scheme-copy-name").fill("");
    await page.getByTestId("location-scheme-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    await expect(page.getByTestId("scheme-copy")).toBeVisible();

    await page.locator("#scheme-copy-name").fill("n".repeat(51));
    await page.getByTestId("location-scheme-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/50 characters or fewer/i);

    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-copy-cancel").click();
    await expect(page.getByTestId("scheme-copy")).toBeHidden();
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    expect(posts, "blank, overlong, and cancel must not POST").toEqual([]);
    expect(schemeGets, "validation and cancel must not load the source").toEqual([]);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 do not list the copy", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const source = `SchErr${Date.now().toString().slice(-8)}`;
    await createScheme(page, source);
    const sourceRow = page
      .getByRole("listitem")
      .filter({ has: page.getByRole("button", { name: source, exact: true }) });
    await sourceRow.getByTestId("location-scheme-copy").click();
    await expect(page.getByTestId("scheme-copy")).toBeVisible();

    const cases = [
      [400, "name and generator are required", /required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Location scheme name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/,
        (route) => {
          if (route.request().method() !== "POST") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      const requested = `WillNotCopy${status}`;
      await page.locator("#scheme-copy-name").fill(requested);
      await page.getByTestId("location-scheme-copy-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("scheme-copy")).toBeVisible();
      await expect(page.getByRole("button", { name: requested, exact: true })).toHaveCount(
        0,
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-copy-cancel").click();
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: /WillNotCopy/ })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
