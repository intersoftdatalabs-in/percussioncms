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
 * PublishingShell Design — set a location scheme generator (#5299 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designLocationSchemeGenerator.spec.js
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
  const schemes = page.waitForResponse(
    (res) =>
      res.request().method() === "GET" &&
      /\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Contexts/i }).click();
  await expect(page.getByTestId("contexts-panel")).toBeVisible();
  await schemes;
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

function schemeReadGet(url, method) {
  return (
    method === "GET" &&
    /\/publishingdesign\/schemes\/[^/]+(?:\?|$)/.test(url) &&
    !/\/publishingdesign\/contexts\/[^/]+\/schemes(?:\?|$)/.test(url)
  );
}

function parameterRows(raw) {
  if (Array.isArray(raw)) {
    return raw;
  }
  if (!raw || typeof raw !== "object") {
    return [];
  }
  const inner = raw.schemeParameter;
  if (Array.isArray(inner)) {
    return inner;
  }
  if (inner && typeof inner === "object") {
    return [inner];
  }
  if (raw.name != null || raw.value != null) {
    return [raw];
  }
  return [];
}

function parameterValues(payload) {
  const root =
    payload && payload.locationScheme && typeof payload.locationScheme === "object"
      ? payload.locationScheme
      : payload || {};
  return parameterRows(root.parameters ?? root.schemeParameter)
    .map((row) => ({
      name: row && row.name != null ? String(row.name) : "",
      type: row && row.type != null ? String(row.type) : "",
      value: row && row.value != null ? String(row.value) : "",
    }))
    .filter((row) => row.name || row.value);
}

function schemeFields(payload) {
  const root =
    payload && payload.locationScheme && typeof payload.locationScheme === "object"
      ? payload.locationScheme
      : payload || {};
  const text = (value) => (value == null || value === "" ? "" : String(value));
  return {
    name: text(root.name),
    generator: text(root.generator),
    description: text(root.description),
    contentTypeId: text(root.contentTypeId),
    templateId: text(root.templateId),
    parameters: parameterValues(payload),
  };
}

