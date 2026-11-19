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
 * Developer Slots — set-label / set-description editor keyboard path
 * (#6713 / parent #1690; mirrors display-format #6630 / #5404).
 *
 * The label and description inputs autofocus when the editor opens, and
 * Escape cancels the edit without writing (same as Cancel): no PUT fires
 * and the stored value is kept.
 *
 * Focus return (#6733): when an editor closes (Escape, Cancel, or a
 * successful save) the input unmounts, so focus must return to the
 * invoking edit button instead of falling back to <body>.
 *
 *   npm run test:surface -- --path tests/developer-slot-set-editors-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

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
  return `qa6713${`${a}${b}`.slice(0, 8) || "slot"}`;
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

function isSlotWrite(request, name) {
  return (
    request.method() === "PUT" &&
    request.url().includes(`/services/slots/${encodeURIComponent(name)}`)
  );
}

function isSlotDelete(request, name) {
  return (
    request.method() === "DELETE" &&
    request.url().includes(`/services/slots/${encodeURIComponent(name)}`)
  );
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
}

async function deleteSlot(page, name) {
  const deleted = page.waitForResponse((response) => isSlotDelete(response.request(), name));
  await page.locator('[data-testid="developer-slot-delete"]').click();
  await confirmDeveloperCatalogDelete(page);
  const deleteResponse = await deleted;
  expect(deleteResponse.status()).toBe(204);
  await expect(
    page.locator(catalogOpenByExactName("developer-slot-open", "data-slot-name", name)),
  ).toHaveCount(0);
}

test.describe("Developer slot set-label/set-description keyboard path (#6713)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("label input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openSlots(page);
    const name = uniqueSlotName();
    await createSlot(page, name);

    const writes = [];
    page.on("request", (request) => {
      if (isSlotWrite(request, name)) {
        writes.push(request.url());
      }
    });

    await page.locator('[data-testid="developer-slot-set-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-slot-set-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await labelInput.press("Escape");
    await expect(labelInput).toHaveCount(0);
    expect(writes, "Escape must not update the slot").toEqual([]);
    await expect(page.locator('[data-testid="developer-slot-set-label-text"]')).toHaveAttribute(
      "data-slot-label",
      `${name} label`,
    );
    await page.screenshot({ path: `test-results/live-proof-6713-slot-label-keyboard.png` });

    await deleteSlot(page, name);
    assertClean(consoleErrors, pageErrors);
  });

  test("description input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openSlots(page);
    const name = uniqueSlotName();
    await createSlot(page, name);

    const writes = [];
    page.on("request", (request) => {
      if (isSlotWrite(request, name)) {
        writes.push(request.url());
      }
    });

    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    const descriptionInput = page.locator('[data-testid="developer-slot-set-description-input"]');
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(descriptionInput).toHaveCount(0);
    expect(writes, "Escape must not update the slot").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-slot-set-description-text"]'),
    ).toHaveAttribute("data-slot-description", "Folder list");
    await page.screenshot({ path: `test-results/live-proof-6713-slot-description-keyboard.png` });

    await deleteSlot(page, name);
    assertClean(consoleErrors, pageErrors);
  });
});

test.describe("Developer slot set-label/set-description focus return (#6733)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("label editor returns focus to the edit button after Escape, Cancel, and Save", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openSlots(page);
    const name = uniqueSlotName();
    await createSlot(page, name);

    // Escape closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-slot-set-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-slot-set-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await labelInput.press("Escape");
    await expect(labelInput).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-slot-set-label-edit"]')).toBeFocused();
    await expect(
      page.locator('[data-testid="developer-slot-set-label-text"]'),
    ).toHaveAttribute("data-slot-label", `${name} label`);

    // Cancel closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-slot-set-label-edit"]').click();
    await expect(page.locator('[data-testid="developer-slot-set-label-input"]')).toBeFocused();
    await page.locator('[data-testid="developer-slot-set-label-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-slot-set-label-cancel"]').click();
    await expect(page.locator('[data-testid="developer-slot-set-label-input"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-slot-set-label-edit"]')).toBeFocused();
    await expect(
      page.locator('[data-testid="developer-slot-set-label-text"]'),
    ).toHaveAttribute("data-slot-label", `${name} label`);

    // Save writes, then returns focus to the edit button.
    await page.locator('[data-testid="developer-slot-set-label-edit"]').click();
    await page.locator('[data-testid="developer-slot-set-label-input"]').fill("focus-note");
    const saved = page.waitForResponse(
      (response) =>
        response.request().method() === "PUT" &&
        response.url().includes(`/services/slots/${encodeURIComponent(name)}`),
    );
    await page.locator('[data-testid="developer-slot-set-label-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(page.locator('[data-testid="developer-slot-set-label-input"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-slot-set-label-edit"]')).toBeFocused();
    await expect(page.locator('[data-testid="developer-slot-set-label-text"]')).toHaveAttribute(
      "data-slot-label",
      "focus-note",
    );
    await expect(page.locator('[data-testid="developer-slot-set-label-notice"]')).toHaveAttribute(
      "role",
      "status",
    );
    await page.screenshot({ path: `test-results/live-proof-6733-slot-label-focus-return.png` });

    await deleteSlot(page, name);
    assertClean(consoleErrors, pageErrors);
  });

  test("description editor returns focus to the edit button after Escape, Cancel, and Save", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openSlots(page);
    const name = uniqueSlotName();
    await createSlot(page, name);

    // Escape closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    const descriptionInput = page.locator('[data-testid="developer-slot-set-description-input"]');
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(descriptionInput).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-slot-set-description-edit"]'),
    ).toBeFocused();
    await expect(
      page.locator('[data-testid="developer-slot-set-description-text"]'),
    ).toHaveAttribute("data-slot-description", "Folder list");

    // Cancel closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    await expect(
      page.locator('[data-testid="developer-slot-set-description-input"]'),
    ).toBeFocused();
    await page.locator('[data-testid="developer-slot-set-description-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-slot-set-description-cancel"]').click();
    await expect(
      page.locator('[data-testid="developer-slot-set-description-input"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-slot-set-description-edit"]'),
    ).toBeFocused();
    await expect(
      page.locator('[data-testid="developer-slot-set-description-text"]'),
    ).toHaveAttribute("data-slot-description", "Folder list");

    // Save writes, then returns focus to the edit button.
    await page.locator('[data-testid="developer-slot-set-description-edit"]').click();
    await page.locator('[data-testid="developer-slot-set-description-input"]').fill("focus-note");
    const saved = page.waitForResponse(
      (response) =>
        response.request().method() === "PUT" &&
        response.url().includes(`/services/slots/${encodeURIComponent(name)}`),
    );
    await page.locator('[data-testid="developer-slot-set-description-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(
      page.locator('[data-testid="developer-slot-set-description-input"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-slot-set-description-edit"]'),
    ).toBeFocused();
    await expect(
      page.locator('[data-testid="developer-slot-set-description-text"]'),
    ).toHaveAttribute("data-slot-description", "focus-note");
    await expect(
      page.locator('[data-testid="developer-slot-set-description-notice"]'),
    ).toHaveAttribute("role", "status");
    await page.screenshot({
      path: `test-results/live-proof-6733-slot-description-focus-return.png`,
    });

    await deleteSlot(page, name);
    assertClean(consoleErrors, pageErrors);
  });
});
