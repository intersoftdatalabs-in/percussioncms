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
 * Developer Content Types copy (#4893 / parent #1690).
 *
 *   npm run test:surface -- --path tests/developer-content-type-copy.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openContentTypesCatalog(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="developer-ct-panel"]')).toBeVisible({
    timeout: 30_000,
  });
  const listError = page.locator('[data-testid="developer-ct-error"]');
  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
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

async function deleteContentType(page, typeName) {
  return page.evaluate(async (name) => {
    const headers = { Accept: "application/json" };
    const token = document.querySelector('meta[name="_csrf"]');
    const headerName = document.querySelector('meta[name="_csrf_header"]');
    if (token && headerName) {
      headers[headerName.getAttribute("content")] = token.getAttribute("content");
    }
    const key = encodeURIComponent(name);
    await fetch(`/Rhythmyx/services/contenttypes/${key}/lock`, {
      method: "POST",
      credentials: "same-origin",
      headers,
    });
    const res = await fetch(`/Rhythmyx/services/contenttypes/${key}`, {
      method: "DELETE",
      credentials: "same-origin",
      headers,
    });
    return res.status;
  }, typeName);
}

test.describe("Developer content type copy (#4893)", () => {
  test("copy asks for a name, rejects duplicate and Folder, lists the copy", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const stamp = Date.now().toString().slice(-6);
    const sourceName = `percQaSrc${stamp}`;
    const copyName = `percQaCopy${stamp}`;

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    await page.locator('[data-testid="developer-ct-new"]').click();
    await page.locator('[data-testid="developer-ct-create-name"]').fill(sourceName);
    await page.locator('[data-testid="developer-ct-create-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(
      page.locator(`[data-testid="developer-ct-copy"][data-ct-name="${sourceName}"]`),
    ).toBeVisible({ timeout: 20_000 });

    const pageCopy = page.locator(
      '[data-testid="developer-ct-copy"][data-ct-name="percPage"]',
    );
    await expect(pageCopy).toBeVisible({ timeout: 20_000 });
    await pageCopy.click();
    const nameInput = page.locator('[data-testid="developer-ct-copy-name"]');
    const submit = page.locator('[data-testid="developer-ct-copy-submit"]');
    await expect(submit).toBeDisabled();
    await nameInput.fill("bad name");
    await expect(submit).toBeDisabled();
    await nameInput.fill("percPage");
    await expect(submit).toBeEnabled();
    await submit.click();
    const dup = page.locator('[data-testid="developer-ct-copy-error"]');
    await expect(dup).toBeVisible({ timeout: 20_000 });
    await expect(dup).toContainText(/already exists|409/i);

    const folderCopy = page.locator(
      '[data-testid="developer-ct-copy"][data-ct-name="Folder"]',
    );
    if ((await folderCopy.count()) > 0) {
      await folderCopy.click();
      await page.locator('[data-testid="developer-ct-copy-name"]').fill("FolderCopyQa");
      await page.locator('[data-testid="developer-ct-copy-submit"]').click();
      const sysErr = page.locator('[data-testid="developer-ct-copy-error"]');
      await expect(sysErr).toBeVisible({ timeout: 20_000 });
      await expect(sysErr).toContainText(/cannot be copied|400/i);
    }

    await page
      .locator(`[data-testid="developer-ct-copy"][data-ct-name="${sourceName}"]`)
      .click();
    await page.locator('[data-testid="developer-ct-copy-name"]').fill(copyName);
    await page.locator('[data-testid="developer-ct-copy-submit"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(
      page.locator(`[data-testid="developer-ct-open"][data-ct-name="${copyName}"]`),
    ).toBeVisible({ timeout: 20_000 });

    const removed = await deleteContentType(page, copyName);
    expect([204, 200, 404]).toContain(removed);
    const removedSource = await deleteContentType(page, sourceName);
    expect([204, 200, 404]).toContain(removedSource);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
