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
 * Developer → Workflows: set the system default (#4843).
 *
 * Creates two workflows, marks the second default, and checks the catalog
 * shows Yes on only that row.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueName(prefix) {
  return `${prefix} ${Date.now().toString(36)}`;
}

async function createWorkflow(page, wfName) {
  await page.locator('[data-testid="developer-wf-new"]').click();
  await page.locator('[data-testid="developer-wf-create-name"]').fill(wfName);
  await page.locator('[data-testid="developer-wf-create-save"]').click();
  await expect(page.locator('[data-testid="developer-wf-detail"]')).toBeVisible({
    timeout: 30_000,
  });
  const createErr = page.locator('[data-testid="developer-wf-create-error"]');
  if (await createErr.isVisible().catch(() => false)) {
    throw new Error(`create failed: ${(await createErr.innerText()).trim()}`);
  }
}

test.describe("Developer set default workflow (#4843)", () => {
  test("second workflow takes the default and the first clears", async ({ page }) => {
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

    const first = uniqueName("Nightly Def A");
    const second = uniqueName("Nightly Def B");
    await loginAsAdmin(page);
    await page.goto(developerWorkflowsUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-workflows"]')).toBeVisible({
      timeout: 20_000,
    });

    await createWorkflow(page, first);
    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-new"]')).toBeVisible();

    await createWorkflow(page, second);
    const setDefault = page.locator('[data-testid="developer-wf-set-default"]');
    const already = page.locator('[data-testid="developer-wf-default-current"]');
    await expect(setDefault.or(already).first()).toBeVisible({ timeout: 20_000 });
    if (await already.isVisible()) {
      throw new Error("new workflow was already the system default");
    }
    await setDefault.click();
    const notice = page.locator('[data-testid="developer-wf-default-notice"]');
    const defErr = page.locator('[data-testid="developer-wf-default-error"]');
    await expect(notice.or(defErr).first()).toBeVisible({ timeout: 30_000 });
    if (await defErr.isVisible()) {
      throw new Error(`set default failed: ${(await defErr.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-wf-default-flag"]')).toContainText("Yes");

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible();
    const row = (name) =>
      page.locator('[data-testid^="developer-wf-row-"]').filter({
        has: page.locator(`[data-wf-name="${name}"]`),
      });
    await expect(row(second)).toContainText("Yes");
    await expect(row(first)).not.toContainText("Yes");

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
