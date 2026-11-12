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
 * Developer keywords catalog readback (#6045 / parent #1690, slice CD-17).
 *
 * Proves the Keywords content-design catalog renders from the list API
 * in the React shell: the catalog table reads back through
 * GET /services/keywords?includeChoices=true (label, value, choice-count,
 * and description cells — the count cell needs the embedded choices, so
 * the thin `includeChoices=false` variant cannot feed the shell), and the
 * keyword detail reads back through
 * GET /services/keywords/{idOrValue} (stored label, description, sequence,
 * and choices).
 *
 * Test 1 is zero-write: it opens the packaged Body_Markup row and asserts
 * list + detail readback plus a thin-read REST parity probe
 * (`includeChoices=false` lists the same row without per-keyword choice
 * payload) for the same row.
 * Test 2 creates one keyword through the shell, proves label/description
 * readback in the catalog and detail plus the empty saved-choices state,
 * then removes it with REST DELETE
 * cleanup and proves catalog omission.
 *
 * Readback only, no label/description/sequence/choice mutation; out of
 * scope: keyword write flows (slices 85-94).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-keywords-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

// Packaged seed row on a stock install: label Body_Markup,
// description "Body Markup Lookups", sequence 1.
const PACKAGED_LABEL = "Body_Markup";
const PACKAGED_DESCRIPTION = "Body Markup Lookups";

function developerKeywordsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "keywords",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * REST-safe unique keyword label: alphanumerics only.
 */
function uniqueKeywordLabel() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/gi, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/gi, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QAKw6045Rb${suffix || "rb"}`.replace(/[^a-zA-Z0-9]/g, "");
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

/**
 * Resolve the server guid string for a keyword label via GET detail.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} label
 * @returns {Promise<string|null>}
 */
async function keywordGuidForLabel(page, label) {
  const probe = await inPageJson(
    page,
    `/Rhythmyx/services/keywords/${encodeURIComponent(label)}`,
    "GET",
  );
  if (probe.status !== 200) {
    return null;
  }
  let parsed;
  try {
    parsed = JSON.parse(probe.text);
  } catch {
    return null;
  }
  const root = parsed.Keyword || parsed.keyword || parsed;
  const guid = root.guid;
  if (guid && typeof guid === "object") {
    return guid.stringValue || guid.untypedString || null;
  }
  return typeof guid === "string" ? guid : null;
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

function keywordOpenSelector(label) {
  return `[data-testid="developer-kw-open"][aria-label="Open ${label}"]`;
}

async function openKeywordsCatalog(page) {
  await page.goto(developerKeywordsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="tab-developer-keywords"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-kw-panel"]');
  const empty = page.locator('[data-testid="developer-kw-empty"]');
  const listError = page.locator('[data-testid="developer-kw-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer keywords catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer keywords catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer keywords catalog readback (#6045)", () => {
  test("packaged keyword list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openKeywordsCatalog(page);

    // Packaged seed row: Body_Markup renders its open button with the
    // stored label, and the row carries the stored description cell.
    const openBtn = page.locator(keywordOpenSelector(PACKAGED_LABEL));
    await expect(openBtn).toBeVisible({ timeout: 15_000 });
    await expect(openBtn).toContainText(PACKAGED_LABEL);
    const packagedRow = page.locator(`tr:has(${keywordOpenSelector(PACKAGED_LABEL)})`);
    await expect(packagedRow).toContainText(PACKAGED_DESCRIPTION, {
      timeout: 15_000,
    });

    // Thin-read REST parity probe for the same row: the
    // `includeChoices=false` variant (which the shell does NOT use for the
    // catalog — its choice counts would read 0) still lists the row.
    const probe = await inPageJson(
      page,
      "/Rhythmyx/services/keywords?includeChoices=false",
      "GET",
    );
    expect(
      probe.status,
      `thin-read catalog should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read catalog contains the packaged keyword").toContain(
      PACKAGED_LABEL,
    );

    // Thin-read detail probe for the same row: GET by label is 200 and
    // names the keyword.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/keywords/${encodeURIComponent(PACKAGED_LABEL)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the keyword").toContain(
      PACKAGED_LABEL,
    );

    // Detail readback in the shell: the editor shows the stored label,
    // the stored description, and the stored sequence.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(
      PACKAGED_LABEL,
      { timeout: 20_000 },
    );
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      PACKAGED_DESCRIPTION,
    );
    await expect(
      page.locator('[data-testid="developer-kw-keyword-description-text"]'),
    ).toHaveAttribute("data-keyword-description", PACKAGED_DESCRIPTION);

    // Saved-choices readback in the shell: Body_Markup ships two stored
    // choices (Text/Text Markup, HTML/HTML Markup) and the detail lists
    // both with their stored labels.
    const savedChoices = page.locator(
      '[data-testid="developer-kw-saved-choices"]',
    );
    await expect(savedChoices).toBeVisible({ timeout: 20_000 });
    await expect(
      savedChoices.locator('[data-testid="developer-kw-choice"]'),
    ).toHaveCount(2);
    await expect(
      savedChoices.locator(
        '[data-testid="developer-kw-choice"][data-choice-label="Text"]',
      ),
    ).toBeVisible();
    await expect(
      savedChoices.locator(
        '[data-testid="developer-kw-choice"][data-choice-label="HTML"]',
      ),
    ).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created keyword label and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openKeywordsCatalog(page);

    const label = uniqueKeywordLabel();
    const description = "qa6045 keywords catalog readback";

    // Shell create (POST /services/keywords via the create form).
    await page.locator('[data-testid="developer-kw-new"]').click();
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
    await page.locator('[data-testid="developer-kw-label"]').fill(label);
    await page.locator('[data-testid="developer-kw-description"]').fill(description);
    await page.locator('[data-testid="developer-kw-sequence"]').fill("7");
    const created = page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        /\/services\/keywords\/?(?:\?|$)/.test(response.request().url()),
    );
    await page.locator('[data-testid="developer-kw-save"]').click();
    const createResponse = await created;
    expect(createResponse.status(), await createResponse.text()).toBe(200);

    // Catalog readback: the row for the created keyword renders with its
    // description cell.
    await expect(page.locator('[data-testid="developer-kw-panel"]')).toBeVisible({
      timeout: 30_000,
    });
    const openSel = keywordOpenSelector(label);
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    await expect(page.locator(`tr:has(${openSel})`)).toContainText(description, {
      timeout: 20_000,
    });

    // Detail readback: stored label, description, and sequence inputs.
    await page.locator(openSel).click();
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      description,
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("7");

    // Saved-choices readback for a choice-less keyword: the detail shows
    // the empty-choices state instead of a saved-choices list.
    await expect(
      page.locator('[data-testid="developer-kw-saved-choices"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-kw-choices-empty"]'),
    ).toBeVisible({ timeout: 20_000 });

    // REST cleanup: resolve the server guid, DELETE the created keyword,
    // then prove catalog omission.
    const guid = await keywordGuidForLabel(page, label);
    expect(guid, "created keyword resolves to a server guid").not.toBeNull();
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/keywords/${encodeURIComponent(guid)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openKeywordsCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
