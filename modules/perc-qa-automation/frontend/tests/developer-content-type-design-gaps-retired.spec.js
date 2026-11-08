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
 * Developer content type retired design gaps (#5741 / parent #1690).
 *
 * The icon strategy picker, type-level search indexing toggle, and
 * import/export wizard all ship in the Developer content-type editor, so the
 * gaps list must no longer carry the retired {@code CT_SEARCH_INDEXING} /
 * {@code CT_IMPORT_EXPORT} codes (or the stale "not exposed" / "remains
 * Workbench" claims). {@code CT_ICON_STRATEGY} stays visible with corrected
 * text because binary icon upload remains Workbench-only.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-design-gaps-retired.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
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
  expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
}

async function openFirstContentTypeDetail(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const empty = page.locator('[data-testid="developer-ct-empty"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "No content types in catalog — fail closed (H2 QA must include sample types)",
    );
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const targetRow = page.locator(catalogRowSelector("developer-ct-row", 0));
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Content type detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  return detail;
}

test.describe("Developer content type retired design gaps (#5741)", () => {
  test("retired gap codes stay out of the gaps list; icon remainder stays honest", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openFirstContentTypeDetail(page);

    const gaps = page.locator('[data-testid="developer-ct-gaps"]');
    await expect(gaps).toBeVisible({ timeout: 30_000 });
    const text = (await gaps.innerText()) || "";

    expect(
      text,
      `retired search-indexing gap still shown: ${text.slice(0, 500)}`,
    ).not.toMatch(/SPA Properties checkbox is not exposed/i);
    expect(
      text,
      `retired import/export gap still shown: ${text.slice(0, 500)}`,
    ).not.toMatch(/export slice is installed|SPA import wizard is not exposed/i);
    expect(
      text,
      `retired icon picker gap still shown: ${text.slice(0, 500)}`,
    ).not.toMatch(/SPA picker remain Workbench/i);

    await expect(gaps.locator('[data-gap-code="CT_SEARCH_INDEXING"]')).toHaveCount(0);
    await expect(gaps.locator('[data-gap-code="CT_IMPORT_EXPORT"]')).toHaveCount(0);
    await expect(
      gaps.locator('[data-gap-code="CT_SHARED_FIELD_INCLUSION"]'),
    ).toHaveCount(0);

    const iconGap = gaps.locator('[data-gap-code="CT_ICON_STRATEGY"]');
    await expect(iconGap).toHaveCount(1);
    await expect(iconGap).toContainText(/binary upload remains Workbench/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
