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
 * Developer slots catalog readback (#5954 / parent #1690, slice 142).
 *
 * Proves the assembly slot list/detail renders from the thin-read API in
 * the React shell: the catalog table reads back through
 * GET /services/slots (label, name, description cells — no finder,
 * relationship, or association payload), and the detail panel reads back
 * through GET /services/slots/{idOrName} (title, committed name + GUID,
 * label text, description text, slot type).
 *
 * Test 1 is zero-write: it opens the first packaged catalog row and asserts
 * list + detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one slot through the shell, proves name/label/description
 * readback in the catalog and detail, then removes it with REST DELETE
 * cleanup and proves catalog omission.
 *
 * Readback only, no finder/association mutation; out of scope: slices
 * 143/144.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-slots-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * REST-safe unique slot name: alphanumeric only (slot names allow no
 * whitespace or wildcards).
 */
function uniqueSlotName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QASlot5954${label}${suffix || "rb"}`;
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

async function openSlotsCatalog(page) {
  await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="tab-developer-slots"]')).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-slot-panel"]');
  const empty = page.locator('[data-testid="developer-slot-empty"]');
  const listError = page.locator('[data-testid="developer-slot-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer slots catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer slots catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer slots catalog readback (#5954)", () => {
  test("packaged slot list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    // Thin-read catalog: first row renders the label open button plus the
    // name and description cells.
    const firstRow = page.locator(catalogRowSelector("developer-slot-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator('[data-testid="developer-slot-open"]');
    await expect(openBtn).toBeVisible();
    const rowName = await openBtn.getAttribute("data-slot-name");
    expect(rowName, "catalog row carries data-slot-name").toBeTruthy();
    await expect(firstRow).toContainText(rowName);
    const rowLabel = (await openBtn.getAttribute("data-slot-label")) || "";
    const descCell = page.locator(
      `[data-testid="developer-slot-catalog-description"][data-slot-name="${rowName}"]`,
    );
    await expect(descCell).toBeAttached({ timeout: 15_000 });
    const rowDescription = (await descCell.getAttribute("data-slot-description")) || "";

    // Thin-read REST probe for the same row: GET /services/slots contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/slots", "GET");
    expect(
      probe.status,
      `thin-read list should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read list contains the packaged slot").toContain(rowName);

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/slots/${encodeURIComponent(rowName)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the slot").toContain(rowName);

    // Detail readback in the shell: title, committed name + GUID, label and
    // description text, slot type.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-slot-detail-title"]')).toContainText(
      rowLabel || rowName,
      { timeout: 20_000 },
    );
    const committed = page.locator('[data-testid="developer-slot-committed-name"]');
    await expect(committed).toContainText(rowName);
    const committedText = await committed.innerText();
    expect(committedText, `committed name shows a GUID: ${committedText}`).toMatch(
      /\d+-\d+-\d+/,
    );
    const labelText = page.locator('[data-testid="developer-slot-set-label-text"]');
    await expect(labelText).toBeAttached({ timeout: 20_000 });
    if (rowLabel) {
      await expect(labelText).toContainText(rowLabel);
    }
    const descText = page.locator('[data-testid="developer-slot-set-description-text"]');
    await expect(descText).toBeAttached({ timeout: 20_000 });
    if (rowDescription) {
      await expect(descText).toContainText(rowDescription);
    }
    const typeValue = page.locator('[data-testid="developer-slot-type-value"]');
    await expect(typeValue).toBeVisible({ timeout: 20_000 });
    expect((await typeValue.innerText()).trim().length, "slot type reads back").toBeGreaterThan(
      0,
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created slot name, label, and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    const name = uniqueSlotName("readback");
    const label = `QA 5954 slots readback ${name.slice(-4)}`;
    const description = "qa5954 slots catalog readback";

    // Shell create (POST /services/slots).
    await page.locator('[data-testid="developer-slot-new"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-slot-name"]').fill(name);
    await page.locator('[data-testid="developer-slot-label"]').fill(label);
    await page.locator('[data-testid="developer-slot-description"]').fill(description);
    await page.locator('[data-testid="developer-slot-save"]').click();
    const notice = page.locator('[data-testid="developer-slot-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    // Detail readback: title shows the label, committed name + GUID,
    // label/description text echo the create payload.
    await expect(page.locator('[data-testid="developer-slot-detail-title"]')).toHaveText(
      label,
    );
    const committed = page.locator('[data-testid="developer-slot-committed-name"]');
    await expect(committed).toContainText(name);
    const committedText = await committed.innerText();
    expect(committedText, `detail header shows a GUID: ${committedText}`).toMatch(
      /\d+-\d+-\d+/,
    );
    await expect(page.locator('[data-testid="developer-slot-set-label-text"]')).toHaveText(
      label,
    );
    await expect(
      page.locator('[data-testid="developer-slot-set-description-text"]'),
    ).toHaveText(description);

    // Catalog readback: row for the created slot with its description.
    await page.locator('[data-testid="developer-slot-back"]').click();
    const openSel = `[data-testid="developer-slot-open"][data-slot-name="${name}"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });
    const row = page.locator(`tr:has(${openSel})`);
    await expect(row).toContainText(description, { timeout: 20_000 });

    // REST cleanup: DELETE the created slot, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/slots/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openSlotsCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
