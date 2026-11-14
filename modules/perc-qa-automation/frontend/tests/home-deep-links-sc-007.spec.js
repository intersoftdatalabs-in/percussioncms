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
 * Home deep links SC-007 (spec {@code specs/989-react-cui-widget-builder},
 * T056, tracked by #6082).
 *
 * <p>Covers the contract
 * {@code specs/989-react-cui-widget-builder/contracts/home-deep-links.md}:
 * legacy {@code ?entry=home&initialScreen=<value>} URLs land on the mapped
 * modern Home section ({@code library→library}, {@code list→recent},
 * {@code search→search}, {@code newitem→create}; absent/unknown defaults to
 * Recent per {@code WebUI/src/main/ts/home/deepLinkMap.ts}). The Widget
 * Builder route is covered as the SC-002 entry point (enabled → app mounts;
 * disabled → redirects to {@code /home} without mounting).</p>
 *
 * <p>Unmapped/obsolete-path on-page messaging is T056b (#6084), not this
 * spec.</p>
 *
 * <p>Surface-filtered only:
 *   npm run test:surface -- --path tests/home-deep-links-sc-007.spec.js</p>
 *
 * <p>QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeEntryUrl(initialScreen) {
  const qs =
    initialScreen == null || initialScreen === ""
      ? "entry=home"
      : `entry=home&initialScreen=${encodeURIComponent(initialScreen)}`;
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${qs}&_=${Date.now()}`;
}

function widgetBuilderUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=widget-builder&_=${Date.now()}`;
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
 * @param {import('@playwright/test').Page} page
 * @param {string} section
 */
async function expectHomeSectionActive(page, section) {
  await expect(page.getByTestId("home-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId(`home-nav-${section}`)).toHaveAttribute(
    "aria-current",
    "page",
    { timeout: 15_000 },
  );
}

test.describe("Home deep links SC-007 (T056 / #6082)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  const cases = [
    { initialScreen: "library", section: "library" },
    { initialScreen: "list", section: "recent" },
    { initialScreen: "search", section: "search" },
    { initialScreen: "newitem", section: "create" },
  ];
  for (const { initialScreen, section } of cases) {
    test(
      `initialScreen=${initialScreen} lands on ${section} @smoke @ui`,
      { tag: ["@smoke", "@ui"] },
      async ({ page }) => {
        test.setTimeout(120_000);
        const consoleErrors = collectConsoleErrors(page);
        await page.goto(homeEntryUrl(initialScreen), {
          waitUntil: "domcontentloaded",
        });
        await expectHomeSectionActive(page, section);
        expectConsoleClean(consoleErrors);
      },
    );
  }

  test(
    "absent initialScreen defaults to Recent @smoke @ui",
    { tag: ["@smoke", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const consoleErrors = collectConsoleErrors(page);
      await page.goto(homeEntryUrl(""), { waitUntil: "domcontentloaded" });
      await expectHomeSectionActive(page, "recent");
      expectConsoleClean(consoleErrors);
    },
  );

  test(
    "unknown initialScreen defaults to Recent (documented map) @smoke @ui",
    { tag: ["@smoke", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const consoleErrors = collectConsoleErrors(page);
      await page.goto(homeEntryUrl("frobnicate"), {
        waitUntil: "domcontentloaded",
      });
      await expectHomeSectionActive(page, "recent");
      expectConsoleClean(consoleErrors);
    },
  );

  test(
    "Widget Builder entry mounts when enabled, else redirects to Home @smoke @ui",
    { tag: ["@smoke", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const consoleErrors = collectConsoleErrors(page);
      await page.goto(widgetBuilderUrl(), {
        waitUntil: "domcontentloaded",
      });
      const wb = page.getByTestId("widget-builder-app");
      await expect(
        wb.or(page.getByTestId("home-shell")),
      ).toBeVisible({ timeout: 30_000 });
      if ((await wb.count()) > 0) {
        console.log("SC-002: Widget Builder ENABLED — app mounted");
      } else {
        console.log(
          "SC-002: Widget Builder DISABLED — redirected to Home, app not mounted",
        );
        await expect(wb).toHaveCount(0);
        await expect(page).toHaveURL(/entry=home|\/home/);
      }
      expectConsoleClean(consoleErrors);
    },
  );
});
