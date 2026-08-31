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
 * Developer Content Type detail rename chrome (#4058 CD-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-rename.spec.js
 *   perc-devctl qa-down
 * </pre>
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
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const empty = page.locator('[data-testid="developer-ct-empty"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible();
}

async function openPercPageDetail(page) {
  const openBtn = page.locator(
    '[data-testid="developer-ct-open"][data-ct-name="percPage"]',
  );
  const anyOpen = page.locator('[data-testid="developer-ct-open"]').first();
  if ((await openBtn.count()) > 0) {
    await openBtn.click();
  } else {
    await anyOpen.click();
  }
  await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 20_000,
  });
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

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 */
async function inPagePutNameStatus(page, typeName, newName) {
  const url = `/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/name`;
  return page.evaluate(
    async ({ path, name }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(path, {
        method: "PUT",
        credentials: "same-origin",
        headers,
        body: JSON.stringify({ ContentTypeName: { name } }),
      });
      return res.status;
    },
    { path: url, name: newName },
  );
}

async function inPageGetStatus(page, typeName) {
  const url = `/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`;
  return page.evaluate(async (path) => {
    const res = await fetch(path, {
      method: "GET",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
    return res.status;
  }, url);
}

test.describe("Developer content type rename (#4058 / CD-01)", () => {
  test("unlocked 409, invalid names, unique rename GET 200/404, duplicate rejected", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const tmpName = "percPageTmpRn";

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    const unlockedStatus = await inPagePutNameStatus(
      page,
      "percPage",
      "percShouldNotRename",
    );
    expect(
      unlockedStatus,
      `unlocked PUT .../name should be 409 (got ${unlockedStatus})`,
    ).toBe(409);
    expect(await inPageGetStatus(page, "percPage"), "percPage after unlocked PUT").toBe(
      200,
    );

    await openPercPageDetail(page);
    const renameBtn = page.locator('[data-testid="developer-ct-rename"]');
    const nameInput = page.locator('[data-testid="developer-ct-name"]');
    await expect(renameBtn).toBeDisabled();
    await expect(nameInput).toBeDisabled();

    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Locked by you/i,
      { timeout: 20_000 },
    );
    await expect(nameInput).toBeEnabled();
    await nameInput.fill("bad name");
    await expect(renameBtn).toBeDisabled();
    await nameInput.fill("   ");
    await expect(renameBtn).toBeDisabled();

    await nameInput.fill(tmpName);
    await expect(renameBtn).toBeEnabled();
    await page.locator('[data-testid="developer-ct-rename"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-notice"]')).toContainText(
      /renamed/i,
      { timeout: 20_000 },
    );
    try {
      expect(await inPageGetStatus(page, tmpName), `GET ${tmpName}`).toBe(200);
      expect(await inPageGetStatus(page, "percPage"), "GET old percPage").toBe(404);
    } finally {
      await nameInput.fill("percPage");
      await page.locator('[data-testid="developer-ct-rename"]').click();
      await expect
        .poll(async () => inPageGetStatus(page, "percPage"), { timeout: 20_000 })
        .toBe(200);
    }

    await nameInput.fill("Folder");
    await page.locator('[data-testid="developer-ct-rename"]').click();
    const err = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|duplicate|400|409/i);
    expect(await inPageGetStatus(page, "percPage"), "percPage after duplicate").toBe(
      200,
    );

    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
    await expect(renameBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
