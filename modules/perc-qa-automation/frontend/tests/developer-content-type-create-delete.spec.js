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
 * Developer Content Types create / lock-held delete chrome (#4055 CD-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-create-delete.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openContentTypesCatalog(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const empty = page.locator('[data-testid="developer-ct-empty"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible();
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

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply. Returns HTTP status.
 */
async function inPageDeleteStatus(page, typeName) {
  const url = `/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`;
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
    const res = await fetch(path, {
      method: "DELETE",
      credentials: "same-origin",
      headers,
    });
    return res.status;
  }, url);
}

async function inPageGetStatus(page, typeName) {
  const url = `/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`;
  return page.evaluate(async (path) => {
    const res = await fetch(path, {
      method: "GET",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
    return res.status;
  }, url);
}

test.describe("Developer content type create/delete (#4055 / CD-01)", () => {
  test("New chrome, invalid name, unlocked DELETE 409, lock enables delete", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    await page.locator('[data-testid="developer-ct-new"]').click();
    await expect(page.locator('[data-testid="developer-ct-create"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-ct-create-save"]');
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-ct-create-name"]').fill("bad name");
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-ct-create-name"]').fill("percQaLive");
    await expect(saveBtn).toBeEnabled();
    await page.locator('[data-testid="developer-ct-create-back"]').click();
    await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible();

    const unlockedStatus = await inPageDeleteStatus(page, "percPage");
    expect(
      unlockedStatus,
      `unlocked DELETE should be 409 (got ${unlockedStatus})`,
    ).toBe(409);
    const stillThere = await inPageGetStatus(page, "percPage");
    expect(stillThere, "percPage remains after unlocked DELETE").toBe(200);

    const openBtn = page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]');
    const anyOpen = page.locator('[data-testid="developer-ct-open"]').first();
    if ((await openBtn.count()) > 0) {
      await openBtn.click();
    } else {
      await anyOpen.click();
    }
    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const deleteBtn = page.locator('[data-testid="developer-ct-delete"]');
    await expect(deleteBtn).toBeDisabled();
    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Locked by you/i,
      { timeout: 20_000 },
    );
    await expect(deleteBtn).toBeEnabled();
    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
    await expect(deleteBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    await page.locator('[data-testid="developer-ct-new"]').click();
    await expect(page.locator('[data-testid="developer-ct-create"]')).toBeVisible();
    await page.locator('[data-testid="developer-ct-create-name"]').fill("percPage");
    await page.locator('[data-testid="developer-ct-create-label"]').fill("Duplicate Page");
    await page.locator('[data-testid="developer-ct-create-save"]').click();

    const err = page.locator('[data-testid="developer-ct-create-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
