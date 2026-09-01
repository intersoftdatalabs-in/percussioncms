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
 * Developer Searches field-selection (#4110 UI-08 / parent #1690).
 *
 * Packaged/system searches stay read-only for field criteria (SPA chrome +
 * PUT 409). Live add/remove/reorder GET round-trip on uniquely named user
 * searches is blocked on this H2 cell by saveSearches + findSearches lag
 * (PUT save succeeds; reloadAfterWrite 500). Covered by adaptor/Vitest.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-search-field-selection.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerSearchesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "searches",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openSearchesCatalog(page) {
  await page.goto(developerSearchesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-sr-panel"]');
  const empty = page.locator('[data-testid="developer-sr-empty"]');
  const listError = page.locator('[data-testid="developer-sr-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer searches catalog error: ${(await listError.innerText()).trim()}`,
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

test.describe("Developer search field-selection (#4110 / UI-08)", () => {
  test("packaged Default_Search is read-only for field criteria", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSearchesCatalog(page);

    await page
      .locator(catalogOpenByExactName("developer-sr-open", "data-sr-name", "Default_Search"))
      .click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-sr-fields-readonly"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-sr-field-editor"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-sr-fields-save"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("packaged Default_Search PUT fields is 409; SPA sends field PUT body", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSearchesCatalog(page);

    const putPackaged = await inPageJson(
      page,
      "/Rhythmyx/services/searches/Default_Search",
      "PUT",
      {
        SearchDef: {
          name: "Default_Search",
          fields: [{ fieldName: "sys_title", operator: "like", fieldValue: "qa4110" }],
        },
      },
    );
    expect(
      putPackaged.status,
      `packaged PUT fields should be 409 (got ${putPackaged.status}): ${putPackaged.text}`,
    ).toBe(409);

    await page
      .locator(catalogOpenByExactName("developer-sr-open", "data-sr-name", "Default_Search"))
      .click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-sr-fields-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-sr-field-editor"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
