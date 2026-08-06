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
 * Developer catalog smoke (post-P0 / #1690).
 *
 * Loads each primary Developer SPA section against a live CMS and asserts the
 * catalog reaches a non-error state (panel, empty, or loading resolved to data).
 * Content-types also asserts table body cells are not only empty / "—"
 * placeholders (empty DTOs); any other cell text counts as real data.
 *
 * Also includes **REST** probes for critical catalogs via Basic auth +
 * {@code RX_USEBASICAUTH}:
 * - slots residual #2121 after unit-test slice #2115 / #2122
 * - keywords residual #2124 after unit slice #2116 / #2125
 * - searches + C-slice peers residual #2142 after unit slice #2127 (and common
 *   jaxrs registration fix for views/cecontrols/serverconfigs/relationshiptypes)
 *
 * Entry: spa.jsp?entry=developer&section=<slug>
 * Refs #1690 (design-WS retargets #1700–#1704), #1694, #2117, #2121, #2124, #2142.
 *
 * Live REST probe (after {@code perc-devctl qa-up}):
 * <pre>
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm test -- tests/developer-catalog-smoke.spec.js -g "REST: GET /services/"
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { catalogRowsSelector } = require("./helpers/developer-catalog-selectors");

/**
 * @type {{
 *   section: string,
 *   successTestIds: string[],
 *   errorTestId: string,
 * }[]}
 */
const CATALOGS = [
  {
    section: "content-types",
    successTestIds: ["developer-ct-panel", "developer-ct-empty"],
    errorTestId: "developer-ct-error",
  },
  {
    section: "keywords",
    successTestIds: ["developer-kw-panel", "developer-kw-empty"],
    errorTestId: "developer-kw-error",
  },
  {
    section: "locales",
    successTestIds: ["developer-loc-panel", "developer-loc-empty"],
    errorTestId: "developer-loc-error",
  },
  {
    section: "slots",
    successTestIds: ["developer-slot-panel", "developer-slot-empty"],
    errorTestId: "developer-slot-error",
  },
  {
    section: "shared-fields",
    successTestIds: ["developer-sf-panel", "developer-sf-empty"],
    errorTestId: "developer-sf-error",
  },
  {
    section: "system-def",
    successTestIds: ["developer-sys-panel", "developer-sys-empty"],
    errorTestId: "developer-sys-error",
  },
];

function developerUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Critical REST catalog residuals under #1694 / #2117.
 *
 * Live H2 qa-up:
 * - GET /services/slots → 200 Jackson {@code Slot} array (residual #2121)
 * - GET /services/keywords → 200 {@code Keyword} (residual #2124)
 * - GET /services/searches → 200 {@code SearchDef} (residual #2142). Was CXF
 *   404 until restSearchResource listed on rest-jax-rs serviceBeans (same class
 *   as #1714). Empty {@code SearchDef: []} is valid when no CX searches exist.
 * - Peers views/cecontrols/serverconfigs/relationshiptypes share the same
 *   missing-registration root and go green with the same sitemanage-beans fix.
 *
 * Auth: {@code Authorization: Basic …} + {@code RX_USEBASICAUTH: true}.
 * Kept outside the SPA describe so page login beforeEach does not run.
 */
test.describe("Developer catalog REST smoke (#2121 / #2124 / #2142 / #1694)", () => {
  /**
   * @type {{
   *   path: string,
   *   wrapperKey: string,
   *   issue: string,
   *   nameFields?: string[],
   * }[]}
   */
  const REST_CATALOGS = [
    {
      path: "slots",
      wrapperKey: "Slot",
      issue: "#2121",
      nameFields: ["name", "label"],
    },
    {
      path: "keywords",
      wrapperKey: "Keyword",
      issue: "#2124",
      nameFields: ["label", "value", "description"],
    },
    {
      path: "searches",
      wrapperKey: "SearchDef",
      issue: "#2142",
      nameFields: ["name", "label"],
    },
    {
      path: "views",
      wrapperKey: "ViewDef",
      issue: "#2144",
      nameFields: ["name", "label"],
    },
    {
      path: "cecontrols",
      wrapperKey: "ControlDef",
      issue: "#2149",
      nameFields: ["name", "label"],
    },
    {
      path: "serverconfigs",
      wrapperKey: "ServerConfig",
      issue: "#2151",
      nameFields: ["name", "displayName", "fileName"],
    },
    {
      path: "relationshiptypes",
      wrapperKey: "RelationshipType",
      issue: "#2152",
      nameFields: ["name", "label"],
    },
    {
      path: "locales",
      wrapperKey: "Locale",
      issue: "#2140",
      nameFields: ["languageString", "displayName", "label"],
    },
    {
      path: "extensions/catalog",
      wrapperKey: "Extension",
      issue: "#2146",
      nameFields: ["name", "label"],
    },
  ];

  for (const cat of REST_CATALOGS) {
    test(`REST: GET /services/${cat.path} returns 2xx (${cat.issue})`, async ({
      request,
    }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = `${BASE_URL}/Rhythmyx/services/${cat.path}`;
      const res = await request.get(url, { headers });
      expect(
        res.status(),
        `GET ${url} must be 2xx (catalog residual ${cat.issue}; was 404/500 on older QA)`,
      ).toBeGreaterThanOrEqual(200);
      expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

      const body = await res.json();
      // Wire format is often Jackson-wrapped under the element name.
      const rows = Array.isArray(body) ? body : body?.[cat.wrapperKey];
      expect(
        rows,
        `${cat.path} response must be a JSON array or { ${cat.wrapperKey}: [...] } wrapper`,
      ).toBeTruthy();
      expect(
        Array.isArray(rows),
        `${cat.path} payload must be an array`,
      ).toBe(true);
      // Empty catalog is still a valid 2xx (e.g. SearchDef:[] on stock H2).
      if (rows.length > 0 && cat.nameFields?.length) {
        const first = rows[0];
        const hasLabel = cat.nameFields.some((f) => first?.[f] != null && first?.[f] !== "");
        expect(
          hasLabel,
          `first ${cat.path} row should expose one of: ${cat.nameFields.join(", ")}`,
        ).toBe(true);
      }
    });
  }

  test("REST: GET /services/keywords?includeChoices=true returns 2xx with choices (#2124)", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
    };
    const url = `${BASE_URL}/Rhythmyx/services/keywords?includeChoices=true`;
    const res = await request.get(url, { headers });
    expect(
      res.status(),
      `GET ${url} must be 2xx (includeChoices residual #2124)`,
    ).toBeGreaterThanOrEqual(200);
    expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

    const body = await res.json();
    const keywords = Array.isArray(body) ? body : body?.Keyword;
    expect(
      keywords,
      "includeChoices response must be a JSON array or { Keyword: [...] }",
    ).toBeTruthy();
    expect(Array.isArray(keywords), "keywords payload must be an array").toBe(
      true,
    );

    // When the catalog is non-empty, at least one stock keyword should embed a
    // non-empty choices list (design-WS findKeywords + choice mapping).
    if (keywords.length > 0) {
      const withChoices = keywords.find(
        (kw) => Array.isArray(kw.choices) && kw.choices.length > 0,
      );
      expect(
        withChoices,
        "includeChoices=true should embed at least one keyword with choices",
      ).toBeTruthy();
      const choice = withChoices.choices[0];
      expect(
        choice.label != null || choice.value != null || choice.description,
        "choice entries should expose label, value, or description",
      ).toBeTruthy();
    }
  });
});

test.describe("Developer catalog smoke (#1690)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  for (const cat of CATALOGS) {
    test(`${cat.section}: catalog loads without API error`, async ({
      page,
    }) => {
      await page.goto(developerUrl(cat.section), { waitUntil: "networkidle" });

      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator(`[data-testid="tab-developer-${cat.section}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const error = page.locator(`[data-testid="${cat.errorTestId}"]`);
      const success = page.locator(
        cat.successTestIds.map((id) => `[data-testid="${id}"]`).join(", "),
      );

      // Wait until loading finishes: either success surface or error alert
      await expect(success.or(error).first()).toBeVisible({ timeout: 30_000 });

      if (await error.isVisible()) {
        const msg = (await error.innerText()).trim();
        throw new Error(
          `Developer section "${cat.section}" showed catalog error: ${msg}`,
        );
      }

      await expect(success.first()).toBeVisible();

      // Content-types: panel with rows must expose real labels/names, not only
      // "—" placeholders (empty DTOs when list JSON only carries hideFromMenu).
      // Empty catalog (developer-ct-empty) is a valid success surface.
      if (cat.section === "content-types") {
        await assertContentTypesRowsUsable(page);
      }
    });
  }
});

/**
 * When the content-types panel is shown (not the empty state), require at least
 * one data row whose cells are not only empty / "—" placeholders (empty DTOs).
 * Accepts any non-placeholder text: single letters, digits, "Label", "C++", etc.
 *
 * @param {import('@playwright/test').Page} page
 */
async function assertContentTypesRowsUsable(page) {
  const panel = page.locator('[data-testid="developer-ct-panel"]');
  if (!(await panel.isVisible())) {
    return;
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 10_000 });

  // WebUI SimpleCatalogTable uses indexed testids: developer-ct-row-0, …
  // (Vitest: getByTestId("developer-ct-row-0")). Bare developer-ct-row never
  // matches — matrix #2185 / harden #2186.
  const rows = table.locator(catalogRowsSelector("developer-ct-row"));
  const rowCount = await rows.count();
  expect(
    rowCount,
    "content type table should have at least one indexed row (developer-ct-row-N) when panel is shown",
  ).toBeGreaterThan(0);

  // Body cells only (skip thead). Placeholder UI uses em dash / hyphen when
  // label/name are missing from the DTO — any other trimmed text is real data.
  const hasRealCell = await rows.evaluateAll((trs) => {
    const isPlaceholder = (raw) => {
      const t = (raw || "").replace(/\u00a0/g, " ").trim();
      return t === "" || t === "—" || t === "–" || t === "-";
    };
    return trs.some((tr) =>
      Array.from(tr.querySelectorAll("td")).some(
        (td) => !isPlaceholder(td.textContent),
      ),
    );
  });
  expect(
    hasRealCell,
    "content type rows look empty (labels/names missing from API/DTO) — redeploy rest/WebUI or fix ContentType list mapping",
  ).toBe(true);
}
