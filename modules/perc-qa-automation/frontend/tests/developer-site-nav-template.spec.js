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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer site: change a navigation section landing-page template (#4958 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-template.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSitesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "sites",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return {
    assertClean() {
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter(
        (t) =>
          !/Failed to load resource/i.test(t) &&
          !/favicon/i.test(t) &&
          !/404/.test(t) &&
          !/403/.test(t) &&
          !/400/.test(t),
      );
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  };
}

async function openFirstEditableSite(page) {
  const rows = page.locator('[data-testid^="developer-site-row-"]');
  const count = await rows.count();
  expect(count).toBeGreaterThan(0);
  for (let i = 0; i < count; i += 1) {
    const open = rows.nth(i).locator('[data-testid="developer-site-open"]');
    const siteName = ((await open.getAttribute("aria-label")) || "")
      .replace(/^Open\s+/, "")
      .trim();
    await open.click();
    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const panel = page.locator('[data-testid="developer-site-nav-template"]');
    if (await panel.isVisible().catch(() => false)) {
      return siteName;
    }
    const back = page.locator('[data-testid="developer-site-back"]');
    if (await back.isVisible().catch(() => false)) {
      await back.click();
    } else {
      await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    }
  }
  throw new Error("No traditional site with a section template control");
}

test.describe("Developer change navigation section template (#4958)", () => {
  test("cancel does not post; 400 stays on the panel; a different template reloads", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    let writes = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/section/landingTemplate")) {
        writes += 1;
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const siteName = await openFirstEditableSite(page);
    await expect(page.locator('[data-testid="developer-site-nav-loading"]')).toHaveCount(0, {
      timeout: 30_000,
    });
    const loadErr = page.locator('[data-testid="developer-site-nav-load-error"]');
    if (await loadErr.isVisible().catch(() => false)) {
      throw new Error(`nav load failed: ${await loadErr.innerText()}`);
    }
    const target = page.locator('[data-testid="developer-site-nav-template-target"]');
    await expect(target).toBeEnabled({ timeout: 20_000 });
    const targetCount = await target.locator("option").count();
    if (targetCount > 1) {
      const switched = page.waitForResponse(
        (res) =>
          res.url().includes("/section/landingTemplate/") && res.request().method() === "GET",
        { timeout: 20_000 },
      );
      await target.selectOption({ index: 1 });
      await switched;
    }
    const settled = page.waitForResponse(
      (res) =>
        res.url().includes("/section/landingTemplate/") && res.request().method() === "GET",
      { timeout: 20_000 },
    );
    await target.selectOption({ index: 0 });
    await settled;
    const select = page.locator('[data-testid="developer-site-nav-template-id"]');
    await expect(select).toBeEnabled({ timeout: 20_000 });

    const loaded = (await select.getAttribute("data-loaded-template")) || "";
    const options = select.locator("option");
    const count = await options.count();
    expect(count).toBeGreaterThan(0);
    let other = "";
    for (let i = 0; i < count; i += 1) {
      const value = (await options.nth(i).getAttribute("value")) || "";
      if (value && value !== loaded) {
        other = value;
        break;
      }
    }
    expect(other, "need a second template to prove a change").not.toEqual("");

    await select.selectOption(other);
    const beforeCancel = writes;
    await page.locator('[data-testid="developer-site-nav-template-cancel"]').click();
    await expect(select).toHaveValue(loaded);
    expect(writes).toBe(beforeCancel);

    await page.route("**/section/landingTemplate", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({ message: "bad template" }),
      });
    });
    await select.selectOption(other);
    await page.locator('[data-testid="developer-site-nav-template-save"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-template-error"]')).toBeVisible();
    await page.unroute("**/section/landingTemplate");

    await select.selectOption(other);
    await page.locator('[data-testid="developer-site-nav-template-save"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-template-notice"]')).toBeVisible({
      timeout: 30_000,
    });

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const again = page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first();
    await again.click();
    const reloaded = page.locator('[data-testid="developer-site-nav-template-id"]');
    await expect
      .poll(async () => reloaded.getAttribute("data-loaded-template"), { timeout: 20_000 })
      .toBe(other);

    await page.route("**/section/landingTemplate", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 403,
        contentType: "application/json",
        body: JSON.stringify({ message: "forbidden" }),
      });
    });
    const third = loaded && loaded !== other ? loaded : other;
    if (third !== other) {
      await reloaded.selectOption(third);
      await page.locator('[data-testid="developer-site-nav-template-save"]').click();
      await expect(page.locator('[data-testid="developer-site-nav-template-error"]')).toBeVisible();
    }
    await page.unroute("**/section/landingTemplate");

    const templateTarget = page.locator('[data-testid="developer-site-nav-template-target"]');
    const templateTargetCount = await templateTarget.locator("option").count();
    if (templateTargetCount > 1) {
      await page.route("**/section/landingTemplate/**", async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 404,
          contentType: "application/json",
          body: JSON.stringify({ message: "missing" }),
        });
      });
      await templateTarget.selectOption({ index: 1 });
      await expect(page.locator('[data-testid="developer-site-nav-template-error"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.unroute("**/section/landingTemplate/**");
    }

    guards.assertClean();
  });
});
