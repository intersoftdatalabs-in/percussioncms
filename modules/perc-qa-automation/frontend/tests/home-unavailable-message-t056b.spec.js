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
 * Unavailable-message UAT T056b (spec {@code specs/989-react-cui-widget-builder},
 * FR-013 / SC-007, tracked by #6084).
 *
 * <p>Covers the moved/unavailable contract: unmapped legacy {@code ?view=<retired>}
 * URLs are dispatched (302) to {@code spa.jsp?entry=unavailable}, which mounts
 * the on-page Unavailable message ({@code route-unavailable}); deleted classic
 * entry JSPs ({@code home.jsp} / {@code widgetBuilder.jsp}, hard-cut with no
 * stubs) serve the branded server 404 page instead of a blank screen; and the
 * unmapped-view mapping does not swallow known views ({@code ?view=home}
 * regression guard).</p>
 *
 * <p>Known deep links are T056 (#6082), not this spec.</p>
 *
 * <p>Surface-filtered only:
 *   npm run test:surface -- --path tests/home-unavailable-message-t056b.spec.js</p>
 *
 * <p>QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function dispatcherUrl(view) {
  return `${BASE_URL}/Rhythmyx/cm/app/?view=${encodeURIComponent(view)}&_=${Date.now()}`;
}

function unavailableEntryUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=unavailable&_=${Date.now()}`;
}

function deletedEntryUrl(jsp) {
  return `${BASE_URL}/Rhythmyx/cm/app/${jsp}?_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 * @returns {string[]} collector backing the test
 */
function collectConsoleErrors(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return consoleErrors;
}

/**
 * Same environmental-noise filter as the T056/SC-007 seat
 * (home-deep-links-sc-007.spec.js): favicon/asset 404 probes and offline
 * net::ERR text are not product defects.
 * @param {string[]} consoleErrors
 */
function expectConsoleClean(consoleErrors) {
  const unexpected = consoleErrors.filter(
    (t) =>
      !/favicon|404|net::ERR|Failed to load resource/i.test(t) &&
      !/Download the React DevTools/i.test(t),
  );
  expect(unexpected, `JS console errors: ${unexpected.join(" | ")}`).toEqual(
    [],
  );
}

/**
 * On-page moved/unavailable message shared by every unavailable sample.
 * @param {import('@playwright/test').Page} page
 */
async function expectUnavailableMessage(page) {
  await expect(page.getByTestId("route-unavailable")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("route-unavailable-title")).toHaveText(
    "Unavailable",
  );
  await expect(page.getByTestId("route-unavailable")).toContainText(
    /SPA route is active/,
  );
  // No classic CM1 host chrome on the SPA message surface.
  await expect(page.locator("#perc_site_map")).toHaveCount(0);
  await expect(page.locator(".perc-mcol")).toHaveCount(0);
}

test.describe("Unavailable message UAT T056b (FR-013 / #6084)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  const unmappedViews = ["retiredview123", "retiredwbview456"];
  for (const view of unmappedViews) {
    test(
      `unmapped ?view=${view} dispatches to the on-page Unavailable message @smoke @ui`,
      { tag: ["@smoke", "@ui"] },
      async ({ page }) => {
        test.setTimeout(120_000);
        const consoleErrors = collectConsoleErrors(page);
        await page.goto(dispatcherUrl(view), {
          waitUntil: "domcontentloaded",
        });
        // Dispatcher 302 targets spa.jsp?entry=unavailable; the SPA then
        // client-routes to the clean /cm/app/unavailable path — accept the
        // transient query form or the settled client path (poll races the
        // history.replace, so both are valid final states).
        await expect(page).toHaveURL(/entry=unavailable|\/app\/unavailable/, {
          timeout: 30_000,
        });
        await expectUnavailableMessage(page);
        expectConsoleClean(consoleErrors);
      },
    );
  }

  test(
    "direct spa.jsp?entry=unavailable shows the on-page message @smoke @ui",
    { tag: ["@smoke", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const consoleErrors = collectConsoleErrors(page);
      await page.goto(unavailableEntryUrl(), {
        waitUntil: "domcontentloaded",
      });
      await expectUnavailableMessage(page);
      // Live proof (C5): committed copy under docs/ai-generated/proofs/.
      await page.screenshot({
        path: "test-results/live-proof-6084-unavailable.png",
      });
      expectConsoleClean(consoleErrors);
    },
  );

  const deletedEntries = ["home.jsp", "widgetBuilder.jsp"];
  for (const jsp of deletedEntries) {
    test(
      `deleted classic entry ${jsp} serves the branded 404 page, never blank @smoke @ui`,
      { tag: ["@smoke", "@ui"] },
      async ({ page }) => {
        test.setTimeout(120_000);
        const consoleErrors = collectConsoleErrors(page);
        const response = await page.goto(deletedEntryUrl(jsp), {
          waitUntil: "domcontentloaded",
        });
        expect(response && response.status(), `GET ${jsp} status`).toBe(404);
        const heading = page.getByRole("heading").first();
        await expect(heading).toBeVisible({ timeout: 30_000 });
        const rawHeading = (await heading.textContent()) || "";
        const headingText = rawHeading.replace(/ /g, " ");
        expect(
          headingText,
          `branded 404 heading for ${jsp}: ${JSON.stringify(rawHeading)}`,
        ).toMatch(/cannot be found/i);
        // Branded exits (Main Page / Percussion Help / Support) — never blank.
        const exits = page.getByRole("button").or(page.getByRole("link"));
        expect(await exits.count(), `branded 404 exits for ${jsp}`).toBeGreaterThan(0);
        const bodyText = ((await page.locator("body").textContent()) || "").trim();
        expect(bodyText.length, `non-blank 404 body for ${jsp}`).toBeGreaterThan(0);
        // Sub-resource 404 probes (favicon/asset) and the error page's own
        // missing stylesheets (bootstrap 4.0.0 is not shipped in the CMS-only
        // QA cell install — "Refused to apply style … MIME type text/html")
        // are environmental noise on the server error page; only hard
        // page-errors fail this sample.
        const hardErrors = consoleErrors.filter(
          (t) =>
            !/favicon|404|net::ERR|Failed to load resource|Refused to apply style|MIME type/i.test(
              t,
            ),
        );
        expect(
          hardErrors,
          `hard JS errors on ${jsp} 404: ${hardErrors.join(" | ")}`,
        ).toEqual([]);
      },
    );
  }

  test(
    "?view=home regression guard still mounts the Home shell @smoke @ui",
    { tag: ["@smoke", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const consoleErrors = collectConsoleErrors(page);
      await page.goto(dispatcherUrl("home"), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("home-shell")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
        timeout: 30_000,
      });
      expectConsoleClean(consoleErrors);
    },
  );
});
