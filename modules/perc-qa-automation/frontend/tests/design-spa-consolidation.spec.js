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
 * Design SPA Playwright consolidation (#3307 / parent #2631).
 *
 * Surface-filtered only (do not run the full suite):
 *   npm run test:surface -- --path tests/design-spa-consolidation.spec.js
 *   npm run test:surface -- --tag design-spa
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Library + edit run when Design chrome is on the cell (#2808–#2810).
 * Create (#3305 / #3578) is required — fail if design-tpl-create is missing.
 * Classic-list redirect (#3306) still skips cleanly when that cutover is not
 * deployed yet.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");
const {
  TEST_IDS,
  CLASSIC_ASSIGNED_TEMPLATES_ID,
  SKIP,
  designTemplatesUrl,
  designLegacyAdminUrl,
  designLegacyViewUrl,
  skipReasonForChrome,
  filterConsoleNoise,
  softVisible,
} = require("./helpers/design-spa-surface");

function tid(id) {
  return `[data-testid="${id}"]`;
}

function attachConsoleGate(page) {
  const errors = [];
  page.on("pageerror", (err) => {
    errors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
  return errors;
}

async function openDesignLibrary(page) {
  await page.goto(designTemplatesUrl(BASE_URL), { waitUntil: "networkidle" });
  const shell = page.locator(tid(TEST_IDS.shell));
  const present = await softVisible(shell, 20_000);
  const reason = skipReasonForChrome({ shellPresent: present });
  if (reason) {
    test.skip(true, reason);
  }
  return shell;
}

test.describe("Design SPA consolidation (#3307)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("library shell lists templates or empty @smoke @ui @design-spa", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await openDesignLibrary(page);

    await expect(page.locator(tid(TEST_IDS.nav))).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(tid(TEST_IDS.tabTemplates))).toBeVisible({
      timeout: 15_000,
    });

    const error = page.locator(tid(TEST_IDS.error));
    const panel = page.locator(tid(TEST_IDS.panel));
    const empty = page.locator(tid(TEST_IDS.empty));
    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });

  test("open first template editor @smoke @ui @design-spa", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await openDesignLibrary(page);

    const error = page.locator(tid(TEST_IDS.error));
    const panel = page.locator(tid(TEST_IDS.panel));
    const empty = page.locator(tid(TEST_IDS.empty));
    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });
    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    const catalogEmpty = await empty.isVisible();
    const editSkip = skipReasonForChrome({
      shellPresent: true,
      wantEdit: true,
      catalogEmpty,
    });
    if (editSkip) {
      test.skip(true, editSkip);
    }

    const firstRow = page.locator(catalogRowSelector("design-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="design-tpl-open-0"]').click();

    await expect(page.locator(tid(TEST_IDS.editor))).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(tid(TEST_IDS.editorSource))).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(tid(TEST_IDS.editorName))).toBeVisible();

    await page.locator(tid(TEST_IDS.editorBack)).click();
    await expect(page.locator(tid(TEST_IDS.editor))).toHaveCount(0);
    await expect(page.locator(tid(TEST_IDS.panel))).toBeVisible();
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });

  test("create template when chrome present @smoke @ui @design-spa", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await openDesignLibrary(page);

    const createBtn = page.locator(tid(TEST_IDS.create));
    await expect(createBtn).toBeVisible({ timeout: 30_000 });

    const error = page.locator(tid(TEST_IDS.error));
    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    const name = `qa.consol.tpl.${Date.now()}`;
    await createBtn.click();
    await expect(page.locator(tid(TEST_IDS.createDialog))).toBeVisible({
      timeout: 10_000,
    });
    await page.locator(tid(TEST_IDS.createName)).fill(name);
    await page.locator(tid(TEST_IDS.createSubmit)).click();
    await expect(page.locator(tid(TEST_IDS.createDialog))).toHaveCount(0, {
      timeout: 20_000,
    });
    await expect(page.locator(tid(TEST_IDS.table))).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(tid(TEST_IDS.table))).toContainText(name);
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });

  test("legacy Design list lands on SPA when cutover present @smoke @ui @design-spa", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await page.goto(designLegacyAdminUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });

    const spaShell = page.locator(tid(TEST_IDS.shell));
    const redirectToSpa = await softVisible(spaShell, 15_000);
    const redirectSkip = skipReasonForChrome({
      shellPresent: true,
      wantRedirect: true,
      redirectToSpa,
    });
    if (redirectSkip) {
      // Extra evidence: classic list marker may still be present on main.
      const classic = page.locator(`#${CLASSIC_ASSIGNED_TEMPLATES_ID}`);
      if (await softVisible(classic, 3_000)) {
        test.skip(true, `${SKIP.REDIRECT} Classic #${CLASSIC_ASSIGNED_TEMPLATES_ID} still visible.`);
      }
      test.skip(true, redirectSkip);
    }

    await expect(page.locator(tid(TEST_IDS.shell))).toBeVisible({
      timeout: 15_000,
    });
    await expect(
      page.locator(`#${CLASSIC_ASSIGNED_TEMPLATES_ID}`),
    ).toHaveCount(0);

    await page.goto(designLegacyViewUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator(tid(TEST_IDS.shell))).toBeVisible({
      timeout: 20_000,
    });
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });
});
