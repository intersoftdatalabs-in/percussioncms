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
 * Developer Display Formats — set one display format description (#5381 / #1690).
 *
 * The new description shows only after the existing display-format update
 * succeeds. The update sends the description only, so the name, columns, and
 * allowed communities stay. A blank description clears it. Cancel does not
 * write. HTTP 400, 403, and 409 leave the previous description.
 *
 *   npm run test:surface -- --path tests/developer-display-format-description.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueDisplayFormatName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
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

function isDisplayFormatWrite(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/displayformats\/[^/?]+/.test(request.url())
  );
}

function isDisplayFormatDelete(request) {
  return (
    request.method() === "DELETE" &&
    /\/services\/displayformats\/[^/?]+/.test(request.url())
  );
}

function formatBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.DisplayFormat || payload.displayFormat || payload;
}

function descriptionText(page) {
  return page.locator('[data-testid="developer-df-set-description-text"]');
}

async function openDisplayFormats(page) {
  await page.goto(developerDisplayFormatsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-df-panel"]');
  const empty = page.locator('[data-testid="developer-df-empty"]');
  const listError = page.locator('[data-testid="developer-df-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer display formats catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-df-new"]')).toBeVisible();
}

function isDisplayFormatList(response) {
  return (
    response.request().method() === "GET" &&
    /\/services\/displayformats\/?(?:\?|$)/.test(response.url()) &&
    response.status() === 200
  );
}

