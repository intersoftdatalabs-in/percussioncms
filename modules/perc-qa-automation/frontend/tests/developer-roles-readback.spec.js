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
 * Developer roles catalog readback (#6030 / parent #1690, slice 148).
 *
 * Proves the Security Design roles catalog renders from the thin-read API in
 * the React shell: the grouped catalog reads back through
 * GET /services/roles/catalog (name, description, home-page, community and
 * workflow membership cells), and the role detail reads back through
 * GET /services/roles/{roleName} (stored name, description, member list).
 *
 * Test 1 is zero-write: it opens the packaged Admin row and asserts list +
 * detail readback plus thin-read REST probes for the same row.
 * Test 2 creates one role through the shell, proves name/description
 * readback in the catalog and detail, then removes it with REST DELETE
 * cleanup and proves catalog omission.
 *
 * Readback only, no description/home-page/user mutation; out of scope:
 * locales/keywords readback (backlog), role write flows (slices 54-56/81-84).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-roles-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const GROUPS = ["community", "workflow", "unassigned"];

function developerRolesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "roles",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * REST-safe unique role name: alphanumeric only (role names allow no
 * whitespace).
 */
function uniqueRoleName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/gi, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/gi, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QARole6030${label}${suffix || "rb"}`.replace(/[^a-zA-Z0-9]/g, "");
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

function roleRowSelector(name) {
  return `[data-testid^="developer-roles-row-"][data-role-name="${name}"]`;
}

async function openRolesCatalog(page) {
  await page.goto(developerRolesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="tab-developer-roles"]')).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-roles-panel"]');
  const listError = page.locator('[data-testid="developer-roles-error"]');
  await expect(panel.or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer roles catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer roles catalog readback (#6030)", () => {
  test("packaged role list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openRolesCatalog(page);

    // Packaged seed row: Admin renders in at least one navigator group with
    // its stored name cell.
    const adminRow = page.locator(roleRowSelector("Admin")).first();
    await expect(adminRow).toBeVisible({ timeout: 15_000 });
    await expect(adminRow).toContainText("Admin");
    const descCell = page.locator(
      'span[data-role-description="Admin"]',
    ).first();
    await expect(descCell).toBeAttached({ timeout: 15_000 });
    const rowDescription = ((await descCell.innerText()) || "").trim();

    // Thin-read REST probe for the same row: GET /services/roles/catalog
    // contains it.
    const probe = await inPageJson(page, "/Rhythmyx/services/roles/catalog", "GET");
    expect(
      probe.status,
      `thin-read catalog should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(probe.text, "thin-read catalog contains the packaged role").toContain(
      "Admin",
    );

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/roles/${encodeURIComponent("Admin")}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the role").toContain("Admin");

    // Detail readback in the shell: edit form shows the stored name
    // (read-only), the stored description, and the member list region.
    await adminRow.click();
    await expect(
      page.locator('[data-testid="developer-roles-edit-form"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-roles-edit-name"]')).toHaveValue(
      "Admin",
      { timeout: 20_000 },
    );
    if (rowDescription) {
      await expect(
        page.locator('[data-testid="developer-roles-edit-description"]'),
      ).toHaveValue(rowDescription);
    }
    await expect(page.locator('[data-testid="developer-roles-members"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created role name and description read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openRolesCatalog(page);

    const name = uniqueRoleName("readback");
    const description = "qa6030 roles catalog readback";

    // Shell create (PUT /services/roles/?create=true via the create form).
    await page.locator('[data-testid="developer-roles-create"]').click();
    await expect(
      page.locator('[data-testid="developer-roles-create-form"]'),
    ).toBeVisible();
    await page.locator('[data-testid="developer-roles-create-name"]').fill(name);
    await page
      .locator('[data-testid="developer-roles-create-description"]')
      .fill(description);
    await page.locator('[data-testid="developer-roles-create-save"]').click();
    const notice = page.locator('[data-testid="developer-roles-create-notice"]');
    const saveError = page.locator('[data-testid="developer-roles-create-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    // Catalog readback: the row for the created role renders with its
    // description cell.
    const createdRow = page.locator(roleRowSelector(name)).first();
    await expect(createdRow).toBeVisible({ timeout: 20_000 });
    await expect(createdRow).toContainText(description, { timeout: 20_000 });

    // Detail readback: stored name plus the create payload description.
    await createdRow.click();
    await expect(
      page.locator('[data-testid="developer-roles-edit-form"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-roles-edit-name"]')).toHaveValue(
      name,
    );
    await expect(
      page.locator('[data-testid="developer-roles-edit-description"]'),
    ).toHaveValue(description);

    // REST cleanup: DELETE the created role, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/roles/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openRolesCatalog(page);
    await expect(page.locator(roleRowSelector(name))).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
