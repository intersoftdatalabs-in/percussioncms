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
 * Developer site: reparent a navigation section (#4957 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-reparent.spec.js
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
        (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
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
    const add = page.locator('[data-testid="developer-site-nav-add"]');
    if (await add.isVisible().catch(() => false)) {
      return siteName;
    }
    const back = page.locator('[data-testid="developer-site-back"]');
    if (await back.isVisible().catch(() => false)) {
      await back.click();
    } else {
      await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    }
  }
  throw new Error("No traditional site with a navigation section editor");
}

async function parentOf(page, name) {
  const item = page.locator('[data-testid="developer-site-nav-item"]', { hasText: name });
  return item.first().getAttribute("data-parent-id");
}

test.describe("Developer reparent navigation section (#4957)", () => {
  test("cancel and same parent do not move; a new parent persists", async ({ page }) => {
    test.setTimeout(240_000);
    const guards = attachConsoleGuards(page);
    let moves = 0;
    let moveBody = "";
    let moveStatus = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/section/move")) {
        moves += 1;
        moveBody = req.postData() || "";
      }
    });
    page.on("response", (res) => {
      if (res.request().method() === "POST" && res.url().includes("/section/move")) {
        moveStatus = res.status();
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });
    const siteName = await openFirstEditableSite(page);
    const stamp = Date.now().toString().slice(-6);
    const moving = `ChildMove${stamp}`;
    const newParent = `ParentHold${stamp}`;
    const name = page.locator('[data-testid="developer-site-nav-name"]');
    async function addSection(sectionName) {
      await name.fill(sectionName);
      const created = page.waitForResponse(
        (res) =>
          res.request().method() === "POST" && res.url().includes("/section/create"),
        { timeout: 60_000 },
      );
      await page.locator('[data-testid="developer-site-nav-add"]').click();
      const response = await created;
      expect(response.status(), await response.text()).toBe(200);
      await expect(
        page.locator('[data-testid="developer-site-nav-item"]', { hasText: sectionName }),
      ).toBeVisible({ timeout: 20_000 });
    }
    await addSection(newParent);
    await addSection(moving);

    const target = page.locator('[data-testid="developer-site-nav-reparent-target"]');
    const parentSelect = page.locator('[data-testid="developer-site-nav-reparent-parent"]');
    async function choose(select, sectionName) {
      const id = await page
        .locator('[data-testid="developer-site-nav-item"]', { hasText: sectionName })
        .first()
        .getAttribute("data-section-id");
      expect(id, sectionName).toBeTruthy();
      await select.evaluate((el, value) => {
        const proto = Object.getOwnPropertyDescriptor(
          window.HTMLSelectElement.prototype,
          "value",
        );
        proto.set.call(el, value);
        el.dispatchEvent(new Event("change", { bubbles: true }));
      }, id);
      await expect(select).toHaveValue(id);
      return id;
    }
    await choose(target, moving);
    const sameParent = await parentSelect.inputValue();
    await page.locator('[data-testid="developer-site-nav-reparent-cancel"]').click();
    await page.locator('[data-testid="developer-site-nav-reparent-confirm"]').click();
    expect(moves).toBe(0);
    expect(await parentOf(page, moving)).toBe(sameParent);

    const newParentId = await choose(parentSelect, newParent);
    await page.locator('[data-testid="developer-site-nav-reparent-confirm"]').click();
    const error = page.locator('[data-testid="developer-site-nav-reparent-error"]');
    const notice = page.locator('[data-testid="developer-site-nav-reparent-notice"]');
    await expect(notice.or(error)).toBeVisible({ timeout: 60_000 });
    if (await error.isVisible()) {
      throw new Error(`reparent failed: ${await error.innerText()}`);
    }
    expect(moves).toBe(1);
    let after = sameParent;
    try {
      await expect
        .poll(async () => {
          after = await parentOf(page, moving);
          return after;
        }, { timeout: 20_000 })
        .not.toBe(sameParent);
    } catch (err) {
      throw new Error(
        `${err && err.message ? err.message : err} status ${moveStatus} body ${moveBody} selected ${await target.locator("option:checked").textContent()} after ${after}`,
      );
    }

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first()
      .click();
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: moving }),
    ).toBeVisible({ timeout: 20_000 });
    expect(await parentOf(page, moving)).toBe(newParentId);
    guards.assertClean();
  });
});
