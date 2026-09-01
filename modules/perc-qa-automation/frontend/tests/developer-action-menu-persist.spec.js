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
 * REST action-menu persist after Admin POST/DELETE (#4119 / parent #1690).
 *
 * POST `/services/actions` must be durable in Hibernate `RXMENUACTION` so GET
 * `/services/actions/catalog` and GET by name include the user menu. DELETE of
 * that user menu is 204 then GET 404. System menus (`Menus/System`, e.g. Edit)
 * are 409 without stealing the design lock. Does not re-implement finder helpers
 * or SPA UI-03/UI-04 chrome.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-action-menu-persist.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin } = require("./helpers/auth");

/** REST-safe unique action-menu name (no spaces, wildcards, or path characters). */
function uniqueActionMenuName(prefix) {
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

function nameInJson(text, name) {
  return new RegExp(`"name"\\s*:\\s*"${name}"`).test(text);
}

test.describe("REST action menu persist (#4119 / UI-02)", () => {
  test("Admin POST is in GET catalog and GET by name; DELETE 204 then 404", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);

    const menuName = uniqueActionMenuName("qa4119");
    expect(menuName.startsWith("qa4119")).toBeTruthy();

    const create = await inPageJson(page, "/Rhythmyx/services/actions", "POST", {
      ActionMenu: {
        name: menuName,
        label: `${menuName} label`,
        description: "qa4119 persist",
        menuType: "MENU",
      },
    });
    expect(
      create.status,
      `POST create should be 200 (got ${create.status}): ${create.text}`,
    ).toBe(200);
    expect(nameInJson(create.text, menuName), create.text).toBeTruthy();

    const catalog = await inPageJson(
      page,
      "/Rhythmyx/services/actions/catalog",
      "GET",
    );
    expect(catalog.status, `GET catalog (got ${catalog.status}): ${catalog.text}`).toBe(
      200,
    );
    expect(
      nameInJson(catalog.text, menuName),
      `GET catalog must list ${menuName}: ${catalog.text}`,
    ).toBeTruthy();

    const byName = await inPageJson(
      page,
      `/Rhythmyx/services/actions/catalog/${encodeURIComponent(menuName)}`,
      "GET",
    );
    expect(
      byName.status,
      `GET by name after create (got ${byName.status}): ${byName.text}`,
    ).toBe(200);
    expect(nameInJson(byName.text, menuName), byName.text).toBeTruthy();

    const dup = await inPageJson(page, "/Rhythmyx/services/actions", "POST", {
      ActionMenu: {
        name: menuName,
        label: `${menuName} dup`,
        menuType: "MENU",
      },
    });
    expect(dup.status, `duplicate POST (got ${dup.status}): ${dup.text}`).toBe(409);

    const invalid = await inPageJson(page, "/Rhythmyx/services/actions", "POST", {
      ActionMenu: {
        name: "has space",
        label: "bad",
        menuType: "MENU",
      },
    });
    expect(
      invalid.status,
      `invalid name POST (got ${invalid.status}): ${invalid.text}`,
    ).toBe(400);

    const del = await inPageJson(
      page,
      `/Rhythmyx/services/actions/${encodeURIComponent(menuName)}`,
      "DELETE",
    );
    expect(del.status, `DELETE should be 204 (got ${del.status}): ${del.text}`).toBe(
      204,
    );

    const afterDelete = await inPageJson(
      page,
      `/Rhythmyx/services/actions/catalog/${encodeURIComponent(menuName)}`,
      "GET",
    );
    expect(
      afterDelete.status,
      `GET after DELETE must be 404 (got ${afterDelete.status}): ${afterDelete.text}`,
    ).toBe(404);

    const catalogAfter = await inPageJson(
      page,
      "/Rhythmyx/services/actions/catalog",
      "GET",
    );
    expect(catalogAfter.status).toBe(200);
    expect(
      nameInJson(catalogAfter.text, menuName),
      `GET catalog must omit deleted ${menuName}: ${catalogAfter.text}`,
    ).toBeFalsy();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("DELETE/PUT of a packaged catalog menu is 409", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);

    const catalog = await inPageJson(
      page,
      "/Rhythmyx/services/actions/catalog",
      "GET",
    );
    expect(catalog.status, catalog.text).toBe(200);
    const names = [...catalog.text.matchAll(/"name"\s*:\s*"([^"]+)"/g)].map(
      (m) => m[1],
    );
    const preferred = ["Copy", "Preview", "Open", "Checkout", "Checkin", "View"];
    const systemName =
      preferred.find((n) => names.includes(n)) ||
      names.find((n) => n && !/^qa4119/i.test(n));
    expect(systemName, `catalog must include a packaged menu: ${catalog.text}`).toBeTruthy();

    const del = await inPageJson(
      page,
      `/Rhythmyx/services/actions/${encodeURIComponent(systemName)}`,
      "DELETE",
    );
    expect(
      del.status,
      `DELETE ${systemName} should be 409 (got ${del.status}): ${del.text}`,
    ).toBe(409);

    const put = await inPageJson(
      page,
      `/Rhythmyx/services/actions/${encodeURIComponent(systemName)}`,
      "PUT",
      {
        ActionMenu: {
          label: "Must not mutate packaged menu",
        },
      },
    );
    expect(
      put.status,
      `PUT ${systemName} should be 409 (got ${put.status}): ${put.text}`,
    ).toBe(409);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
