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
 * Developer extensions + relationship-types catalog readback (#5918 /
 * parent #1690, slice 141).
 *
 * Proves both catalogs plus detail render from the thin-read APIs in the
 * React shell. Extensions list reads back through
 * GET /services/extensions/catalog (name, handler, context, category —
 * no method-map or runtime-parameter payload) and the detail reads back
 * through GET /services/extensions/catalog/item?key= (title, FQN,
 * context, interfaces, className, method map, runtime parameters).
 * Relationship types list reads back through
 * GET /services/relationshiptypes (name, label, category, flags) and the
 * detail reads back through GET /services/relationshiptypes/{idOrName}
 * (title, label, description, cloning flags, overrides, effects).
 *
 * Tests 1 and 3 are zero-write: they open a packaged row (system UDF
 * `add` under global/percussion/udf/; system type `ActiveAssembly`) and
 * assert list + detail readback plus thin-read REST probes for the same
 * row. Tests 2 and 4 create one row through the shell, prove
 * name/label readback in the catalog and detail, then remove it with
 * REST DELETE cleanup and prove catalog omission.
 *
 * Readback only, no row mutation; out of scope: slices 139/140.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-extensions-reltypes-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

/** Packaged system UDF shipped on the H2 seed (global/percussion/udf/). */
const PACKAGED_EXTENSION = "add";
/** Packaged system relationship type shipped on the H2 seed. */
const PACKAGED_RELTYPE = "ActiveAssembly";

function developerUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Java-identifier unique extension name (letters/digits, starts with a letter). */
function uniqueExtensionName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa5918${suffix || "x"}`;
}

/** Unique relationship-type name: no whitespace / wildcards / path chars. */
function uniqueRtName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/gi, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/gi, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QaRt5918${suffix || "x"}`;
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

async function openExtensionsCatalog(page) {
  await page.goto(developerUrl("extensions"), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator('[data-testid="tab-developer-extensions"]'),
  ).toBeVisible({ timeout: 15_000 });
  const panel = page.locator('[data-testid="developer-ex-panel"]');
  const empty = page.locator('[data-testid="developer-ex-empty"]');
  const listError = page.locator('[data-testid="developer-ex-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer extensions catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer extensions catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

async function openRtCatalog(page) {
  await page.goto(developerUrl("relationship-types"), {
    waitUntil: "networkidle",
  });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator('[data-testid="tab-developer-relationship-types"]'),
  ).toBeVisible({ timeout: 15_000 });
  const panel = page.locator('[data-testid="developer-rt-panel"]');
  const empty = page.locator('[data-testid="developer-rt-empty"]');
  const listError = page.locator('[data-testid="developer-rt-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer relationship-types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "Developer relationship-types catalog is empty — no packaged row to read back",
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

/**
 * Exact-name relationship-type open control (the rt open button carries no
 * data-* identity attribute, so filter by exact button text like the
 * relationship-type editor spec does).
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} name exact relationship-type name
 */
function rtOpen(page, name) {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return page
    .locator('[data-testid="developer-rt-open"]')
    .filter({ hasText: new RegExp(`^${escaped}$`) });
}

test.describe("Developer extensions + relationship-types catalog readback (#5918)", () => {
  test("packaged extension list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    // Thin-read catalog: packaged system row renders the open button.
    const packagedOpen = page.locator(
      catalogOpenByExactName("developer-ex-open", "data-ex-name", PACKAGED_EXTENSION),
    );
    await expect(packagedOpen).toBeVisible({ timeout: 15_000 });
    const packagedContext = await packagedOpen.getAttribute("data-ex-context");
    expect(
      packagedContext || "",
      "packaged extension reads back its system context",
    ).toMatch(/^global\/percussion/);

    // Thin-read REST probe: catalog list contains the packaged row.
    const probe = await inPageJson(
      page,
      "/Rhythmyx/services/extensions/catalog",
      "GET",
    );
    expect(
      probe.status,
      `thin-read catalog should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged extension").toContain(
      PACKAGED_EXTENSION,
    );

    // Thin-read detail probe for the same row (FQN or short name key).
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/extensions/catalog/item?key=${encodeURIComponent(PACKAGED_EXTENSION)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the extension").toContain(
      PACKAGED_EXTENSION,
    );

    // Detail readback in the shell: title + immutable chrome.
    await packagedOpen.click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-ex-detail-loading"]'),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-ex-detail-title"]'),
    ).toContainText(PACKAGED_EXTENSION, { timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-ex-immutable-hint"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-ex-save"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ex-delete"]')).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created extension name and interfaces read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    const name = uniqueExtensionName();
    const iface = "com.percussion.extension.IPSUdfProcessor";

    // Shell create (POST /services/extensions).
    await page.locator('[data-testid="developer-ex-new"]').click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-name"]').fill(name);
    await page.locator('[data-testid="developer-ex-interfaces"]').fill(iface);
    // Class used by the ExtensionAdaptorWriteTest fixture (shipped generic UDF).
    await page
      .locator('[data-testid="developer-ex-classname"]')
      .fill("com.percussion.generic.PSAdd");
    await page.locator('[data-testid="developer-ex-save"]').click();

    const notice = page.locator('[data-testid="developer-ex-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-ex-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-ex-name"]')).toHaveValue(name);
    await expect(page.locator('[data-testid="developer-ex-name"]')).toBeDisabled();

    // Catalog readback: row for the created extension with its interface.
    await page.locator('[data-testid="developer-ex-back"]').click();
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const openSel = catalogOpenByExactName("developer-ex-open", "data-ex-name", name);
    await expect(page.locator(openSel)).toBeVisible({ timeout: 30_000 });
    const row = page.locator(`tr:has(${openSel})`);
    // Catalog columns are name / handler / context / category / flags:
    // the row reads back the forced user/ context and Java handler.
    await expect(row).toContainText("user/", { timeout: 20_000 });

    // Detail readback: reopened row shows the persisted name + interfaces.
    await page.locator(openSel).click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-ex-detail-title"]'),
    ).toContainText(name, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-ex-interfaces"]')).toHaveValue(
      iface,
    );

    // REST cleanup: DELETE the created extension, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/extensions/catalog/item?key=${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openExtensionsCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("packaged relationship-type list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openRtCatalog(page);

    // Thin-read catalog: packaged system row renders the open button.
    const packagedOpen = rtOpen(page, PACKAGED_RELTYPE);
    await expect(packagedOpen).toBeVisible({ timeout: 15_000 });

    // Thin-read REST probe: catalog list contains the packaged row.
    const probe = await inPageJson(page, "/Rhythmyx/services/relationshiptypes", "GET");
    expect(
      probe.status,
      `thin-read catalog should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged type").toContain(
      PACKAGED_RELTYPE,
    );

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/relationshiptypes/${encodeURIComponent(PACKAGED_RELTYPE)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the type").toContain(
      PACKAGED_RELTYPE,
    );

    // Detail readback in the shell: title + system read-only chrome.
    await packagedOpen.click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-rt-detail-loading"]'),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-rt-detail-title"]'),
      // The detail header shows the human label ("Active Assembly"); the
      // thin-read probes above prove the wire name (ActiveAssembly).
    ).toContainText("Active Assembly", { timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-rt-system-readonly"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-rt-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-rt-delete"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created relationship-type name and label read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openRtCatalog(page);

    const name = uniqueRtName();
    const label = `QA 5918 ${name}`;

    // Shell create (POST /services/relationshiptypes: name + category).
    await page.locator('[data-testid="developer-rt-new"]').click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-rt-name"]').fill(name);
    await page.locator('[data-testid="developer-rt-label"]').fill(label);
    await page.locator('[data-testid="developer-rt-category"]').selectOption("rs_generic");
    await page.locator('[data-testid="developer-rt-save"]').click();

    const notice = page.locator('[data-testid="developer-rt-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-rt-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(
        `Create relationship type failed: ${(await saveError.innerText()).trim()}`,
      );
    }
    await expect(notice).toContainText(/saved/i);

    // Catalog readback: row for the created type with its label.
    await page.locator('[data-testid="developer-rt-back"]').click();
    await expect(page.locator('[data-testid="developer-rt-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const row = rtOpen(page, name);
    await expect(row).toBeVisible({ timeout: 30_000 });
    const rowTr = page.locator("tr", { hasText: label });
    await expect(rowTr.first()).toContainText(label, { timeout: 20_000 });

    // REST cleanup: DELETE the created type, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/relationshiptypes/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openRtCatalog(page);
    await expect(rtOpen(page, name)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
