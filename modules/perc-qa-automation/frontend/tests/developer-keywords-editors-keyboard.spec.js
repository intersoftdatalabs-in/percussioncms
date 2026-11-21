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
 * Developer Keywords — inline editor keyboard path (#6907 / parent #1690;
 * mirrors slot #6713 / #6733, display-format #6630 / #5404, roles #6905).
 *
 * Each inline editor input (keyword label/description/sequence and per-choice
 * label/value/description/sequence) autofocuses when the editor opens, and
 * Escape cancels the edit without writing (same as Cancel): no PUT fires and
 * the stored value is kept.
 *
 * Focus return (#6733 pattern): when an editor closes (Escape, Cancel, or a
 * successful save) the input unmounts, so focus returns to the invoking edit
 * button instead of falling back to <body>. Save confirmations carry
 * `role="status"` (slot/display-format parity).
 *
 *   npm run test:surface -- --path tests/developer-keywords-editors-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerKeywordsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "keywords",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueKeywordLabel() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `qa6907${`${a}${b}`.slice(0, 8) || "kw"}`;
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
    if (/Failed to load resource: the server responded with a status of (400|403|409)/.test(text)) {
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

function isKeywordCreate(request) {
  return (
    request.method() === "POST" &&
    /\/services\/keywords\/?(?:\?|$)/.test(request.url())
  );
}

function isKeywordUpdate(request) {
  return (
    request.method() === "PUT" && /\/services\/keywords\/[^/?]+/.test(request.url())
  );
}

function isKeywordDelete(request) {
  return (
    request.method() === "DELETE" && /\/services\/keywords\/[^/?]+/.test(request.url())
  );
}

function keywordOpenButton(page, label) {
  return page.locator(`[data-testid="developer-kw-open"][aria-label="Open ${label}"]`);
}

function keywordLabelText(page) {
  return page.locator('[data-testid="developer-kw-keyword-label-text"]');
}

function keywordDescriptionText(page) {
  return page.locator('[data-testid="developer-kw-keyword-description-text"]');
}

async function openKeywords(page) {
  await page.goto(developerKeywordsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="tab-developer-keywords"]')).toBeVisible({
    timeout: 20_000,
  });
  const error = page.locator('[data-testid="developer-kw-error"]');
  const panel = page.locator('[data-testid="developer-kw-panel"]');
  const empty = page.locator('[data-testid="developer-kw-empty"]');
  await expect(panel.or(empty).or(error).first()).toBeVisible({ timeout: 30_000 });
  if (await error.isVisible()) {
    throw new Error(`Keywords catalog error: ${(await error.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-kw-new"]')).toBeVisible();
}

async function createKeyword(page, label) {
  await page.locator('[data-testid="developer-kw-new"]').click();
  await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-kw-keyword-label"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-kw-label"]').fill(label);
  await page.locator('[data-testid="developer-kw-description"]').fill("Item priority");
  await page.locator('[data-testid="developer-kw-sequence"]').fill("4");
  await page.locator('[data-testid="developer-kw-choices"]').fill("High|high|1\nLow|low|2");
  const created = page.waitForResponse((response) => isKeywordCreate(response.request()));
  await page.locator('[data-testid="developer-kw-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator('[data-testid="developer-kw-panel"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(keywordOpenButton(page, label)).toBeVisible();
}

async function openKeyword(page, label) {
  const readDone = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      /\/services\/keywords\/[^/?]+/.test(response.url()) &&
      response.status() === 200,
  );
  await keywordOpenButton(page, label).click();
  await readDone;
  await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
  await expect(page.locator('[data-testid="developer-kw-keyword-label-edit"]')).toBeEnabled();
}

async function deleteKeyword(page, label) {
  const deleted = page.waitForResponse((response) => isKeywordDelete(response.request()));
  await page.locator('[data-testid="developer-kw-delete"]').click();
  await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
  const deleteResponse = await deleted;
  expect(deleteResponse.status()).toBe(204);
  await expect(keywordOpenButton(page, label)).toHaveCount(0);
}

test.describe("Developer keywords inline editor keyboard path (#6907)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("keyword label input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openKeywords(page);
    const label = uniqueKeywordLabel();
    await createKeyword(page, label);
    await openKeyword(page, label);

    const updates = [];
    page.on("request", (request) => {
      if (isKeywordUpdate(request)) {
        updates.push(request.url());
      }
    });

    await page.locator('[data-testid="developer-kw-keyword-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-kw-keyword-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await labelInput.press("Escape");
    await expect(labelInput).toHaveCount(0);
    expect(updates, "Escape must not update the keyword").toEqual([]);
    await expect(keywordLabelText(page)).toHaveAttribute("data-keyword-label", label);
    await expect(page.locator('[data-testid="developer-kw-keyword-label-edit"]')).toBeFocused();
    await page.screenshot({ path: `test-results/live-proof-6907-kw-label-keyboard.png` });

    await deleteKeyword(page, label);
    assertClean(consoleErrors, pageErrors);
  });

  test("keyword label Cancel and Save return focus; save notice is status", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openKeywords(page);
    const label = uniqueKeywordLabel();
    const renamed = `${label}x`;
    await createKeyword(page, label);
    await openKeyword(page, label);

    // Cancel closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-kw-keyword-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-kw-keyword-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await page.locator('[data-testid="developer-kw-keyword-label-cancel"]').click();
    await expect(labelInput).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-keyword-label-edit"]')).toBeFocused();
    await expect(keywordLabelText(page)).toHaveAttribute("data-keyword-label", label);

    // Save writes, then returns focus to the edit button with a status notice.
    await page.locator('[data-testid="developer-kw-keyword-label-edit"]').click();
    await page.locator('[data-testid="developer-kw-keyword-label-input"]').fill(renamed);
    const saved = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-keyword-label-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(
      page.locator('[data-testid="developer-kw-keyword-label-input"]'),
    ).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-keyword-label-edit"]')).toBeFocused();
    await expect(keywordLabelText(page)).toHaveAttribute("data-keyword-label", renamed);
    await expect(page.locator('[data-testid="developer-kw-keyword-label-notice"]')).toHaveAttribute(
      "role",
      "status",
    );
    await page.screenshot({ path: `test-results/live-proof-6907-kw-label-focus-return.png` });

    await deleteKeyword(page, renamed);
    assertClean(consoleErrors, pageErrors);
  });

  test("keyword description and choice label Escape-cancel without writing (regression)", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openKeywords(page);
    const label = uniqueKeywordLabel();
    await createKeyword(page, label);
    await openKeyword(page, label);

    const updates = [];
    page.on("request", (request) => {
      if (isKeywordUpdate(request)) {
        updates.push(request.url());
      }
    });

    // Keyword description editor: autofocus, Escape cancels, focus returns.
    await page.locator('[data-testid="developer-kw-keyword-description-edit"]').click();
    const descriptionInput = page.locator(
      '[data-testid="developer-kw-keyword-description-input"]',
    );
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(descriptionInput).toHaveCount(0);
    expect(updates, "Escape must not update the keyword").toEqual([]);
    await expect(keywordDescriptionText(page)).toHaveAttribute(
      "data-keyword-description",
      "Item priority",
    );
    await expect(
      page.locator('[data-testid="developer-kw-keyword-description-edit"]'),
    ).toBeFocused();

    // Per-choice label editor: autofocus, Escape cancels, focus returns to the row button.
    await page
      .locator('[data-testid="developer-kw-choice-label-edit"][data-choice-label="Low"]')
      .click();
    const choiceInput = page.locator('[data-testid="developer-kw-choice-label-input"]');
    await expect(choiceInput).toBeFocused();
    await choiceInput.fill("not-saved");
    await choiceInput.press("Escape");
    await expect(choiceInput).toHaveCount(0);
    expect(updates, "Escape must not update the choice").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toHaveAttribute("data-choice-value", "low");
    await expect(
      page.locator('[data-testid="developer-kw-choice-label-edit"][data-choice-label="Low"]'),
    ).toBeFocused();
    await page.screenshot({ path: `test-results/live-proof-6907-kw-description-choice.png` });

    await deleteKeyword(page, label);
    assertClean(consoleErrors, pageErrors);
  });
});
