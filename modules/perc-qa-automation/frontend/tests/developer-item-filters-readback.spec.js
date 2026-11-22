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
 * Developer item-filters catalog readback (#6934 / parent #1690).
 *
 * Proves the assembly item-filter list/detail renders from the thin-read API
 * in the React shell: the catalog table reads back through
 * GET /services/itemfilters (name, rule count, parent, description cells),
 * and the detail panel reads back through
 * GET /services/itemfilters/{idOrName} (title, committed name + GUID,
 * description text).
 *
 * Test 1 is zero-write: it opens the first packaged catalog row and asserts
 * list + detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one filter through the shell, proves name/description
 * readback in the catalog and detail, then removes it with REST DELETE
 * cleanup and proves catalog omission.
 *
 * Readback only, no rule mutation; rules coverage stays with
 * developer-item-filter-editor.spec.js (#4438 / AS-07).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-item-filters-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerItemFiltersUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "item-filters",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * REST-safe unique filter name (no spaces, wildcards, or path characters).
 */
function uniqueFilterName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa6934${label}${suffix || "rb"}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method) {
  return page.evaluate(
    async ({ path: url, method: httpMethod }) => {
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
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method },
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

async function openItemFiltersCatalog(page) {
  await page.goto(developerItemFiltersUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator('[data-testid="tab-developer-item-filters"]'),
  ).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-if-panel"]');
  const empty = page.locator('[data-testid="developer-if-empty"]');
  const listError = page.locator('[data-testid="developer-if-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer item filters catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "Developer item filters catalog is empty — no packaged row to read back",
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer item filters catalog readback (#6934)", () => {
  test("packaged filter list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openItemFiltersCatalog(page);

    // Thin-read catalog: first row renders the name open button.
    const firstRow = page.locator(catalogRowSelector("developer-if-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator('[data-testid="developer-if-open"]');
    await expect(openBtn).toBeVisible();
    const rowName = await openBtn.getAttribute("data-if-name");
    expect(rowName, "catalog row carries data-if-name").toBeTruthy();
    await expect(firstRow).toContainText(rowName);

    // Thin-read REST probe for the same row: GET /services/itemfilters
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/itemfilters", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged filter").toContain(
      rowName,
    );

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/itemfilters/${encodeURIComponent(rowName)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the filter").toContain(
      rowName,
    );

    // Detail readback in the shell: title shows the filter name, the name
    // input echoes it (read-only after create), and the GUID block renders.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-if-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-if-detail-title"]'),
    ).toContainText(rowName, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-if-name"]')).toHaveValue(
      rowName,
    );
    const detailText = await page
      .locator('[data-testid="developer-if-detail"]')
      .innerText();
    expect(detailText, `detail header shows a GUID: ${detailText}`).toMatch(
      /\d+-\d+-\d+/,
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created filter name and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openItemFiltersCatalog(page);

    const name = uniqueFilterName("readback");
    const description = "qa6934 item filters catalog readback";

    // Shell create (POST /services/itemfilters).
    await page.locator('[data-testid="developer-if-new"]').click();
    await expect(page.locator('[data-testid="developer-if-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-if-save"]');
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-if-name"]').fill(name);
    await expect(saveBtn).toBeEnabled();
    await page.locator('[data-testid="developer-if-description"]').fill(description);
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-if-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-if-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    // Detail readback: title shows the name, the name input echoes it
    // (read-only after create), and the description echoes the payload.
    await expect(
      page.locator('[data-testid="developer-if-detail-title"]'),
    ).toHaveText(name);
    await expect(page.locator('[data-testid="developer-if-name"]')).toHaveValue(
      name,
    );
    await expect(page.locator('[data-testid="developer-if-name"]')).toBeDisabled();
    await expect(
      page.locator('[data-testid="developer-if-description"]'),
    ).toHaveValue(description);

    // Catalog readback: row for the created filter with its description.
    await page.locator('[data-testid="developer-if-back"]').click();
    const openSel = `[data-testid="developer-if-open"][data-if-name="${name}"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const row = page.locator(`tr:has(${openSel})`);
    await expect(row).toContainText(description, { timeout: 20_000 });

    // REST cleanup: DELETE the created filter, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/itemfilters/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openItemFiltersCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
