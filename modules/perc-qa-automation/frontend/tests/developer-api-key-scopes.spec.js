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
 * Developer API key scope editor (#6481 / parent #1690).
 *
 * Opens Developer → API Keys, creates one key from the readonly preset,
 * edits its scope set to a custom combination, saves, and proves the
 * assignment survives a full page reload (JSON registry persistence).
 *
 * Entry: spa.jsp?entry=developer&section=api-keys
 * Refs #6481, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerApiKeysUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "api-keys",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer API key scope editor (#6481)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("creates a key, edits scopes to custom, saves, and persists across reload", async ({
    page,
  }) => {
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

    await page.goto(developerApiKeysUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="tab-developer-api-keys"]')).toBeVisible({
      timeout: 15_000,
    });

    const panel = page.locator('[data-testid="developer-api-keys-panel"]');
    const error = page.locator('[data-testid="developer-api-keys-error"]');
    const empty = page.locator('[data-testid="developer-api-keys-empty"]');

    await expect(panel.or(error).or(empty).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      throw new Error(`API keys panel error: ${(await error.innerText()).trim()}`);
    }
    await expect(panel).toBeVisible();

    const keyName = `pw-scopes-${Date.now().toString(36)}`;

    // Create one key from the readonly preset.
    await page.locator('[data-testid="developer-api-keys-new"]').click();
    await page.locator('[data-testid="developer-api-keys-new-name"]').fill(keyName);
    await expect(page.locator('[data-testid="developer-api-keys-new-preset"]')).toBeVisible();
    await page.locator('[data-testid="developer-api-keys-new-save"]').click();

    const row = page.locator(`[data-testid="developer-api-keys-row-${keyName}"]`);
    await expect(row).toBeVisible({ timeout: 30_000 });

    // Open the editor: the readonly preset shows before editing.
    await row.click();
    await expect(page.locator('[data-testid="developer-api-keys-editor"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-api-keys-preset-current"]'),
    ).toContainText("readonly", { timeout: 15_000 });

    // Toggle content:delete (in no preset) → custom combination with unsaved-changes signal.
    await page.locator('[data-testid="developer-api-keys-scope-content:delete"]').check();
    await expect(page.locator('[data-testid="developer-api-keys-dirty"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-api-keys-preset-current"]'),
    ).toContainText("Custom");

    // Applying the author preset re-selects a known preset label.
    await page.locator('[data-testid="developer-api-keys-preset-author"]').click();
    await expect(
      page.locator('[data-testid="developer-api-keys-preset-current"]'),
    ).toContainText("author");

    // Back to a custom set (author + content:delete matches no preset), then save via PUT.
    await page.locator('[data-testid="developer-api-keys-scope-content:delete"]').check();
    await page.locator('[data-testid="developer-api-keys-save"]').click();
    await expect(page.locator('[data-testid="developer-api-keys-saved"]')).toBeVisible({
      timeout: 30_000,
    });

    // Re-enter through the entry URL with a fresh timestamp: the catalog
    // readback comes from the server registry file, not session state.
    // (A bare reload would request the pretty /developer/api-keys route,
    // which the cell serves as 404 — pre-existing platform behavior shared
    // by long-shipped sections like keywords — so re-enter like the
    // keywords specs do instead of asserting on the 404 load.)
    await page.goto(developerApiKeysUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-api-keys"]')).toBeVisible({
      timeout: 15_000,
    });
    const reloadedRow = page.locator(
      `[data-testid="developer-api-keys-row-${keyName}"]`,
    );
    await expect(reloadedRow).toBeVisible({ timeout: 30_000 });
    await expect(
      page.locator(`[data-testid="developer-api-keys-row-preset-${keyName}"]`),
    ).toContainText("Custom");
    await reloadedRow.click();
    await expect(
      page.locator('[data-testid="developer-api-keys-scope-content:delete"]'),
    ).toBeChecked({ timeout: 15_000 });

    expect(pageErrors).toEqual([]);
    expect(consoleErrors).toEqual([]);
  });
});
