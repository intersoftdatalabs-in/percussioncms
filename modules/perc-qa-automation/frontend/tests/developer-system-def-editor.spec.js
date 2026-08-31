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
 * Developer System Def field save / add / delete chrome (#4030 CD-16 / parent #1690).
 * Live H2 add/save/delete of persistable fields is residual #4037 (REST 500 / missing column).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-system-def-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSystemDefUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "system-def",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openSystemDefCatalog(page) {
  await page.goto(developerSystemDefUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-sys-panel"]');
  const empty = page.locator('[data-testid="developer-sys-empty"]');
  const listError = page.locator('[data-testid="developer-sys-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer system def catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-sys-add"]')).toBeVisible();
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

test.describe("Developer system-def field editor (#4030 / CD-16)", () => {
  test("Admin sees system-def save/add/delete chrome", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSystemDefCatalog(page);

    await expect(page.locator('[data-testid="developer-sys-add"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-sys-add-btn"]')).toBeDisabled();
    await page.locator('[data-testid="developer-sys-new-name"]').fill("qa_ok_name");
    await expect(page.locator('[data-testid="developer-sys-add-btn"]')).toBeEnabled();
    await expect(page.locator('[data-testid="developer-sys-save"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-sys-fields-table"]')).toBeVisible();
    await expect(page.locator('[data-sys-field="sys_title"]')).toHaveCount(1);
    await expect(
      page.locator('[data-sys-field="sys_title"]').locator('[data-testid="developer-sys-delete"]'),
    ).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate field name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSystemDefCatalog(page);

    await page.locator('[data-testid="developer-sys-new-name"]').fill("sys_title");
    await page.locator('[data-testid="developer-sys-add-btn"]').click();

    const err = page.locator('[data-testid="developer-sys-write-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
