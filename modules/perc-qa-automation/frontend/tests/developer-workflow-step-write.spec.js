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
 * Developer → Workflows: create a custom workflow then add a step (#4705).
 *
 * Packaged Default Workflow / Simple Workflow stay protected (403).
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

test.describe("Developer workflow step write (#4705)", () => {
  test("creates a custom workflow then adds a step", async ({ page }) => {
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

    const wfName = uniqueName("Nightly Step");
    await loginAsAdmin(page);
    await page.goto(developerWorkflowsUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="tab-developer-workflows"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-wf-new"]').click();
    await page.locator('[data-testid="developer-wf-create-name"]').fill(wfName);
    await page.locator('[data-testid="developer-wf-create-save"]').click();

    const detail = page.locator('[data-testid="developer-wf-detail"]');
    await expect(detail).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-step-editor"]')).toBeVisible();

    const stepName = "QA Gate";
    await page.locator('[data-testid="developer-wf-step-name"]').fill(stepName);
    await page.locator('[data-testid="developer-wf-step-save"]').click();

    const notice = page.locator('[data-testid="developer-wf-step-notice"]');
    const stepErr = page.locator('[data-testid="developer-wf-step-error"]');
    await expect(notice.or(stepErr).first()).toBeVisible({ timeout: 30_000 });
    if (await stepErr.isVisible()) {
      throw new Error(`step save failed: ${(await stepErr.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-wf-steps-table"]')).toContainText(
      stepName,
    );

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
