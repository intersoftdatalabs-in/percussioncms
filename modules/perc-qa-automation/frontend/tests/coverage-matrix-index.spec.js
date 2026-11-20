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
 * Coverage-matrix index (#6238 aggregate, SC-004).
 *
 * <p>Tags: {@code @coverage-matrix-index}</p>
 *
 * <p>The single accounting point over the three QA coverage matrices — US1
 * creation ({@code content-create-matrix}, #6239), US2 static render
 * ({@code published-widget-matrix}, #6240), and US3 DTS interaction
 * ({@code dts-interaction-matrix}, #6241). Fails when any matrix inventory
 * is empty, unfrozen, or has duplicate names; when a dynamic widget names a
 * DTS service with no interaction case; when a DTS service has no proving
 * artifact (static widget, published surface, or deep matrix); or when the
 * operator docs page ({@code product-docs/8.2/admin/qa-coverage-matrices.md})
 * drifts from the pinned suites and counts. Named exceptions (forms deep
 * matrix held out under #6482, membership with no surface row) are recorded
 * in the helper — never unexplained.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/coverage-matrix-index.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 *
 * <p>Provisioning-aware skip contract (mirrors the matrix specs): a
 * {@code beforeEach} liveness probe skips every case by name when the
 * CMS-only cell is unprovisioned or unreachable instead of failing at
 * login.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, BASE_URL_SOURCE } = require("./helpers/auth");
const {
  BUILT_IN_TYPES,
  SYSTEM_EXCLUSIONS,
  cmsLivenessUrl,
  skipNoCmsCell,
} = require("./helpers/content-create-matrix");
const { WIDGETS, watchNoise, assertQuiet } = require("./helpers/published-widget-matrix");
const { DTS_SERVICES } = require("./helpers/dts-interaction-matrix");
const {
  MATRIX_SPEC,
  DOC_ID,
  matrixInventories,
  findDuplicates,
  uncoveredDynamicWidgets,
  serviceCoverage,
  unaccountedServices,
  repoRootFromHelper,
  docPagePath,
  indexSummary,
} = require("./helpers/coverage-matrix-index");

// Cell liveness is probed once per worker and cached (see the matrix specs).
let cmsLiveness = null;

/**
 * Skips the calling case with the named provisioning reason when the
 * CMS-only cell cannot serve the index check; otherwise returns.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 */
async function ensureCmsCell(request) {
  if (!cmsLiveness) {
    cmsLiveness = (async () => {
      try {
        const probe = await request.get(cmsLivenessUrl(BASE_URL), {
          timeout: 15_000,
        });
        if (!probe.ok()) {
          return { ok: false, detail: `liveness answered HTTP ${probe.status()}` };
        }
        return { ok: true, detail: "" };
      } catch (error) {
        return {
          ok: false,
          detail: `probe unreachable (${String((error && error.message) || error).slice(0, 120)})`,
        };
      }
    })();
  }
  const result = await cmsLiveness;
  test.skip(!result.ok, skipNoCmsCell(BASE_URL, BASE_URL_SOURCE, result.detail));
}

test.describe("coverage matrix index", () => {
  test.beforeEach(async ({ page, request }) => {
    test.setTimeout(90_000);
    await ensureCmsCell(request);
    await loginAsAdmin(page);
  });

  test(
    "three matrix inventories are closed",
    { tag: ["@coverage-matrix-index"] },
    async ({ page }) => {
      // Shared US2 quiet contract: page errors, failed requests/4xx+, and
      // JS console errors all fail the index login page (favicon exempt).
      const noise = watchNoise(page);
      await page.goto(`${BASE_URL}/Rhythmyx/login`, {
        waitUntil: "domcontentloaded",
      });

      const inventories = matrixInventories();
      expect(inventories.map((entry) => entry.suite)).toEqual([
        "content-create-matrix",
        "published-widget-matrix",
        "dts-interaction-matrix",
      ]);
      for (const entry of inventories) {
        expect(
          entry.count,
          `${entry.suite}: inventory must be non-empty`,
        ).toBeGreaterThan(0);
      }

      const frozen = [
        ["content types", BUILT_IN_TYPES],
        ["type exclusions", SYSTEM_EXCLUSIONS],
        ["widgets", WIDGETS],
        ["DTS services", DTS_SERVICES],
      ];
      for (const [label, list] of frozen) {
        expect(Object.isFrozen(list), `${label}: inventory must be frozen`).toBe(true);
      }

      const dupes = findDuplicates([
        ...BUILT_IN_TYPES.map((entry) => `type:${entry.name}`),
        ...WIDGETS.map((entry) => `widget:${entry.name}`),
        ...DTS_SERVICES.map((entry) => `service:${entry.name}`),
      ]);
      expect(dupes, `duplicate inventory names: ${dupes.join(", ")}`).toEqual([]);
      assertQuiet({ name: "coverage-matrix-index" }, noise, expect);
    },
  );

  test(
    "every dynamic widget names a DTS service with an interaction case",
    { tag: ["@coverage-matrix-index"] },
    async () => {
      const uncovered = uncoveredDynamicWidgets();
      expect(
        uncovered,
        `dynamic widgets with no interaction service: ${uncovered
          .map((entry) => `${entry.widget} -> ${entry.dts}`)
          .join(", ")}`,
      ).toEqual([]);
    },
  );

  test(
    "every DTS service has a proving artifact",
    { tag: ["@coverage-matrix-index"] },
    async () => {
      const unaccounted = unaccountedServices();
      expect(
        unaccounted,
        `DTS services with no proving artifact: ${unaccounted.join(", ")}`,
      ).toEqual([]);

      const rows = serviceCoverage();
      expect(rows.map((row) => row.service).sort()).toEqual(
        DTS_SERVICES.map((entry) => entry.name).sort(),
      );
      for (const row of rows) {
        expect(row.covered, `${row.service}: expected a proving artifact`).toBe(true);
      }
    },
  );

  test(
    "operator docs page lists the three matrices with matching counts",
    { tag: ["@coverage-matrix-index"] },
    async () => {
      const repoRoot = repoRootFromHelper(path.join(__dirname, "helpers"));
      const docPath = docPagePath(repoRoot);
      expect(
        fs.existsSync(docPath),
        `operator docs page missing: ${path.relative(repoRoot, docPath)}`,
      ).toBe(true);
      const doc = fs.readFileSync(docPath, "utf8");
      expect(doc.includes(`id: ${DOC_ID}`)).toBe(true);
      for (const entry of matrixInventories()) {
        expect(doc.includes(entry.suite)).toBe(true);
        expect(
          doc.includes(String(entry.count)),
          `docs page must pin ${entry.suite} count ${entry.count}`,
        ).toBe(true);
      }
      expect(doc.includes(MATRIX_SPEC)).toBe(true);
    },
  );

  test.afterAll(async () => {
    const summary = indexSummary();
    const outDir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(
      path.join(outDir, "coverage-matrix-index.json"),
      `${JSON.stringify(summary, null, 2)}\n`,
    );
  });
});
