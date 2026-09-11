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
 * Developer Templates detail lock / save / unlock (#4454 / parent #1690 AS-08).
 *
 * Admin locks a template, saves a description while the lock is held, then unlocks.
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-lock-save.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = " [#4454-lock-save]";

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function releaseLeftoverTemplateLocks(page) {
  for (const name of [
    "perc.base.cClampBottom",
    "perc.page",
    "perc.pageXml",
    "perc.pageDatabase",
  ]) {
    await page.request.post(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
    );
  }
}

async function openTemplateDetail(page, namePattern) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });

  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const empty = page.locator('[data-testid="developer-tpl-empty"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "No templates in catalog — fail closed (H2 QA must include sample templates)",
    );
  }

  const table = page.locator('[data-testid="developer-tpl-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-tpl-row-"]').filter({
    hasText: namePattern || /\bperc\.page\b/,
  });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-tpl-row", 0));
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-tpl-detail"]');
  const detailError = page.locator('[data-testid="developer-tpl-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Template detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-tpl-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-tpl-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
  const nameEl = page.locator('[data-testid="developer-tpl-detail-name"]');
  if ((await nameEl.count()) > 0) {
    const name = (await nameEl.innerText()).trim();
    if (name) {
      // Release a leftover lock from a prior session (same Admin, different JSESSION).
      await page.request.post(
        `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
      );
    }
  }
  return detail;
}

test.describe("Developer template lock/save chrome (#4454)", () => {
  test("Admin can lock, save a description, and unlock", async ({ page }) => {
    test.setTimeout(120_000);
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

    await loginAsAdmin(page);
    await releaseLeftoverTemplateLocks(page);
    await openTemplateDetail(page);

    const desc = page.locator('[data-testid="developer-tpl-description"]');
    const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const unlockBtn = page.locator('[data-testid="developer-tpl-unlock"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');

    await expect(page.locator('[data-testid="developer-tpl-lock-toolbar"]')).toBeVisible();
    await expect(lockBtn).toBeEnabled();
    await expect(saveBtn).toBeDisabled();
    await expect(unlockBtn).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    const waitLock = () =>
      page.waitForResponse(
        (res) =>
          res.request().method() === "POST" && /\/templates\/[^/?]+\/lock(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
    let lockWait = waitLock();
    await lockBtn.click();
    let lockHttp = await lockWait;
    if (lockHttp.status() === 409) {
      const name = (await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()).trim();
      await page.request.post(
        `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
      );
      lockWait = waitLock();
      await lockBtn.click();
      lockHttp = await lockWait;
    }
    expect(lockHttp.status(), `lock HTTP ${lockHttp.status()}`).toBe(200);
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(unlockBtn).toBeEnabled();

    const original = (await desc.inputValue()).replace(MARKER, "");
    await desc.fill(`${original}${MARKER}`);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-tpl-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-tpl-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await desc.fill(original);
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Restore save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    await expect(saveBtn).toBeDisabled();
    await expect(lockBtn).toBeEnabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });

  test("other-user lock is 409; save stays disabled (#4454)", async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
    await releaseLeftoverTemplateLocks(page);
    await openTemplateDetail(page);

    await page.route("**/services/templates/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');

    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-tpl-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(saveBtn).toBeDisabled();
  });
});
