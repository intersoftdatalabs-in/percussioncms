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
 * US1 creation-matrix inventory check (#6239 / parent #6238).
 *
 * <p>Tags: {@code @content-create} {@code @content-create-inventory} {@code @explorer}</p>
 *
 * <p>Fails when a pinned built-in type is missing from the live CMS catalog
 * (closed inventory, T001) and when the live catalog contains a creatable
 * type that is neither pinned nor documented in {@code SYSTEM_EXCLUSIONS}
 * (none unaccounted). Also validates one results-matrix row per pinned entry
 * against {@code contracts/results-matrix.schema.json}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/content-create-inventory.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  BUILT_IN_TYPES,
  SYSTEM_EXCLUSIONS,
  INVENTORY_SPEC,
  buildResultRow,
  validateResultRow,
} = require("./helpers/content-create-matrix");

async function liveCatalogNames(page) {
  return page.evaluate(async () => {
    const tokenObj = window.OWASP_CSRFTOKEN;
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const token = (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
    const headerName = (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
    const headers = { Accept: "application/json" };
    if (token) {
      headers[headerName] = token;
    }
    const res = await fetch("/Rhythmyx/services/contenttypes", {
      headers,
      credentials: "same-origin",
    });
    const payload = await res.json();
    const list = payload.ContentType || payload.contentType || [];
    return list.map((item) => String(item.name || "").trim()).filter(Boolean);
  });
}

test.describe("content creation inventory", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "live catalog covers the pinned inventory and nothing is unaccounted",
    { tag: ["@content-create", "@content-create-inventory", "@explorer"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=developer`, {
        waitUntil: "networkidle",
      });
      const catalog = await liveCatalogNames(page);
      expect(catalog.length).toBeGreaterThan(0);

      const pinned = BUILT_IN_TYPES.map((entry) => entry.name);
      const excluded = SYSTEM_EXCLUSIONS.map((entry) => entry.name);
      const missing = pinned.filter((name) => !catalog.includes(name));
      expect(missing, `pinned types missing from the CMS catalog: ${missing.join(", ")}`).toEqual(
        [],
      );

      const accounted = new Set([...pinned, ...excluded]);
      const unaccounted = catalog.filter((name) => !accounted.has(name));
      expect(
        unaccounted,
        `catalog types with no matrix case and no exclusion reason: ${unaccounted.join(", ")}`,
      ).toEqual([]);
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    },
  );

  test(
    "every pinned entry builds a schema-valid results-matrix row",
    { tag: ["@content-create", "@content-create-inventory", "@explorer"] },
    async () => {
      const problems = [];
      for (const entry of BUILT_IN_TYPES) {
        const row = buildResultRow(
          entry,
          { createRoundTrip: "pass", negativeRequired: "pass" },
          { spec: INVENTORY_SPEC },
        );
        for (const error of validateResultRow(row)) {
          problems.push(`${entry.name}: ${error}`);
        }
      }
      expect(problems, problems.join(" | ")).toEqual([]);
      expect(BUILT_IN_TYPES.length).toBeGreaterThan(0);
    },
  );
});
