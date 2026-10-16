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
 * PublishingShell Design — clear one context variable value (#5438 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextVariableClear.spec.js
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

function postBody(request) {
  return request.postData() || "";
}

function isClearPut(request) {
  if (!propertyPut(request.url(), request.method())) {
    return false;
  }
  return postBody(request).includes('"clearValue":true');
}

function isValuePut(request) {
  if (!propertyPut(request.url(), request.method())) {
    return false;
  }
  return postBody(request).includes('"updateValue":true');
}

function isCreatePut(request) {
  if (!propertyPut(request.url(), request.method())) {
    return false;
  }
  const body = postBody(request);
  return (
    !body.includes('"updateValue":true') &&
    !body.includes('"clearValue":true') &&
    !body.includes('"renameName":true')
  );
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
    (res) => isCreatePut(res.request()) && res.ok(),
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

test.describe("PublishingShell Design clear one context variable value", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("confirm clears one value and leaves the name and the other variable", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    const clearPuts = [];
    const valuePuts = [];
    page.on("request", (req) => {
      if (isClearPut(req)) {
        clearPuts.push(req.postData() || "");
      }
      if (isValuePut(req)) {
        valuePuts.push(req.postData() || "");
      }
    });

    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvClrKeep${stamp}`;
    const nextName = `CvClr${stamp}`;
    await createVariable(page, keptName, "kept-value");
    await createVariable(page, nextName, "before-value");

    await rowByName(page, nextName).getByTestId("context-variable-change-value").click();
    await page.getByTestId("context-variable-value-input").fill("   ");
    await page.getByTestId("context-variable-value-save").click();
    await expect(page.getByTestId("context-variable-value-error")).toContainText(
      /value is required/i,
    );
    expect(valuePuts).toEqual([]);
    expect(clearPuts).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "before-value",
    );
    await page.getByTestId("context-variable-value-cancel").click();

    await rowByName(page, nextName).getByTestId("context-variable-clear").click();
    await expect(page.getByTestId("context-variable-clear-form")).toBeVisible();
    await expect(page.getByTestId("context-variable-clear-name")).toHaveText(nextName);
    await expect(page.getByTestId("context-variable-clear-current")).toHaveText("before-value");
    await page.getByTestId("context-variable-clear-cancel").click();
    await expect(page.getByTestId("context-variable-clear-form")).toHaveCount(0);
    expect(clearPuts).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "before-value",
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );

    await rowByName(page, nextName).getByTestId("context-variable-clear").click();
    const saved = page.waitForResponse((res) => isClearPut(res.request()) && res.ok(), {
      timeout: 30000,
    });
    await page.getByTestId("context-variable-clear-confirm").click();
    const posted = await saved;
    const body = posted.request().postData() || "";
    expect(body).toContain("siteProperty");
    expect(body).toContain('"clearValue":true');
    expect(body).toContain(nextName);
    expect(body).not.toContain('"updateValue":true');
    expect(body).not.toContain('"value"');
    expect(body).not.toContain(keptName);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-name")).toHaveText(
      nextName,
    );
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "",
      { timeout: 20000 },
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );

    await openSites(page);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-name")).toHaveText(
      nextName,
      { timeout: 20000 },
    );
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "",
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous value", async ({ page }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvClrErrKeep${stamp}`;
    const nextName = `CvClrErr${stamp}`;
    await createVariable(page, keptName, "kept-on-error");
    await createVariable(page, nextName, "before-error");

    const cases = [
      [400, "Context variable name is required", /name is required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Context variable is not listed", /not listed|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/sites\/[^/]+\/properties(?:\?|$)/,
        (route) => {
          const request = route.request();
          if (!isClearPut(request)) {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await rowByName(page, nextName).getByTestId("context-variable-clear").click();
      await page.getByTestId("context-variable-clear-confirm").click();
      await expect(page.getByTestId("context-variable-clear-error")).toContainText(pattern);
      await expect(rowByName(page, nextName).getByTestId("context-variable-row-name")).toHaveText(
        nextName,
      );
      await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
        "before-error",
      );
      await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
        "kept-on-error",
      );
      await page.getByTestId("context-variable-clear-cancel").click();
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
