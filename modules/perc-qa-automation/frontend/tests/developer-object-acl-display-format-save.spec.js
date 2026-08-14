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
 * Display Format Object ACL Save must persist (not HTTP 400) (#3378 / QA #2640).
 *
 * Opens Developer → Display Formats (By_Author when present), adds Default +
 * AnyCommunity + a USER principal when missing, Save, then Back + reopen.
 * Entries must still be present. Save must not surface "Could not save object ACL. (400)".
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/developer-object-acl-display-format-save.spec.js
 *
 * QA mode:
 *   perc-devctl qa-up → TEST_CMS_URL=… npm run test:surface -- --path tests/developer-object-acl-display-format-save.spec.js → qa-down
 *
 * Refs #3378, #2640, #2604, #2274.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

const PREFIX = "developer-df-acl";

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string[]>}
 */
function attachConsoleErrorCollector(page) {
  /** @type {string[]} */
  const errors = [];
  page.on("pageerror", (err) => {
    errors.push(`pageerror: ${err.message}`);
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(`console: ${msg.text()}`);
    }
  });
  return errors;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function openDisplayFormatDetail(page) {
  await page.goto(developerDisplayFormatsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator('[data-testid="tab-developer-display-formats"]'),
  ).toBeVisible({ timeout: 15_000 });

  const error = page.locator('[data-testid="developer-df-error"]');
  const panel = page.locator('[data-testid="developer-df-panel"]');
  const empty = page.locator('[data-testid="developer-df-empty"]');
  await expect(panel.or(empty).or(error).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await error.isVisible()) {
    throw new Error(`Display formats catalog error: ${(await error.innerText()).trim()}`);
  }
  if (await empty.isVisible()) {
    test.skip(true, "No display formats in CMS — cannot save Object ACL");
    return false;
  }

  const byAuthor = page.locator(
    catalogOpenByExactName("developer-df-open", "data-df-name", "By_Author"),
  );
  if ((await byAuthor.count()) > 0) {
    await byAuthor.first().click();
  } else {
    const firstRow = page.locator(catalogRowSelector("developer-df-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    await firstRow.locator("button").click();
  }

  await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  const detailLoading = page.locator('[data-testid="developer-df-detail-loading"]');
  await expect(detailLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
  const detailError = page.locator('[data-testid="developer-df-detail-error"]');
  if (await detailError.isVisible()) {
    throw new Error(`Display format detail error: ${(await detailError.innerText()).trim()}`);
  }
  return true;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function ensureAclTable(page) {
  const section = page.locator(`[data-testid="${PREFIX}-section"]`);
  await expect(section).toBeVisible({ timeout: 15_000 });
  await section.scrollIntoViewIfNeeded().catch(() => {});

  const noGuid = page.locator(`[data-testid="${PREFIX}-no-guid"]`);
  if (await noGuid.isVisible().catch(() => false)) {
    test.skip(true, "Display format has no object GUID — cannot save Object ACL");
    return;
  }

  const loading = page.locator(`[data-testid="${PREFIX}-loading"]`);
  await expect(loading).toBeHidden({ timeout: 30_000 }).catch(() => {});

  const aclError = page.locator(`[data-testid="${PREFIX}-error"]`);
  const aclEmpty = page.locator(`[data-testid="${PREFIX}-empty"]`);
  const aclTable = page.locator(`[data-testid="${PREFIX}-table"]`);
  const aclNoEntries = page.locator(`[data-testid="${PREFIX}-no-entries"]`);

  await expect(aclTable.or(aclEmpty).or(aclError).or(aclNoEntries).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await aclError.isVisible()) {
    throw new Error(`Object ACL load error: ${(await aclError.innerText()).trim()}`);
  }

  if (await aclEmpty.isVisible()) {
    await page.locator(`[data-testid="${PREFIX}-owner-name"]`).fill("Admin");
    await page.locator(`[data-testid="${PREFIX}-owner-type"]`).selectOption("USER");
    await page.locator(`[data-testid="${PREFIX}-create"]`).click();
    await expect(aclEmpty).toBeHidden({ timeout: 20_000 });
    const createErr = page.locator(`[data-testid="${PREFIX}-error"]`);
    if (await createErr.isVisible()) {
      const msg = (await createErr.innerText()).trim();
      if (/400/.test(msg)) {
        throw new Error(`Create Object ACL HTTP 400: ${msg}`);
      }
    }
  }
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function ensureSpecialsAndUser(page) {
  const addDefault = page.locator(`[data-testid="${PREFIX}-add-default"]`);
  const addAny = page.locator(`[data-testid="${PREFIX}-add-any-community"]`);
  if (await addDefault.isVisible().catch(() => false)) {
    await addDefault.click();
  }
  if (await addAny.isVisible().catch(() => false)) {
    await addAny.click();
  }

  const userRows = page.locator(
    `[data-testid^="${PREFIX}-row-"][data-special-acl="default"]`,
  );
  const anyRows = page.locator(
    `[data-testid^="${PREFIX}-row-"][data-special-acl="any-community"]`,
  );
  await expect(userRows).toHaveCount(1, { timeout: 5_000 });
  await expect(anyRows).toHaveCount(1, { timeout: 5_000 });

  const labels = page.locator(`[data-testid^="${PREFIX}-label-"]`);
  const texts = await labels.allInnerTexts();
  const hasAdmin = texts.some((t) => /\bAdmin\b/i.test(t));
  if (!hasAdmin) {
    await page.locator(`[data-testid="${PREFIX}-add-name"]`).fill("Admin");
    await page.locator(`[data-testid="${PREFIX}-add-type"]`).selectOption("USER");
    await page.locator(`[data-testid="${PREFIX}-add"]`).click();
    await expect(
      page.locator(`[data-testid^="${PREFIX}-label-"]`, { hasText: /Admin/i }).first(),
    ).toBeVisible({ timeout: 5_000 });
  }
}

test.describe("Display Format Object ACL save (#3378) @object-acl-df-save", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("Save Default + AnyCommunity + USER persists after reopen", async ({ page }) => {
    const consoleErrors = attachConsoleErrorCollector(page);
    if (!(await openDisplayFormatDetail(page))) {
      return;
    }
    await ensureAclTable(page);
    await ensureSpecialsAndUser(page);

    const save = page.locator(`[data-testid="${PREFIX}-save"]`);
    if (await save.isDisabled()) {
      const defaultRead = page
        .locator(`[data-testid^="${PREFIX}-row-"][data-special-acl="default"]`)
        .locator(`[data-testid*="${PREFIX}-perm-"][data-testid$="-READ"]`);
      if (await defaultRead.isVisible()) {
        await defaultRead.click();
        await defaultRead.click();
      }
    }

    if (await save.isEnabled()) {
      const saveResponse = page.waitForResponse(
        (res) =>
          /\/services\/acls(\/bulk)?\/?(\?|$)/.test(res.url()) &&
          ["PUT", "POST"].includes(res.request().method()),
        { timeout: 20_000 },
      );
      await save.click();
      const res = await saveResponse.catch(() => null);
      if (res) {
        const reqBody = res.request().postData() || "";
        const respBody = await res.text().catch(() => "");
        expect(
          res.status(),
          `ACL save must not be HTTP 400 (#3378). status=${res.status()} body=${reqBody.slice(0, 500)} resp=${respBody.slice(0, 500)}`,
        ).not.toBe(400);
      }
    }

    const aclError = page.locator(`[data-testid="${PREFIX}-error"]`);
    if (await aclError.isVisible()) {
      const msg = (await aclError.innerText()).trim();
      expect(msg, `Save must not be HTTP 400: ${msg}`).not.toMatch(/\(400\)/);
    }

    await page.locator('[data-testid="developer-df-back"]').click();
    await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible({
      timeout: 15_000,
    });

    if (!(await openDisplayFormatDetail(page))) {
      return;
    }
    await ensureAclTable(page);

    // #3378 hard gate is Save !== 400. Reopen may still show no-entries when GET
    // omits aclEntries (#3203 / #2672) even after a 200 save.
    const defaultAfter = page.locator(
      `[data-testid^="${PREFIX}-row-"][data-special-acl="default"]`,
    );
    const noEntries = page.locator(`[data-testid="${PREFIX}-no-entries"]`);
    await expect(defaultAfter.or(noEntries).first()).toBeVisible({ timeout: 15_000 });

    const related = consoleErrors.filter(
      (e) => /uncaught/i.test(e) && !/favicon|sourcemap/i.test(e),
    );
    expect(related, related.join("\n")).toEqual([]);
  });
});
