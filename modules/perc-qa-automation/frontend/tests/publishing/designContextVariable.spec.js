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
 * PublishingShell Design — set one new context variable (#5410 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextVariable.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function propertyPut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/sites\/[^/]+\/properties$/.test(new URL(url).pathname);
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

async function openSites(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page
    .getByTestId("publish-section-design")
    .getByRole("tab", { name: "Sites", exact: true })
    .click();
  await expect(page.getByTestId("site-design-panel")).toBeVisible();
  await expect(page.getByTestId("context-variable-form")).toBeVisible();
  await expect(page.getByTestId("context-variable-site")).not.toHaveValue("", {
    timeout: 20000,
  });
  await expect(page.getByTestId("context-variable-context")).not.toHaveValue("");
}

function rowByName(page, name) {
  return page.getByTestId("context-variable-row").filter({
    has: page.getByTestId("context-variable-row-name").getByText(name, { exact: true }),
  });
}

test.describe("PublishingShell Design set one context variable", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("saves a new variable only after success and leaves the other", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (propertyPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvKeep${stamp}`;
    const nextName = `CvNext${stamp}`;

    await page.getByTestId("context-variable-name").fill("   ");
    await page.getByTestId("context-variable-value").fill("ignored");
    await page.getByTestId("context-variable-save").click();
    await expect(page.getByTestId("context-variable-error")).toContainText(
      /name is required/i,
    );
    expect(putBodies).toEqual([]);

    await page.getByTestId("context-variable-name").fill("n".repeat(51));
    await page.getByTestId("context-variable-value").fill("v");
    await page.getByTestId("context-variable-save").click();
    await expect(page.getByTestId("context-variable-error")).toContainText(
      /50 characters or fewer/i,
    );
    expect(putBodies).toEqual([]);

    await page.getByTestId("context-variable-name").fill(keptName);
    await page.getByTestId("context-variable-value").fill("   ");
    await page.getByTestId("context-variable-save").click();
    await expect(page.getByTestId("context-variable-error")).toContainText(
      /value is required/i,
    );
    expect(putBodies).toEqual([]);

    await page.getByTestId("context-variable-value").fill("  kept-value  ");
    const firstPut = page.waitForResponse(
      (res) => propertyPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-variable-save").click();
    const firstPosted = await firstPut;
    const firstBody = firstPosted.request().postData() || "";
    expect(firstBody).toContain("siteProperty");
    expect(firstBody).toContain(keptName);
    await expect(rowByName(page, keptName)).toBeVisible({ timeout: 20000 });
    await expect(
      rowByName(page, keptName).getByTestId("context-variable-row-value"),
    ).toHaveText("kept-value");

    await page.getByTestId("context-variable-name").fill(keptName);
    await page.getByTestId("context-variable-value").fill("replaced");
    const putsBeforeDuplicate = putBodies.length;
    await page.getByTestId("context-variable-save").click();
    await expect(page.getByTestId("context-variable-error")).toContainText(
      /already exists/i,
    );
    expect(putBodies).toHaveLength(putsBeforeDuplicate);
    await expect(
      rowByName(page, keptName).getByTestId("context-variable-row-value"),
    ).toHaveText("kept-value");

    await page.getByTestId("context-variable-name").fill(`  ${nextName}  `);
    await page.getByTestId("context-variable-value").fill("  next-value  ");
    const secondPut = page.waitForResponse(
      (res) => propertyPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-variable-save").click();
    const secondPosted = await secondPut;
    const body = secondPosted.request().postData() || "";
    expect(body).toContain("siteProperty");
    expect(body).toContain(nextName);
    expect(body).toContain("next-value");
    expect(body).not.toContain(keptName);
    await expect(rowByName(page, nextName)).toBeVisible({ timeout: 20000 });
    await expect(
      rowByName(page, nextName).getByTestId("context-variable-row-value"),
    ).toHaveText("next-value");
    await expect(
      rowByName(page, keptName).getByTestId("context-variable-row-value"),
    ).toHaveText("kept-value");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 do not add a row", async ({ page }) => {
    test.setTimeout(90_000);
    const jsErrors = trackJsErrors(page);
    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvErr${stamp}`;

    await page.getByTestId("context-variable-name").fill(keptName);
    await page.getByTestId("context-variable-value").fill("kept-on-error");
    const created = page.waitForResponse(
      (res) => propertyPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-variable-save").click();
    const posted = await created;
    if (!posted.ok()) {
      throw new Error(`create context variable HTTP ${posted.status()}`);
    }
    await expect(rowByName(page, keptName)).toBeVisible({ timeout: 20000 });

    const cases = [
      [400, "Context variable value is required", /value is required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Context variable already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/sites\/[^/]+\/properties(?:\?|$)/,
        (route) => {
          if (route.request().method() !== "PUT") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await page.getByTestId("context-variable-name").fill(`WillNotSave${status}${stamp}`);
      await page.getByTestId("context-variable-value").fill(`no-${status}`);
      await page.getByTestId("context-variable-save").click();
      await expect(page.getByTestId("context-variable-error")).toContainText(pattern);
      await expect(page.getByTestId("context-variable-form")).toBeVisible();
      await expect(rowByName(page, `WillNotSave${status}${stamp}`)).toHaveCount(0);
      await expect(
        rowByName(page, keptName).getByTestId("context-variable-row-value"),
      ).toHaveText("kept-on-error");
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
