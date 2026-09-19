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
 * Developer CE Controls XSL source view/edit (#4619 / parent #1690).
 * User controls: GET round-trip + PUT persist. System controls: read-only snippet.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-control-xsl-source.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

function developerControlsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "ce-controls",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueControlName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

async function openControlsCatalog(page) {
  await page.goto(developerControlsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ctl-panel"]');
  const empty = page.locator('[data-testid="developer-ctl-empty"]');
  const listError = page.locator('[data-testid="developer-ctl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer CE controls catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
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

async function createUserControl(page, controlName) {
  await page.locator('[data-testid="developer-ctl-new"]').click();
  await expect(page.locator('[data-testid="developer-ctl-create"]')).toBeVisible();
  await page.locator('[data-testid="developer-ctl-create-name"]').fill(controlName);
  await page.locator('[data-testid="developer-ctl-create-display"]').fill(`${controlName} label`);
  await page.locator('[data-testid="developer-ctl-create-save"]').click();
  const detail = page.locator('[data-testid="developer-ctl-detail"]');
  const saveError = page.locator('[data-testid="developer-ctl-create-error"]');
  await expect(detail.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
}

test.describe("Developer CE control XSL source (#4619)", () => {
  test("system control XSL is read-only (no save chrome)", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openControlsCatalog(page);

    await page
      .locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", "sys_EditBox"))
      .click();
    await expect(page.locator('[data-testid="developer-ctl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ctl-system-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ctl-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ctl-edit-xsl"]')).toHaveCount(0);
    const viewXsl = page.locator('[data-testid="developer-ctl-view-xsl"]');
    if (await viewXsl.count()) {
      await expect(viewXsl).toBeVisible();
      expect(await viewXsl.inputValue()).toMatch(/sys_EditBox|ControlMeta/i);
      await expect(viewXsl).toHaveAttribute("readonly", "");
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin edits and saves user-control XSL source", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openControlsCatalog(page);

    const controlName = uniqueControlName("qa4619");
    await createUserControl(page, controlName);

    const xslBox = page.locator('[data-testid="developer-ctl-edit-xsl"]');
    await expect(xslBox).toBeVisible();
    const original = await xslBox.inputValue();
    expect(original, "GET should populate user-control XSL").toMatch(/xsl:stylesheet/i);
    const marker = `<!-- qa4619-${controlName} -->`;
    const updated = original.includes("?>")
      ? original.replace("?>", `?>\n${marker}`)
      : `${marker}\n${original}`;
    await xslBox.fill(updated);
    await page.locator('[data-testid="developer-ctl-save"]').click();

    const saveError = page.locator('[data-testid="developer-ctl-detail-error"]');
    const notice = page.locator('[data-testid="developer-ctl-detail-notice"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`XSL save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await page.locator('[data-testid="developer-ctl-back"]').click();
    await expect(page.locator('[data-testid="developer-ctl-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName))
      .click();
    const reopened = page.locator('[data-testid="developer-ctl-edit-xsl"]');
    await expect(reopened).toBeVisible({ timeout: 20_000 });
    expect(await reopened.inputValue()).toContain(marker);

    await page.locator('[data-testid="developer-ctl-delete"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(page.locator('[data-testid="developer-ctl-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
