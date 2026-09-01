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
 * Developer Display Formats REST DELETE persist (#4091 / parent #4086 / #1690).
 *
 * Creates a user format via Admin POST (does not re-implement SPA create chrome),
 * asserts the catalog lists it, DELETE returns 204, following GET is 404, and the
 * SPA catalog omits the row. Does not delete packaged system formats.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-display-format-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique display-format name (no spaces, wildcards, or path characters). */
function uniqueDisplayFormatName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4091${suffix || "x"}`;
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
        body:
          payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
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

function createdRow(page, formatName) {
  return page.locator(
    catalogOpenByExactName("developer-df-open", "data-df-name", formatName),
  );
}

test.describe("Developer display format editor (#4091 / UI-05 DELETE persist)", () => {
  test("Admin DELETE of a POST-created user format is 204, GET 404, catalog omits the row", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName();
    expect(formatName.startsWith("qa4091")).toBeTruthy();
    expect(/By_Author|Default/i.test(formatName)).toBeFalsy();

    const create = await inPageJson(page, "/Rhythmyx/services/displayformats", "POST", {
      DisplayFormat: {
        name: formatName,
        internalName: formatName,
        label: `${formatName} label`,
        displayName: `${formatName} label`,
        description: "qa4091 delete persist",
      },
    });
    expect(
      create.status,
      `POST create should be 201 (got ${create.status}): ${create.text}`,
    ).toBe(201);

    const afterCreate = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "GET",
    );
    expect(afterCreate.status, `GET after create (got ${afterCreate.status}): ${afterCreate.text}`).toBe(200);
    expect(
      afterCreate.text,
      `GET after create must be the user format, not a packaged replay: ${afterCreate.text}`,
    ).toMatch(new RegExp(`"name"\\s*:\\s*"${formatName}"`));

    await openDisplayFormatsCatalog(page);
    await expect(createdRow(page, formatName)).toHaveCount(1, { timeout: 20_000 });

    const del = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "DELETE",
    );
    expect(
      del.status,
      `DELETE should be 204 (got ${del.status}): ${del.text}`,
    ).toBe(204);

    const afterDelete = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "GET",
    );
    expect(
      afterDelete.status,
      `GET after DELETE must be 404 (got ${afterDelete.status}): ${afterDelete.text}`,
    ).toBe(404);

    await openDisplayFormatsCatalog(page);
    await expect(createdRow(page, formatName)).toHaveCount(0, { timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
