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
 * Profile Preferences section (#2396 / parent #2374 slice 4).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/profile-preferences.spec.js
 *
 * Axe form root (#2503 / residual #2427):
 *   npm run test:surface -- --path tests/profile-preferences.spec.js --grep "axe-core"
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Covers: Preferences form mounts (no #2746 load JAXB / retry error);
 * change default landing; reload persists (#3207).
 * Axe: zero serious/critical on [data-testid="perc-profile-preferences"].
 * Console/pageerror + prefs/homepage 4xx/5xx must stay clean on this surface.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  loginAsContributor,
  BASE_URL,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/** Form root for preferences edit (not the whole profile shell). */
const PREFERENCES_FORM_SCOPE = '[data-testid="perc-profile-preferences"]';

function profileDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}#perc-profile-preferences`;
}

/**
 * @param {import('@playwright/test').Page} page
 * @returns {{ jsErrors: string[], httpErrors: string[] }}
 */
function attachSurfaceErrorCollectors(page) {
  const jsErrors = [];
  const httpErrors = [];
  page.on("pageerror", (err) => {
    jsErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    // Chrome logs 404 fetches as console.error (empty prefs, maps, favicon).
    // Those are not uncaught page errors — gate on pageerror + 4xx/5xx below.
    if (/Failed to load resource:.*\b404\b/i.test(text)) {
      return;
    }
    jsErrors.push(text);
  });
  page.on("response", (response) => {
    const url = response.url();
    const status = response.status();
    // 404 on GET /preferences/ means no stored entries (empty list).
    if (status < 400 || status === 404) {
      return;
    }
    const isPrefs =
      url.includes("/services/preferences") ||
      url.includes("/user/user/homepage");
    if (isPrefs) {
      httpErrors.push(`${status} ${url}`);
    }
  });
  return { jsErrors, httpErrors };
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectPreferencesMounted(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
    timeout: 30_000,
  });
  await expect(
    page.getByTestId("perc-profile-section-preferences"),
  ).toBeVisible();
  await expect(page.getByTestId("perc-profile-preferences")).toBeVisible({
    timeout: 30_000,
  });
  // #2746: GET /preferences/ JAXB failure left load-error + Try again.
  await expect(
    page.getByTestId("perc-profile-preferences-load-error"),
  ).toHaveCount(0);
  await expect(page.getByTestId("perc-profile-preferences-retry")).toHaveCount(
    0,
  );
  await expect(
    page.getByTestId("perc-profile-preferences-landing"),
  ).toBeVisible({ timeout: 30_000 });
}

test.describe("Profile preferences @profile @preferences @smoke", () => {
  test("deep link shows preferences landing control for Admin", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const { jsErrors, httpErrors } = attachSurfaceErrorCollectors(page);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);
    await page.getByTestId("perc-profile-nav-preferences").click();

    const landing = page.getByTestId("perc-profile-preferences-landing");
    await expect(landing).toBeEnabled();
    const optionValues = await landing.locator("option").evaluateAll((els) =>
      els.map((o) => o.value),
    );
    expect(optionValues).toContain("Home");
    expect(optionValues).toContain("Explorer");
    expect(optionValues).toContain("Architecture");
    expect(optionValues).toContain("Developer");
    expect(optionValues).toContain("Publish");
    expect(optionValues).toContain("Workflow");
    const current = await landing.inputValue();
    if (current !== "Editor") {
      expect(optionValues).not.toContain("Editor");
    }
    if (current !== "Designer") {
      expect(optionValues).not.toContain("Designer");
    }
    await expect(
      page.getByTestId("perc-profile-preferences-save"),
    ).toBeVisible();
    await expect(
      page.getByTestId("perc-profile-preferences-count"),
    ).toBeVisible();
    expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    expect(httpErrors, httpErrors.join("\n")).toEqual([]);
  });

  /**
   * #2503 residual of #2427 — axe serious/critical zero on preferences form
   * root (landing select, save, live status, or load-error/retry). Scope is the
   * form section, not perc-profile-shell. Waits for the form root only so a
   * load-error state (missing preferences API on a partial stack) is still
   * axe-scanned — functional persist coverage remains in the smoke tests.
   */
  test("axe-core a11y gate — preferences form root (#2503)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-app")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-profile-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.getByTestId("perc-profile-section-preferences"),
    ).toBeVisible();
    // Root is present for loading, ready, and error — do not require landing.
    await expect(page.getByTestId("perc-profile-preferences")).toBeVisible({
      timeout: 30_000,
    });
    // Leave loading (aria-busy) before axe when possible.
    await expect(page.getByTestId("perc-profile-preferences")).not.toHaveAttribute(
      "aria-busy",
      "true",
      { timeout: 30_000 },
    );

    await expectNoSeriousA11yViolations(page, {
      scope: PREFERENCES_FORM_SCOPE,
    });
  });

  test("Contributor profile does not list Admin/Developer/Publish (#3538)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const { jsErrors, httpErrors } = attachSurfaceErrorCollectors(page);
    await loginAsContributor(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);
    await page.getByTestId("perc-profile-nav-preferences").click();

    const landing = page.getByTestId("perc-profile-preferences-landing");
    await expect(landing).toBeEnabled();
    const optionValues = await landing.locator("option").evaluateAll((els) =>
      els.map((o) => o.value),
    );
    expect(optionValues).toContain("");
    expect(optionValues).toContain("Home");
    expect(optionValues).toContain("Explorer");
    expect(optionValues).not.toContain("Workflow");
    expect(optionValues).not.toContain("Developer");
    expect(optionValues).not.toContain("Publish");
    expect(optionValues).not.toContain("Architecture");
    const current = await landing.inputValue();
    if (current !== "Editor") {
      expect(optionValues).not.toContain("Editor");
    }
    if (current !== "Designer") {
      expect(optionValues).not.toContain("Designer");
    }
    if (current !== "WidgetBuilder") {
      expect(optionValues).not.toContain("WidgetBuilder");
    }
    expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    expect(httpErrors, httpErrors.join("\n")).toEqual([]);
  });

  test("change landing preference, reload, value persists", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { jsErrors, httpErrors } = attachSurfaceErrorCollectors(page);
    await loginAsAdmin(page);

    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);
    await page.getByTestId("perc-profile-nav-preferences").click();

    const landing = page.getByTestId("perc-profile-preferences-landing");
    await expect(landing).toBeVisible();

    const original = await landing.inputValue();
    // Prefer Home ↔ Architecture (long-supported persist types). Explorer is
    // listed for Admin in the option-set test above; persist of Explorer
    // requires the #3536 server catalog and can 400 on skip-image-build cells.
    const next = original === "Architecture" ? "Home" : "Architecture";

    await landing.selectOption(next);
    await page.getByTestId("perc-profile-preferences-save").click();
    await expect(
      page.getByTestId("perc-profile-preferences-success"),
    ).toBeVisible({ timeout: 30_000 });
    await expect(landing).toHaveValue(next);

    // Reload profile hub — value must come back from server
    await page.goto(profileDeepLink(), { waitUntil: "domcontentloaded" });
    await expectPreferencesMounted(page);
    await page.getByTestId("perc-profile-nav-preferences").click();
    await expect(
      page.getByTestId("perc-profile-preferences-landing"),
    ).toHaveValue(next, { timeout: 30_000 });

    // Best-effort restore original for stable re-runs
    if (original !== next) {
      const landing2 = page.getByTestId("perc-profile-preferences-landing");
      await landing2.selectOption(original);
      await page.getByTestId("perc-profile-preferences-save").click();
      await expect(
        page.getByTestId("perc-profile-preferences-success"),
      ).toBeVisible({ timeout: 30_000 });
    }
    expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    expect(httpErrors, httpErrors.join("\n")).toEqual([]);
  });
});
