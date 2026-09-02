/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
 * Developer Content Type SPA local field create/delete (CD-03 / #4045 / parent #1690).
 *
 * Admin locks a type, POSTs a uniquely named local field, GET catalog includes
 * it, DELETEs it, catalog omits it, then unlocks. Unlocked add/delete stay
 * blocked; duplicate name is 409. Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-local-fields.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const {
  catalogRowSelector,
  catalogRowsSelector,
} = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueFieldName() {
  return `rxcd03${Date.now().toString(36)}`;
}

async function openContentTypeDetail(page, namePattern) {
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
  const named = namePattern
    ? table.locator('[data-testid^="developer-ct-row-"]').filter({
        hasText: namePattern,
      })
    : table.locator('[data-testid^="developer-ct-row-"]');
  const rowCount = await named.count();
  const fallbackCount = await page.locator(catalogRowsSelector("developer-ct-row")).count();
  const total = rowCount > 0 ? rowCount : fallbackCount;
  if (total < 1) {
    throw new Error("No content type catalog rows");
  }

  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  let lastError = "";
  for (let i = 0; i < total; i++) {
    const targetRow =
      rowCount > 0 ? named.nth(i) : page.locator(catalogRowSelector("developer-ct-row", i));
    await expect(targetRow).toBeVisible();
    const openBtn = targetRow.locator('button[aria-label^="Open "]');
    if (await openBtn.count()) {
      await openBtn.click();
    } else {
      await targetRow.click();
    }
    await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible({
      timeout: 30_000,
    });
    if (await detailError.isVisible()) {
      lastError = (await detailError.innerText()).trim();
      const back = page.locator('[data-testid="developer-ct-back"]');
      if (await back.count()) {
        await back.click();
        await expect(table).toBeVisible({ timeout: 15_000 });
      }
      continue;
    }
    try {
      await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
        timeout: 30_000,
      });
    } catch (err) {
      throw new Error(
        `Content type lock not enabled: ${lastError || String(err && err.message ? err.message : err)}`,
      );
    }
    break;
  }
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${lastError || (await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-ct-fields"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
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

test.describe("Developer content type local field create/delete (CD-03 / #4045)", () => {
  test("unlocked add/delete stay blocked; 409 lock is not stolen", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const addName = page.locator('[data-testid="developer-ct-field-add-name"]');
    const addBtn = page.locator('[data-testid="developer-ct-field-add"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-fields"]')).toBeVisible();
    await expect(addName).toBeDisabled();
    await expect(addBtn).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    await page.route("**/services/contenttypes/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(addName).toBeDisabled();
    await expect(addBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock enables add/delete; SPA POSTs wrapped local field and surfaces duplicate 409", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const addName = page.locator('[data-testid="developer-ct-field-add-name"]');
    const addLabel = page.locator('[data-testid="developer-ct-field-add-label"]');
    const addBtn = page.locator('[data-testid="developer-ct-field-add"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const detailError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(page.locator('[data-testid="developer-ct-detail-name"]')).toBeVisible({
      timeout: 30_000,
    });
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name").toBeGreaterThan(0);

    const fieldName = uniqueFieldName();
    const fieldRow = page.locator(
      `[data-testid="developer-ct-field-row"][data-field-name="${fieldName}"]`,
    );
    const deleteBtn = page.locator(`[data-testid="developer-ct-field-delete-${fieldName}"]`);

    let fieldPosts = 0;
    await page.route("**/services/contenttypes/**", async (route) => {
      const req = route.request();
      const url = req.url();
      const isFieldPost =
        req.method() === "POST" &&
        /\/contenttypes\/[^/?]+\/fields(?:\?|$)/.test(url) &&
        !/\/fields\/include/.test(url);
      const isFieldDelete =
        req.method() === "DELETE" && /\/contenttypes\/[^/?]+\/fields\//.test(url);
      if (isFieldPost) {
        const postBody = req.postData() || "";
        expect(postBody, "POST wrap").toContain("ContentTypeField");
        expect(postBody, "POST origin local").toMatch(/"fieldType"\s*:\s*"local"/);
        fieldPosts += 1;
        if (fieldPosts === 1) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ContentTypeDetail: {
                name: typeName,
                fields: [
                  {
                    name: fieldName,
                    label: "CD-03 local",
                    fieldType: "local",
                    dataType: "text",
                    control: "sys_EditBox",
                  },
                ],
              },
            }),
          });
          return;
        }
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: `Field already exists: ${fieldName}` }),
        });
        return;
      }
      if (isFieldDelete) {
        await route.fulfill({ status: 204 });
        return;
      }
      await route.continue();
    });

    await expect(addName).toBeDisabled();
    await expect(lockBtn).toBeEnabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await addName.scrollIntoViewIfNeeded();
    await expect(addName).toBeEnabled();
    await expect(unlockBtn).toBeEnabled();

    try {
      await addName.fill(fieldName);
      await addLabel.fill("CD-03 local");
      await expect(addBtn).toBeEnabled();
      await addBtn.click();
      await expect(notice).toContainText(/Local field added/i, { timeout: 20_000 });
      await expect(fieldRow).toBeVisible({ timeout: 15_000 });
      expect(fieldPosts).toBe(1);

      await addName.fill(fieldName);
      await addBtn.click();
      await expect(detailError).toBeVisible({ timeout: 20_000 });
      await expect(detailError).toContainText(/already exists|Could not add local field/i);
      await expect(status).toHaveText(/Locked by you/i);
      expect(fieldPosts).toBe(2);

      await expect(deleteBtn).toBeEnabled();
      await deleteBtn.click();
      await confirmDeveloperCatalogDelete(page);
      await expect(notice).toContainText(/Local field deleted/i, { timeout: 20_000 });
      await expect(fieldRow).toHaveCount(0);
    } finally {
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
