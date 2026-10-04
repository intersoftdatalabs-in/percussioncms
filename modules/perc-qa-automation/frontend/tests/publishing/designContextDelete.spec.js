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
 * PublishingShell Design — delete a publishing context (#5136 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContextDelete.spec.js
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

/**
 * Seed a disposable context. Design create still posts a flat body; JAXB
 * requires the {@code context} root (context save stays #4703).
 */
async function seedContext(request, name) {
  const res = await request.post(
    `${BASE_URL}/Rhythmyx/services/sitemanage/publishingdesign/contexts`,
    {
      headers: {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      data: { context: { name, description: "night-issue-prs #5136" } },
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

test.describe("PublishingShell Design delete publishing context", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("deletes a context with no schemes only after confirm succeeds", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const name = `CtxDel${Date.now().toString().slice(-8)}`;
    await seedContext(request, name);
    await openContexts(page);
    await selectContext(page, name);
    await expect(page.getByTestId("context-delete")).toBeVisible();

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(/Delete this design object/i);
      await dialog.accept();
    });
    const deleteResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "DELETE" &&
        /\/publishingdesign\/contexts\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 30000 },
    );
    await page.getByTestId("context-delete").click();
    const deleted = await deleteResponse;
    if (deleted.status() !== 200 && deleted.status() !== 204) {
      throw new Error(
        `delete HTTP ${deleted.status()} ${deleted.url()} ${(await deleted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByRole("option", { name, exact: true })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not call the server", async ({ page, request }) => {
    const jsErrors = trackJsErrors(page);
    const name = `CtxKeep${Date.now().toString().slice(-8)}`;
    await seedContext(request, name);
    await openContexts(page);
    await selectContext(page, name);
    let deletes = 0;
    await page.route(
      "**/services/sitemanage/publishingdesign/contexts/**",
      (route) => {
        if (route.request().method() === "DELETE") {
          deletes += 1;
          return route.abort();
        }
        return route.continue();
      },
    );
    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await page.getByTestId("context-delete").click();
    await expect(page.getByRole("option", { name, exact: true })).toBeAttached();
    expect(deletes).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps a context that still has location schemes", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const name = `CtxSch${Date.now().toString().slice(-8)}`;
    await seedContext(request, name);
    await openContexts(page);
    await selectContext(page, name);
    await createSchemeOnSelected(page, `Sch${name}`);

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.accept();
    });
    const deleteResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "DELETE" &&
        /\/publishingdesign\/contexts\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 30000 },
    );
    await page.getByTestId("context-delete").click();
    const denied = await deleteResponse;
    expect(denied.status()).toBe(409);
    await expect(page.getByRole("alert")).toContainText(/location schemes/i);
    await expect(page.getByRole("option", { name, exact: true })).toBeAttached();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("surfaces HTTP 400, 403, and 409 without removing the context", async ({
    page,
    request,
  }) => {
    const jsErrors = trackJsErrors(page);
    const name = `CtxErr${Date.now().toString().slice(-8)}`;
    await seedContext(request, name);
    await openContexts(page);
    await selectContext(page, name);

    async function stubDelete(status, message) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        "**/services/sitemanage/publishingdesign/contexts/**",
        (route) => {
          if (route.request().method() !== "DELETE") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      page.once("dialog", (dialog) => dialog.accept());
      await page.getByTestId("context-delete").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByRole("option", { name, exact: true })).toBeAttached();
    }

    await stubDelete(400, "contextId is required");
    await stubDelete(403, "Admin or Designer role required");
    await stubDelete(409, "Publishing context has location schemes");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
