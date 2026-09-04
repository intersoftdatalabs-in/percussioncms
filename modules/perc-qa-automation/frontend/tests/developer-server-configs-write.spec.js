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
 * Developer → Configurations SY-02 allow-listed save (#4277 / parent #1690).
 *
 * Opens an allow-listed server config (TIDY_CONFIG), appends a unique marker,
 * saves, asserts notice + refreshed content, re-opens after back/list, then
 * restores the original body.
 *
 * Consumes REST/SPA tips #4275 / #4276 (#4280 / #4281).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-server-configs-write.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

/**
 * Prefer NAV_CONFIG — Navigation.properties ships under rxconfig/Server on H2.
 * TIDY_CONFIG / SERVER_PAGE_TAGS live under rxconfig/XSpLit which is often absent
 * on fresh QA cells (save would 500 creating the file without the parent dir).
 */
const CONFIG_KEY = process.env.SERVER_CONFIG_WRITE_KEY || "NAV_CONFIG";

function developerServerConfigsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "server-configs",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueMarker() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `# QA-4277-${a}${b || "x"}`;
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

async function openServerConfigsCatalog(page) {
  await page.goto(developerServerConfigsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-cfg-panel"]');
  const empty = page.locator('[data-testid="developer-cfg-empty"]');
  const listError = page.locator('[data-testid="developer-cfg-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer server configs catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer server configs catalog empty on H2 (expected allow-listed rows)");
  }
  await expect(page.locator('[data-testid="developer-cfg-table"]')).toBeVisible();
}

async function openConfigByKey(page, key) {
  const open = page.locator(
    catalogOpenByExactName("developer-cfg-open", "data-cfg-name", key),
  );
  await expect(open, `expected catalog open for ${key}`).toBeVisible({
    timeout: 20_000,
  });
  await open.click();
  await expect(page.locator('[data-testid="developer-cfg-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-cfg-detail-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });
  const detailError = page.locator('[data-testid="developer-cfg-detail-error"]');
  if (await detailError.isVisible()) {
    throw new Error(`Config detail error: ${(await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-cfg-content-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-cfg-save"]')).toBeVisible();
}

async function saveConfigContent(page, content) {
  const editor = page.locator('[data-testid="developer-cfg-content-editor"]');
  await editor.fill(content);
  const saveBtn = page.locator('[data-testid="developer-cfg-save"]');
  await expect(saveBtn).toBeEnabled();
  const putWait = page.waitForResponse(
    (r) =>
      /\/services\/serverconfigs\/[^/?#]+/i.test(r.url()) &&
      r.request().method() === "PUT",
    { timeout: 30_000 },
  );
  await saveBtn.click();
  const putResp = await putWait;
  const putStatus = putResp.status();
  const putText = await putResp.text();
  const notice = page.locator('[data-testid="developer-cfg-editor-notice"]');
  const err = page.locator('[data-testid="developer-cfg-detail-error"]');
  await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
  if (await err.isVisible()) {
    throw new Error(
      `Save failed: ${(await err.innerText()).trim()} | PUT ${putStatus} resp=${putText}`,
    );
  }
  expect(putStatus, `PUT status resp=${putText}`).toBe(200);
  await expect(notice).toContainText(/Configuration saved/i);
}

test.describe("Developer server configs write (#4277 / SY-02)", () => {
  test("Admin can edit allow-listed config, save, and see refresh", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const marker = uniqueMarker();

    await loginAsAdmin(page);
    await openServerConfigsCatalog(page);
    await openConfigByKey(page, CONFIG_KEY);

    const editor = page.locator('[data-testid="developer-cfg-content-editor"]');
    const original = await editor.inputValue();
    const withMarker = original.endsWith("\n")
      ? `${original}${marker}\n`
      : `${original}\n${marker}\n`;

    await saveConfigContent(page, withMarker);
    await expect(editor).toHaveValue(new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));

    await page.locator('[data-testid="developer-cfg-back"]').click();
    await expect(page.locator('[data-testid="developer-cfg-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await openConfigByKey(page, CONFIG_KEY);
    await expect(editor).toHaveValue(new RegExp(marker.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));

    // Restore original body so the H2 cell stays clean for later runs.
    await saveConfigContent(page, original);
    await expect(editor).toHaveValue(original);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
