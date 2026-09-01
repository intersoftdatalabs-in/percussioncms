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
 * Also includes **REST** probes for critical catalogs (slots, keywords) via
 * Basic auth + {@code RX_USEBASICAUTH} — residuals #2121 / #2124 after unit
 * slices #2115 / #2116.
 *
 * Entry: spa.jsp?entry=developer&section=<slug>
 * Refs #1690 (design-WS retargets #1700–#1704), #1694, #2121, #2124.
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
  {
    section: "item-filters",
    successTestIds: ["developer-if-panel", "developer-if-empty"],
    errorTestId: "developer-if-error",
  },
  {
    section: "searches",
    successTestIds: ["developer-sr-panel", "developer-sr-empty"],
    errorTestId: "developer-sr-error",
  },
  {
    section: "views",
    successTestIds: ["developer-vw-panel", "developer-vw-empty"],
    errorTestId: "developer-vw-error",
  },
  {
    section: "sites",
    successTestIds: ["developer-site-panel", "developer-site-empty"],
    errorTestId: "developer-site-error",
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
 * Critical REST catalog residuals of #1694 slices A/B (#2121 slots, #2124 keywords).
 *
 * Live H2 qa-up (2026-08-06):
 * - GET /Rhythmyx/services/slots → HTTP 200 Jackson {@code Slot} array
 * - GET /Rhythmyx/services/keywords → HTTP 200 Jackson {@code Keyword} array
 * - GET …/keywords?includeChoices=true → HTTP 200 with embedded choices
 *
 * No product stack fix on either residual — static wiring healthy after unit
 * slices #2122 / #2125. Auth: Basic + {@code RX_USEBASICAUTH: true}.
 *
 * Kept outside the SPA describe so page login beforeEach does not run.
 */
test.describe("Developer catalog REST smoke (#2121 / #2124 / #1694)", () => {
  test("REST: GET /services/slots returns 2xx (#2121)", async ({ request }) => {
    test.setTimeout(30_000);
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
    };
    const url = `${BASE_URL}/Rhythmyx/services/slots`;
    const res = await request.get(url, { headers });
    expect(
      res.status(),
      `GET ${url} must be 2xx (catalog residual #2121; was 500 on older QA)`,
    ).toBeGreaterThanOrEqual(200);
    expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

    const body = await res.json();
    // Wire format is Jackson-wrapped list under "Slot" (see SlotSummary root).
    const slots = Array.isArray(body) ? body : body?.Slot;
    expect(
      slots,
      "slots response must be a JSON array or { Slot: [...] } wrapper",
    ).toBeTruthy();
    expect(Array.isArray(slots), "slots payload must be an array").toBe(true);
    // Stock H2 / package install ships system + rff slots; empty list is still
    // a valid 2xx catalog (adaptor findSlots → empty), but assert structure.
    if (slots.length > 0) {
      const first = slots[0];
      expect(
        first.name || first.label,
        "first slot should expose name or label",
      ).toBeTruthy();
    }
  });

  test("REST: GET /services/keywords returns 2xx (#2124)", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
    };
    const url = `${BASE_URL}/Rhythmyx/services/keywords`;
    const res = await request.get(url, { headers });
    expect(
      res.status(),
      `GET ${url} must be 2xx (catalog residual #2124; was 500 on older QA)`,
    ).toBeGreaterThanOrEqual(200);
    expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

    const body = await res.json();
    // Wire format is Jackson-wrapped list under "Keyword" (KeywordSummary root).
    const keywords = Array.isArray(body) ? body : body?.Keyword;
    expect(
      keywords,
      "keywords response must be a JSON array or { Keyword: [...] } wrapper",
    ).toBeTruthy();
    expect(Array.isArray(keywords), "keywords payload must be an array").toBe(
      true,
    );
    // Stock H2 ships design keywords (e.g. Adhoc_Type); empty list is still a
    // valid 2xx catalog, but assert structure when data is present.
    if (keywords.length > 0) {
      const first = keywords[0];
      expect(
        first.label || first.value != null || first.description,
        "first keyword should expose label, value, or description",
      ).toBeTruthy();
    }
  });

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
