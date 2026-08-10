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
 * Design SPA source + JEXL bindings editor (#2809 / parent #2631).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/design-template-source-editor.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=design&section=templates
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function designTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "design",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Design template source + JEXL editor (#2809)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("open template, edit source, save happy path @smoke @ui", async ({
    page,
  }) => {
    await page.goto(designTemplatesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="perc-design-shell"]')).toBeVisible({
      timeout: 20_000,
    });

    const error = page.locator('[data-testid="design-tpl-error"]');
    const panel = page.locator('[data-testid="design-tpl-panel"]');
    const empty = page.locator('[data-testid="design-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in catalog — cannot exercise editor save");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("design-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = page.locator('[data-testid="design-tpl-open-0"]');
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="design-tpl-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    const source = page.locator('[data-testid="design-tpl-editor-source-edit"]');
    await expect(source).toBeVisible({ timeout: 20_000 });

    const original = await source.inputValue();
    const marker = `<!-- design-2809-${Date.now()} -->`;
    // Append a harmless comment-like marker; restore on success path below.
    const nextSource = original.endsWith("\n")
      ? `${original}${marker}\n`
      : `${original}\n${marker}\n`;
    await source.fill(nextSource);

    const saveBtn = page.locator('[data-testid="design-tpl-editor-save"]');
    await expect(saveBtn).toBeEnabled({ timeout: 5_000 });
    await saveBtn.click();

    await expect(
      page.locator('[data-testid="design-tpl-editor-notice"]'),
    ).toBeVisible({ timeout: 30_000 });
    await expect(
      page.locator('[data-testid="design-tpl-editor-error"]'),
    ).toHaveCount(0);

    // Best-effort restore so repeated QA runs do not accumulate markers.
    await source.fill(original);
    if (await saveBtn.isEnabled()) {
      await saveBtn.click();
      await expect(
        page.locator('[data-testid="design-tpl-editor-notice"]'),
      ).toBeVisible({ timeout: 30_000 });
    }

    await page.locator('[data-testid="design-tpl-editor-back"]').click();
    await expect(page.locator('[data-testid="design-tpl-panel"]')).toBeVisible({
      timeout: 15_000,
    });
  });
});
