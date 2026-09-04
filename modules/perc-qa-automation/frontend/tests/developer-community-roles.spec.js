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
 * Developer Communities SE-02 role assign / unassign (#4267 / parent #1690).
 *
 * Admin creates a community, assigns a system role via the membership table,
 * saves, then unassigns and clears. Asserts SPA chrome + GET roleList.
 *
 * Consumes REST/SPA tips #4272 / #4273 (unwrap PUT response; role identity).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-community-roles.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique community name; spaces are allowed by REST. */
function uniqueCommunityName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QA 4267 ${suffix || "roles"}`;
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

async function openCommunitiesCatalog(page) {
  await page.goto(developerCommunitiesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-comm-panel"]');
  const empty = page.locator('[data-testid="developer-comm-empty"]');
  const listError = page.locator('[data-testid="developer-comm-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer communities catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-comm-new"]')).toBeVisible();
}

async function createCommunity(page, name) {
  await page.locator('[data-testid="developer-comm-new"]').click();
  await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-comm-name"]').fill(name);
  await page.locator('[data-testid="developer-comm-create"]').click();
  const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-comm-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-comm-roles-save"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-comm-roles-table"]')).toBeVisible({
    timeout: 20_000,
  });
}

/**
 * Pick a role row that is currently unchecked (prefer Editor, else first unchecked).
 * @returns {Promise<{ check: import("@playwright/test").Locator, key: string, roleName: string }>}
 */
async function pickUncheckedRole(page) {
  const rows = page.locator('[data-testid="developer-comm-roles-table"] tbody tr');
  const count = await rows.count();
  expect(count, "roles table should list system roles").toBeGreaterThan(0);

  let editorCandidate = null;
  let firstUnchecked = null;
  for (let i = 0; i < count; i++) {
    const row = rows.nth(i);
    const check = row.locator('input[type="checkbox"][data-testid^="developer-comm-role-check-"]');
    await expect(check).toBeVisible();
    const testId = await check.getAttribute("data-testid");
    const key = (testId || "").replace("developer-comm-role-check-", "");
    const roleName = (await row.locator("td").nth(1).innerText()).trim();
    const checked = await check.isChecked();
    if (!checked) {
      const candidate = { check, key, roleName };
      if (!firstUnchecked) firstUnchecked = candidate;
      if (/^Editor$/i.test(roleName)) {
        editorCandidate = candidate;
        break;
      }
    }
  }
  const chosen = editorCandidate || firstUnchecked;
  expect(
    chosen,
    "expected at least one unchecked role to assign (H2 should ship Editor)",
  ).toBeTruthy();
  return chosen;
}

async function saveRolesAndAssertOk(page, expectedCountRegex) {
  const saveBtn = page.locator('[data-testid="developer-comm-roles-save"]');
  await expect(saveBtn).toBeEnabled();
  const putWait = page.waitForResponse(
    (r) =>
      /\/services\/communities\/[^/]+\/roles/i.test(r.url()) &&
      r.request().method() === "PUT",
    { timeout: 30_000 },
  );
  await saveBtn.click();
  const putResp = await putWait;
  const putReqBody = putResp.request().postData() || "";
  const putStatus = putResp.status();
  const putText = await putResp.text();
  const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
  const err = page.locator('[data-testid="developer-comm-detail-error"]');
  await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
  if (await err.isVisible()) {
    throw new Error(
      `Save roles failed: ${(await err.innerText()).trim()} | PUT ${putStatus} req=${putReqBody} resp=${putText}`,
    );
  }
  await expect(
    notice,
    `PUT ${putStatus} req=${putReqBody} resp=${putText}`,
  ).toContainText(expectedCountRegex);
  await expect(page.locator('[data-testid="developer-comm-roles-dirty"]')).toHaveCount(0);
}

async function inPageCommunityGet(page, name) {
  return page.evaluate(async (communityName) => {
    const tokenObj = window.OWASP_CSRFTOKEN;
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const token =
      (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
    const headerName =
      (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
    const headers = { Accept: "application/json" };
    if (token) {
      headers[headerName] = token;
    }
    const res = await fetch(
      `/Rhythmyx/services/communities/${encodeURIComponent(communityName)}`,
      { method: "GET", credentials: "same-origin", headers },
    );
    const text = await res.text();
    return { status: res.status, text };
  }, name);
}

function roleNamesFromCommunityJson(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch (e) {
    throw new Error(`Community GET not JSON: ${text.slice(0, 400)}`);
  }
  const root = parsed.Community || parsed.community || parsed;
  const raw = root.roleList || root.RoleList || [];
  let list;
  if (Array.isArray(raw)) {
    list = raw;
  } else if (raw && typeof raw === "object") {
    const nested = raw.CommunityRole || raw.communityRole || raw.Role;
    if (Array.isArray(nested)) {
      list = nested;
    } else if (nested && typeof nested === "object") {
      list = [nested];
    } else if (raw.roleName != null || raw.roleId != null || raw.roleGuid != null) {
      // Jackson one-item list: roleList is a bare CommunityRole object.
      list = [raw];
    } else {
      list = [];
    }
  } else {
    list = [];
  }
  return list
    .map((r) => (r && (r.roleName || r.RoleName || r.name)) || "")
    .map((n) => String(n).trim())
    .filter(Boolean);
}

async function deleteCurrentCommunity(page) {
  await page.locator('[data-testid="developer-comm-delete"]').click();
  await confirmDeveloperCatalogDelete(page);
  await expect(page.locator('[data-testid="developer-comm-panel"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer community roles SE-02 (#4267)", () => {
  test("Admin assigns then unassigns a role on a community", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName();
    await createCommunity(page, name);

    // Baseline: newly created community typically has no memberships checked.
    const toAssign = await pickUncheckedRole(page);
    await toAssign.check.check();
    await expect(toAssign.check).toBeChecked();
    await expect(page.locator('[data-testid="developer-comm-roles-dirty"]')).toBeVisible();

    await saveRolesAndAssertOk(page, /1 roles/i);
    await expect(toAssign.check).toBeChecked();

    const afterAssign = await inPageCommunityGet(page, name);
    expect(
      afterAssign.status,
      `GET after assign (got ${afterAssign.status}): ${afterAssign.text}`,
    ).toBe(200);
    const assignedNames = roleNamesFromCommunityJson(afterAssign.text);
    expect(
      assignedNames.join(" "),
      `GET roleList should include ${toAssign.roleName}: ${afterAssign.text}`,
    ).toMatch(new RegExp(toAssign.roleName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i"));

    // Unassign the same role (omit from full-set replace → empty).
    await toAssign.check.uncheck();
    await expect(toAssign.check).not.toBeChecked();
    await expect(page.locator('[data-testid="developer-comm-roles-dirty"]')).toBeVisible();
    await saveRolesAndAssertOk(page, /0 roles/i);
    await expect(toAssign.check).not.toBeChecked();

    const afterUnassign = await inPageCommunityGet(page, name);
    expect(
      afterUnassign.status,
      `GET after unassign (got ${afterUnassign.status}): ${afterUnassign.text}`,
    ).toBe(200);
    const clearedNames = roleNamesFromCommunityJson(afterUnassign.text);
    expect(
      clearedNames,
      `GET roleList should be empty after unassign, got: ${afterUnassign.text}`,
    ).toEqual([]);

    await deleteCurrentCommunity(page);
    await expect(page.locator(`[data-comm-name="${name}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
