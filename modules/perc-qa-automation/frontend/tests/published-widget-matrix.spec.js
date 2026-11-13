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
 * US2 published-widget static-render matrix (#6240 / parent #6238): every
 * pinned widget type gets one static case asserting its expected visible
 * (or {@code <head>}) output on a published fixture page with zero missing
 * resources.
 *
 * <p>Tags: {@code @published} {@code @published-widget} plus one
 * {@code @widget-<name>} tag per widget.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/published-widget-matrix.js} (closed inventory, 45
 * widgets). {@code afterAll} assembles one results-matrix row per widget,
 * validates every row against
 * {@code contracts/results-matrix.schema.json}, and writes the matrix to
 * {@code test-results/published-widget-matrix.json}. Widgets backed by a DTS
 * service assert their static shell here; service interaction is US3
 * (#6241).</p>
 *
 * <p>Run (needs the in-cell published site + fixture pages — the full
 * edition/publish path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * npm run test:surface -- --path tests/published-widget-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Single widget:
 * {@code npm run test:surface -- --path tests/published-widget-matrix.spec.js
 * --grep percCategoryList}. Without {@code PUBLISHED_SITE_URL} every case
 * skips with the named reason; a served site whose fixture page is not
 * published yet skips that widget with its own named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  WIDGETS,
  MATRIX_SPEC,
  CASE_STATIC,
  SKIP_NO_SITE,
  skipNoFixture,
  tagForWidget,
  fixtureUrlFor,
  publishedSiteUrl,
  buildWidgetRow,
  validateWidgetRow,
} = require("./helpers/published-widget-matrix");

const caseOutcomes = new Map();

function recordCase(entry) {
  caseOutcomes.set(entry.name, "pass");
}

function fixtureUrlForEntry(site, entry) {
  if (entry.name === "percCategoryList") {
    const categoryPage = process.env.PUBLISHED_CATEGORY_URL || "";
    if (categoryPage) {
      return categoryPage;
    }
  }
  return fixtureUrlFor(site, entry);
}

function watchNoise(page, hosts) {
  const pageErrors = [];
  const failedRequests = [];
  page.on("pageerror", (err) => pageErrors.push(String((err && err.stack) || err)));
  page.on("requestfailed", (req) => {
    failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
  });
  page.on("response", (resp) => {
    if (resp.status() >= 400) {
      failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
    }
  });
  return { pageErrors, failedRequests, hosts };
}

function assertQuiet(entry, pageErrors, failedRequests) {
  const seriousFailures = failedRequests.filter(
    (text) => !/favicon\.ico/i.test(text),
  );
  expect(
    seriousFailures,
    `${entry.name}: missing published resources:\n${seriousFailures.join("\n")}`,
  ).toEqual([]);
  expect(
    pageErrors,
    `${entry.name}: page errors:\n${pageErrors.join("\n")}`,
  ).toEqual([]);
}

test.describe("published widget static-render matrix", () => {
  test.afterAll(async () => {
    // One row per pinned widget. A case that never ran (grep-filtered run)
    // or never recorded (its test failed) is reported as skipped; a red run
    // is still red because the failing test itself fails.
    const rows = WIDGETS.map((entry) =>
      buildWidgetRow(
        entry,
        { [CASE_STATIC]: caseOutcomes.get(entry.name) || "skip" },
        { spec: MATRIX_SPEC },
      ),
    );
    const problems = [];
    for (const row of rows) {
      for (const error of validateWidgetRow(row)) {
        problems.push(`${row.contentType}: ${error}`);
      }
    }
    if (problems.length > 0) {
      throw new Error(`matrix rows fail schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "published-widget-matrix.json"),
      `${JSON.stringify(rows, null, 2)}\n`,
      "utf8",
    );
  });

  WIDGETS.forEach((entry) => {
    test(
      `${entry.name}: static output renders with zero missing resources`,
      { tag: ["@published", "@published-widget", tagForWidget(entry.name)] },
      async ({ page, request }) => {
        test.setTimeout(90_000);
        const site = publishedSiteUrl();
        test.skip(!site, SKIP_NO_SITE);

        const pageUrl = fixtureUrlForEntry(site, entry);
        const probe = await request.get(pageUrl);
        test.skip(
          probe.status() === 404,
          skipNoFixture(entry, pageUrl),
        );
        expect(probe.status(), `expected 200 for ${entry.name} fixture ${pageUrl}`).toBe(200);

        const { pageErrors, failedRequests } = watchNoise(page);
        await page.goto(pageUrl, { waitUntil: "networkidle" });

        if (entry.marker) {
          const target = page.locator(entry.marker).first();
          if (entry.kind === "head") {
            await expect(
              target,
              `${entry.name}: expected head output ${entry.marker}`,
            ).toBeAttached({ timeout: 15_000 });
          } else {
            await expect(
              target,
              `${entry.name}: expected visible output ${entry.marker}`,
            ).toBeVisible({ timeout: 15_000 });
          }
        } else {
          // Passthrough widgets (raw HTML / rich text / simple text / title)
          // render authored content with no stable hook: the static contract
          // is non-empty visible text.
          const text = await page.locator("body").innerText();
          expect(
            text.trim().length,
            `${entry.name}: expected visible authored text on ${pageUrl}`,
          ).toBeGreaterThan(0);
        }

        assertQuiet(entry, pageErrors, failedRequests);
        recordCase(entry);
      },
    );
  });
});
