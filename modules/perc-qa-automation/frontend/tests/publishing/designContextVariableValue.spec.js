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
 * PublishingShell Design — change one context variable value (#5413 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextVariableValue.spec.js
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

function isValuePut(request) {
  if (!propertyPut(request.url(), request.method())) {
    return false;
  }
  return (request.postData() || "").includes('"updateValue":true');
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

async function createVariable(page, name, value) {
  await page.getByTestId("context-variable-name").fill(name);
  await page.getByTestId("context-variable-value").fill(value);
  const created = page.waitForResponse(
    (res) =>
      propertyPut(res.url(), res.request().method()) &&
      !(res.request().postData() || "").includes('"updateValue":true') &&
      res.ok(),
    { timeout: 30000 },
  );
  await page.getByTestId("context-variable-save").click();
  const posted = await created;
  if (!posted.ok()) {
    throw new Error(`create context variable HTTP ${posted.status()}`);
  }
  await expect(rowByName(page, name)).toBeVisible({ timeout: 20000 });
  await expect(rowByName(page, name).getByTestId("context-variable-row-value")).toHaveText(
    value.trim(),
  );
}

test.describe("PublishingShell Design change one context variable value", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("saves a new value only after success and leaves the other", async ({ page }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    const valuePuts = [];
    page.on("request", (req) => {
      if (isValuePut(req)) {
        valuePuts.push(req.postData() || "");
      }
    });

    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvKeep${stamp}`;
    const nextName = `CvVal${stamp}`;
    await createVariable(page, keptName, "kept-value");
    await createVariable(page, nextName, "before-value");

    await rowByName(page, nextName).getByTestId("context-variable-change-value").click();
    await expect(page.getByTestId("context-variable-value-form")).toBeVisible();
    await expect(page.getByTestId("context-variable-value-name")).toHaveText(nextName);
    await expect(page.getByTestId("context-variable-value-current")).toHaveText("before-value");

    await page.getByTestId("context-variable-value-input").fill("   ");
    await page.getByTestId("context-variable-value-save").click();
    await expect(page.getByTestId("context-variable-value-error")).toContainText(
      /value is required/i,
    );
    expect(valuePuts).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "before-value",
    );

    await page.getByTestId("context-variable-value-input").fill("v".repeat(256));
    await page.getByTestId("context-variable-value-save").click();
    await expect(page.getByTestId("context-variable-value-error")).toContainText(
      /255 characters or fewer/i,
    );
    expect(valuePuts).toEqual([]);

    await page.getByTestId("context-variable-value-input").fill("will-cancel");
    await page.getByTestId("context-variable-value-cancel").click();
    await expect(page.getByTestId("context-variable-value-form")).toHaveCount(0);
    expect(valuePuts).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "before-value",
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );

    await rowByName(page, nextName).getByTestId("context-variable-change-value").click();
    await page.getByTestId("context-variable-value-input").fill("  next-value  ");
    const saved = page.waitForResponse(
      (res) => isValuePut(res.request()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-variable-value-save").click();
    const posted = await saved;
    const body = posted.request().postData() || "";
    expect(body).toContain("siteProperty");
    expect(body).toContain('"updateValue":true');
    expect(body).toContain(nextName);
    expect(body).toContain("next-value");
    expect(body).not.toContain(keptName);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "next-value",
      { timeout: 20000 },
    );
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-name")).toHaveText(
      nextName,
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );

    await openSites(page);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "next-value",
      { timeout: 20000 },
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 do not change the value", async ({ page }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvErrKeep${stamp}`;
    const nextName = `CvErrVal${stamp}`;
    await createVariable(page, keptName, "kept-on-error");
    await createVariable(page, nextName, "before-error");

    const cases = [
      [400, "Context variable value is required", /value is required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Context variable is not listed", /not listed|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/sites\/[^/]+\/properties(?:\?|$)/,
        (route) => {
          const request = route.request();
          if (!isValuePut(request)) {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await rowByName(page, nextName).getByTestId("context-variable-change-value").click();
      await page.getByTestId("context-variable-value-input").fill(`no-${status}`);
      await page.getByTestId("context-variable-value-save").click();
      await expect(page.getByTestId("context-variable-value-error")).toContainText(pattern);
      await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
        "before-error",
      );
      await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
        "kept-on-error",
      );
      await page.getByTestId("context-variable-value-cancel").click();
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
