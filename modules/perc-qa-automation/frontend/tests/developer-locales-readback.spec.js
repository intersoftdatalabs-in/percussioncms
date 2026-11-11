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
 * Developer locales catalog readback (#6044 / parent #1690, slice CD-18).
 *
 * Proves the CMS locale catalog renders from the thin-read API in the React
 * shell: the catalog table reads back through GET /services/locales
 * (language string, label, status, base flag, format-profile and description
 * cells), and the locale detail reads back through
 * GET /services/locales/{idOrLang} (stored language, label, description,
 * status, base flag, numeric id).
 *
 * Test 1 is zero-write: it opens the packaged en-us row and asserts list +
 * detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one locale through the shell, proves language/label
 * readback in the catalog and detail plus a description write read back in
 * both places, then removes it with REST DELETE cleanup and proves catalog
 * omission.
 *
 * Readback only, no format-profile/auto-translation/fallback mutation;
 * out of scope: locale write flows (slices CD-18 follow-ons).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-locales-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerLocalesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "locales",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** BCP-47-style unique language matching REST LANGUAGE_PATTERN. */
function uniqueLang() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `xx-${suffix || "qa6044"}`;
}

function locOpenSelector(lang) {
  return `[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @param {object} [body]
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload }) => {
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
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
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

async function openLocalesCatalog(page) {
  await page.goto(developerLocalesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-loc-panel"]');
  const empty = page.locator('[data-testid="developer-loc-empty"]');
  const listError = page.locator('[data-testid="developer-loc-error"]');
  const sectionError = page.locator('[data-testid="developer-section-error"]');
  await expect(panel.or(empty).or(listError).or(sectionError).first()).toBeVisible({
    timeout: 30_000,
  });
  if ((await listError.isVisible()) || (await sectionError.isVisible())) {
    const node = (await listError.isVisible()) ? listError : sectionError;
    throw new Error(
      `Developer locales catalog error: ${(await node.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer locales catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer locales catalog readback (#6044)", () => {
  test("packaged locale list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openLocalesCatalog(page);

    // Packaged seed row: en-us renders its open button plus label cell.
    const openBtn = page.locator(locOpenSelector("en-us"));
    await expect(openBtn).toBeVisible({ timeout: 15_000 });
    await expect(openBtn).toContainText("en-us");
    const labelCell = page.locator(
      '[data-testid="developer-loc-catalog-label"][data-loc-lang="en-us"]',
    );
    await expect(labelCell).toBeAttached({ timeout: 15_000 });
    const catalogLabel = ((await labelCell.innerText()) || "").trim();
    const expectedTitle =
      catalogLabel && catalogLabel !== "—" ? catalogLabel : "en-us";

    // Thin-read REST probe for the same row: GET /services/locales
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/locales", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged locale").toContain(
      "en-us",
    );

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/locales/${encodeURIComponent("en-us")}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the locale").toContain(
      "en-us",
    );

    // Detail readback in the shell: title shows the stored label (or the
    // language string), the header shows language plus numeric id, the
    // language input is read-only, and label/status/base regions render.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-loc-detail-title"]')).toContainText(
      expectedTitle,
      { timeout: 20_000 },
    );
    const headerMeta = page.locator('[data-testid="developer-loc-detail"] header');
    await expect(headerMeta).toContainText("en-us");
    const headerText = await headerMeta.innerText();
    expect(headerText, `detail header shows a numeric id: ${headerText}`).toMatch(
      /id \d+/,
    );
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(
      "en-us",
    );
    await expect(page.locator('[data-testid="developer-loc-language"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-loc-label"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-loc-status"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-loc-base"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created locale language, label, and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openLocalesCatalog(page);

    const lang = uniqueLang();
    const label = `QA 6044 ${lang}`;
    const description = "qa6044 locales catalog readback";

    // Shell create (POST /services/locales via the create form).
    await page.locator('[data-testid="developer-loc-new"]').click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-language"]').fill(lang);
    await page.locator('[data-testid="developer-loc-label"]').fill(label);
    await page.locator('[data-testid="developer-loc-save"]').click();
    const notice = page.locator('[data-testid="developer-loc-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-loc-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    // Catalog readback: back returns to the stale list, so reload and prove
    // the created row renders with its label cell.
    await openLocalesCatalog(page);
    const openSel = locOpenSelector(lang);
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const createdRow = page.locator(`tr:has(${openSel})`);
    await expect(createdRow).toContainText(label, { timeout: 20_000 });

    // Detail readback: stored label title plus read-only language.
    await page.locator(openSel).click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-loc-detail-title"]')).toContainText(
      label,
      { timeout: 20_000 },
    );
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(
      lang,
    );

    // Description write then readback on detail and in the catalog.
    await page.locator('[data-testid="developer-loc-set-description-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-description-input"]').fill(description);
    await page.locator('[data-testid="developer-loc-set-description-save"]').click();
    await expect(
      page.locator('[data-testid="developer-loc-set-description-notice"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-loc-set-description-text"]'),
    ).toHaveText(description);
    await openLocalesCatalog(page);
    await expect(page.locator(`tr:has(${openSel})`)).toContainText(description, {
      timeout: 20_000,
    });

    // REST cleanup: DELETE the created locale, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/locales/${encodeURIComponent(lang)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openLocalesCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
