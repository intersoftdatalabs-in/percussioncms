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
 * PublishingShell edit the open site protocol (#5001 / parent #4531).
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/siteProtocol.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell edit the open site protocol", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("save shows http or https after refresh; cancel and 409 do not", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (
        text.includes("status of 400") ||
        text.includes("status of 403") ||
        text.includes("status of 409")
      ) {
        return;
      }
      jsErrors.push(text);
    });

    const stamp = Date.now().toString().slice(-8);
    const created = `Proto${stamp}`;
    let putCalls = 0;
    page.on("request", (req) => {
      const path = new URL(req.url()).pathname;
      if (req.method() === "PUT" && /\/sites\/[^/]+$/.test(path)) {
        putCalls += 1;
      }
    });

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("publish-sites-create").click();
    await page.getByTestId("publish-site-create-name").fill(created);
    await page.getByTestId("publish-site-create-save").click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
      timeout: 30000,
    });
    const shown = page.getByTestId("publish-site-protocol");
    await expect(shown).toBeVisible();
    const before = ((await shown.textContent()) || "").trim().toLowerCase();
    const next = before === "http" ? "https" : "http";

    const putsBeforeCancel = putCalls;
    await page.getByTestId("publish-site-protocol-edit").click();
    await page.getByTestId("publish-site-protocol-input").selectOption(next);
    await page.getByTestId("publish-site-protocol-cancel").click();
    await expect(page.getByTestId("publish-site-protocol-form")).toHaveCount(0);
    expect(putCalls).toBe(putsBeforeCancel);
    await expect(shown).toHaveText(before || "—");

    await page.route("**/services/sites/**", async (route) => {
      if (route.request().method() !== "PUT") {
        return route.continue();
      }
      return route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "conflict" }),
      });
    });
    await page.getByTestId("publish-site-protocol-edit").click();
    await page.getByTestId("publish-site-protocol-input").selectOption(next);
    await page.getByTestId("publish-site-protocol-save").click();
    await expect(page.getByTestId("publish-site-protocol-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-protocol-form")).toBeVisible();
    await expect(page.getByTestId("publish-site-protocol-saved")).toHaveCount(0);
    await expect(shown).not.toHaveText(next);
    await page.unrouteAll({ behavior: "ignoreErrors" });

    await page.getByTestId("publish-site-protocol-input").selectOption(next);
    const putResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "PUT" &&
        /\/sites\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 20000 },
    );
    await page.getByTestId("publish-site-protocol-save").click();
    const saved = await putResponse;
    if (saved.status() !== 200) {
      throw new Error(
        `update HTTP ${saved.status()} ${saved.url()} ${(await saved.text()).slice(0, 400)}`,
      );
    }
    await expect(shown).toHaveText(next);
    await expect(page.getByTestId("publish-site-protocol-saved")).toBeVisible();

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 20000,
    });
    await page.getByTestId(`publish-site-card-${created}`).click();
    await expect(page.getByTestId("publish-site-protocol")).toHaveText(next, {
      timeout: 20000,
    });

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
