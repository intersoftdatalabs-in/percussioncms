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
 * Developer → Application Files folder create/delete/rename (#4544 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-application-files-folders.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, loginAsEditor, BASE_URL } = require("./helpers/auth");

const APP_NAME = process.env.APPLICATION_FILE_APP || "sys_resources";

function developerApplicationFilesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "application-files",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueFolderName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4) || "xxxx";
  const b =
    Math.random().toString(36).padEnd(8, "x").replace(/[^a-z0-9]/g, "").slice(2, 6) || "xxxx";
  return `qa-4544-${a}${b}`;
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
}

test.describe("Developer application files folders (#4544 / SY-05)", () => {
  test("Admin can create, rename, and delete a folder under an application", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const folderName = uniqueFolderName();
    const folderPath = `ApplicationFiles/${folderName}`;
    const renamedPath = `ApplicationFiles/${folderName}-renamed`;

    await loginAsAdmin(page);
    await openApplicationFilesCatalog(page);
    await openAppByName(page, APP_NAME);

    await expect(page.locator('[data-testid="developer-appfile-create-folder"]')).toBeVisible();
    await page.locator('[data-testid="developer-appfile-folder-path"]').fill(folderPath);
    const postWait = page.waitForResponse(
      (r) =>
        /\/services\/applicationfiles\/[^/?#]+\/folders/i.test(r.url()) &&
        r.request().method() === "POST",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-appfile-create-folder"]').click();
    const postResp = await postWait;
    expect(postResp.status(), `POST folders status`).toBe(200);
    await expect(page.locator('[data-testid="developer-appfile-files-notice"]')).toContainText(
      /Folder created/i,
    );
    const renameFolder = page.getByLabel(`Rename ${folderPath}`);
    await expect(renameFolder).toBeVisible({ timeout: 20_000 });

    await page.getByLabel(`Rename ${folderPath}`).click();
    await page.locator('[data-testid="developer-appfile-rename-path"]').fill(renamedPath);
    const moveWait = page.waitForResponse(
      (r) =>
        /\/services\/applicationfiles\/[^/?#]+\/move/i.test(r.url()) &&
        r.request().method() === "POST",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-appfile-rename-save"]').click();
    const moveResp = await moveWait;
    expect(moveResp.status(), `POST move status`).toBe(200);
    await expect(page.locator('[data-testid="developer-appfile-files-notice"]')).toContainText(
      /Path renamed/i,
    );
    await expect(page.getByLabel(`Delete ${renamedPath}`)).toBeVisible({ timeout: 20_000 });

    await page.getByLabel(`Delete ${renamedPath}`).click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible();
    const delWait = page.waitForResponse(
      (r) =>
        /\/services\/applicationfiles\/[^/?#]+\/content/i.test(r.url()) &&
        r.request().method() === "DELETE",
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const delResp = await delWait;
    expect(delResp.status(), `DELETE status`).toBe(204);
    await expect(page.locator('[data-testid="developer-appfile-files-notice"]')).toContainText(
      /Path deleted/i,
    );
    await expect(page.getByLabel(`Delete ${renamedPath}`)).toHaveCount(0);

    const unsafeCreate = await page.request.post(
      `${BASE_URL}/Rhythmyx/services/applicationfiles/${encodeURIComponent(APP_NAME)}/folders?path=${encodeURIComponent("../escape")}`,
      { headers: { Accept: "application/json" } },
    );
    expect(unsafeCreate.status(), await unsafeCreate.text()).toBe(400);

    const missingDelete = await page.request.delete(
      `${BASE_URL}/Rhythmyx/services/applicationfiles/${encodeURIComponent(APP_NAME)}/content?path=${encodeURIComponent("ApplicationFiles/no-such-qa-4544-file.txt")}`,
      { headers: { Accept: "application/json" } },
    );
    expect(missingDelete.status(), await missingDelete.text()).toBe(404);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("non-admin folder create is 403", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsEditor(page);
    const editorCreate = await page.request.post(
      `${BASE_URL}/Rhythmyx/services/applicationfiles/${encodeURIComponent(APP_NAME)}/folders?path=${encodeURIComponent("ApplicationFiles/qa-editor")}`,
      { headers: { Accept: "application/json" } },
    );
    // Editor may be filtered before the adaptor (404 HTML) or receive Admin 403.
    expect([403, 404], `editor POST status=${editorCreate.status()}`).toContain(
      editorCreate.status(),
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
