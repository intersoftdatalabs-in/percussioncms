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
 * Developer shared-fields catalog readback (#5955 / parent #1690, slice 143).
 *
 * Proves the shared field group list/detail renders from the thin-read API
 * in the React shell: the catalog table reads back through
 * GET /services/sharedfields (name, filename, field-count cells — no field,
 * control-property, or choice payload), and the detail panel reads back
 * through GET /services/sharedfields/{name} (title, committed name and
 * filename inputs, nested-field table).
 *
 * Test 1 is zero-write: it opens a packaged catalog row (the `rssfeeds`
 * widget group when installed, else the first catalog row) and asserts list
 * + detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one group through the shell, proves name/filename readback
 * in the catalog and detail, then removes it with REST DELETE cleanup and
 * proves catalog omission.
 *
 * Readback only, no nested-field or control-property mutation; out of scope:
 * slices 142/144.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-shared-fields-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

/**
 * Packaged shared field group shipped by the Page Auto List widget
 * (perc.PageAutoListWidget/rssfeeds.sharedGroup). Preferred zero-write
 * readback target; the spec falls back to the first catalog row when it is
 * not installed.
 */
const PACKAGED_SHARED_GROUP = "rssfeeds";

function developerSharedFieldsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "shared-fields",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique group name: no spaces, wildcards, or path chars. */
function uniqueGroupName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa5955${suffix || "rb"}`;
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

async function openSharedFieldsCatalog(page) {
  await page.goto(developerSharedFieldsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-sf-new"]')).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-sf-panel"]');
  const empty = page.locator('[data-testid="developer-sf-empty"]');
  const listError = page.locator('[data-testid="developer-sf-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer shared fields catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "Developer shared fields catalog is empty — no packaged row to read back",
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer shared-fields catalog readback (#5955)", () => {
  test("packaged shared group list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    // Thin-read catalog: prefer the packaged widget group, else the first
    // row. The row renders the name open button plus filename and
    // field-count cells.
    const packagedOpen = page.locator(
      `[data-testid="developer-sf-open"][data-sf-name="${PACKAGED_SHARED_GROUP}"]`,
    );
    const firstRow = page.locator(catalogRowSelector("developer-sf-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const packagedCount = await packagedOpen.count();
    const openBtn =
      packagedCount > 0
        ? packagedOpen.first()
        : firstRow.locator('[data-testid="developer-sf-open"]');
    await expect(openBtn).toBeVisible();
    const rowName = await openBtn.getAttribute("data-sf-name");
    expect(rowName, "catalog row carries data-sf-name").toBeTruthy();
    const row =
      packagedCount > 0
        ? page.locator(
            `tr:has([data-testid="developer-sf-open"][data-sf-name="${rowName}"])`,
          )
        : firstRow;
    await expect(row).toContainText(rowName);

    // Thin-read REST probe for the same row: GET /services/sharedfields
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/sharedfields", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged group").toContain(rowName);

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/sharedfields/${encodeURIComponent(rowName)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the group").toContain(rowName);

    // Detail readback in the shell: title, committed name and filename
    // inputs, nested-field section.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-sf-detail-title"]')).toHaveText(
      rowName,
      { timeout: 20_000 },
    );
    await expect(page.locator('[data-testid="developer-sf-name"]')).toHaveValue(rowName);
    const filenameValue = await page
      .locator('[data-testid="developer-sf-filename"]')
      .inputValue();
    expect(filenameValue, "detail filename reads back").toBeTruthy();
    const fieldsSection = page.locator('[data-testid="developer-sf-fields"]');
    await expect(fieldsSection).toBeVisible({ timeout: 20_000 });
    const fieldsTable = page.locator('[data-testid="developer-sf-fields-table"]');
    const fieldsEmpty = page.locator('[data-testid="developer-sf-fields-empty"]');
    await expect(fieldsTable.or(fieldsEmpty).first()).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created group name and filename read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    const name = uniqueGroupName();
    const filename = `${name}.xml`;

    // Shell create (POST /services/sharedfields).
    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-sf-save"]');
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-sf-name"]').fill(name);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-sf-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    // Detail readback: title shows the name; filename defaults to {name}.xml.
    await expect(page.locator('[data-testid="developer-sf-detail-title"]')).toHaveText(
      name,
    );
    await expect(page.locator('[data-testid="developer-sf-name"]')).toHaveValue(name);
    await expect(page.locator('[data-testid="developer-sf-filename"]')).toHaveValue(
      filename,
    );

    // Catalog readback: row for the created group with its filename.
    await page.locator('[data-testid="developer-sf-back"]').click();
    const openSel = `[data-testid="developer-sf-open"][data-sf-name="${name}"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const createdRow = page.locator(`tr:has(${openSel})`);
    await expect(createdRow).toContainText(filename, { timeout: 20_000 });

    // REST cleanup: DELETE the created group, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/sharedfields/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openSharedFieldsCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
