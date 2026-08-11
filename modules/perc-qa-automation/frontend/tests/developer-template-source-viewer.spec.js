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
 * Developer template detail — source viewer (UI-SRC-01 / #2088) + load binding (#3039).
 *
 * Asserts line-number gutter, copy control, edit/preview chrome, and that
 * non-empty server templateSource is bound into the Source editor (Jackson
 * WRAP_ROOT_VALUE unwrap — #3039).
 *
 * Live run requires a CMS with at least one template. Prefer QA mode:
 *   perc-devctl qa-up → TEST_CMS_URL → npm run test:surface -- --path tests/developer-template-source-viewer.spec.js
 *
 * Entry: spa.jsp?entry=developer&section=templates
 * Refs #2088, #1690, #3039.
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

test.describe("Developer template source viewer (#2088 UI-SRC-01)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("template detail source shows line numbers and copy control", async ({
    page,
  }) => {
    await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });

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
      test.skip(true, "No templates in CMS — cannot open detail source viewer");
      return;
    }

    // Indexed CatalogTable rows (developer-tpl-row-0 …); bare developer-tpl-row
    // never matches WebUI. Prefer first-row open button so product detail/DTO
    // failures (#2189) surface cleanly after this selector harden (#2186).
    const firstRow = page.locator(catalogRowSelector("developer-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(
      openBtn,
      "first template row should expose Open control when selectionKey is set",
    ).toBeVisible({ timeout: 5_000 });

    // Capture detail GET (idOrName path segment) to assert UI binds templateSource (#3039).
    const detailResponsePromise = page.waitForResponse(
      (r) => {
        if (r.request().method() !== "GET" || !r.ok()) return false;
        try {
          const u = new URL(r.url());
          // /services/templates/{idOrName} — not the list endpoint
          return /\/services\/templates\/[^/]+$/.test(u.pathname);
        } catch {
          return false;
        }
      },
      { timeout: 30_000 },
    );

    await openBtn.click();

    let serverSource = "";
    try {
      const detailResp = await detailResponsePromise;
      const raw = await detailResp.json();
      const body =
        raw && typeof raw === "object"
          ? raw.TemplateDetail || raw.templateDetail || raw
          : {};
      serverSource =
        typeof body.templateSource === "string" ? body.templateSource : "";
    } catch {
      // Non-JSON or aborted — fall through; chrome assertions still run
    }

    await expect(
      page.locator('[data-testid="developer-tpl-detail"]'),
    ).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-tpl-source"]'),
    ).toBeVisible();

    // Line-number gutter (at least line 1)
    await expect(
      page.locator('[data-testid="developer-tpl-source-lines"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-tpl-source-ln-1"]'),
    ).toBeVisible();

    // Edit surface by default — must mirror server source when non-empty (#3039)
    const sourceEdit = page.locator(
      '[data-testid="developer-tpl-source-edit"]',
    );
    await expect(sourceEdit).toBeVisible();
    if (serverSource.length > 0) {
      await expect(
        sourceEdit,
        "Source editor must load non-empty templateSource from GET (#3039)",
      ).toHaveValue(serverSource);
    }

    // Copy control present; grant clipboard permissions when possible
    const copyBtn = page.locator('[data-testid="developer-tpl-source-copy"]');
    await expect(copyBtn).toBeVisible();
    try {
      await page
        .context()
        .grantPermissions(["clipboard-read", "clipboard-write"]);
    } catch {
      // Some browsers / contexts ignore grantPermissions
    }
    await copyBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-copy-feedback"]'),
    ).toBeVisible({ timeout: 5_000 });

    // Preview highlight mode
    const modeBtn = page.locator('[data-testid="developer-tpl-source-mode"]');
    await modeBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-preview"]'),
    ).toBeVisible({ timeout: 5_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-source-edit"]'),
    ).toHaveCount(0);

    // Back to edit
    await modeBtn.click();
    await expect(
      page.locator('[data-testid="developer-tpl-source-edit"]'),
    ).toBeVisible();
  });
});
