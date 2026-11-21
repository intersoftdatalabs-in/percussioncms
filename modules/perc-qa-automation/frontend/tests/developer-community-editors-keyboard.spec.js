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
 * Developer Communities rename/description editors keyboard path
 * (#6906 / parent #1690; mirrors slot #6713/#6720, display-format
 * #6630/#5404, content-type/locale #6747/#6797, and roles-edit #6905,
 * plus the #6733 focus-return pattern).
 *
 * The rename input and description textarea autofocus when their editors
 * open, and Escape cancels without writing (same as Cancel): no POST
 * fires and the stored values are kept.
 *
 * Focus return (#6733 pattern): when an editor closes (Escape, Cancel, or
 * a successful save) focus returns to the invoking edit button instead
 * of falling back to <body>, and save notices carry role=status.
 *
 *   npm run test:surface -- --path tests/developer-community-editors-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

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
  return `QA 6906 ${label} ${suffix || "comm"}`;
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

function communityWritePosts(page, bucket) {
  page.on("request", (req) => {
    if (
      req.method() === "POST" &&
      /\/(rename|description)(?:\?|$)/.test(req.url()) &&
      /\/communities\//.test(req.url())
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

async function openCommunityDetail(page, name) {
  await page.locator(`[data-comm-name="${name}"]`).click();
  await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-comm-detail-title"]')).toContainText(name);
}

test.describe("Developer community editors keyboard path (#6906 / #1690)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("rename autofocuses and Escape cancels without writing; focus returns to Edit", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await openCommunitiesCatalog(page);
    const name = uniqueCommunityName("keep");
    await createCommunity(page, name);
    await openCommunityDetail(page, name);

    const writes = [];
    communityWritePosts(page, writes);

    const title = page.locator('[data-testid="developer-comm-detail-title"]');
    const edit = page.locator('[data-testid="developer-comm-rename-edit"]');
    const input = page.locator('[data-testid="developer-comm-rename-name"]');

    await edit.click();
    await expect(input).toBeFocused();

    await input.fill("not-saved");
    await expect(title).toContainText(name);
    await input.press("Escape");
    await expect(input).toHaveCount(0);
    expect(writes, `Escape must not write: ${writes.join(" | ")}`).toEqual([]);
    await expect(title).toContainText(name);
    await expect(title).not.toContainText("not-saved");
    await expect(edit).toBeFocused();
    await page.screenshot({ path: `test-results/live-proof-6906-comm-rename-keyboard.png` });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("description autofocuses; Cancel and Save return focus; save notice is status", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await openCommunitiesCatalog(page);
    const name = uniqueCommunityName("notes");
    await createCommunity(page, name);
    await openCommunityDetail(page, name);

    const writes = [];
    communityWritePosts(page, writes);

    const stored = page.locator('[data-testid="developer-comm-description"]');
    const edit = page.locator('[data-testid="developer-comm-description-edit"]');
    const input = page.locator('[data-testid="developer-comm-description-input"]');
    const notice = page.locator('[data-testid="developer-comm-detail-notice"]');

    await edit.click();
    await expect(input).toBeFocused();

    await input.fill("not-saved");
    await expect(stored).toHaveText("");
    await page.locator('[data-testid="developer-comm-description-cancel"]').click();
    await expect(input).toHaveCount(0);
    expect(writes, `Cancel must not write: ${writes.join(" | ")}`).toEqual([]);
    await expect(stored).toHaveText("");
    await expect(edit).toBeFocused();

    const notes = `Notes ${Date.now().toString(36)}`;
    await edit.click();
    await expect(input).toBeFocused();
    await input.fill(notes);
    await page.locator('[data-testid="developer-comm-description-save"]').click();
    await expect(notice).toBeVisible({ timeout: 20_000 });
    await expect(notice).toHaveAttribute("role", "status");
    await expect(notice).toContainText(/saved/i);
    await expect(stored).toContainText(notes, { timeout: 20_000 });
    await expect(input).toHaveCount(0);
    await expect(edit).toBeFocused();
    expect(writes, `one description write expected: ${writes.join(" | ")}`).toHaveLength(1);

    await expectNoSeriousA11yViolations(page, {
      // Full detail-panel scope (#6973): the two pre-existing
      // label-content-name-mismatch hits (developer-comm-back,
      // developer-comm-roles-save) were resolved by #6942, so the
      // narrowing to the two editor sections is now stale.
      scope: '[data-testid="developer-comm-detail"]',
    });
    await page.screenshot({ path: `test-results/live-proof-6906-comm-description-keyboard.png` });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
