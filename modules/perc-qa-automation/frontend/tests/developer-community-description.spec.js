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
 * Developer Communities description (#5178 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-community-description.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueCommunityName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QA 5178 ${label} ${suffix || "comm"}`;
}

async function openCommunitiesCatalog(page) {
  await page.goto(developerCommunitiesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-comm-panel"]');
  const empty = page.locator('[data-testid="developer-comm-empty"]');
  const listError = page.locator('[data-testid="developer-comm-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer communities catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-comm-new"]')).toBeVisible();
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
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

function descriptionPosts(page, bucket) {
  page.on("request", (req) => {
    if (
      req.method() === "POST" &&
      /\/communities\/[^/]+\/description(?:\?|$)/.test(req.url())
    ) {
      bucket.push(req.url());
    }
  });
}

async function createCommunity(page, name) {
  await page.locator('[data-testid="developer-comm-new"]').click();
  await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-comm-name"]').fill(name);
  await page.locator('[data-testid="developer-comm-create"]').click();
  const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-comm-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
  await page.locator('[data-testid="developer-comm-back"]').click();
  await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible({
    timeout: 20_000,
  });
}

async function openCommunity(page, name) {
  await page.locator(`[data-comm-name="${name}"]`).click();
  await expect(page.locator('[data-testid="developer-comm-description-input"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-comm-detail-title"]')).toContainText(name);
}

test.describe("Developer community description (#5178 / #1690)", () => {
  test("cancel, unchanged, and overlong text do not save", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const posts = [];
    descriptionPosts(page, posts);
    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName("keep");
    await createCommunity(page, name);
    await openCommunity(page, name);

    const stored = page.locator('[data-testid="developer-comm-description"]');
    const input = page.locator('[data-testid="developer-comm-description-input"]');
    const save = page.locator('[data-testid="developer-comm-description-save"]');
    const title = page.locator('[data-testid="developer-comm-detail-title"]');
    await expect(input).toHaveValue("");
    await expect(save).toBeDisabled();
    await expect(stored).toHaveText("");

    await input.fill("   ");
    await expect(save).toBeDisabled();
    expect(posts, `unexpected description POST: ${posts.join(" | ")}`).toEqual([]);

    const drafted = "Draft only";
    await input.fill(drafted);
    await expect(stored).toHaveText("");
    await expect(stored).not.toContainText(drafted);
    await expect(title).toContainText(name);
    await page.locator('[data-testid="developer-comm-description-cancel"]').click();
    await expect(input).toHaveValue("");
    expect(posts, `unexpected description POST: ${posts.join(" | ")}`).toEqual([]);

    await input.fill("D".repeat(256));
    await expect(page.locator('[data-testid="developer-comm-description-error"]')).toBeVisible();
    await expect(save).toBeDisabled();
    await expect(stored).toHaveText("");
    expect(posts, `unexpected description POST: ${posts.join(" | ")}`).toEqual([]);

    await page.locator('[data-testid="developer-comm-back"]').click();
    await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible();
    await expect(page.locator('[data-testid="developer-comm-table"]')).not.toContainText(drafted);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin save updates the detail and catalog only after success, including clear", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName("desc");
    const notes = `Notes ${Date.now().toString(36)}`;
    await createCommunity(page, name);
    await openCommunity(page, name);

    const stored = page.locator('[data-testid="developer-comm-description"]');
    const input = page.locator('[data-testid="developer-comm-description-input"]');
    const title = page.locator('[data-testid="developer-comm-detail-title"]');
    let failDescriptionOnce = true;
    await page.route(/\/communities\/[^/?]+\/description(?:\?|$)/, async (route) => {
      if (failDescriptionOnce && route.request().method() === "POST") {
        failDescriptionOnce = false;
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "design lock" }),
        });
        return;
      }
      await route.continue();
    });

    await input.fill(notes);
    await expect(stored).toHaveText("");
    await expect(title).toContainText(name);
    await page.locator('[data-testid="developer-comm-description-save"]').click();
    const err = page.locator('[data-testid="developer-comm-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/lock|409/i);
    await expect(stored).toHaveText("");
    await expect(stored).not.toContainText(notes);
    await expect(title).toContainText(name);

    await page.locator('[data-testid="developer-comm-description-save"]').click();
    await expect(stored).toContainText(notes, { timeout: 20_000 });
    await expect(err).toHaveCount(0);
    await expect(title).toContainText(name);

    await page.locator('[data-testid="developer-comm-back"]').click();
    const table = page.locator('[data-testid="developer-comm-table"]');
    await expect(table).toContainText(notes, { timeout: 20_000 });
    await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible();

    await openCommunity(page, name);
    await expect(stored).toContainText(notes);
    await input.fill("");
    await page.locator('[data-testid="developer-comm-description-save"]').click();
    await expect(stored).toHaveText("", { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-comm-detail-notice"]')).toContainText(
      /cleared/i,
    );

    await page.locator('[data-testid="developer-comm-back"]').click();
    await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible({
      timeout: 20_000,
    });
    await expect(table).not.toContainText(notes);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
