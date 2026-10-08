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
 * Developer Slots — set one slot description (#5408 / #1690).
 *
 * The new description shows only after the existing slot PUT succeeds.
 * The update sends the description only, so the name, label, type, and
 * finder stay. A blank description clears it. Cancel does not write.
 * HTTP 400, 403, and 409 leave the previous description.
 *
 *   npm run test:surface -- --path tests/developer-slot-description.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const VALID_FINDER =
  "Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder";
const VALID_RELATIONSHIP = "ActiveAssembly";

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueSlotName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `qa5408${`${a}${b}`.slice(0, 8) || "slot"}`;
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
      /Failed to load resource: the server responded with a status of (400|403|409)/.test(text)
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

function isSlotWrite(request) {
  return request.method() === "PUT" && /\/services\/slots\/[^/?]+$/.test(request.url());
}

function isSlotDelete(request) {
  return request.method() === "DELETE" && /\/services\/slots\/[^/?]+$/.test(request.url());
}

function isSlotList(response) {
  return (
    response.request().method() === "GET" &&
    /\/services\/slots\/?(?:\?|$)/.test(response.url()) &&
    response.status() === 200
  );
}

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.SlotDetail || payload.slotDetail || payload;
}

function descriptionText(page) {
  return page.locator('[data-testid="developer-slot-set-description-text"]');
}

async function openSlots(page) {
  await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-slot-panel"]');
  const empty = page.locator('[data-testid="developer-slot-empty"]');
  const listError = page.locator('[data-testid="developer-slot-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(`Developer slots catalog error: ${(await listError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-slot-new"]')).toBeVisible();
}

async function createSlot(page, name) {
  await page.locator('[data-testid="developer-slot-new"]').click();
  await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-slot-set-description"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-slot-name"]').fill(name);
  await page.locator('[data-testid="developer-slot-label"]').fill(`${name} label`);
  await page.locator('[data-testid="developer-slot-description"]').fill("Folder list");
  await page.locator('[data-testid="developer-slot-type"]').selectOption("INLINE");
  const created = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      /\/services\/slots\/?(?:\?|$)/.test(response.url()),
  );
  await page.locator('[data-testid="developer-slot-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator('[data-testid="developer-slot-detail-notice"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);
  await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");
  await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);
}

async function saveFinder(page) {
  await page.locator('[data-testid="developer-slot-lock"]').click();
  await expect(page.locator('[data-testid="developer-slot-finder"]')).toBeEnabled({
    timeout: 20_000,
  });
  await page.locator('[data-testid="developer-slot-finder"]').fill(VALID_FINDER);
  await page.locator('[data-testid="developer-slot-relationship"]').fill(VALID_RELATIONSHIP);
  const saved = page.waitForResponse((response) => isSlotWrite(response.request()));
  await page.locator('[data-testid="developer-slot-save"]').click();
  const response = await saved;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
  await expect(page.locator('[data-testid="developer-slot-relationship"]')).toHaveValue(
    VALID_RELATIONSHIP,
  );
  await page.locator('[data-testid="developer-slot-unlock"]').click();
  await expect(page.locator('[data-testid="developer-slot-finder"]')).toBeDisabled({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
}

test.describe("Developer set a slot description (#5408)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the description only after success, keeps name label type and finder, and a blank description clears it", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    const deleteUrls = [];
    page.on("request", (request) => {
      if (isSlotWrite(request)) {
        updateUrls.push(request.url());
      }
      if (isSlotDelete(request)) {
        deleteUrls.push(request.url());
      }
    });

    await openSlots(page);
    const name = uniqueSlotName();
    await createSlot(page, name);
    await saveFinder(page);
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");
    await expect(page.locator('[data-testid="developer-slot-label"]')).toHaveValue(`${name} label`);
    await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);

    const writesBeforeDescription = updateUrls.length;
    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    const descriptionInput = page.locator('[data-testid="developer-slot-set-description-input"]');
    await expect(descriptionInput).toHaveValue("Folder list");
    await page.locator('[data-testid="developer-slot-set-description-save"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "the same description must not update the slot").toBe(
      writesBeforeDescription,
    );
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");

    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    await descriptionInput.fill("later");
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");
    await page.locator('[data-testid="developer-slot-set-description-cancel"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the slot").toBe(writesBeforeDescription);
    expect(deleteUrls, "cancel must not delete the slot").toEqual([]);
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
    await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);

    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    await descriptionInput.fill(" note ");
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "Folder list");
    const savedDone = page.waitForResponse((response) => isSlotWrite(response.request()));
    const listRefresh = page.waitForResponse((response) => isSlotList(response));
    await page.locator('[data-testid="developer-slot-set-description-save"]').click();
    const saved = await savedDone;
    await listRefresh;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = detailBody(saved.request().postDataJSON());
    expect(savedBody).toEqual({ description: "note" });
    expect(deleteUrls, "setting a description must not delete the slot").toEqual([]);
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "note");
    await expect(page.locator('[data-testid="developer-slot-description"]')).toHaveValue("note");
    await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);
    await expect(page.locator('[data-testid="developer-slot-label"]')).toHaveValue(`${name} label`);
    await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
    await expect(page.locator('[data-testid="developer-slot-relationship"]')).toHaveValue(
      VALID_RELATIONSHIP,
    );
    await expect(page.locator('[data-testid="developer-slot-set-description-notice"]')).toBeVisible();

    await page.locator('[data-testid="developer-slot-back"]').click();
    await expect(page.locator('[data-testid="developer-slot-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator(
        `[data-testid="developer-slot-catalog-description"][data-slot-name="${name}"]`,
      ),
    ).toHaveAttribute("data-slot-description", "note");
    await page
      .locator(catalogOpenByExactName("developer-slot-open", "data-slot-name", name))
      .click();
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "note");
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
    await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);
    await expect(page.locator('[data-testid="developer-slot-label"]')).toHaveValue(`${name} label`);

    const beforeForced = updateUrls.length;
    let forced = 0;
    const statuses = [400, 403, 409];
    await page.route("**/services/slots/**", async (route) => {
      const request = route.request();
      if (isSlotWrite(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            SlotDetail: {
              name: "Renamed",
              label: "Renamed",
              description: "changed",
              slotType: "REGULAR",
              finderName: "otherFinder",
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isSlotWrite(response.request()));
      const draft = page.locator('[data-testid="developer-slot-set-description-input"]');
      if ((await draft.count()) === 0) {
        await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
      }
      await draft.fill("later");
      await page.locator('[data-testid="developer-slot-set-description-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-slot-set-description-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "note");
      await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);
      await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);
      await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/slots/**");
    await page.locator('[data-testid="developer-slot-set-description-cancel"]').click();

    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    await page.locator('[data-testid="developer-slot-set-description-input"]').fill("   ");
    const clearedDone = page.waitForResponse((response) => isSlotWrite(response.request()));
    await page.locator('[data-testid="developer-slot-set-description-save"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    const clearedBody = detailBody(cleared.request().postDataJSON());
    expect(clearedBody).toEqual({ description: "" });
    await expect(descriptionText(page)).toHaveAttribute("data-slot-description", "");
    await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);
    await expect(page.locator('[data-testid="developer-slot-label"]')).toHaveValue(`${name} label`);
    await expect(page.locator('[data-testid="developer-slot-type-value"]')).toHaveText(/INLINE/i);
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(VALID_FINDER);

    const deleted = page.waitForResponse((response) => isSlotDelete(response.request()));
    await page.locator('[data-testid="developer-slot-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(
      page.locator(catalogOpenByExactName("developer-slot-open", "data-slot-name", name)),
    ).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
