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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer → Server Configs design lock (#4623 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up && perc-devctl qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-server-configs-lock.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const CONFIG_KEY = process.env.SERVER_CONFIG_WRITE_KEY || "NAV_CONFIG";

function developerServerConfigsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "server-configs",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

test.describe("Developer server configs locking (#4623)", () => {
  test("Admin can lock, see held status, and unlock without silent overwrite", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await page.goto(developerServerConfigsUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    const cfgTab = page.getByRole("tab", { name: /Server Configs/i });
    if (await cfgTab.isVisible()) {
      await cfgTab.click();
    }
    await expect(page.locator('[data-testid="developer-cfg-table"]')).toBeVisible({
      timeout: 30_000,
    });
    const open = page.locator(
      catalogOpenByExactName("developer-cfg-open", "data-cfg-name", CONFIG_KEY),
    );
    await open.click();
    await expect(page.locator('[data-testid="developer-cfg-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-cfg-detail-loading"]')).toHaveCount(0, {
      timeout: 30_000,
    });
    await expect(page.locator('[data-testid="developer-cfg-content-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-cfg-lock"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-cfg-save"]')).toBeDisabled();

    const lockWait = page.waitForResponse(
      (r) =>
        /\/services\/serverconfigs\/[^/?#]+\/lock$/i.test(r.url()) &&
        r.request().method() === "POST",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-cfg-lock"]').click();
    const lockResp = await lockWait;
    expect(lockResp.status(), `POST lock ${await lockResp.text()}`).toBe(200);
    await expect(page.locator('[data-testid="developer-cfg-lock-status"]')).toContainText(
      /Locked by you/i,
    );

    const unlockWait = page.waitForResponse(
      (r) =>
        /\/services\/serverconfigs\/[^/?#]+\/unlock$/i.test(r.url()) &&
        r.request().method() === "POST",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-cfg-unlock"]').click();
    const unlockResp = await unlockWait;
    expect(unlockResp.status(), `POST unlock`).toBe(204);
    await expect(page.locator('[data-testid="developer-cfg-lock-status"]')).toContainText(
      /Lock the configuration before saving/i,
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
