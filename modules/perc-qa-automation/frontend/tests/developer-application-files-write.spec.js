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
 * Developer → Application Files SY-05 browse/save (#4289 / parent #1690).
 *
 * Opens an XML application (sys_resources by default), picks the first editable
 * CMS/resource file (or APPLICATION_FILE_PATH), appends a unique marker, saves,
 * asserts notice + refreshed content, re-opens after back, then restores.
 *
 * Consumes REST tip #4288 / PR #4292.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-application-files-write.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, loginAsEditor, BASE_URL } = require("./helpers/auth");

const APP_NAME = process.env.APPLICATION_FILE_APP || "sys_resources";
const FILE_PATH = process.env.APPLICATION_FILE_PATH || "";

function developerApplicationFilesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "application-files",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function escapeRegex(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** @returns {{ value: string, escaped: string }} */
function uniqueMarker() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4) || "xxxx";
  const b =
    Math.random().toString(36).padEnd(8, "x").replace(/[^a-z0-9]/g, "").slice(2, 6) || "xxxx";
  const value = `/* QA-4289-${a}${b} */`;
  return { value, escaped: escapeRegex(value) };
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

async function openApplicationFilesCatalog(page) {
  await page.goto(developerApplicationFilesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-appfile-panel"]');
  const empty = page.locator('[data-testid="developer-appfile-apps-empty"]');
  const listError = page.locator('[data-testid="developer-appfile-apps-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer application files apps error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer application files: no applications on H2");
  }
  await expect(page.locator('[data-testid="developer-appfile-apps-table"]')).toBeVisible();
}

async function openAppByName(page, appName) {
  const open = page.locator(
    `[data-testid="developer-appfile-app-open"][aria-label="Open ${appName}"]`,
  );
  if (await open.count()) {
    await expect(open.first()).toBeVisible({ timeout: 20_000 });
    await open.first().click();
  } else {
    // Fall back to first listed application when preferred name is absent.
    const first = page.locator('[data-testid="developer-appfile-app-open"]').first();
    await expect(first).toBeVisible({ timeout: 20_000 });
    await first.click();
  }
  const files = page.locator('[data-testid="developer-appfile-files"]');
  const filesEmpty = page.locator('[data-testid="developer-appfile-files-empty"]');
  const filesError = page.locator('[data-testid="developer-appfile-files-error"]');
  await expect(files.or(filesEmpty).or(filesError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await filesError.isVisible()) {
    throw new Error(
      `Application files list error: ${(await filesError.innerText()).trim()}`,
    );
  }
  if (await filesEmpty.isVisible()) {
    throw new Error(`No CMS/resource files under application (wanted ${appName})`);
  }
  await expect(page.locator('[data-testid="developer-appfile-table"]')).toBeVisible();
}

async function openFileRow(page, preferredPath) {
  if (preferredPath) {
    const rowOpen = page.locator('[data-testid="developer-appfile-open"]').filter({
      hasText: preferredPath.split("/").pop() || preferredPath,
    });
    if (await rowOpen.count()) {
      await rowOpen.first().click();
    } else {
      await page.locator('[data-testid="developer-appfile-open"]').first().click();
    }
  } else {
    await page.locator('[data-testid="developer-appfile-open"]').first().click();
  }
  await expect(page.locator('[data-testid="developer-appfile-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-appfile-detail-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });
  const detailError = page.locator('[data-testid="developer-appfile-detail-error"]');
  if (await detailError.isVisible()) {
    throw new Error(`File detail error: ${(await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-appfile-content-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-appfile-save"]')).toBeVisible();
}

async function saveFileContent(page, content) {
  const editor = page.locator('[data-testid="developer-appfile-content-editor"]');
  await editor.fill(content);
  const saveBtn = page.locator('[data-testid="developer-appfile-save"]');
  await expect(saveBtn).toBeEnabled();
  const putWait = page.waitForResponse(
    (r) =>
      /\/services\/applicationfiles\/[^/?#]+\/content/i.test(r.url()) &&
      r.request().method() === "PUT",
    { timeout: 30_000 },
  );
  await saveBtn.click();
  const putResp = await putWait;
  const putStatus = putResp.status();
  const putText = await putResp.text();
  const notice = page.locator('[data-testid="developer-appfile-editor-notice"]');
  const err = page.locator('[data-testid="developer-appfile-detail-error"]');
  await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
  if (await err.isVisible()) {
    throw new Error(
      `Save failed: ${(await err.innerText()).trim()} | PUT ${putStatus} resp=${putText}`,
    );
  }
  expect(putStatus, `PUT status resp=${putText}`).toBe(200);
  await expect(notice).toContainText(/Application file saved/i);
}

test.describe("Developer application files write (#4289 / SY-05)", () => {
  test("Admin can browse app files, save, and see refresh", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const marker = uniqueMarker();

    await loginAsAdmin(page);
    await openApplicationFilesCatalog(page);
    await openAppByName(page, APP_NAME);
    await openFileRow(page, FILE_PATH);

    const editor = page.locator('[data-testid="developer-appfile-content-editor"]');
    const original = await editor.inputValue();
    const withMarker = original.endsWith("\n")
      ? `${original}${marker.value}\n`
      : `${original}\n${marker.value}\n`;

    await saveFileContent(page, withMarker);
    await expect(editor).toHaveValue(new RegExp(marker.escaped));

    await page.locator('[data-testid="developer-appfile-back"]').click();
    await expect(page.locator('[data-testid="developer-appfile-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await openFileRow(page, FILE_PATH);
    await expect(editor).toHaveValue(new RegExp(marker.escaped));

    // Restore original body so the H2 cell stays clean for later runs.
    await saveFileContent(page, original);
    await expect(editor).toHaveValue(original);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("non-admin Save stays disabled; unsafe path PUT is not 200", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsEditor(page);
    await openApplicationFilesCatalog(page);
    // Editor may still browse; Save must stay disabled when detail opens.
    const appOpen = page.locator('[data-testid="developer-appfile-app-open"]').first();
    if (await appOpen.isVisible({ timeout: 15_000 }).catch(() => false)) {
      await openAppByName(page, APP_NAME);
      await openFileRow(page, FILE_PATH);
      const saveBtn = page.locator('[data-testid="developer-appfile-save"]');
      await expect(saveBtn).toBeVisible();
      await expect(saveBtn).toBeDisabled();
      await expect(page.locator('[data-testid="developer-appfile-admin-hint"]')).toBeVisible();
    }

    // Path-safety: traversal must never succeed (Admin or Editor session cookie).
    const putResp = await page.request.put(
      `${BASE_URL}/Rhythmyx/services/applicationfiles/${encodeURIComponent(APP_NAME)}/content?path=${encodeURIComponent("../escape.css")}`,
      {
        data: { ApplicationFile: { content: "x" } },
        headers: { "Content-Type": "application/json", Accept: "application/json" },
      },
    );
    expect([403, 404], `unsafe PUT status=${putResp.status()}`).toContain(putResp.status());

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
