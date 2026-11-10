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
 * Developer content-types catalog readback (#5956 / parent #1690, slice 144).
 *
 * Proves the design content-type list/detail renders from the thin-read API
 * in the React shell: the catalog table reads back through
 * GET /services/contenttypes (label open button, name, GUID, description
 * cells — no field, workflow, template, or choice payload), and the detail
 * panel reads back through GET /services/contenttypes/{idOrName} (title,
 * committed name + GUID, label text, description text).
 *
 * Test 1 is zero-write: it opens a packaged catalog row (the `percPage`
 * design type when installed, else the first catalog row) and asserts list
 * + detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one type through the shell, proves name/label/description
 * readback in the detail and catalog, then locks and removes it with REST
 * DELETE cleanup (content-type DELETE requires a held design-session lock)
 * and proves catalog omission.
 *
 * Readback only, no field/workflow/template/choice mutation; out of scope:
 * slices 142/143.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-content-types-readback.spec.js
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
 * REST-safe unique content-type name: letters, digits, underscore, and
 * period only (no spaces or wildcards).
 */
function uniqueTypeName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QaCt5956rb${suffix || "t"}`;
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

/**
 * Content-type DELETE requires a design-session lock held by the current
 * user (unlocked DELETE is 409), so cleanup locks first, then deletes.
 */
async function lockAndDeleteContentType(page, typeName) {
  const key = encodeURIComponent(typeName);
  await inPageJson(page, `/Rhythmyx/services/contenttypes/${key}/lock`, "POST");
  return inPageJson(page, `/Rhythmyx/services/contenttypes/${key}`, "DELETE");
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

async function openContentTypesCatalog(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible({
    timeout: 15_000,
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
  if (await empty.isVisible()) {
    throw new Error(
      "Developer content types catalog is empty — no packaged row to read back",
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer content-types catalog readback (#5956)", () => {
  test("packaged content type list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    // Thin-read catalog: prefer the packaged design type, else the first
    // row. The row renders the label open button plus name, GUID, and
    // description cells.
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
    const rowLabel = ((await openBtn.innerText()) || "").trim();
    const row =
      packagedCount > 0
        ? page.locator(
            `tr:has([data-testid="developer-ct-open"][data-ct-name="${rowName}"])`,
          )
        : firstRow;
    await expect(row).toContainText(rowName);
    const descCell = page.locator(
      `[data-testid="developer-ct-catalog-description"][data-ct-name="${rowName}"]`,
    );
    await expect(descCell).toBeAttached({ timeout: 15_000 });
    const rowDescription =
      (await descCell.getAttribute("data-ct-description")) || "";

    // Thin-read REST probe for the same row: GET /services/contenttypes
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/contenttypes", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged type").toContain(rowName);

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(rowName)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the type").toContain(rowName);

    // Detail readback in the shell: title, committed name + GUID, label and
    // description text.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ct-detail-title"]')).toContainText(
      rowLabel || rowName,
      { timeout: 20_000 },
    );
    await expect(page.locator('[data-testid="developer-ct-detail-name"]')).toContainText(
      rowName,
    );
    const guid = page.locator('[data-testid="developer-ct-detail-guid"]');
    await expect(guid).toBeAttached({ timeout: 20_000 });
    expect(((await guid.innerText()) || "").trim().length, "detail GUID reads back")
      .toBeGreaterThan(0);
    const labelText = page.locator('[data-testid="developer-ct-set-label-text"]');
    await expect(labelText).toBeAttached({ timeout: 20_000 });
    if (rowLabel) {
      await expect(labelText).toContainText(rowLabel);
    }
    const descText = page.locator('[data-testid="developer-ct-set-description-text"]');
    await expect(descText).toBeAttached({ timeout: 20_000 });
    if (rowDescription) {
      await expect(descText).toContainText(rowDescription);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created type name, label, and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypesCatalog(page);

    const name = uniqueTypeName();
    const label = `QA 5956 content types readback ${name.slice(-4)}`;
    const description = "qa5956 content types catalog readback";

    // Shell create (POST /services/contenttypes) lands on the new detail.
    await page.locator('[data-testid="developer-ct-new"]').click();
    await expect(page.locator('[data-testid="developer-ct-create"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-ct-create-save"]');
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-ct-create-name"]').fill(name);
    await expect(saveBtn).toBeEnabled();
    await page.locator('[data-testid="developer-ct-create-label"]').fill(label);
    await page.locator('[data-testid="developer-ct-create-description"]').fill(description);
    await saveBtn.click();
    const detail = page.locator('[data-testid="developer-ct-detail"]');
    const createError = page.locator('[data-testid="developer-ct-create-error"]');
    await expect(detail.or(createError).first()).toBeVisible({ timeout: 30_000 });
    if (await createError.isVisible()) {
      throw new Error(`Create failed: ${(await createError.innerText()).trim()}`);
    }

    // Detail readback: title shows the label, committed name, label and
    // description text echo the create payload.
    await expect(page.locator('[data-testid="developer-ct-detail-title"]')).toHaveText(
      label,
    );
    await expect(page.locator('[data-testid="developer-ct-detail-name"]')).toContainText(
      name,
    );
    await expect(page.locator('[data-testid="developer-ct-name"]')).toHaveValue(name);
    await expect(page.locator('[data-testid="developer-ct-set-label-text"]')).toHaveText(
      label,
    );
    await expect(
      page.locator('[data-testid="developer-ct-set-description-text"]'),
    ).toHaveText(description);

    // Catalog readback: row for the created type with its description.
    await page.locator('[data-testid="developer-ct-back"]').click();
    const openSel = `[data-testid="developer-ct-open"][data-ct-name="${name}"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const createdRow = page.locator(`tr:has(${openSel})`);
    await expect(createdRow).toContainText(description, { timeout: 20_000 });

    // REST cleanup: lock, then DELETE the created type; prove catalog omission.
    const del = await lockAndDeleteContentType(page, name);
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openContentTypesCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
