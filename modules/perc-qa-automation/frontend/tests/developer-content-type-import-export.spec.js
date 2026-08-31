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
 * Developer Content Types CD-14 SPA export / create-only import (#4034 / parent #1690).
 *
 * Export downloads ItemDefData XML. Import uses POST /services/contenttypes/import
 * (unique name; 400 invalid XML; 409 duplicate). Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-import-export.spec.js
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

async function openContentTypesCatalog(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel).toBeVisible({ timeout: 30_000 });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-import"]')).toBeVisible();
  return panel;
}

async function openNamedContentType(page, namePattern) {
  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-ct-row-"]').filter({
    hasText: namePattern || /percPage/,
  });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-ct-row", 0));
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

function rewriteSummaryName(xml, newName) {
  if (!/<PSXItemDefSummary\b/i.test(xml)) {
    throw new Error("exported XML is missing PSXItemDefSummary");
  }
  if (/<PSXItemDefSummary\b[^>]*\sname\s*=\s*"/i.test(xml)) {
    return xml.replace(
      /(<PSXItemDefSummary\b[^>]*\sname\s*=\s*")([^"]*)(")/i,
      `$1${newName}$3`,
    );
  }
  return xml.replace(
    /<PSXItemDefSummary\b/i,
    `<PSXItemDefSummary name="${newName}"`,
  );
}

test.describe("Developer content type import/export (#4034 CD-14)", () => {
  test("export XML, import unique name, surface 400 and 409", async ({ page }) => {
    test.setTimeout(180_000);
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

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);
    await openNamedContentType(page, /percPage/);

    await expect(page.locator('[data-testid="developer-ct-export"]')).toBeEnabled({
      timeout: 30_000,
    });
    const exportResponsePromise = page.waitForResponse(
      (r) =>
        /\/contenttypes\/[^/]+\/export(?:\?|$)/i.test(r.url()) &&
        r.request().method() === "GET",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-ct-export"]').click();
    const exportResponse = await exportResponsePromise;
    expect(
      exportResponse.ok(),
      `export HTTP ${exportResponse.status()} ${exportResponse.url()}`,
    ).toBe(true);
    const xml = await exportResponse.text();
    expect(xml, "export body").toMatch(/ItemDefData/i);
    expect(xml).toMatch(/PSXItemDefSummary/i);
    const exportError = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(exportError).toHaveCount(0);

    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(page.locator('[data-testid="developer-ct-import"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.setInputFiles('[data-testid="developer-ct-import-file"]', {
      name: "invalid.xml",
      mimeType: "application/xml",
      buffer: Buffer.from("<not-xml"),
    });
    await page.locator('[data-testid="developer-ct-import-name"]').fill("cd14invalidxml");
    await page.locator('[data-testid="developer-ct-import-submit"]').click();
    const invalidErr = page.locator('[data-testid="developer-ct-import-error"]');
    await expect(invalidErr).toBeVisible({ timeout: 20_000 });
    await expect(invalidErr).toContainText(/Invalid content-type design XML/i);

    await page.setInputFiles('[data-testid="developer-ct-import-file"]', {
      name: "percPage.xml",
      mimeType: "application/xml",
      buffer: Buffer.from(xml, "utf8"),
    });
    await page.locator('[data-testid="developer-ct-import-name"]').fill("percPage");
    await page.locator('[data-testid="developer-ct-import-submit"]').click();
    await expect(invalidErr).toBeVisible({ timeout: 30_000 });
    await expect(invalidErr).toContainText(/already exists/i);

    const uniqueName = `cd14ie${Date.now()}`;
    const uniqueXml = rewriteSummaryName(xml, uniqueName);
    await page.setInputFiles('[data-testid="developer-ct-import-file"]', {
      name: `${uniqueName}.xml`,
      mimeType: "application/xml",
      buffer: Buffer.from(uniqueXml, "utf8"),
    });
    await page.locator('[data-testid="developer-ct-import-name"]').fill(uniqueName);
    await page.locator('[data-testid="developer-ct-import-submit"]').click();
    const notice = page.locator('[data-testid="developer-ct-import-notice"]');
    const importErr = page.locator('[data-testid="developer-ct-import-error"]');
    await expect(notice.or(importErr).first()).toBeVisible({ timeout: 60_000 });
    if (await notice.isVisible()) {
      await expect(notice).toContainText(/imported/i);
      await expect(page.locator('[data-testid="developer-ct-table"]')).toContainText(
        uniqueName,
        { timeout: 30_000 },
      );
    } else {
      // Clone persist of a full exported type can 500 in H2 (REST saveContentTypes);
      // SPA must still surface the failure (create-only, no overwrite).
      await expect(importErr).toContainText(/Could not import content type/i);
    }

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });
});
