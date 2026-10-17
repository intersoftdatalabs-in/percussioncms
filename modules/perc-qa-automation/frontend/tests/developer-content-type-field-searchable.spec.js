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
 * Developer Content Types — mark one field searchable (#5466 / #1690).
 *
 * The searchable flag shows only after the existing content-type field update
 * succeeds. The update sends that one field's searchable flag, so other fields
 * and the type-level search indexing flag stay. Cancel does not write. HTTP
 * 400, 403, and 409 leave the previous flag.
 *
 *   npm run test:surface -- --path tests/developer-content-type-field-searchable.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueFieldName() {
  return `rxsrch${Date.now().toString(36)}`;
}

function attachConsole(page) {
  const consoleErrors = [];
  const pageErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  return { consoleErrors, pageErrors };
}

function assertClean(consoleErrors, pageErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
      /Failed to load resource: the server responded with a status of (400|403|409)/.test(
        text,
      )
    ) {
      return false;
    }
    if (/Failed to load resource: the server responded with a status of 404/.test(text)) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    if (/favicon/i.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isContentTypeDetailWrite(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/contenttypes\/[^/?]+\/?(?:\?|$)/.test(request.url()) &&
    !/\/searchIndexing(?:\?|$)/.test(request.url())
  );
}

function isSearchIndexingWrite(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/contenttypes\/[^/?]+\/searchIndexing(?:\?|$)/.test(request.url())
  );
}

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.ContentTypeDetail || payload.contentTypeDetail || payload;
}

function unwrapFields(payload) {
  const nested = detailBody(payload);
  const fields = nested.fields || nested.Fields || [];
  return Array.isArray(fields) ? fields : [];
}

function unwrapSearchIndexing(payload) {
  if (payload == null || typeof payload !== "object") {
    return undefined;
  }
  const nested =
    payload.ContentTypeSearchIndexing && typeof payload.ContentTypeSearchIndexing === "object"
      ? payload.ContentTypeSearchIndexing
      : payload.contentTypeSearchIndexing &&
          typeof payload.contentTypeSearchIndexing === "object"
        ? payload.contentTypeSearchIndexing
        : payload;
  return nested.searchIndexing;
}

function searchableText(page) {
  return page.locator('[data-testid="developer-ct-field-searchable-text"]');
}

async function openPercPage(page) {
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
    throw new Error("No content types in catalog — fail closed");
  }
  const openBtn = page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]');
  await expect(openBtn, "H2 catalog must include percPage").toBeVisible({ timeout: 20_000 });
  await openBtn.click();
  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-ct-field-searchable"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
}

async function fieldSnapshot(page, typeName) {
  const res = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`,
  );
  expect(res.ok(), `GET content type HTTP ${res.status()}`).toBe(true);
  const payload = await res.json();
  const detail = detailBody(payload);
  const flags = {};
  for (const field of unwrapFields(payload)) {
    if (field && field.name) {
      flags[field.name] = field.searchable === true;
    }
  }
  return {
    label: detail.label == null ? "" : String(detail.label),
    description: detail.description == null ? "" : String(detail.description),
    flags,
  };
}

async function searchIndexingOn(page, typeName) {
  const res = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/searchIndexing`,
  );
  expect(res.ok(), `GET searchIndexing HTTP ${res.status()}`).toBe(true);
  return unwrapSearchIndexing(await res.json()) !== false;
}

async function selectField(page, fieldName) {
  const editor = page.locator('[data-testid="developer-ct-field-searchable-editor"]');
  if ((await editor.count()) === 0) {
    await page.locator('[data-testid="developer-ct-field-searchable-edit"]').click();
  }
  await expect(editor).toBeVisible();
  await page.locator('[data-testid="developer-ct-field-searchable-field"]').selectOption(fieldName);
  await expect(searchableText(page)).toHaveAttribute("data-field-name", fieldName);
}

