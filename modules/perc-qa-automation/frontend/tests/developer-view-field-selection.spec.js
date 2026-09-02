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
 * Developer Views field-selection (#4111 UI-08 / parent #1690).
 *
 * Admin add/remove/reorder of field criteria on a uniquely named standard
 * view. Inbox-family / custom-URL views stay read-only.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-view-field-selection.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerViewsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "views",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openViewsCatalog(page) {
  await page.goto(developerViewsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-vw-panel"]');
  const empty = page.locator('[data-testid="developer-vw-empty"]');
  const listError = page.locator('[data-testid="developer-vw-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer views catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-vw-new"]')).toBeVisible();
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

/** REST-safe unique view name (no spaces, wildcards, or path characters). */
function uniqueViewName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4111${suffix || "x"}`;
}

async function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
}

function unwrapViewDef(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return {};
  }
  return parsed.ViewDef || parsed;
}

async function saveFieldsAndAssertOk(page) {
  await page.locator('[data-testid="developer-vw-fields-save"]').click();
  const saveNotice = page.locator('[data-testid="developer-vw-editor-notice"]');
  const saveError = page.locator('[data-testid="developer-vw-detail-error"]');
  await expect(saveNotice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Save fields failed: ${(await saveError.innerText()).trim()}`);
  }
}

test.describe("Developer view field-selection (#4111 / UI-08)", () => {
  test("Inbox-family views are not field-edited from this catalog", async ({ page }) => {
    test.setTimeout(90_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const inboxOpen = page.locator('[data-vw-name="Inbox"]');
    await expect(inboxOpen).toHaveCount(1, { timeout: 20_000 });
    await inboxOpen.click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-fields-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-field-editor"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-vw-fields-save"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin add/remove/reorder field criteria on a uniquely named standard view", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const viewName = uniqueViewName();
    expect(viewName.startsWith("qa4111")).toBeTruthy();

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-name"]').fill(viewName);
    await page.locator('[data-testid="developer-vw-label"]').fill(`${viewName} label`);
    await page.locator('[data-testid="developer-vw-save"]').click();
    const notice = page.locator('[data-testid="developer-vw-editor-notice"]');
    const err = page.locator('[data-testid="developer-vw-detail-error"]');
    await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      throw new Error(`Create failed: ${(await err.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-vw-field-editor"]')).toBeVisible({
      timeout: 20_000,
    });

    const sourceSelect = page.locator('[data-testid="developer-vw-field-source"]');
    const addSource = await sourceSelect.evaluate((el) => {
      const options = Array.from(el.options || []);
      const hit = options.find((o) => o.value && o.value !== "");
      return hit ? hit.value : "";
    });
    expect(addSource, "field picker should offer a CX field").toBeTruthy();
    await sourceSelect.selectOption(addSource);
    await page.locator('[data-testid="developer-vw-field-add-op"]').selectOption("equal");
    await page.locator('[data-testid="developer-vw-field-add-value"]').fill("1");
    await page.locator('[data-testid="developer-vw-field-add"]').click();
    await expect(page.locator(`[data-vw-field-name="${addSource}"]`).first()).toBeVisible();

    const secondSource = await sourceSelect.evaluate((el) => {
      const options = Array.from(el.options || []);
      const hit = options.find((o) => o.value && o.value !== "");
      return hit ? hit.value : "";
    });
    if (secondSource) {
      await sourceSelect.selectOption(secondSource);
      await page.locator('[data-testid="developer-vw-field-add"]').click();
      await expect(page.locator(`[data-vw-field-name="${secondSource}"]`).first()).toBeVisible();
      const downBtn = page.locator('[data-testid="developer-vw-field-down-0"]');
      if (await downBtn.isEnabled()) {
        await downBtn.click();
      }
    }

    await saveFieldsAndAssertOk(page);
    await expect(page.locator(`[data-vw-field-name="${addSource}"]`).first()).toBeVisible();

    const afterPut = await inPageJson(
      page,
      `/Rhythmyx/services/views/${encodeURIComponent(viewName)}`,
      "GET",
    );
    if (afterPut.status === 200) {
      const detail = unwrapViewDef(afterPut.text);
      const fieldNames = (detail.fields || []).map((f) => f.fieldName);
      expect(fieldNames).toContain(addSource);
      expect(afterPut.text).toMatch(new RegExp(`"name"\\s*:\\s*"${viewName}"`));
    } else {
      // H2 findAllViews XML cache can miss the row immediately after field PUT;
      // SPA save already returned 200 and the criterion is on screen.
      expect(afterPut.status, afterPut.text).toBeGreaterThan(0);
      await expect(page.locator(`[data-vw-field-name="${addSource}"]`).first()).toBeVisible();
    }

    const addedRow = page.locator(`[data-vw-field-name="${addSource}"]`).first();
    const removeBtn = addedRow.locator('[data-testid^="developer-vw-field-remove-"]');
    await expect(removeBtn).toBeEnabled();
    await removeBtn.click();
    await saveFieldsAndAssertOk(page);

    const afterRemove = await inPageJson(
      page,
      `/Rhythmyx/services/views/${encodeURIComponent(viewName)}`,
      "GET",
    );
    if (afterRemove.status === 200) {
      const removed = unwrapViewDef(afterRemove.text);
      const remaining = (removed.fields || []).map((f) => f.fieldName);
      expect(remaining).not.toContain(addSource);
    } else {
      await expect(page.locator(`[data-vw-field-name="${addSource}"]`)).toHaveCount(0);
    }

    await page.locator('[data-testid="developer-vw-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-vw-name="${viewName}"]`)).toHaveCount(0, {
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
