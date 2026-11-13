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
 * Main-nav smoke checklist SC-005 / FR-020 (spec
 * {@code specs/989-react-cui-widget-builder}, T056a, tracked by #6083).
 *
 * <p>Covers the four quickstart SC-005 steps against the modern SPA shells
 * (Dashboard is not a top-nav item — gadgets compose as the Home
 * {@code gadgets} section; see {@code TopNav.tsx}):</p>
 * <ol>
 *   <li>Dashboard gadgets open under Home.</li>
 *   <li>At least one other non-Home tab opens (Explorer).</li>
 *   <li>Home still opens from the main nav.</li>
 *   <li>Widget Builder opens only when enabled (Developer sub-entry when
 *   active; direct route redirects to {@code /home} when inactive).</li>
 * </ol>
 *
 * <p>Surface-filtered only:
 *   npm run test:surface -- --path tests/main-nav-smoke-sc-005.spec.js</p>
 *
 * <p>QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

function widgetBuilderRoute() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=widget-builder&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectSpaTopNav(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
    timeout: 30_000,
  });
}

/**
 * Collect unexpected JS console / page errors (same filter as
 * top-nav-restructure.spec.js).
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

test.describe("Main-nav smoke SC-005 (T056a / #6083)", () => {
  test("Dashboard gadgets + Explorer + Home + Widget Builder gate @smoke @ui", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const consoleErrors = collectConsoleErrors(page);

    await loginAsAdmin(page);
    await page.goto(homeDeepLink(), { waitUntil: "domcontentloaded" });
    await expectSpaTopNav(page);

    // SC-005 step 1: Dashboard gadgets open under Home (Dashboard is not a
    // top-nav item; gadgets compose as the Home `gadgets` section).
    await expect(page.getByTestId("home-shell")).toBeVisible({
      timeout: 30_000,
    });
    await page.getByTestId("home-nav-gadgets").click();
    await expect(page.getByTestId("home-gadgets-section")).toBeVisible({
      timeout: 30_000,
    });

    // SC-005 step 2: at least one other non-Home tab opens (Explorer).
    await page.getByTestId("nav-explorer").click();
    await expect(page.getByTestId("content-explorer-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page).toHaveURL(/\/explorer(?:\/|$|\?)/);

    // SC-005 step 3: Home still opens from the main nav.
    await page.getByTestId("nav-home").click();
    await expect(page.getByTestId("home-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("home-title")).toBeVisible();
    await expect(page).toHaveURL(/\/home(?:\/|$|\?)/);

    // SC-005 step 4: Widget Builder opens only when enabled. The Developer
    // shell exposes the sub-entry link when the feature is active; otherwise
    // the direct route must redirect away (RequireRole → /home) instead of
    // mounting the app.
    await page.getByTestId("nav-developer").click();
    await expect(page.getByTestId("perc-developer-shell")).toBeVisible({
      timeout: 30_000,
    });
    const wbLink = page.getByTestId("developer-widget-builder-link");
    if ((await wbLink.count()) > 0) {
      console.log("SC-005 step 4: Widget Builder ENABLED — opening via link");
      await expect(wbLink).toBeVisible();
      await wbLink.click();
      await expect(page.getByTestId("widget-builder-app")).toBeVisible({
        timeout: 30_000,
      });
    } else {
      console.log(
        "SC-005 step 4: Widget Builder DISABLED — asserting route redirect",
      );
      await page.goto(widgetBuilderRoute(), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("home-shell")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.getByTestId("widget-builder-app")).toHaveCount(0);
    }

    expectConsoleClean(consoleErrors);
  });
});
