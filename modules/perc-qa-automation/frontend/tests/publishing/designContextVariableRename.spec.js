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
 * PublishingShell Design — rename one context variable (#5437 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextVariableRename.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function propertyPath(url) {
  try {
    return /\/publishingdesign\/sites\/[^/]+\/properties$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
}

function postData(request) {
  return request.postData() || "";
}

function isCreatePut(request) {
  if (request.method() !== "PUT" || !propertyPath(request.url())) {
    return false;
  }
  const body = postData(request);
  return !body.includes('"updateValue":true') && !body.includes('"renameName":true');
}

function isRenamePut(request) {
  if (request.method() !== "PUT" || !propertyPath(request.url())) {
    return false;
  }
  return postData(request).includes('"renameName":true');
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

test.describe("PublishingShell Design rename one context variable", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("save renames one name and cancel keeps both", async ({ page }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    const renames = [];
    page.on("request", (req) => {
      if (isRenamePut(req)) {
        renames.push(req.postData() || "");
      }
    });

    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvRenKeep${stamp}`;
    const nextName = `CvRen${stamp}`;
    const renamedName = `CvRenNew${stamp}`;
    await createVariable(page, keptName, "kept-value");
    await createVariable(page, nextName, "stays-value");

    await rowByName(page, nextName).getByTestId("context-variable-rename").click();
    await expect(page.getByTestId("context-variable-rename-form")).toBeVisible();
    await expect(page.getByTestId("context-variable-rename-name")).toHaveText(nextName);
    await expect(page.getByTestId("context-variable-rename-value")).toHaveText("stays-value");
    await expect(
      page.getByTestId("context-variable-rename-other").filter({ hasText: keptName }),
    ).toContainText(keptName);
    expect(renames).toEqual([]);

    await page.getByTestId("context-variable-rename-input").fill("   ");
    await page.getByTestId("context-variable-rename-save").click();
    await expect(page.getByTestId("context-variable-rename-error")).toContainText(
      /name is required/i,
    );
    expect(renames).toEqual([]);

    await page.getByTestId("context-variable-rename-input").fill("n".repeat(51));
    await page.getByTestId("context-variable-rename-save").click();
    await expect(page.getByTestId("context-variable-rename-error")).toContainText(
      /50 characters or fewer/i,
    );
    expect(renames).toEqual([]);

    await page.getByTestId("context-variable-rename-input").fill(keptName);
    await page.getByTestId("context-variable-rename-save").click();
    await expect(page.getByTestId("context-variable-rename-error")).toContainText(
      /already exists/i,
    );
    expect(renames).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "stays-value",
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );

    await page.getByTestId("context-variable-rename-input").fill(`  ${renamedName}  `);
    await page.getByTestId("context-variable-rename-cancel").click();
    await expect(page.getByTestId("context-variable-rename-form")).toHaveCount(0);
    expect(renames).toEqual([]);
    await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
      "stays-value",
    );

    await rowByName(page, nextName).getByTestId("context-variable-rename").click();
    await page.getByTestId("context-variable-rename-input").fill(`  ${renamedName}  `);
    const renamed = page.waitForResponse(
      (res) => isRenamePut(res.request()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-variable-rename-save").click();
    const posted = await renamed;
    const body = posted.request().postData() || "";
    expect(body).toContain('"renameName":true');
    expect(body).toContain(`"name":"${nextName}"`);
    expect(body).toContain(`"newName":"${renamedName}"`);
    expect(body).not.toContain('"updateValue":true');
    expect(body).not.toContain(keptName);
    await expect(rowByName(page, nextName)).toHaveCount(0, { timeout: 20000 });
    await expect(rowByName(page, renamedName).getByTestId("context-variable-row-value")).toHaveText(
      "stays-value",
    );
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );
    expect(renames).toHaveLength(1);

    await openSites(page);
    await expect(rowByName(page, nextName)).toHaveCount(0);
    await expect(
      rowByName(page, renamedName).getByTestId("context-variable-row-value"),
    ).toHaveText("stays-value", { timeout: 20000 });
    await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
      "kept-value",
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old name", async ({ page }) => {
    test.setTimeout(120_000);
    const jsErrors = trackJsErrors(page);
    await openSites(page);
    const stamp = Date.now().toString().slice(-8);
    const keptName = `CvRenErrKeep${stamp}`;
    const nextName = `CvRenErr${stamp}`;
    await createVariable(page, keptName, "kept-on-error");
    await createVariable(page, nextName, "stays-on-error");

    const cases = [
      [400, "Context variable name is required", /name is required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Context variable already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/sites\/[^/]+\/properties(?:\?|$)/,
        (route) => {
          const request = route.request();
          if (!isRenamePut(request)) {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await rowByName(page, nextName).getByTestId("context-variable-rename").click();
      await page.getByTestId("context-variable-rename-input").fill(`no-${status}-${stamp}`);
      await page.getByTestId("context-variable-rename-save").click();
      await expect(page.getByTestId("context-variable-rename-error")).toContainText(pattern);
      await expect(rowByName(page, nextName).getByTestId("context-variable-row-value")).toHaveText(
        "stays-on-error",
      );
      await expect(rowByName(page, keptName).getByTestId("context-variable-row-value")).toHaveText(
        "kept-on-error",
      );
      await page.getByTestId("context-variable-rename-cancel").click();
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
