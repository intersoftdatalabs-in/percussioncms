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
 * PublishingShell Design — copy one publishing context (#5183 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextCopy.spec.js
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

test.describe("PublishingShell Design copy publishing context", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("copies a context under a new name and leaves schemes on the source", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now().toString().slice(-8);
    const source = `CtxSrc${stamp}`;
    const copied = `CtxDst${stamp}`;
    const description = `Night copy source ${stamp}`;
    const scheme = `Sch${stamp}`;
    await seedContext(request, source, description);
    await openContexts(page);
    await selectContext(page, source);
    await createSchemeOnSelected(page, scheme);

    await page.getByTestId("context-copy").click();
    await expect(page.getByTestId("context-copy-form")).toBeVisible();
    await expect(page.getByTestId("context-copy-description")).toHaveText(description);
    await expect(page.getByRole("option", { name: copied, exact: true })).toHaveCount(0);

    await page.locator("#context-copy-name").fill(copied);
    let releaseCopy = () => undefined;
    const holdCopy = new Promise((resolve) => {
      releaseCopy = resolve;
    });
    let postedBody = "";
    await page.route(
      /\/services\/sitemanage\/publishingdesign\/contexts(?:\?|$)/,
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
        /\/publishingdesign\/contexts(?:\?|$)/.test(res.url()),
      { timeout: 30000 },
    );
    await page.getByTestId("context-copy-submit").click();
    await expect(page.getByTestId("context-copy-form")).toBeVisible();
    await expect.poll(() => postedBody).toContain(copied);
    expect(postedBody).toContain('"context"');
    expect(postedBody).toContain(description);
    expect(postedBody).not.toContain(source);
    expect(postedBody).not.toContain("defaultSchemeId");
    await expect(page.getByRole("option", { name: copied, exact: true })).toHaveCount(0);
    releaseCopy();
    const posted = await copyResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `copy context HTTP ${posted.status()} ${(await posted.text()).slice(0, 500)}`,
      );
    }
    await expect(page.getByTestId("context-copy-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.getByRole("option", { name: copied, exact: true })).toBeAttached();
    await expect(page.getByRole("option", { name: source, exact: true })).toBeAttached();
    await expect(page.getByLabel("Publishing context").locator("option:checked")).toHaveText(
      source,
    );
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();

    await selectContext(page, copied);
    await expect(page.getByText("No schemes for this context.")).toBeVisible();
    await page.getByRole("button", { name: "Edit context" }).click();
    await expect(page.getByTestId("context-editor")).toBeVisible();
    await expect(page.locator("#ctx-desc")).toHaveValue(description);
    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("contexts-panel")).toBeVisible();

    await selectContext(page, source);
    await expect(page.getByRole("button", { name: scheme, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("rejects a blank or overlong name and cancel does not call the server", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const source = `CtxVal${Date.now().toString().slice(-8)}`;
    await seedContext(request, source, "kept");
    await openContexts(page);
    await selectContext(page, source);

    const posts = [];
    page.on("request", (req) => {
      if (
        req.method() === "POST" &&
        /\/publishingdesign\/contexts(?:\?|$)/.test(req.url())
      ) {
        posts.push(req.url());
      }
    });

    await page.getByTestId("context-copy").click();
    await expect(page.getByTestId("context-copy-form")).toBeVisible();
    await page.locator("#context-copy-name").fill("");
    await page.getByTestId("context-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    await expect(page.getByTestId("context-copy-form")).toBeVisible();

    await page.locator("#context-copy-name").fill("n".repeat(51));
    await page.getByTestId("context-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/50 characters or fewer/i);

    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("context-copy-cancel").click();
    await expect(page.getByTestId("context-copy-form")).toBeHidden();
    await expect(page.getByRole("option", { name: source, exact: true })).toBeAttached();
    expect(posts, "blank, overlong, and cancel must not POST").toEqual([]);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("duplicate name and HTTP 400 or 403 do not list the copy", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const source = `CtxErr${Date.now().toString().slice(-8)}`;
    await seedContext(request, source, "kept");
    await openContexts(page);
    await selectContext(page, source);
    await page.getByTestId("context-copy").click();
    await expect(page.getByTestId("context-copy-form")).toBeVisible();

    await page.locator("#context-copy-name").fill(source);
    await page.getByTestId("context-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/already exists|409|Conflict/i);
    await expect(page.getByTestId("context-copy-form")).toBeVisible();

    const cases = [
      [400, "name is required", /required|400|Bad Request/i],
      [403, "Admin or Designer role required", /Admin or Designer|403|Forbidden/i],
    ];
    for (const [status, message, pattern] of cases) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        /\/services\/sitemanage\/publishingdesign\/contexts(?:\?|$)/,
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
      await page.locator("#context-copy-name").fill(requested);
      await page.getByTestId("context-copy-submit").click();
      await expect(page.getByRole("alert")).toContainText(pattern);
      await expect(page.getByTestId("context-copy-form")).toBeVisible();
    }
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("context-copy-cancel").click();
    await expect(page.getByRole("option", { name: source, exact: true })).toHaveCount(1);
    await expect(page.getByRole("option", { name: /WillNotCopy/ })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
