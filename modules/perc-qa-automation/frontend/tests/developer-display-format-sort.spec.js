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
 * Developer Display Format default sort column + direction (#4221 UI-05 /
 * parent #1690).
 *
 * Packaged/system formats stay read-only. On a uniquely named user format,
 * Admin sets default sort via SPA PUT columns + sortedColumnNames; GET
 * reloads that column and descending/ascending flags.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-display-format-sort.spec.js
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

/** REST-safe unique display-format name (no spaces, wildcards, or path characters). */
function uniqueDisplayFormatName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @param {object} [body]
 * @returns {Promise<{status: number, text: string}>}
 */
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

function unwrapDisplayFormat(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return {};
  }
  return parsed.DisplayFormat || parsed;
}

function unwrapColumns(detail) {
  const raw = detail.columns;
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw && Array.isArray(raw.DisplayFormatColumn)) {
    return raw.DisplayFormatColumn;
  }
  if (raw && raw.DisplayFormatColumn) {
    return [raw.DisplayFormatColumn];
  }
  return [];
}

async function saveColumnsAndAssertOk(page) {
  await page.locator('[data-testid="developer-df-columns-save"]').click();
  const saveNotice = page.locator('[data-testid="developer-df-editor-notice"]');
  const saveError = page.locator('[data-testid="developer-df-detail-error"]');
  await expect(saveNotice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Save columns failed: ${(await saveError.innerText()).trim()}`);
  }
}

test.describe("Developer display format default sort (#4221 / UI-05)", () => {
  test("packaged By_Author format is read-only for default sort", async ({ page }) => {
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
    await expect(page.locator('[data-testid="developer-df-column-sort-0"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-column-sort-dir-0"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-column-sort-readonly-0"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-df-gaps"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("user format default sort column and descending direction persist on PUT/GET", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName("qa4221");
    expect(formatName.startsWith("qa4221")).toBeTruthy();
    expect(/By_Author|Default/i.test(formatName)).toBeFalsy();

    const create = await inPageJson(page, "/Rhythmyx/services/displayformats", "POST", {
      DisplayFormat: {
        name: formatName,
        internalName: formatName,
        label: `${formatName} label`,
        displayName: `${formatName} label`,
        description: "qa4221 default sort",
      },
    });
    expect(
      create.status,
      `POST create should be 201 (got ${create.status}): ${create.text}`,
    ).toBe(201);

    await openDisplayFormatsCatalog(page);
    const createdOpen = page.locator(
      catalogOpenByExactName("developer-df-open", "data-df-name", formatName),
    );
    await expect(createdOpen).toHaveCount(1, { timeout: 20_000 });
    await createdOpen.click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-column-editor"]')).toBeVisible();

    const sourceSelect = page.locator('[data-testid="developer-df-column-source"]');
    const addSource = await sourceSelect.evaluate((el) => {
      const options = Array.from(el.options || []);
      const hit = options.find(
        (o) => o.value && o.value !== "sys_title" && o.value !== "",
      );
      return hit ? hit.value : "";
    });
    expect(addSource, "column picker should offer a field besides sys_title").toBeTruthy();
    await sourceSelect.selectOption(addSource);
    await page.locator('[data-testid="developer-df-column-add"]').click();
    const addedRow = page.locator(`[data-df-column-source="${addSource}"]`).first();
    await expect(addedRow).toBeVisible();

    const addedRadio = addedRow.locator('input[type="radio"][data-testid^="developer-df-column-sort-"]');
    await addedRadio.check();
    const addedDir = addedRow.locator('select[data-testid^="developer-df-column-sort-dir-"]');
    await addedDir.selectOption("desc");
    await saveColumnsAndAssertOk(page);

    const afterPut = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "GET",
    );
    expect(afterPut.status, `GET after PUT sort (got ${afterPut.status}): ${afterPut.text}`).toBe(
      200,
    );
    const named = unwrapDisplayFormat(afterPut.text);
    expect(named.name || named.internalName).toBe(formatName);
    expect(named.sortedColumnNames).toBe(addSource);
    expect(named.descendingSort === true || named.ascendingSort === false).toBeTruthy();
    const cols = unwrapColumns(named);
    const sortCol = cols.find((c) => c && c.source === addSource);
    expect(sortCol, `GET columns missing ${addSource}: ${afterPut.text}`).toBeTruthy();
    expect(sortCol.ascendingSort).toBe(false);

    await openDisplayFormatsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-df-open", "data-df-name", formatName))
      .click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const reloaded = page.locator(`[data-df-column-source="${addSource}"]`).first();
    await expect(reloaded).toBeVisible();
    await expect(
      reloaded.locator('input[type="radio"][data-testid^="developer-df-column-sort-"]'),
    ).toBeChecked();
    await expect(
      reloaded.locator('select[data-testid^="developer-df-column-sort-dir-"]'),
    ).toHaveValue("desc");

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