test.describe("Developer mark one field searchable (#5466)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets and clears searchable on one field and leaves the other fields and search indexing", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    const indexingWrites = [];
    page.on("request", (request) => {
      if (isContentTypeDetailWrite(request)) {
        updateUrls.push(request.url());
      }
      if (isSearchIndexingWrite(request)) {
        indexingWrites.push(request.url());
      }
    });

    await openPercPage(page);
    const typeName = (await page.locator('[data-testid="developer-ct-detail-name"]').innerText()).trim();
    const labelInput = page.locator('[data-testid="developer-ct-label"]');
    const description = page.locator('[data-testid="developer-ct-description"]');
    const indexing = page.locator('[data-testid="developer-ct-search-indexing"]');
    const originalLabel = await labelInput.inputValue();
    const originalDescription = await description.inputValue();
    const before = await fieldSnapshot(page, typeName);
    const indexingBefore = await searchIndexingOn(page, typeName);
    const fieldName = uniqueFieldName();
    const fieldRow = page.locator(
      `[data-testid="developer-ct-field-row"][data-field-name="${fieldName}"]`,
    );

    const editBeforeLock = page.locator('[data-testid="developer-ct-field-searchable-edit"]');
    if (await editBeforeLock.count()) {
      await expect(editBeforeLock).toBeDisabled();
    } else {
      await expect(page.locator('[data-testid="developer-ct-field-searchable-none"]')).toBeVisible();
    }
    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Locked by you/i,
      { timeout: 20_000 },
    );
    expect(await indexing.isChecked()).toBe(indexingBefore);

    try {
      await page.locator('[data-testid="developer-ct-field-add-name"]').fill(fieldName);
      await page.locator('[data-testid="developer-ct-field-add-label"]').fill("Searchable note");
      await page.locator('[data-testid="developer-ct-field-add"]').click();
      await expect(fieldRow).toBeVisible({ timeout: 30_000 });
      await expect(fieldRow).toHaveAttribute("data-field-origin", "local");

      const writesBefore = updateUrls.length;
      await selectField(page, fieldName);
      const optionValues = await page
        .locator('[data-testid="developer-ct-field-searchable-field"] option')
        .evaluateAll((nodes) => nodes.map((node) => node.value));
      expect(optionValues).toContain(fieldName);
      const systemRow = page
        .locator('[data-testid="developer-ct-field-row"][data-field-origin="system"]')
        .first();
      if (await systemRow.count()) {
        const systemName = await systemRow.getAttribute("data-field-name");
        expect(optionValues).toContain(systemName);
      }
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "true");
      await page.locator('[data-testid="developer-ct-field-searchable-save"]').click();
      await expect(page.locator('[data-testid="developer-ct-field-searchable-editor"]')).toHaveCount(0);
      expect(updateUrls.length, "the same searchable flag must not update the type").toBe(
        writesBefore,
      );
      expect(indexingWrites, "searchable save must not write type-level search indexing").toEqual(
        [],
      );

      await selectField(page, fieldName);
      await page.locator('[data-testid="developer-ct-field-searchable-input"]').uncheck();
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "true");
      await page.locator('[data-testid="developer-ct-field-searchable-cancel"]').click();
      expect(updateUrls.length, "cancel must not update the type").toBe(writesBefore);
      await expect(searchableText(page)).toHaveAttribute("data-field-name", fieldName);
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "true");

      await selectField(page, fieldName);
      await page.locator('[data-testid="developer-ct-field-searchable-input"]').uncheck();
      const clearedDone = page.waitForResponse((response) =>
        isContentTypeDetailWrite(response.request()),
      );
      await page.locator('[data-testid="developer-ct-field-searchable-save"]').click();
      const cleared = await clearedDone;
      expect(cleared.status(), await cleared.text()).toBe(200);
      expect(detailBody(cleared.request().postDataJSON())).toEqual({
        fields: [{ name: fieldName, searchable: false }],
      });
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "false");
      await expect(page.locator('[data-testid="developer-ct-field-searchable-notice"]')).toBeVisible();
      await expect(labelInput).toHaveValue(originalLabel);
      await expect(description).toHaveValue(originalDescription);
      expect(await indexing.isChecked()).toBe(indexingBefore);
      expect(await searchIndexingOn(page, typeName)).toBe(indexingBefore);
      expect(indexingWrites).toEqual([]);
      const clearedSnap = await fieldSnapshot(page, typeName);
      expect(clearedSnap.flags[fieldName]).toBe(false);
      expect(clearedSnap.label).toBe(before.label);
      expect(clearedSnap.description).toBe(before.description);
      for (const [name, searchable] of Object.entries(before.flags)) {
        expect(clearedSnap.flags[name], name).toBe(searchable);
      }

      await page.locator('[data-testid="developer-ct-back"]').click();
      await expect(page.locator('[data-testid="developer-ct-panel"]')).toBeVisible({
        timeout: 20_000,
      });
      await page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]').click();
      await expect(page.locator('[data-testid="developer-ct-field-searchable"]')).toBeVisible({
        timeout: 30_000,
      });
      await page.locator('[data-testid="developer-ct-lock"]').click();
      await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
        /Locked by you/i,
        { timeout: 20_000 },
      );
      await selectField(page, fieldName);
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "false");
      await expect(page.locator('[data-testid="developer-ct-field-searchable-input"]')).not.toBeChecked();
      await page.locator('[data-testid="developer-ct-field-searchable-cancel"]').click();
      expect(await page.locator('[data-testid="developer-ct-search-indexing"]').isChecked()).toBe(
        indexingBefore,
      );

      let forced = 0;
      const statuses = [400, 403, 409];
      await page.route("**/services/contenttypes/**", async (route) => {
        const request = route.request();
        if (isContentTypeDetailWrite(request) && forced < statuses.length) {
          const status = statuses[forced];
          forced += 1;
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({
              message: `forced ${status}`,
              ContentTypeDetail: {
                name: "Renamed",
                label: "Renamed",
                description: "changed",
                fields: [],
              },
            }),
          });
          return;
        }
        await route.continue();
      });
      for (const status of statuses) {
        const forcedDone = page.waitForResponse((response) =>
          isContentTypeDetailWrite(response.request()),
        );
        await selectField(page, fieldName);
        await page.locator('[data-testid="developer-ct-field-searchable-input"]').check();
        await page.locator('[data-testid="developer-ct-field-searchable-save"]').click();
        const forcedResponse = await forcedDone;
        expect(forcedResponse.status()).toBe(status);
        await expect(page.locator('[data-testid="developer-ct-field-searchable-error"]')).toContainText(
          `forced ${status}`,
        );
        await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "false");
        await expect(labelInput).toHaveValue(originalLabel);
        await expect(description).toHaveValue(originalDescription);
        expect(await page.locator('[data-testid="developer-ct-search-indexing"]').isChecked()).toBe(
          indexingBefore,
        );
        await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
          /Locked by you/i,
        );
        await page.locator('[data-testid="developer-ct-field-searchable-cancel"]').click();
      }
      await page.unroute("**/services/contenttypes/**");

      await selectField(page, fieldName);
      await page.locator('[data-testid="developer-ct-field-searchable-input"]').check();
      const savedDone = page.waitForResponse((response) =>
        isContentTypeDetailWrite(response.request()),
      );
      await page.locator('[data-testid="developer-ct-field-searchable-save"]').click();
      const saved = await savedDone;
      expect(saved.status(), await saved.text()).toBe(200);
      expect(detailBody(saved.request().postDataJSON())).toEqual({
        fields: [{ name: fieldName, searchable: true }],
      });
      await expect(searchableText(page)).toHaveAttribute("data-field-searchable", "true");
      const marked = await fieldSnapshot(page, typeName);
      expect(marked.flags[fieldName]).toBe(true);
      expect(marked.label).toBe(before.label);
      expect(marked.description).toBe(before.description);
      for (const [name, searchable] of Object.entries(before.flags)) {
        expect(marked.flags[name], name).toBe(searchable);
      }
      expect(await searchIndexingOn(page, typeName)).toBe(indexingBefore);
      expect(indexingWrites).toEqual([]);

      await page.locator(`[data-testid="developer-ct-field-delete-${fieldName}"]`).click();
      await confirmDeveloperCatalogDelete(page);
      await expect(fieldRow).toHaveCount(0, { timeout: 20_000 });
    } finally {
      if ((await page.locator('[data-testid="developer-ct-detail"]').count()) === 0) {
        const openBtn = page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]');
        if (await openBtn.count()) {
          await openBtn.click();
          await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
            timeout: 30_000,
          });
        }
      }
      const row = page.locator(
        `[data-testid="developer-ct-field-row"][data-field-name="${fieldName}"]`,
      );
      if (await row.count()) {
        const status = page.locator('[data-testid="developer-ct-lock-status"]');
        if (!(await status.innerText()).match(/Locked by you/i)) {
          await page.locator('[data-testid="developer-ct-lock"]').click();
          await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
        }
        await page.locator(`[data-testid="developer-ct-field-delete-${fieldName}"]`).click();
        await confirmDeveloperCatalogDelete(page);
        await expect(row).toHaveCount(0, { timeout: 20_000 });
      }
      const unlock = page.locator('[data-testid="developer-ct-unlock"]');
      if (await unlock.count() && (await unlock.isEnabled())) {
        await unlock.click();
        await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
          /Not locked/i,
          { timeout: 20_000 },
        );
      }
    }

    assertClean(consoleErrors, pageErrors);
  });
});