test.describe("PublishingShell Design set location scheme generator", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets one scheme generator only after save and leaves the other fields stored", async ({
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
    const nextGenerator = `sys_Gen${stamp}`;
    const name = `SchGen${stamp}`;
    const meta = await createScheme(page, name);

    const schemeRead = page.waitForResponse(
      (res) => schemeReadGet(res.url(), res.request().method()),
      { timeout: 30000 },
    );
    await schemeRow(page, name).getByTestId("location-scheme-generator").click();
    const loaded = await schemeRead;
    const loadedBody = await loaded.text();
    let stored = schemeFields({});
    try {
      stored = schemeFields(JSON.parse(loadedBody));
    } catch {
      stored = schemeFields({});
    }
    await expect(page.getByTestId("scheme-generator")).toBeVisible();
    await expect(page.getByTestId("scheme-generator-name")).toHaveText(name);
    await expect(page.getByTestId("scheme-generator-description")).toHaveText(
      stored.description || meta.description,
    );
    await expect(page.getByTestId("scheme-generator-content-type")).toHaveText(
      stored.contentTypeId || meta.contentTypeId,
    );
    await expect(page.getByTestId("scheme-generator-template")).toHaveText(
      stored.templateId || meta.templateId,
    );
    await expect(page.getByTestId("scheme-generator-parameter")).toContainText(
      "$sys.site.path",
    );
    await expect(page.getByTestId("scheme-generator-parameters-note")).toContainText(
      /parameters stay/i,
    );
    await expect(page.locator("#scheme-generator-input")).toHaveValue(
      stored.generator || GENERATOR,
    );
    await expect(schemeRow(page, name).getByTestId(/scheme-list-generator-/)).toHaveCount(0);

    await page.locator("#scheme-generator-input").fill(nextGenerator);
    const putsBeforeCancel = putBodies.length;
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-generator-cancel").click();
    await expect(page.getByTestId("scheme-generator")).toBeHidden();
    await expect(schemeRow(page, name).getByTestId(/scheme-list-generator-/)).toHaveText(
      stored.generator || GENERATOR,
    );
    expect(putBodies.length).toBe(putsBeforeCancel);

    await schemeRow(page, name).getByTestId("location-scheme-generator").click();
    await expect(page.getByTestId("scheme-generator")).toBeVisible();
    await page.locator("#scheme-generator-input").fill("   ");
    await page.getByTestId("location-scheme-generator-save").click();
    await expect(page.getByRole("alert")).toContainText(/generator is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#scheme-generator-input").fill("g".repeat(256));
    await page.getByTestId("location-scheme-generator-save").click();
    await expect(page.getByRole("alert")).toContainText(/255 characters or fewer/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("scheme-generator")).toBeVisible();

    await page.locator("#scheme-generator-input").fill(`  ${nextGenerator}  `);
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
    await page.getByTestId("location-scheme-generator-save").click();
    await expect(page.getByTestId("scheme-generator")).toBeVisible();
    await expect.poll(() => postedBody).toContain(nextGenerator);
    expect(postedBody).toContain('"locationScheme"');
    expect(postedBody).toContain('"generator"');
    expect(postedBody).not.toContain('"name"');
    expect(postedBody).not.toContain('"description"');
    expect(postedBody).not.toContain('"contentTypeId"');
    expect(postedBody).not.toContain('"templateId"');
    expect(postedBody).not.toContain("schemeParameter");
    await expect(schemeRow(page, name).getByTestId(/scheme-list-generator-/)).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `generator scheme HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("scheme-generator")).toBeHidden({ timeout: 20000 });
    await expect(schemeRow(page, name).getByTestId(/scheme-list-generator-/)).toHaveText(
      nextGenerator,
      { timeout: 20000 },
    );
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();

    await page.getByRole("button", { name, exact: true }).click();
    await expect(page.getByTestId("scheme-editor")).toBeVisible();
    await expect(page.locator("#sch-name")).toHaveValue(name);
    await expect(page.locator("#sch-gen")).toHaveValue(nextGenerator);
    await expect(page.locator("#sch-desc")).toHaveValue(stored.description || meta.description);
    await expect(page.locator("#sch-ctype")).toHaveValue(
      stored.contentTypeId || meta.contentTypeId,
    );
    await expect(page.locator("#sch-tpl")).toHaveValue(stored.templateId || meta.templateId);
    await expect(page.getByTestId("scheme-editor")).toContainText("path");
    await expect(page.getByTestId("scheme-editor")).toContainText("$sys.site.path");
    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("contexts-panel")).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the old generator", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const source = `SchGErr${Date.now().toString().slice(-8)}`;
    const meta = await createScheme(page, source);
    await schemeRow(page, source).getByTestId("location-scheme-generator").click();
    await expect(page.getByTestId("scheme-generator")).toBeVisible();
    await expect(page.getByTestId("scheme-generator-description")).toHaveText(meta.description);
    await expect(page.locator("#scheme-generator-input")).toHaveValue(GENERATOR);

    const cases = [
      [
        400,
        "Location scheme generator must be 255 characters or fewer",
        /255 characters or fewer|400|Bad Request/i,
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
      const requested = `WillNotSave${status}`;
      await page.locator("#scheme-generator-input").fill(requested);
      await page.getByTestId("location-scheme-generator-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("scheme-generator")).toBeVisible();
      await expect(page.getByTestId("scheme-generator-name")).toHaveText(source);
      await expect(page.getByTestId("scheme-generator-description")).toHaveText(meta.description);
      await expect(page.getByTestId("scheme-generator-parameters")).toBeAttached();
      await expect(schemeRow(page, source).getByTestId(/scheme-list-generator-/)).toHaveCount(
        0,
      );
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-generator-cancel").click();
    await expect(schemeRow(page, source).getByTestId(/scheme-list-generator-/)).toHaveText(
      GENERATOR,
    );
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
