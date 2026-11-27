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
 * Developer CE control display-name required-field inline error (#7345 /
 * parent #1690). Clearing the display name blocks the PUT before it fires
 * with a named role="alert" message (stored value kept); Escape in the
 * property input cancels the edit without writing (mirrors #6713/#6905).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-control-props-required.spec.js
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

/** REST-safe unique control name (no spaces or wildcards). */
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
  await expect(page.locator('[data-testid="developer-ctl-new"]')).toBeVisible();
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
  await expect(page.locator('[data-testid="developer-ctl-detail-name"]')).toHaveText(
    controlName,
  );
}

async function deleteCurrentControl(page) {
  await page.locator('[data-testid="developer-ctl-delete"]').click();
  await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible();
  await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
  await expect(page.locator('[data-testid="developer-ctl-panel"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer CE control required display name (#7345)", () => {
  test("blank display name blocks save with a named alert; Escape cancels", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openControlsCatalog(page);

    const controlName = uniqueControlName("qa7345");
    await createUserControl(page, controlName);

    let putCount = 0;
    await page.route("**/services/cecontrols/*", (route) => {
      if (route.request().method() === "PUT") {
        putCount += 1;
      }
      return route.continue();
    });

    const display = page.locator('[data-testid="developer-ctl-edit-display"]');
    await expect(display).toBeVisible();
    const stored = await display.inputValue();
    expect(stored.trim().length).toBeGreaterThan(0);

    await display.fill("   ");
    await page.locator('[data-testid="developer-ctl-save"]').click();

    const requiredError = page.locator('[data-testid="developer-ctl-prop-required-error"]');
    await expect(requiredError).toBeVisible({ timeout: 20_000 });
    await expect(requiredError).toHaveAttribute("role", "alert");
    await expect(requiredError).toContainText(/display name is required/i);
    expect(putCount, "blocked save must not fire PUT").toBe(0);
    await expect(page.locator('[data-testid="developer-ctl-detail-notice"]')).toHaveCount(0);
    await page
      .locator('[data-testid="developer-ctl-prop-required-error"]')
      .scrollIntoViewIfNeeded();
    await page.screenshot({ path: "test-results/live-proof-7345-control-required-error.png" });

    await display.press("Escape");
    await expect(display).toHaveValue(stored);
    await expect(requiredError).toHaveCount(0);
    expect(putCount, "Escape cancel must not fire PUT").toBe(0);

    await deleteCurrentControl(page);
    await expect(
      page.locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName)),
    ).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
