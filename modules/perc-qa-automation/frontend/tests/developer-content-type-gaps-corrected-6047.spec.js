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
 * Developer content-type corrected gap claims (#6047 / parent #1690).
 *
 * Proves the two stale content-type-api-gaps claims are corrected end to
 * end: the `CT_FIELD_RULE_EXPR` gap no longer says field-validation
 * apply-when is read-only (REST PUT persists it, #4449; the SPA Field rule
 * expressions section edits it), and `CT_SHARED_FIELD_INCLUSION` no longer
 * says the SPA field picker remains Workbench (the Developer editor ships
 * the include picker plus Detach confirm).
 *
 * Zero-write: opens a packaged catalog row and reads the SPA gaps list plus
 * thin-read REST probes for the same row. Out of scope: sibling slices.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-content-type-gaps-corrected-6047.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

/**
 * Packaged design content type shipped with the CMS. Preferred zero-write
 * readback target; the spec falls back to the first catalog row when it is
 * not listed.
 */
const PACKAGED_CONTENT_TYPE = "percPage";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method) {
  return page.evaluate(
    async ({ path: url, method: httpMethod }) => {
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
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method },
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

async function openPackagedContentTypeDetail(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible({
    timeout: 15_000,
  });
  const packagedOpen = page.locator(
    `[data-testid="developer-ct-open"][data-ct-name="${PACKAGED_CONTENT_TYPE}"]`,
  );
  const firstRow = page.locator(catalogRowSelector("developer-ct-row", 0));
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
  const packagedCount = await packagedOpen.count();
  const openBtn =
    packagedCount > 0
      ? packagedOpen.first()
      : firstRow.locator('[data-testid="developer-ct-open"]');
  await expect(openBtn).toBeVisible();
  const rowName = await openBtn.getAttribute("data-ct-name");
  expect(rowName, "catalog row carries data-ct-name").toBeTruthy();
  await openBtn.click();
  await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  return rowName;
}

test.describe("Developer content-type corrected gap claims (#6047)", () => {
  test("gaps list shows the corrected rule-expression and inclusion claims", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const rowName = await openPackagedContentTypeDetail(page);

    // SPA gaps list: corrected CT_FIELD_RULE_EXPR stays visible (detail
    // field rows remain summary strings) with the writable apply-when
    // claim; the stale read-only sentence is gone.
    const gaps = page.locator('[data-testid="developer-ct-gaps"]');
    await expect(gaps).toBeVisible({ timeout: 20_000 });
    const ruleExprGap = gaps.locator('li[data-gap-code="CT_FIELD_RULE_EXPR"]');
    await expect(ruleExprGap).toBeVisible({ timeout: 15_000 });
    await expect(ruleExprGap).toContainText("apply-when is writable");
    await expect(ruleExprGap).not.toContainText("is read-only");
    // Retired inclusion code stays hidden; no stale picker claim anywhere.
    await expect(
      gaps.locator('li[data-gap-code="CT_SHARED_FIELD_INCLUSION"]'),
    ).toHaveCount(0);
    const gapsText = ((await gaps.innerText()) || "").toLowerCase();
    expect(gapsText, "no stale picker claim").not.toContain(
      "spa field picker remains workbench",
    );
    expect(gapsText, "no stale read-only apply-when claim").not.toContain(
      "apply-when on field validation is read-only",
    );
    // The one true "remains Workbench" remainder (child field-set order,
    // CT_FIELD_CREATE_DELETE) still renders — proof the list is live, and
    // out of scope for this slice.
    await expect(
      gaps.locator('li[data-gap-code="CT_FIELD_CREATE_DELETE"]'),
    ).toContainText("Child field-set order remains Workbench");

    // Thin-read REST probe for the same row: server designGaps carry the
    // corrected messages.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(rowName)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    const body = JSON.parse(detailProbe.text);
    // Jackson WRAP_ROOT_VALUE: detail nests under the class-name root.
    const detail =
      body.ContentTypeDetail ?? body.contentTypeDetail ?? body.ContentType ?? body;
    const designGaps = Array.isArray(detail.designGaps) ? detail.designGaps : [];
    const byCode = (code) => designGaps.find((g) => g && g.code === code);
    const ruleExpr = byCode("CT_FIELD_RULE_EXPR");
    expect(ruleExpr, "REST designGaps carry CT_FIELD_RULE_EXPR").toBeTruthy();
    expect(
      String(ruleExpr.message),
      "server rule-expression gap states apply-when is writable",
    ).toContain("apply-when is writable");
    expect(
      String(ruleExpr.message),
      "server rule-expression gap drops the stale read-only sentence",
    ).not.toContain("Apply-when on field validation is read-only");
    const inclusion = byCode("CT_SHARED_FIELD_INCLUSION");
    expect(
      inclusion,
      "REST designGaps carry CT_SHARED_FIELD_INCLUSION",
    ).toBeTruthy();
    expect(
      String(inclusion.message),
      "server inclusion gap states the SPA picker shipped",
    ).toContain("include picker");
    expect(
      String(inclusion.message),
      "server inclusion gap drops the stale Workbench-only sentence",
    ).not.toContain("SPA field picker remains Workbench");

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
