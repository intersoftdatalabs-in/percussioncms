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
 * PublishingShell Design — set or clear one publishing context description (#5250 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextDescription.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");

const GENERATOR =
  "Java/global/percussion/contentassembler/sys_JexlAssemblyLocation";

function contextUpdatePut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/contexts\/[^/]+$/.test(new URL(url).pathname);
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

async function seedContext(request, name, description) {
  const res = await request.post(
    `${BASE_URL}/Rhythmyx/services/sitemanage/publishingdesign/contexts`,
    {
      headers: {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      data: { context: { name, description } },
    },
  );
  if (res.status() !== 200 && res.status() !== 201) {
    throw new Error(
      `seed context HTTP ${res.status()} ${(await res.text()).slice(0, 400)}`,
    );
  }
}

async function selectContext(page, name) {
  const option = page.getByRole("option", { name, exact: true });
  await expect(option).toBeAttached({ timeout: 20000 });
  await page.getByLabel("Publishing context").selectOption({ label: name });
  return option;
}

async function createSchemeOnSelected(page, name) {
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

test.describe("PublishingShell Design publishing context description", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("saves a description only after reload and can clear it", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (contextUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    const stamp = Date.now().toString().slice(-8);
    const name = `CtxDsc${stamp}`;
    const original = `Night description ${stamp}`;
    const updated = `Updated description ${stamp}`;
    const scheme = `Sch${stamp}`;
    await seedContext(request, name, original);
    await openContexts(page);
    await selectContext(page, name);
    await createSchemeOnSelected(page, scheme);

    await expect(page.locator("[data-testid^='context-description-']")).toHaveText(original);
    await expect(page.getByRole("option", { name, exact: true })).toBeAttached();
    await page.getByTestId("context-describe").click();
    await expect(page.getByTestId("context-describe-form")).toBeVisible();
    await expect(page.getByTestId("context-describe-name")).toHaveText(name);
    await expect(page.getByTestId("context-describe-scheme")).toHaveText(scheme);
    await expect(page.locator("#context-describe-description")).toHaveValue(original);

    await page.locator("#context-describe-description").fill(updated);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("context-describe-cancel").click();
    await expect(page.getByTestId("context-describe-form")).toBeHidden();
    await expect(page.locator("[data-testid^='context-description-']")).toHaveText(original);
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByTestId("context-describe").click();
    await page.locator("#context-describe-description").fill("d".repeat(256));
    await page.getByTestId("context-describe-submit").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#context-describe-description").fill(updated);
    let releasePut = () => undefined;
    const holdPut = new Promise((resolve) => {
      releasePut = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/contexts\/[^/]+(?:\?|$)/,
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
      (res) => contextUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-describe-submit").click();
    await expect(page.getByTestId("context-describe-form")).toBeVisible();
    await expect.poll(() => postedBody).toContain(updated);
    expect(postedBody).toContain('"context"');
    expect(postedBody).not.toContain(name);
    expect(postedBody).not.toContain(scheme);
    expect(postedBody).not.toContain("defaultSchemeId");
    await expect(page.locator("[data-testid^='context-description-']")).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `describe context HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("context-describe-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.locator("[data-testid^='context-description-']")).toHaveText(updated, {
      timeout: 20000,
    });
    await expect(page.getByLabel("Publishing context").locator("option:checked")).toHaveText(
      name,
    );
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();

    await page.getByTestId("context-describe").click();
    await page.locator("#context-describe-description").fill("   ");
    const clearResponse = page.waitForResponse(
      (res) => contextUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 30000 },
    );
    await page.getByTestId("context-describe-submit").click();
    const cleared = await clearResponse;
    expect(cleared.request().postData() || "").toContain('"description":""');
    expect(cleared.request().postData() || "").not.toContain(name);
    expect(cleared.request().postData() || "").not.toContain(scheme);
    await expect(page.getByTestId("context-describe-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.locator("[data-testid^='context-description-']")).toHaveText("");
    await expect(page.getByLabel("Publishing context").locator("option:checked")).toHaveText(
      name,
    );
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old description and scheme", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `CtxErr${stamp}`;
    const description = "kept on error";
    const scheme = `SchErr${stamp}`;
    await seedContext(request, name, description);
    await openContexts(page);
    await selectContext(page, name);
    await createSchemeOnSelected(page, scheme);
    await page.getByTestId("context-describe").click();
    await expect(page.getByTestId("context-describe-form")).toBeVisible();

    const cases = [
      [
        400,
        "Publishing context description must be 255 characters or fewer",
        /255 characters or fewer|400|Bad Request/i,
      ],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Publishing context name already exists", /already exists|409|Conflict/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/contexts\/[^/]+(?:\?|$)/,
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
      await page.locator("#context-describe-description").fill(`WillNotSave${status}`);
      await page.getByTestId("context-describe-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("context-describe-form")).toBeVisible();
      await expect(page.getByTestId("context-describe-name")).toHaveText(name);
      await expect(page.getByTestId("context-describe-scheme")).toHaveText(scheme);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("context-describe-cancel").click();
    await expect(page.locator("[data-testid^='context-description-']")).toHaveText(description);
    await expect(page.getByLabel("Publishing context").locator("option:checked")).toHaveText(
      name,
    );
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
