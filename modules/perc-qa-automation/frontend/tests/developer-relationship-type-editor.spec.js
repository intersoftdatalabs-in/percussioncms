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
 * Developer Relationship Types create / save / delete chrome (#4252 / SY-03 / parent #1690).
 * Stacks on REST write (#4251 / PR #4254). Deeper H2 matrix may live on #4253.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-relationship-type-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerRelationshipTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "relationship-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique name: no whitespace / wildcards (REST requireValidName). */
function uniqueRtName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/gi, "").slice(-5);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/gi, "").slice(2, 6);
  return `QaRt${a}${b}`.slice(0, 24);
}

async function openRtCatalog(page) {
  await page.goto(developerRelationshipTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-rt-panel"]');
  const empty = page.locator('[data-testid="developer-rt-empty"]');
  const listError = page.locator('[data-testid="developer-rt-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer relationship types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-rt-new"]')).toBeVisible();
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

test.describe("Developer relationship type editor (#4252 / SY-03)", () => {
  test("system types are read-only; Admin can create, save, and delete a user type", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    // System type chrome: open ActiveAssembly when present.
    const systemOpen = page
      .locator('[data-testid="developer-rt-open"]')
      .filter({ hasText: /^ActiveAssembly$/ });
    if ((await systemOpen.count()) > 0) {
      await systemOpen.first().click();
      await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-rt-system-readonly"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-rt-save"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-rt-delete"]')).toHaveCount(0);
      await page.locator('[data-testid="developer-rt-back"]').click();
      await expect(page.locator('[data-testid="developer-rt-new"]')).toBeVisible();
    }

    const name = uniqueRtName();
    const label = `QA 4252 ${name}`;
    const savedLabel = `${label} saved`;

    await page.locator('[data-testid="developer-rt-new"]').click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-rt-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-rt-name"]').fill(name);
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-rt-category"]').selectOption("rs_generic");
    await page.locator('[data-testid="developer-rt-label"]').fill(label);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-rt-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-rt-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(
        `Create relationship type failed: ${(await saveError.innerText()).trim()}`,
      );
    }
    await expect(notice).toContainText(/saved/i);

    await page.locator('[data-testid="developer-rt-label"]').fill(savedLabel);
    await saveBtn.click();
    await expect(notice).toBeVisible({ timeout: 20_000 });

    await page.locator('[data-testid="developer-rt-back"]').click();
    await expect(page.locator('[data-testid="developer-rt-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-rt-open"]').filter({ hasText: new RegExp(`^${name}$`) }),
    ).toBeVisible();

    await page
      .locator('[data-testid="developer-rt-open"]')
      .filter({ hasText: new RegExp(`^${name}$`) })
      .click();
    await expect(page.locator('[data-testid="developer-rt-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-rt-delete"]').click();
    await confirmDeveloperCatalogDelete(page);

    await expect(page.locator('[data-testid="developer-rt-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-rt-open"]').filter({ hasText: new RegExp(`^${name}$`) }),
    ).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
