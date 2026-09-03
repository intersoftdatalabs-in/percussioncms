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
 * Developer CE Controls create chrome (#4213 UI-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-control-create.spec.js
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

async function inPageDeleteStatus(page, controlName) {
  const url = `/Rhythmyx/services/cecontrols/${encodeURIComponent(controlName)}`;
  return page.evaluate(async (path) => {
    const tokenObj = window.OWASP_CSRFTOKEN;
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const token = (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
    const headerName =
      (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
    const headers = { Accept: "application/json" };
    if (token) {
      headers[headerName] = token;
    }
    const res = await fetch(path, { method: "DELETE", credentials: "same-origin", headers });
    return res.status;
  }, url);
}

test.describe("Developer CE control create (#4213 / UI-01)", () => {
  test("catalog lists system sys_EditBox and opens create chrome", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openControlsCatalog(page);

    await expect(
      page.locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", "sys_EditBox")),
    ).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-ctl-new"]').click();
    await expect(page.locator('[data-testid="developer-ctl-create"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-ctl-create-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-ctl-create-name"]').fill("has space");
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-ctl-create-name"]').fill(uniqueControlName("qa4213"));
    await expect(saveBtn).toBeEnabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin create POST lists the row and name is read-only", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openControlsCatalog(page);

    const controlName = uniqueControlName("qa4213");
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
    await expect(page.locator('[data-testid="developer-ctl-create-name"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ctl-create-save"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-ctl-back"]').click();
    await expect(
      page.locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName)),
    ).toBeVisible({
      timeout: 20_000,
    });

    const deleteStatus = await inPageDeleteStatus(page, controlName);
    expect([204, 404]).toContain(deleteStatus);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system control detail stays non-creatable", async ({ page }) => {
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
    await expect(page.locator('[data-testid="developer-ctl-detail-name"]')).toHaveText(
      "sys_EditBox",
    );
    await expect(page.locator('[data-testid="developer-ctl-system-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ctl-create-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ctl-create-name"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
