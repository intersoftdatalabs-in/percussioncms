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
 * Developer content-types thin read catalog (#5993 / parent #1690, slice 1).
 *
 * Proves the catalog contract from the issue: rows appear only after a
 * successful `GET /services/contenttypes` load, and a load failure is named
 * with a Retry control (no silent empty catalog).
 *
 * Test 1 lists from REST: the catalog table renders rows whose open buttons
 * carry `data-ct-name`, and a same-origin thin-read probe of
 * `GET /services/contenttypes` is 200 and contains a listed row name.
 * Test 2 aborts the first list request: the panel shows the named load error
 * (`developer-ct-error`, "Could not load content types") plus
 * `developer-ct-retry` (no table, no empty state); clicking Retry reloads
 * and the table reads back.
 *
 * No design writes: no create/rename/delete/lock calls. Out of scope:
 * sibling slices #5994 (templates) and #5995 (shell search/filter).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-content-type-catalog-retry.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageGet(page, path) {
  return page.evaluate(async ({ url }) => {
    const tokenObj = window.OWASP_CSRFTOKEN;
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const token =
      (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
    const headerName =
      (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
    const headers = { Accept: "application/json" };
    if (token) {
      headers[headerName] = token;
    }
    const res = await fetch(url, {
      method: "GET",
      credentials: "same-origin",
      headers,
    });
    return { status: res.status, text: await res.text() };
  }, { url: path });
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

async function openContentTypesCatalog(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Developer content-types thin read catalog (#5993)", () => {
  test("catalog lists rows from the thin-read REST API", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    // Rows appear only after a successful load.
    const panel = page.locator('[data-testid="developer-ct-panel"]');
    await expect(panel).toBeVisible({ timeout: 30_000 });
    const firstRow = page.locator(catalogRowSelector("developer-ct-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator('[data-testid="developer-ct-open"]');
    await expect(openBtn).toBeVisible();
    const rowName = await openBtn.getAttribute("data-ct-name");
    expect(rowName, "catalog row carries data-ct-name").toBeTruthy();

    // Thin-read REST probe for the same row.
    const probe = await inPageGet(page, "/Rhythmyx/services/contenttypes");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the listed row").toContain(rowName);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("load failure is named and Retry reloads the catalog", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);

    // Fail the first thin-read list request only; the Retry click re-lists.
    await page.route(
      "**/services/contenttypes",
      (route) => route.abort(),
      { times: 1 },
    );
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });

    const loadError = page.locator('[data-testid="developer-ct-error"]');
    await expect(loadError).toBeVisible({ timeout: 30_000 });
    await expect(loadError).toContainText("Could not load content types");
    const retry = page.locator('[data-testid="developer-ct-retry"]');
    await expect(retry).toBeVisible();
    await expect(retry).toHaveText("Retry");
    // Failure is named — not an empty catalog, not a silent table.
    await expect(page.locator('[data-testid="developer-ct-empty"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ct-table"]')).toHaveCount(0);

    await retry.click();
    const panel = page.locator('[data-testid="developer-ct-panel"]');
    await expect(panel).toBeVisible({ timeout: 30_000 });
    await expect(
      page.locator(catalogRowSelector("developer-ct-row", 0)),
    ).toBeVisible({ timeout: 15_000 });
    await expect(loadError).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
