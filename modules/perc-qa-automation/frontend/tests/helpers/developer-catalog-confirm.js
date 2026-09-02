/**
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
 * In-app Developer catalog delete confirm (#4122 / 508).
 * Native window.confirm is not used on these surfaces.
 */

"use strict";

const { expect } = require("@playwright/test");

/**
 * @param {import("@playwright/test").Page} page
 */
async function confirmDeveloperCatalogDelete(page) {
  const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
  await expect(dialog).toBeVisible({ timeout: 10_000 });
  await expect(dialog).toHaveAttribute("role", "dialog");
  await expect(dialog).toHaveAttribute("aria-modal", "true");
  await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function cancelDeveloperCatalogDelete(page) {
  const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
  await expect(dialog).toBeVisible({ timeout: 10_000 });
  await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
  await expect(dialog).toHaveCount(0);
}

module.exports = {
  confirmDeveloperCatalogDelete,
  cancelDeveloperCatalogDelete,
};
