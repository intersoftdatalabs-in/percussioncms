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
 * Developer content-type list copy-form keyboard path (#7344 / parent #1690;
 * mirrors Roles #6905, slot #6713, content-type/locale #6747).
 *
 * The copy-name input autofocuses when the copy form opens, and Escape
 * cancels the form without posting (same as Cancel): no POST fires and no
 * content type is created.
 *
 * Focus return (#6733 pattern): when the form closes (Escape or Cancel) the
 * input unmounts, so focus must return to the invoking catalog Copy button
 * instead of falling back to <body>.
 *
 *   npm run test:surface -- --path tests/developer-ct-copy-form-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
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

function attachConsole(page) {
  const consoleErrors = [];
  const pageErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  return { consoleErrors, pageErrors };
}

function assertClean(consoleErrors, pageErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409)/.test(
        text,
      )
    ) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    if (/favicon/i.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isContentTypeCopy(request) {
  return (
    request.method() === "POST" && /\/services\/contenttypes\/.+\/copy/.test(request.url())
  );
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
  await expect(
    page.locator('[data-testid="developer-ct-copy"][data-ct-name="percPage"]'),
  ).toBeVisible({ timeout: 20_000 });
}

test.describe("Developer content-type copy-form keyboard path (#7344)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("copy-name autofocuses and Escape cancels without posting", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openContentTypesCatalog(page);

    const posts = [];
    page.on("request", (request) => {
      if (isContentTypeCopy(request)) {
        posts.push(request.url());
      }
    });

    await page
      .locator('[data-testid="developer-ct-copy"][data-ct-name="percPage"]')
      .click();
    const nameInput = page.locator('[data-testid="developer-ct-copy-name"]');
    await expect(nameInput).toBeFocused();
    await nameInput.fill("percPageQaKeyboard");
    await nameInput.press("Escape");
    await expect(page.locator('[data-testid="developer-ct-copy-form"]')).toHaveCount(0);
    expect(posts, "Escape must not post the copy").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-ct-copy"][data-ct-name="percPage"]'),
    ).toBeFocused();
    await page.screenshot({ path: `test-results/live-proof-7344-ct-copy-keyboard.png` });

    assertClean(consoleErrors, pageErrors);
  });

  test("Cancel returns focus to the invoking Copy button", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openContentTypesCatalog(page);

    const posts = [];
    page.on("request", (request) => {
      if (isContentTypeCopy(request)) {
        posts.push(request.url());
      }
    });

    await page
      .locator('[data-testid="developer-ct-copy"][data-ct-name="percPage"]')
      .click();
    await expect(page.locator('[data-testid="developer-ct-copy-name"]')).toBeFocused();
    await page.locator('[data-testid="developer-ct-copy-name"]').fill("percPageQaCancel");
    await page.locator('[data-testid="developer-ct-copy-cancel"]').click();
    await expect(page.locator('[data-testid="developer-ct-copy-form"]')).toHaveCount(0);
    expect(posts, "Cancel must not post the copy").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-ct-copy"][data-ct-name="percPage"]'),
    ).toBeFocused();

    assertClean(consoleErrors, pageErrors);
  });
});
