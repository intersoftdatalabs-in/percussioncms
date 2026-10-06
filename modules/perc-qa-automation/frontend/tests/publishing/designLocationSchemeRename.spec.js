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
 * PublishingShell Design — rename a location scheme (#5270 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designLocationSchemeRename.spec.js
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

function schemeUpdatePut(url, method) {
  return (
    method === "PUT" &&
    /\/publishingdesign\/schemes\/[^/]+(?:\?|$)/.test(url) &&
    !/\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(url)
  );
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
  const description = `Pages ${name}`;
  await page.locator("#sch-desc").fill(description);
  const contentTypeId = Date.now().toString().slice(-8);
  const templateId = String(Number(contentTypeId) + 1);
  await page.locator("#sch-ctype").fill(contentTypeId);
  await page.locator("#sch-tpl").fill(templateId);
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
  return { description, contentTypeId, templateId };
}

function schemeRow(page, name) {
  return page
    .getByRole("listitem")
    .filter({ has: page.getByRole("button", { name, exact: true }) });
}

test.describe("PublishingShell Design rename location scheme", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one scheme only after save and leaves the other fields stored", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (schemeUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    await openContexts(page);
    const stamp = Date.now().toString().slice(-8);
    const original = `SchRen${stamp}`;
    const renamed = `SchNew${stamp}`;
    const meta = await createScheme(page, original);

    await schemeRow(page, original).getByTestId("location-scheme-rename").click();
    await expect(page.getByTestId("scheme-rename")).toBeVisible();
    await expect(page.getByTestId("scheme-rename-generator")).toHaveText(GENERATOR);
    await expect(page.getByTestId("scheme-rename-description")).toHaveText(meta.description);
    await expect(page.getByTestId("scheme-rename-content-type")).toHaveText(
      meta.contentTypeId,
    );
    await expect(page.getByTestId("scheme-rename-template")).toHaveText(meta.templateId);
    await expect(page.getByTestId("scheme-rename-parameter")).toContainText("$sys.site.path");
    await expect(page.getByTestId("scheme-rename-parameters-note")).toContainText(
      /parameters stay/i,
    );
    await expect(page.locator("#scheme-rename-name")).toHaveValue(original);

    await page.locator("#scheme-rename-name").fill(renamed);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-rename-cancel").click();
    await expect(page.getByTestId("scheme-rename")).toBeHidden();
    await expect(page.getByRole("button", { name: original, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: renamed, exact: true })).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await schemeRow(page, original).getByTestId("location-scheme-rename").click();
    await expect(page.getByTestId("scheme-rename")).toBeVisible();
    await page.locator("#scheme-rename-name").fill("   ");
    await page.getByTestId("location-scheme-rename-submit").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#scheme-rename-name").fill("n".repeat(51));
    await page.getByTestId("location-scheme-rename-submit").click();
    await expect(page.getByRole("alert")).toContainText(/50 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("scheme-rename")).toBeVisible();

    await page.locator("#scheme-rename-name").fill(renamed);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/schemes\/[^/]+(?:\?|$)/,
      async (route) => {
        if (route.request().method() !== "PUT") {
          return route.continue();
        }
        postedBody = route.request().postData() || "";
        await holdPut;
        return route.continue();
      },
    );
    const putResponse = page.waitForResponse(
      (res) => schemeUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("location-scheme-rename-submit").click();
    await expect(page.getByTestId("scheme-rename")).toBeVisible();
    await expect.poll(() => postedBody).toContain(renamed);
    expect(postedBody).toContain('"locationScheme"');
    expect(postedBody).not.toContain(GENERATOR);
    expect(postedBody).not.toContain(meta.description);
    expect(postedBody).not.toContain("schemeParameter");
    expect(postedBody).not.toContain('"generator"');
    expect(postedBody).not.toContain('"contentTypeId"');
    expect(postedBody).not.toContain('"templateId"');
    await expect(page.getByRole("button", { name: renamed, exact: true })).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `rename scheme HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("scheme-rename")).toBeHidden({ timeout: 20000 });
    await expect(page.getByRole("button", { name: renamed, exact: true })).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name: original, exact: true })).toHaveCount(0);

    await page.getByRole("button", { name: renamed, exact: true }).click();
    await expect(page.getByTestId("scheme-editor")).toBeVisible();
    await expect(page.locator("#sch-name")).toHaveValue(renamed);
    await expect(page.locator("#sch-gen")).toHaveValue(GENERATOR);
    await expect(page.locator("#sch-desc")).toHaveValue(meta.description);
    await expect(page.locator("#sch-ctype")).toHaveValue(meta.contentTypeId);
    await expect(page.locator("#sch-tpl")).toHaveValue(meta.templateId);
    await expect(page.getByText("path (String): $sys.site.path")).toBeVisible();
    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("contexts-panel")).toBeVisible();
    await expect(page.getByRole("button", { name: renamed, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old name", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const source = `SchErr${Date.now().toString().slice(-8)}`;
    const meta = await createScheme(page, source);
    await schemeRow(page, source).getByTestId("location-scheme-rename").click();
    await expect(page.getByTestId("scheme-rename")).toBeVisible();
    await expect(page.getByTestId("scheme-rename-description")).toHaveText(meta.description);

    const cases = [
      [
        400,
        "Location scheme name must be 50 characters or fewer",
        /50 characters or fewer|400|Bad Request/i,
      ],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Location scheme name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/schemes\/[^/]+(?:\?|$)/,
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
      const requested = `WillNotRename${status}`;
      await page.locator("#scheme-rename-name").fill(requested);
      await page.getByTestId("location-scheme-rename-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("scheme-rename")).toBeVisible();
      await expect(page.getByTestId("scheme-rename-generator")).toHaveText(GENERATOR);
      await expect(page.getByTestId("scheme-rename-description")).toHaveText(meta.description);
      await expect(page.getByTestId("scheme-rename-parameter")).toContainText("$sys.site.path");
      await expect(page.getByRole("button", { name: requested, exact: true })).toHaveCount(0);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-rename-cancel").click();
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: /WillNotRename/ })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
