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
 * Developer Display Format column field-selection (#4097 UI-08 / parent #1690).
 *
 * Packaged/system formats stay read-only in this catalog. User-format
 * add/remove/reorder is covered by WebUI Vitest; live H2 create+GET of a
 * new user DF is still blocked by design-WS name replay / sys_DisplayFormats
 * save (see parent #1690 / #4091).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-display-format-columns.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openDisplayFormatsCatalog(page) {
  await page.goto(developerDisplayFormatsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-df-panel"]');
  const empty = page.locator('[data-testid="developer-df-empty"]');
  const listError = page.locator('[data-testid="developer-df-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer display formats catalog error: ${(await listError.innerText()).trim()}`,
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

test.describe("Developer display format columns (#4097 / UI-08)", () => {
  test("packaged By_Author format is read-only for columns", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    await page
      .locator(catalogOpenByExactName("developer-df-open", "data-df-name", "By_Author"))
      .click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-columns-readonly"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-column-editor"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-columns-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-columns-table"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
