/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Smoke: Developer module SPA + catalog REST health (Refs #1690).
 *
 * <p>Catches the common post-P0 failure mode: SPA shell mounts but catalog
 * endpoints 500 / return unusable payloads, or the CMS install is behind
 * {@code development} (new REST resources not deployed).</p>
 *
 * <pre>
 *   cd modules/perc-qa-automation/frontend
 *   npm test -- tests/developer-catalog-smoke.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");

const DEVELOPER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=developer&_=${Date.now()}`;

/** Catalog REST paths used by Developer shell sections (thin read catalogs). */
const CATALOG_ENDPOINTS = [
  { name: "contenttypes", path: "/Rhythmyx/services/contenttypes", critical: true },
  { name: "templates", path: "/Rhythmyx/services/templates", critical: true },
  { name: "slots", path: "/Rhythmyx/services/slots", critical: true },
  { name: "keywords", path: "/Rhythmyx/services/keywords", critical: true },
  { name: "locales", path: "/Rhythmyx/services/locales", critical: false },
  { name: "searches", path: "/Rhythmyx/services/searches", critical: false },
  { name: "views", path: "/Rhythmyx/services/views", critical: false },
  { name: "extensions", path: "/Rhythmyx/services/extensions/catalog", critical: false },
  { name: "cecontrols", path: "/Rhythmyx/services/cecontrols", critical: false },
  { name: "serverconfigs", path: "/Rhythmyx/services/serverconfigs", critical: false },
  { name: "sites", path: "/Rhythmyx/services/sites", critical: false },
  { name: "relationshiptypes", path: "/Rhythmyx/services/relationshiptypes", critical: false },
];

test.describe("Developer catalog smoke (#1690)", () => {
  test("SPA Developer shell mounts and content-types table is usable", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
    await page.goto(DEVELOPER_URL, { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="perc-developer-shell"]');
    await expect(shell).toBeVisible({ timeout: 20_000 });

    // Default section: content types list
    const table = page.locator('[data-testid="developer-ct-table"]');
    await expect(table).toBeVisible({ timeout: 20_000 });

    // Fail hard if the panel is in error chrome
    const err = page.locator('[data-testid="developer-ct-error"]');
    if ((await err.count()) > 0) {
      const text = await err.innerText();
      throw new Error(`Developer content-types panel error: ${text}`);
    }

    // Rows should expose a real name/label, not only "—" placeholders from
    // empty DTOs (seen when list JSON only carries hideFromMenu flags).
    const openBtns = table.locator('[data-testid="developer-ct-open"]');
    const rowCount = await table.locator("tbody tr").count();
    expect(rowCount, "content type table should have at least one row").toBeGreaterThan(
      0,
    );

    const bodyText = await table.innerText();
    // If every cell is an em dash, the REST payload is useless for the SPA.
    const onlyPlaceholders =
      !/[A-Za-z]{2,}/.test(bodyText.replace(/Label|Name|Id|Description|Select/gi, ""));
    expect(
      onlyPlaceholders,
      "content type rows look empty (labels/names missing from API/DTO) — redeploy rest/WebUI or fix ContentType list mapping",
    ).toBe(false);

    if ((await openBtns.count()) > 0) {
      await expect(openBtns.first()).toBeVisible();
    }
  });

  test("catalog REST endpoints return 2xx with Basic auth", async ({ request }) => {
    test.setTimeout(120_000);
    const headers = adminBasicAuthHeaders();
    const failures = [];

    for (const ep of CATALOG_ENDPOINTS) {
      const res = await request.get(`${BASE_URL}${ep.path}`, { headers });
      const status = res.status();
      if (status < 200 || status >= 300) {
        failures.push({
          name: ep.name,
          path: ep.path,
          status,
          critical: ep.critical,
        });
      }
    }

    const criticalFails = failures.filter((f) => f.critical);
    const softFails = failures.filter((f) => !f.critical);

    // Soft fails (P0.7+ catalogs) still report — install often lags development.
    if (softFails.length) {
      console.warn(
        "[developer-catalog-smoke] non-critical catalog HTTP failures (often means install not redeployed):\n" +
          softFails.map((f) => `  ${f.status} ${f.name} ${f.path}`).join("\n"),
      );
    }

    expect(
      criticalFails,
      `Critical catalog REST failures (redeploy rest/sitemanage or fix 5xx):\n` +
        criticalFails.map((f) => `  ${f.status} ${f.name} ${f.path}`).join("\n"),
    ).toEqual([]);
  });
});
