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
 * Developer template source editor accessible names + keyboard reachability
 * (#5626 UX polish: edit textarea and highlight preview expose the
 * "Template source" accessible name; the scrollable preview is a labelled,
 * keyboard-focusable region).
 *
 * Opens Developer → Templates, opens the first template detail, asserts the
 * source textarea accessible name, toggles to preview highlight, and asserts
 * the preview region name + Tab reachability.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up --skip-image-build --then-qa-deploy-webui
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-source-a11y.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Tags: @developer-templates @a11y @smoke @ui
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

test.describe("Developer template source editor a11y (#5626)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("Source edit textarea and highlight preview are labelled; preview is keyboard-focusable @developer-templates @a11y @smoke @ui", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await page.goto(developerTemplatesUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-templates"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-tpl-error"]');
    const panel = page.locator('[data-testid="developer-tpl-panel"]');
    const empty = page.locator('[data-testid="developer-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in CMS — cannot exercise source a11y");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-tpl-detail"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-source"]'),
    ).toBeVisible({ timeout: 15_000 });

    // Ensure edit mode (preview hides the textarea).
    if ((await page.locator('[data-testid="developer-tpl-source-edit"]').count()) === 0) {
      await page.locator('[data-testid="developer-tpl-source-mode"]').click();
    }
    const sourceEdit = page.locator('[data-testid="developer-tpl-source-edit"]');
    await expect(sourceEdit).toBeVisible({ timeout: 15_000 });

    // Edit textarea exposes the visible section heading as its accessible name.
    await expect(
      page.getByRole("textbox", { name: "Template source" }),
    ).toBeVisible({ timeout: 10_000 });

    // Toggle to highlight preview: labelled region, keyboard-focusable.
    await page.locator('[data-testid="developer-tpl-source-mode"]').click();
    const preview = page.locator('[data-testid="developer-tpl-source-preview"]');
    await expect(preview).toBeVisible({ timeout: 15_000 });
    await expect(
      page.getByRole("region", { name: "Template source" }),
    ).toBeVisible({ timeout: 10_000 });
    expect(await preview.getAttribute("tabindex")).toBe("0");

    await preview.focus();
    expect(await preview.evaluate((el) => el === document.activeElement)).toBe(
      true,
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
