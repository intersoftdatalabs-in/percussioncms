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
 * Developer → Application Files binary round-trip (#4563 / parent #1690).
 *
 * Creates a real non-UTF-8 fixture via the REST binary endpoint (lock → PUT
 * /binary → unlock), opens it in the SPA and asserts the download/replace view
 * (no text editor, size row, lock-gated replace), downloads the bytes back
 * (Playwright download event), replaces the body with different bytes, and
 * cleans the fixture up.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-application-files-binary.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const APP_NAME = process.env.APPLICATION_FILE_APP || "sys_resources";

/** Non-UTF-8 fixture body (PNG-ish header + NUL) so the server reports binary. */
const FIXTURE_BYTES = Buffer.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x01, 0x02, 0x03,
]);

/** Replacement body, different length and still binary (contains NUL). */
const REPLACEMENT_BYTES = Buffer.from([
  0x50, 0x4b, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
  0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
]);

function developerApplicationFilesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "application-files",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueFileName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4) || "xxxx";
  const b =
    Math.random().toString(36).padEnd(8, "x").replace(/[^a-z0-9]/g, "").slice(2, 6) || "xxxx";
  return `qa-4563-${a}${b}.bin`;
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

async function openBinaryFileRow(page, fileName) {
  const rowOpen = page.locator('[data-testid="developer-appfile-open"]').filter({
    hasText: fileName,
  });
  if (await rowOpen.count()) {
    await rowOpen.first().click();
  } else {
    throw new Error(`Expected fixture row for ${fileName} in the files list`);
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
  // Binary files replace the text editor with the download/replace view.
  await expect(page.locator('[data-testid="developer-appfile-binary"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-appfile-content-editor"]')).toHaveCount(0);
}

test.describe("Developer application files binary (#4563 / SY-05)", () => {
  test("Admin can download and replace a binary application file", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const fileName = uniqueFileName();
    const filePath = `ApplicationFiles/${fileName}`;
    const binaryApi = `${BASE_URL}/Rhythmyx/services/applicationfiles/${encodeURIComponent(APP_NAME)}`;

    await loginAsAdmin(page);

    // Seed a real binary fixture: Admin lock → PUT /binary → unlock.
    const lockResp = await page.request.post(
      `${binaryApi}/lock?path=${encodeURIComponent(filePath)}`,
      { headers: { Accept: "application/json" } },
    );
    expect(lockResp.status(), `fixture lock status=${await lockResp.text()}`).toBe(200);
    const seedResp = await page.request.put(
      `${binaryApi}/binary?path=${encodeURIComponent(filePath)}`,
      {
        data: FIXTURE_BYTES,
        headers: { "Content-Type": "application/octet-stream", Accept: "application/json" },
      },
    );
    expect(seedResp.status(), `fixture PUT /binary status=${await seedResp.text()}`).toBe(200);
    const seedBody = await seedResp.json();
    const seeded = seedBody.ApplicationFile || seedBody;
    expect(seeded.binary, `seed should be flagged binary=${JSON.stringify(seedBody)}`).toBe(true);
    const unlockResp = await page.request.post(
      `${binaryApi}/unlock?path=${encodeURIComponent(filePath)}`,
      { headers: { Accept: "application/json" } },
    );
    // Unlock maps to 204 ("Delete or unlock success") per the SY-05 contract.
    expect([200, 204], `fixture unlock status=${unlockResp.status()}`).toContain(
      unlockResp.status(),
    );

    await openApplicationFilesCatalog(page);
    await openAppByName(page, APP_NAME);
    await openBinaryFileRow(page, fileName);

    // Binary view: no text editor, size row visible, download + replace present.
    const detail = page.locator('[data-testid="developer-appfile-detail"]');
    await expect(detail).toContainText(String(FIXTURE_BYTES.length));
    const downloadBtn = page.locator('[data-testid="developer-appfile-download"]');
    await expect(downloadBtn).toBeEnabled();
    const replaceInput = page.locator('[data-testid="developer-appfile-replace"]');
    await expect(replaceInput).toBeDisabled(); // lock required before replace

    // Download the raw bytes back through the browser download event.
    const downloadPromise = page.waitForEvent("download", { timeout: 15_000 });
    await downloadBtn.click();
    const download = await downloadPromise;
    const suggested = download.suggestedFilename() || "";
    expect(suggested).toBe(fileName);
    expect(suggested).not.toMatch(/[\\/]/);

    // Lock, replace with a different binary body, assert refresh.
    const lockBtn = page.locator('[data-testid="developer-appfile-lock"]');
    await expect(lockBtn).toBeEnabled();
    const lockWait = page.waitForResponse(
      (r) =>
        /\/services\/applicationfiles\/[^/?#]+\/lock/i.test(r.url()) &&
        r.request().method() === "POST",
      { timeout: 30_000 },
    );
    await lockBtn.click();
    expect((await lockWait).status(), `lock status`).toBe(200);
    await expect(
      page.locator('[data-testid="developer-appfile-lock-status"]'),
    ).toContainText(/Locked/i);
    await expect(replaceInput).toBeEnabled();

    const putWait = page.waitForResponse(
      (r) =>
        /\/services\/applicationfiles\/[^/?#]+\/binary/i.test(r.url()) &&
        r.request().method() === "PUT",
      { timeout: 30_000 },
    );
    await replaceInput.setInputFiles({
      name: fileName,
      mimeType: "application/octet-stream",
      buffer: REPLACEMENT_BYTES,
    });
    const putResp = await putWait;
    expect(putResp.status(), `replace PUT /binary status=${await putResp.text()}`).toBe(200);
    await expect(page.locator('[data-testid="developer-appfile-editor-notice"]')).toContainText(
      /Application file replaced/i,
    );
    await expect(detail).toContainText(String(REPLACEMENT_BYTES.length));

    // Cleanup: unlock, delete the fixture so the cell stays clean.
    const unlockBtn = page.locator('[data-testid="developer-appfile-unlock"]');
    await expect(unlockBtn).toBeEnabled();
    await unlockBtn.click();
    await expect(page.locator('[data-testid="developer-appfile-lock-status"]')).toContainText(
      /Unlocked/i,
    );
    const delResp = await page.request.delete(
      `${binaryApi}/content?path=${encodeURIComponent(filePath)}`,
      { headers: { Accept: "application/json" } },
    );
    expect([200, 204], `fixture DELETE status=${delResp.status()}`).toContain(delResp.status());

    assertConsoleClean(pageErrors, consoleErrors);
  });
});