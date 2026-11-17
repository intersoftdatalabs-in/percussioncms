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
 * Template publish-time stale-assembly warning banner (#6478, parent #2626).
 *
 * The Design SPA tracks templates whose assembly inputs changed in this
 * session (source / assembler / bindings / slots) in browser localStorage.
 * The template editor warns while edits are dirty, and the Publish Now
 * panel warns at publish time naming each pending template. Dismissing a
 * row acknowledges the change (republished / accepted).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/design-template-stale-assembly.spec.js
 *
 * QA mode: perc-devctl qa-up  TEST_CMS_URL + ADMIN_*  test:surface  qa-down.
 *
 * Entry: spa.jsp?entry=design&section=templates and spa.jsp?entry=publish.
 * No server writes: the publish banner is seeded via localStorage and the
 * editor banner is exercised with unsaved keystrokes only.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { runA11yCheck } = require("./helpers/a11y");

const STALE_KEY = "perc.design.staleAssembly.v1";
const SAVED_TEMPLATE = "QaStaleSavedTemplate";
const UNSAVED_TEMPLATE = "QaStaleUnsavedTemplate";

function designTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "design",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function publishingSitesUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`;
}

function watchConsole(page, consoleErrors) {
  page.on("pageerror", (err) => consoleErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (text.startsWith("Failed to load resource:")) {
      return;
    }
    // Legacy JSP chrome font CSS served as text/html on the cell; unrelated
    // to the React stale-assembly surface under test.
    if (/googleFonts_opensans\.css.*MIME type/i.test(text)) {
      return;
    }
    consoleErrors.push(text);
  });
}

test.describe("Template stale-assembly warning banner (#6478)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("publish now warns about session template changes and dismisses @ui @publish", async ({
    page,
  }) => {
    const consoleErrors = [];
    watchConsole(page, consoleErrors);

    await page.addInitScript(
      ({ key, value }) => {
        window.localStorage.setItem(key, value);
      },
      {
        key: STALE_KEY,
        value: JSON.stringify({
          [SAVED_TEMPLATE]: "saved",
          [UNSAVED_TEMPLATE]: "unsaved",
        }),
      },
    );

    await page.goto(publishingSitesUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30_000,
    });

    const siteCard = page
      .locator('[data-testid^="publish-site-card-"]')
      .first();
    await expect(siteCard).toBeVisible({ timeout: 30_000 });
    await siteCard.click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
      timeout: 15_000,
    });

    const banner = page.getByTestId("item-publish-now-stale");
    await expect(banner).toBeVisible({ timeout: 15_000 });
    await expect(banner).toContainText(SAVED_TEMPLATE);
    await expect(banner).toContainText(UNSAVED_TEMPLATE);
    await expect(page.getByTestId("item-publish-now-stale-row")).toHaveCount(
      2,
    );

    await runA11yCheck(page, { scope: '[data-testid="item-publish-now"]' });

    await banner.scrollIntoViewIfNeeded();
    await page
      .screenshot({ path: "test-results/live-proof-6478-publish-banner.png" })
      .catch(() => undefined);

    await page.getByTestId("item-publish-now-stale-dismiss-0").click();
    await expect(page.getByTestId("item-publish-now-stale-row")).toHaveCount(
      1,
    );
    await page.getByTestId("item-publish-now-stale-dismiss-0").click();
    await expect(banner).toHaveCount(0);

    // Dismissal must have persisted to the session store (a fresh mount
    // re-reads the same storage instead of re-showing the banner).
    const stored = await page.evaluate(
      (key) => window.localStorage.getItem(key),
      STALE_KEY,
    );
    expect(stored === null || stored === "{}").toBe(true);

    expect(consoleErrors).toEqual([]);
  });

  test("editor warns while assembly edits are dirty without saving @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    watchConsole(page, consoleErrors);

    await page.goto(designTemplatesUrl(), { waitUntil: "networkidle" });
    await expect(page.getByTestId("perc-design-shell")).toBeVisible({
      timeout: 20_000,
    });

    const error = page.locator('[data-testid="design-tpl-error"]');
    const panel = page.locator('[data-testid="design-tpl-panel"]');
    const empty = page.locator('[data-testid="design-tpl-empty"]');
    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });
    if (await error.isVisible()) {
      throw new Error(`Design templates catalog error: ${(await error.innerText()).trim()}`);
    }
    if (await empty.isVisible()) {
      test.skip(true, "No templates in catalog - cannot exercise stale banner");
      return;
    }

    const firstRow = page.locator('[data-testid="design-tpl-row-0"]');
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="design-tpl-open-0"]').click();
    await expect(page.getByTestId("design-tpl-editor")).toBeVisible({
      timeout: 20_000,
    });

    const source = page.getByTestId("design-tpl-editor-source-edit");
    await expect(source).toBeVisible({ timeout: 20_000 });
    await expect(
      page.getByTestId("design-tpl-editor-stale-assembly"),
    ).toHaveCount(0);

    await source.focus();
    await source.press("End");
    await page.keyboard.type(" {/* qa-6478-stale */}");

    const banner = page.getByTestId("design-tpl-editor-stale-assembly");
    await expect(banner).toBeVisible({ timeout: 5_000 });
    await expect(banner).toContainText("Unsaved template changes");
    // Deliberately no save: the session mark must not outlive this test.
    await page.reload({ waitUntil: "networkidle" });

    expect(consoleErrors).toEqual([]);
  });
});
