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
 * PublishingShell Design — rename one location scheme parameter (#5385 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designLocationSchemeParameterName.spec.js
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

function schemeReadGet(url, method) {
  return (
    method === "GET" &&
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

async function createScheme(page, name, description) {
  await page.getByTestId("design-add-location-scheme").click();
  await expect(page.getByTestId("scheme-editor")).toBeVisible();
  await page.locator("#sch-name").fill(name);
  await page.locator("#sch-gen").fill(GENERATOR);
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
  if (raw.name != null || raw.newName != null) {
    return [raw];
  }
  return [];
}

function postedParameters(payload) {
  const root =
    payload && payload.locationScheme && typeof payload.locationScheme === "object"
      ? payload.locationScheme
      : payload || {};
  return parameterRows(root.parameters ?? root.schemeParameter).map((row) => {
    const posted = {
      name: row && row.name != null ? String(row.name) : "",
    };
    if (row && row.newName != null) {
      posted.newName = String(row.newName);
    }
    if (row && row.type != null) {
      posted.type = String(row.type);
    }
    if (row && row.value != null) {
      posted.value = String(row.value);
    }
    if (row && row.sequence != null) {
      posted.sequence = Number(row.sequence);
    }
    return posted;
  });
}

function nameButton(page, schemeName, parameterName) {
  return schemeRow(page, schemeName)
    .getByTestId("scheme-list-parameter")
    .filter({ hasText: parameterName })
    .getByTestId("location-scheme-parameter-name");
}

function listedParameters(page, schemeName) {
  return schemeRow(page, schemeName).getByTestId(/scheme-list-parameters-/);
}

async function addSecondParameter(page, schemeName, paramName, paramValue) {
  const schemeRead = page.waitForResponse(
    (res) => schemeReadGet(res.url(), res.request().method()),
    { timeout: 30000 },
  );
  await schemeRow(page, schemeName).getByTestId("location-scheme-add-parameter").click();
  await schemeRead;
  await expect(page.getByTestId("scheme-add-parameter")).toBeVisible();
  await page.locator("#scheme-add-parameter-name-input").fill(paramName);
  await page.locator("#scheme-add-parameter-type-input").selectOption("BackendColumn");
  await page.locator("#scheme-add-parameter-value-input").fill(paramValue);
  const putResponse = page.waitForResponse(
    (res) => schemeUpdatePut(res.url(), res.request().method()) && res.ok(),
    { timeout: 30000 },
  );
  await page.getByTestId("location-scheme-add-parameter-save").click();
  const posted = await putResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `add parameter HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("scheme-add-parameter")).toBeHidden({ timeout: 20000 });
  const listed = listedParameters(page, schemeName);
  await expect(listed).toContainText(paramName, { timeout: 20000 });
  await expect(listed).toContainText("path");
}

test.describe("PublishingShell Design rename one location scheme parameter", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one parameter only after save and keeps type, value, sequence, and the other parameter", async ({
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
    const name = `SchNam${stamp}`;
    const paramName = `suffix${stamp.slice(-4)}`;
    const nextName = `file${stamp.slice(-4)}`;
    const paramValue = "article";
    const meta = await createScheme(page, name, `Pages ${name}`);
    await addSecondParameter(page, name, paramName, paramValue);

    const schemeRead = page.waitForResponse(
      (res) => schemeReadGet(res.url(), res.request().method()),
      { timeout: 30000 },
    );
    await nameButton(page, name, paramName).click();
    await schemeRead;
    await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
    await expect(page.getByTestId("scheme-parameter-name-name")).toHaveText(name);
    await expect(page.getByTestId("scheme-parameter-name-generator")).toHaveText(GENERATOR);
    await expect(page.getByTestId("scheme-parameter-name-description")).toHaveText(
      meta.description,
    );
    await expect(page.getByTestId("scheme-parameter-name-content-type")).toHaveText(
      meta.contentTypeId,
    );
    await expect(page.getByTestId("scheme-parameter-name-template")).toHaveText(meta.templateId);
    await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText(paramName);
    await expect(page.getByTestId("scheme-parameter-name-type")).toHaveText("BackendColumn");
    await expect(page.getByTestId("scheme-parameter-name-sequence")).toHaveText("1");
    await expect(page.getByTestId("scheme-parameter-name-value")).toHaveText(paramValue);
    await expect(page.getByTestId("scheme-parameter-name-other")).toContainText("path (String) #0");
    await expect(page.locator("#scheme-parameter-name-input")).toHaveValue(paramName);

    const putsBeforeCancel = putBodies.length;
    await page.locator("#scheme-parameter-name-input").fill(nextName);
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-parameter-name-cancel").click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeHidden();
    await expect(listedParameters(page, name)).toContainText(`${paramName} (BackendColumn)`);
    await expect(listedParameters(page, name)).toContainText("path (String)");
    await expect(listedParameters(page, name)).not.toContainText(nextName);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await nameButton(page, name, paramName).click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
    const putsBeforeReject = putBodies.length;
    await page.locator("#scheme-parameter-name-input").fill("   ");
    await page.getByTestId("location-scheme-parameter-name-save").click();
    await expect(page.getByRole("alert")).toContainText(/name is required/i);
    await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText(paramName);

    await page.locator("#scheme-parameter-name-input").fill("n".repeat(51));
    await page.getByTestId("location-scheme-parameter-name-save").click();
    await expect(page.getByRole("alert")).toContainText(/50 characters or fewer/i);

    await page.locator("#scheme-parameter-name-input").fill("path");
    await page.getByTestId("location-scheme-parameter-name-save").click();
    await expect(page.getByRole("alert")).toContainText(/already exists/i);
    await expect(page.getByTestId("scheme-parameter-name-type")).toHaveText("BackendColumn");
    await expect(page.getByTestId("scheme-parameter-name-value")).toHaveText(paramValue);
    await expect(page.getByTestId("scheme-parameter-name-sequence")).toHaveText("1");
    expect(putBodies.length).toBe(putsBeforeReject);

    await page.locator("#scheme-parameter-name-input").fill(nextName);
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
    await page.getByTestId("location-scheme-parameter-name-save").click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
    await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText(paramName);
    await expect.poll(() => postedBody).toContain("updateParameterName");
    const postedJson = JSON.parse(postedBody);
    const postedRoot = postedJson.locationScheme || postedJson;
    expect(postedRoot.updateParameterName).toBe(true);
    expect(postedRoot.updateParameterSequence).toBeUndefined();
    expect(postedRoot.updateParameterType).toBeUndefined();
    expect(postedRoot.updateParameterValue).toBeUndefined();
    expect(postedRoot.addParameter).toBeUndefined();
    expect(postedRoot.removeParameter).toBeUndefined();
    expect(postedRoot.name).toBeUndefined();
    expect(postedRoot.generator).toBeUndefined();
    expect(postedRoot.description).toBeUndefined();
    expect(postedRoot.contentTypeId).toBeUndefined();
    expect(postedRoot.templateId).toBeUndefined();
    expect(postedParameters(postedJson)).toEqual([{ name: paramName, newName: nextName }]);
    await expect(listedParameters(page, name)).toHaveCount(0);
    releasePut();
    const posted = await putResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `rename parameter HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("scheme-parameter-name")).toBeHidden({ timeout: 20000 });
    const listed = listedParameters(page, name);
    await expect(listed).toContainText(`${nextName} (BackendColumn)`, { timeout: 20000 });
    await expect(listed).toContainText(paramValue);
    await expect(listed).toContainText("#1");
    await expect(listed).toContainText("path (String)");
    await expect(listed).toContainText("$sys.site.path");
    await expect(listed).not.toContainText(`${paramName} (BackendColumn)`);
    await expect(schemeRow(page, name).getByTestId(/scheme-list-generator-/)).toHaveText(
      GENERATOR,
    );
    await expect(schemeRow(page, name).getByTestId(/scheme-list-description-/)).toHaveText(
      meta.description,
    );
    await expect(schemeRow(page, name).getByTestId(/scheme-list-content-type-/)).toHaveText(
      meta.contentTypeId,
    );
    await expect(schemeRow(page, name).getByTestId(/scheme-list-template-/)).toHaveText(
      meta.templateId,
    );
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();

    await page.unrouteAll({ behavior: "ignoreErrors" });
    await nameButton(page, name, nextName).click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
    await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText(nextName);
    await expect(page.getByTestId("scheme-parameter-name-type")).toHaveText("BackendColumn");
    await expect(page.getByTestId("scheme-parameter-name-value")).toHaveText(paramValue);
    await expect(page.getByTestId("scheme-parameter-name-sequence")).toHaveText("1");
    await expect(page.locator("#scheme-parameter-name-input")).toHaveValue(nextName);
    await expect(page.getByTestId("scheme-parameter-name-other")).toContainText("#0");
    await page.getByTestId("location-scheme-parameter-name-cancel").click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeHidden();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous parameter name", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContexts(page);
    const source = `SchNErr${Date.now().toString().slice(-8)}`;
    await createScheme(page, source, `Pages ${source}`);
    await nameButton(page, source, "path").click();
    await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
    await expect(page.getByTestId("scheme-parameter-name-generator")).toHaveText(GENERATOR);
    await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText("path");
    await expect(page.getByTestId("scheme-parameter-name-type")).toHaveText("String");
    await expect(page.getByTestId("scheme-parameter-name-value")).toHaveText("$sys.site.path");
    await expect(page.getByTestId("scheme-parameter-name-sequence")).toHaveText("0");
    await page.locator("#scheme-parameter-name-input").fill("folder");

    const cases = [
      [
        400,
        "Rename one location scheme parameter at a time",
        /one location scheme parameter|400|Bad Request/i,
      ],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
      [409, "Parameter is not on this scheme", /not on this scheme|409|Conflict/i],
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
      await page.getByTestId("location-scheme-parameter-name-save").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("scheme-parameter-name")).toBeVisible();
      await expect(page.getByTestId("scheme-parameter-name-name")).toHaveText(source);
      await expect(page.getByTestId("scheme-parameter-name-generator")).toHaveText(GENERATOR);
      await expect(page.getByTestId("scheme-parameter-name-current")).toHaveText("path");
      await expect(page.getByTestId("scheme-parameter-name-type")).toHaveText("String");
      await expect(page.getByTestId("scheme-parameter-name-value")).toHaveText("$sys.site.path");
      await expect(page.getByTestId("scheme-parameter-name-sequence")).toHaveText("0");
      await expect(listedParameters(page, source)).toHaveCount(0);
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("location-scheme-parameter-name-cancel").click();
    await expect(listedParameters(page, source)).toContainText("path (String)");
    await expect(listedParameters(page, source)).toContainText("$sys.site.path");
    await expect(listedParameters(page, source)).not.toContainText("folder (String)");
    await expect(schemeRow(page, source).getByTestId(/scheme-list-generator-/)).toHaveText(
      GENERATOR,
    );
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
