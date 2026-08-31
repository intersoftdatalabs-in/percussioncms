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
 * Developer Templates AS-08 SPA export / create-only import (#4057 / parent #1690).
 *
 * Export downloads assembly-template XML. Import uses POST /services/templates/import
 * (unique name; 400 invalid XML; 409 duplicate). Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-import-export.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openTemplatesCatalog(page) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel).toBeVisible({ timeout: 30_000 });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-tpl-import"]')).toBeVisible();
  return panel;
}

async function openNamedTemplate(page, namePattern) {
  const table = page.locator('[data-testid="developer-tpl-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-tpl-row-"]').filter({
    hasText: namePattern || /\bperc\.page\b/,
  });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-tpl-row", 0));
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }
  const detail = page.locator('[data-testid="developer-tpl-detail"]');
  const detailError = page.locator('[data-testid="developer-tpl-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Template detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  return detail;
}

function rewriteTemplateName(xml, newName) {
  if (!/<assembly-template\b/i.test(xml)) {
    throw new Error("exported XML is missing assembly-template");
  }
  let out = xml;
  if (/<name>[^<]*<\/name>/i.test(out)) {
    out = out.replace(/<name>[^<]*<\/name>/i, `<name>${newName}</name>`);
  } else {
    out = out.replace(
      /<assembly-template\b[^>]*>/i,
      (open) => `${open}<name>${newName}</name>`,
    );
  }
  // Exported binding <id> values collide on create-only import (H2 PK). Server
  // assigns new ids when they are omitted.
  return out.replace(/<id>-?\d+<\/id>/gi, "");
}

test.describe("Developer template import/export (#4057 AS-08)", () => {
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
    await openTemplatesCatalog(page);
    await openNamedTemplate(page, /perc\.page/);

    await expect(page.locator('[data-testid="developer-tpl-export"]')).toBeEnabled({
      timeout: 30_000,
    });
    const exportResponsePromise = page.waitForResponse(
      (r) =>
        /\/templates\/[^/]+\/export(?:\?|$)/i.test(r.url()) &&
        r.request().method() === "GET",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-tpl-export"]').click();
    const exportResponse = await exportResponsePromise;
    expect(
      exportResponse.ok(),
      `export HTTP ${exportResponse.status()} ${exportResponse.url()}`,
    ).toBe(true);
    const xml = await exportResponse.text();
    expect(xml, "export body").toMatch(/assembly-template/i);
    expect(xml).toMatch(/<name>/i);
    const exportError = page.locator('[data-testid="developer-tpl-detail-error"]');
    await expect(exportError).toHaveCount(0);

    await page.locator('[data-testid="developer-tpl-back"]').click();
    await expect(page.locator('[data-testid="developer-tpl-import"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.setInputFiles('[data-testid="developer-tpl-import-file"]', {
      name: "invalid.xml",
      mimeType: "application/xml",
      buffer: Buffer.from("<not-xml"),
    });
    await page.locator('[data-testid="developer-tpl-import-name"]').fill("as08invalidxml");
    await page.locator('[data-testid="developer-tpl-import-submit"]').click();
    const invalidErr = page.locator('[data-testid="developer-tpl-import-error"]');
    await expect(invalidErr).toBeVisible({ timeout: 20_000 });
    await expect(invalidErr).toContainText(/Invalid assembly-template design XML/i);

    await page.setInputFiles('[data-testid="developer-tpl-import-file"]', {
      name: "perc.page.xml",
      mimeType: "application/xml",
      buffer: Buffer.from(xml, "utf8"),
    });
    await page.locator('[data-testid="developer-tpl-import-name"]').fill("perc.page");
    await page.locator('[data-testid="developer-tpl-import-submit"]').click();
    await expect(invalidErr).toBeVisible({ timeout: 30_000 });
    await expect(invalidErr).toContainText(/already exists/i);

    const uniqueName = `as08ie${Date.now()}`;
    const uniqueXml = rewriteTemplateName(xml, uniqueName);
    await page.setInputFiles('[data-testid="developer-tpl-import-file"]', {
      name: `${uniqueName}.xml`,
      mimeType: "application/xml",
      buffer: Buffer.from(uniqueXml, "utf8"),
    });
    await page.locator('[data-testid="developer-tpl-import-name"]').fill(uniqueName);
    await page.locator('[data-testid="developer-tpl-import-submit"]').click();
    const notice = page.locator('[data-testid="developer-tpl-import-notice"]');
    const importErr = page.locator('[data-testid="developer-tpl-import-error"]');
    await expect(notice.or(importErr).first()).toBeVisible({ timeout: 60_000 });
    await expect(notice).toBeVisible({ timeout: 60_000 });
    await expect(notice).toContainText(/imported/i);
    await expect(importErr).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-tpl-table"]')).toContainText(
      uniqueName,
      { timeout: 30_000 },
    );

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
