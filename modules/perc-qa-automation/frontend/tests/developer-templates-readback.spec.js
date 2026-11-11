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
 * Developer templates catalog readback (#6048 / parent #1690).
 *
 * Proves the assembly template list/detail renders from the thin-read API in
 * the React shell: the catalog table reads back through
 * GET /services/templates (label, name, id, description cells), and the
 * detail panel reads back through GET /services/templates/{idOrName}
 * (title, committed name + numeric id + GUID, label text, description text,
 * assembler).
 *
 * Test 1 is zero-write: it opens the packaged perc.page row and asserts list
 * + detail readback plus thin-read REST probes for the same row. The
 * Developer Templates catalog has no shell create form (create stays on
 * POST /services/templates, see product-docs admin/developer-templates.md),
 * so test 2 creates one template through the same-origin REST POST, proves
 * name/label/description/assembler readback in the catalog and detail, then
 * removes it with REST DELETE cleanup and proves catalog omission.
 *
 * Readback only; no lock, save, import, slot, or association mutation; out
 * of scope: template write flows.
 *
 * Surface-filtered QA:
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-templates-readback.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Packaged template every H2 QA cell ships (fail closed when absent). */
const PACKAGED_TEMPLATE = "perc.page";

/**
 * REST-safe unique template name: starts with a letter, letters/digits only
 * (server rejects spaces and leading non-letters).
 */
function uniqueTemplateName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QATpl6048${suffix || "rb"}`;
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

async function openTemplatesCatalog(page) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="tab-developer-templates"]')).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const empty = page.locator('[data-testid="developer-tpl-empty"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "Developer templates catalog is empty — no packaged row to read back",
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer templates catalog readback (#6048)", () => {
  test("packaged template list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openTemplatesCatalog(page);

    // Thin-read catalog: the perc.page row renders the label open button plus
    // the name and description cells. The row <tr> carries data-tpl-name
    // (the description span shares the attribute, so scope to the row).
    const table = page.locator('[data-testid="developer-tpl-table"]');
    await expect(table).toBeVisible({ timeout: 15_000 });
    const row = table.locator(`tr[data-tpl-name="${PACKAGED_TEMPLATE}"]`);
    await expect(row, "packaged template row is listed").toBeVisible({
      timeout: 15_000,
    });
    await expect(row).toContainText(PACKAGED_TEMPLATE);
    const openBtn = row.locator('[data-testid="developer-tpl-catalog-label"]');
    await expect(openBtn).toBeVisible();
    const rowLabel = (await openBtn.getAttribute("data-tpl-label")) || "";
    const descCell = row.locator('[data-testid="developer-tpl-catalog-description"]');
    await expect(descCell).toBeAttached({ timeout: 15_000 });
    const rowDescription =
      (await descCell.getAttribute("data-tpl-description")) || "";

    // Thin-read REST probe for the same row: GET /services/templates
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/templates", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged template").toContain(
      PACKAGED_TEMPLATE,
    );

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/templates/${encodeURIComponent(PACKAGED_TEMPLATE)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the template").toContain(
      PACKAGED_TEMPLATE,
    );

    // Detail readback in the shell: title, committed name + numeric id +
    // GUID, label and description text, assembler.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-tpl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-tpl-detail-title"]'),
    ).toContainText(rowLabel || PACKAGED_TEMPLATE, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(
      PACKAGED_TEMPLATE,
    );
    const detailMeta = await page
      .locator('[data-testid="developer-tpl-detail-guid"]')
      .innerText();
    expect(
      detailMeta.trim().length,
      `detail header shows an object GUID: ${detailMeta}`,
    ).toBeGreaterThan(0);
    const labelText = page.locator('[data-testid="developer-tpl-set-label-text"]');
    await expect(labelText).toBeAttached({ timeout: 20_000 });
    if (rowLabel) {
      await expect(labelText).toContainText(rowLabel);
    }
    const descText = page.locator(
      '[data-testid="developer-tpl-set-description-text"]',
    );
    await expect(descText).toBeAttached({ timeout: 20_000 });
    if (rowDescription) {
      await expect(descText).toContainText(rowDescription);
    }
    const assembler = page.locator('[data-testid="developer-tpl-detail-assembler"]');
    await expect(assembler).toBeVisible({ timeout: 20_000 });
    expect(
      (await assembler.innerText()).trim().length,
      "detail assembler reads back",
    ).toBeGreaterThan(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created template name, label, and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openTemplatesCatalog(page);

    const name = uniqueTemplateName();
    const label = `QA 6048 templates readback ${name.slice(-4)}`;
    const description = "qa6048 templates catalog readback";

    // Create through the public REST POST (the Developer catalog has no
    // shell create form); the wire body is root-wrapped for UNWRAP_ROOT_VALUE.
    const created = await inPageJson(page, "/Rhythmyx/services/templates", "POST", {
      TemplateDetail: { name, label, description },
    });
    expect(
      created.status,
      `REST create should be 200 (got ${created.status}): ${created.text.slice(0, 300)}`,
    ).toBe(200);
    expect(created.text, "REST create echoes the template name").toContain(name);

    // Catalog readback: reload and prove the created row renders with its
    // label and description cells.
    await openTemplatesCatalog(page);
    const openSel = `tr[data-tpl-name="${name}"] [data-testid="developer-tpl-catalog-label"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const createdRow = page.locator(`tr[data-tpl-name="${name}"]`);
    await expect(createdRow).toContainText(label, { timeout: 20_000 });
    await expect(createdRow).toContainText(description, { timeout: 20_000 });

    // Detail readback: title shows the label, committed name echoes the
    // create payload, label/description text match, assembler defaults.
    await page.locator(openSel).click();
    await expect(page.locator('[data-testid="developer-tpl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-tpl-detail-title"]'),
    ).toHaveText(label, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(
      name,
    );
    await expect(
      page.locator('[data-testid="developer-tpl-set-label-text"]'),
    ).toHaveText(label);
    await expect(
      page.locator('[data-testid="developer-tpl-set-description-text"]'),
    ).toHaveText(description);
    const assembler = page.locator('[data-testid="developer-tpl-detail-assembler"]');
    await expect(assembler).toBeVisible({ timeout: 20_000 });
    expect(
      (await assembler.innerText()).trim().length,
      "created template assembler defaults",
    ).toBeGreaterThan(0);

    // REST cleanup: DELETE the created template, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openTemplatesCatalog(page);
    await expect(page.locator(`tr[data-tpl-name="${name}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