async function createDisplayFormat(page, formatName) {
  await page.locator('[data-testid="developer-df-new"]').click();
  await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-df-set-description"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-df-name"]').fill(formatName);
  await page.locator('[data-testid="developer-df-label"]').fill(`${formatName} label`);
  await page.locator('[data-testid="developer-df-description"]').fill("Folder list");
  const created = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      /\/services\/displayformats\/?(?:\?|$)/.test(response.url()),
  );
  const listed = page.waitForResponse((response) => isDisplayFormatList(response));
  await page.locator('[data-testid="developer-df-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(201);
  await listed;
  await expect(page.locator('[data-testid="developer-df-editor-notice"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
  await expect(descriptionText(page)).toHaveAttribute("data-df-description", "Folder list");
}

async function saveColumns(page) {
  const saved = page.waitForResponse((response) => isDisplayFormatWrite(response.request()));
  const listed = page.waitForResponse((response) => isDisplayFormatList(response));
  await page.locator('[data-testid="developer-df-columns-save"]').click();
  const response = await saved;
  expect(response.status(), await response.text()).toBe(200);
  await listed;
  await expect(page.locator('[data-testid="developer-df-editor-notice"]')).toBeVisible({
    timeout: 20_000,
  });
}

async function saveCommunities(page) {
  const saved = page.waitForResponse((response) => isDisplayFormatWrite(response.request()));
  const listed = page.waitForResponse((response) => isDisplayFormatList(response));
  await page.locator('[data-testid="developer-df-communities-save"]').click();
  const response = await saved;
  expect(response.status(), await response.text()).toBe(200);
  await listed;
  await expect(page.locator('[data-testid="developer-df-editor-notice"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer set a display format description (#5381)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the description only after success, keeps columns and communities, and a blank description clears it", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    const deleteUrls = [];
    page.on("request", (request) => {
      if (isDisplayFormatWrite(request)) {
        updateUrls.push(request.url());
      }
      if (isDisplayFormatDelete(request)) {
        deleteUrls.push(request.url());
      }
    });

    await openDisplayFormats(page);
    const formatName = uniqueDisplayFormatName("qa5381");
    await createDisplayFormat(page, formatName);

    const sourceSelect = page.locator('[data-testid="developer-df-column-source"]');
    const addSource = await sourceSelect.evaluate((el) => {
      const options = Array.from(el.options || []);
      const hit = options.find((o) => o.value && o.value !== "sys_title" && o.value !== "");
      return hit ? hit.value : "";
    });
    expect(addSource, "column picker should offer a field besides sys_title").toBeTruthy();
    await sourceSelect.selectOption(addSource);
    await page.locator('[data-testid="developer-df-column-add"]').click();
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await saveColumns(page);
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await expect(page.locator('[data-df-column-source="sys_title"]').first()).toBeVisible();

    const allBox = page.locator('[data-testid="developer-df-communities-all"]');
    const communityBox = page.locator('[data-testid="developer-df-community-Default"]');
    await expect(communityBox).toBeVisible({ timeout: 20_000 });
    await allBox.uncheck();
    await communityBox.check();
    await expect(communityBox).toBeChecked();
    await saveCommunities(page);
    await expect(allBox).not.toBeChecked();
    await expect(communityBox).toBeChecked();

    const writesBeforeDescription = updateUrls.length;
    await page.locator('[data-testid="developer-df-set-description-edit"]').click();
    const descriptionInput = page.locator('[data-testid="developer-df-set-description-input"]');
    await expect(descriptionInput).toHaveValue("Folder list");
    await page.locator('[data-testid="developer-df-set-description-save"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "the same description must not update the format").toBe(
      writesBeforeDescription,
    );
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "Folder list");

    await page.locator('[data-testid="developer-df-set-description-edit"]').click();
    await descriptionInput.fill("later");
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "Folder list");
    await page.locator('[data-testid="developer-df-set-description-cancel"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the format").toBe(writesBeforeDescription);
    expect(deleteUrls, "cancel must not delete the format").toEqual([]);
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "Folder list");
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await expect(communityBox).toBeChecked();

    await page.locator('[data-testid="developer-df-set-description-edit"]').click();
    await descriptionInput.fill(" note ");
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "Folder list");
    const savedDone = page.waitForResponse((response) => isDisplayFormatWrite(response.request()));
    const listRefresh = page.waitForResponse((response) => isDisplayFormatList(response));
    await page.locator('[data-testid="developer-df-set-description-save"]').click();
    const saved = await savedDone;
    await listRefresh;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = formatBody(saved.request().postDataJSON());
    expect(savedBody).toEqual({ description: "note" });
    expect(deleteUrls, "setting a description must not delete the format").toEqual([]);
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "note");
    await expect(page.locator('[data-testid="developer-df-description"]')).toHaveValue("note");
    await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
    await expect(page.locator('[data-testid="developer-df-label"]')).toHaveValue(
      `${formatName} label`,
    );
    await expect(page.locator('[data-df-column-source="sys_title"]').first()).toBeVisible();
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await expect(allBox).not.toBeChecked();
    await expect(communityBox).toBeChecked();
    await expect(page.locator('[data-testid="developer-df-set-description-notice"]')).toBeVisible();

    await page.locator('[data-testid="developer-df-back"]').click();
    await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator(
        `[data-testid="developer-df-catalog-description"][data-df-name="${formatName}"]`,
      ),
    ).toHaveAttribute("data-df-description", "note");
    await page
      .locator(catalogOpenByExactName("developer-df-open", "data-df-name", formatName))
      .click();
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "note");
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await expect(communityBox).toBeChecked({ timeout: 20_000 });

    const beforeForced = updateUrls.length;
    let forced = 0;
    const statuses = [400, 403, 409];
    await page.route("**/services/displayformats/**", async (route) => {
      const request = route.request();
      if (isDisplayFormatWrite(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            DisplayFormat: {
              name: "Renamed",
              label: "Renamed",
              description: "changed",
              columns: [],
              allowedCommunities: [],
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isDisplayFormatWrite(response.request()));
      const draft = page.locator('[data-testid="developer-df-set-description-input"]');
      if ((await draft.count()) === 0) {
        await page.locator('[data-testid="developer-df-set-description-edit"]').click();
      }
      await draft.fill("later");
      await page.locator('[data-testid="developer-df-set-description-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-df-set-description-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(descriptionText(page)).toHaveAttribute("data-df-description", "note");
      await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
      await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
      await expect(communityBox).toBeChecked();
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/displayformats/**");
    await page.locator('[data-testid="developer-df-set-description-cancel"]').click();

    await page.locator('[data-testid="developer-df-set-description-edit"]').click();
    await page.locator('[data-testid="developer-df-set-description-input"]').fill("   ");
    const clearedDone = page.waitForResponse((response) => isDisplayFormatWrite(response.request()));
    await page.locator('[data-testid="developer-df-set-description-save"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    const clearedBody = formatBody(cleared.request().postDataJSON());
    expect(clearedBody).toEqual({ description: "" });
    await expect(descriptionText(page)).toHaveAttribute("data-df-description", "");
    await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
    await expect(page.locator('[data-df-column-source="sys_title"]').first()).toBeVisible();
    await expect(page.locator(`[data-df-column-source="${addSource}"]`).first()).toBeVisible();
    await expect(communityBox).toBeChecked();
    await expect(allBox).not.toBeChecked();

    const deleted = page.waitForResponse((response) => isDisplayFormatDelete(response.request()));
    await page.locator('[data-testid="developer-df-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(
      page.locator(catalogOpenByExactName("developer-df-open", "data-df-name", formatName)),
    ).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
