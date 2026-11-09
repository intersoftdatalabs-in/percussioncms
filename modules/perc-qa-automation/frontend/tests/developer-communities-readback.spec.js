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
 * Developer communities catalog readback (#5916 / parent #1690, slice 139).
 *
 * Proves the communities list/detail renders from the thin-read API in the
 * React shell: the catalog table reads back through
 * GET /services/communities/find?name=* (label, name, id, description — no
 * role payload), and the detail panel reads back through
 * GET /services/communities/{idOrName} (title, name, numeric id, GUID,
 * description, roles lens).
 *
 * Test 1 is zero-write: it opens the first packaged catalog row and asserts
 * list + detail readback plus a thin-read REST probe for the same row. Test 2
 * creates one community through the shell, proves name/description/GUID
 * readback in the catalog and detail, then removes it with REST DELETE
 * cleanup and proves catalog omission.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-communities-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique community name (spaces allowed, no wildcards). */
function uniqueCommunityName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QA 5916 ${label} ${suffix || "comm"}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @param {object} [body]
 * @param {Record<string, string>} [extraHeaders]
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method, body, extraHeaders) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload, headers: extra }) => {
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
      for (const [key, value] of Object.entries(extra || {})) {
        headers[key] = value;
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
    { path, method, body, headers: extraHeaders },
  );
}

/** Unwrap a Community DTO from find/detail envelopes. */
function unwrapCommunity(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return {};
  }
  if (parsed && typeof parsed === "object") {
    if (parsed.Community && typeof parsed.Community === "object") {
      return parsed.Community;
    }
    if (parsed.community && typeof parsed.community === "object") {
      return parsed.community;
    }
  }
  return parsed && typeof parsed === "object" ? parsed : {};
}

/** Unwrap a CommunityList payload from the thin-read find endpoint. */
function unwrapCommunityList(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return [];
  }
  if (Array.isArray(parsed)) {
    return parsed;
  }
  if (parsed && typeof parsed === "object") {
    for (const key of ["CommunityList", "Community", "communityList"]) {
      if (Array.isArray(parsed[key])) {
        return parsed[key];
      }
    }
  }
  return [];
}

function guidStringValue(community) {
  if (!community || typeof community !== "object") {
    return "";
  }
  const guid = community.guid;
  if (guid && typeof guid === "object" && guid.stringValue) {
    return String(guid.stringValue);
  }
  if (typeof community.guidString === "string" && community.guidString) {
    return community.guidString;
  }
  return "";
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
  if (await empty.isVisible()) {
    throw new Error("Developer communities catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
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
  await page.locator('[data-testid="developer-comm-back"]').click();
  await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer communities catalog readback (#5916)", () => {
  test("packaged community list and detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    // Thin-read catalog: first row renders label open button + name/id cells.
    const firstRow = page.locator(catalogRowSelector("developer-comm-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator('[data-testid="developer-comm-open"]');
    await expect(openBtn).toBeVisible();
    const rowName = await openBtn.getAttribute("data-comm-name");
    expect(rowName, "catalog row carries data-comm-name").toBeTruthy();
    await expect(firstRow).toContainText(rowName);
    const rowText = (await firstRow.innerText()).trim();
    expect(rowText.length, "catalog row renders thin-read cells").toBeGreaterThan(
      rowName.length,
    );

    // Thin-read REST probe for the same row: find?name=* contains it.
    const probe = await inPageJson(
      page,
      "/Rhythmyx/services/communities/find?name=%2A",
      "GET",
    );
    expect(
      probe.status,
      `thin-read find should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    const catalog = unwrapCommunityList(probe.text);
    const match = catalog.find((c) => c && c.name === rowName);
    expect(match, `thin-read list contains ${rowName}`).toBeTruthy();

    // Detail readback through GET /services/communities/{idOrName}.
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const title = page.locator('[data-testid="developer-comm-detail-title"]');
    await expect(title).toContainText(rowName, { timeout: 20_000 });
    const headerMeta = page.locator('[data-testid="developer-comm-detail"] header');
    await expect(headerMeta).toContainText(rowName);
    const guidText = await headerMeta.innerText();
    expect(guidText, `detail header shows a GUID: ${guidText}`).toMatch(
      /\d+-\d+-\d+/,
    );
    await expect(page.locator('[data-testid="developer-comm-description"]')).toBeVisible();
    // Roles lens is part of detail readback (picker or empty-state text).
    await expect(page.locator('[data-testid="developer-comm-roles"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created community name, description, and GUID read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName("readback");
    const description = "qa5916 communities catalog readback";
    await createCommunity(page, name);

    // Catalog readback: row for the created community.
    const openSel = `[data-testid="developer-comm-open"][data-comm-name="${name}"]`;
    await expect(page.locator(openSel)).toBeVisible({ timeout: 20_000 });

    // Detail readback: title, header meta, empty description text.
    await page.locator(openSel).click();
    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-comm-detail-title"]'),
    ).toHaveText(name);
    const headerMeta = page.locator('[data-testid="developer-comm-detail"] header');
    await expect(headerMeta).toContainText(name);
    const guidText = await headerMeta.innerText();
    expect(guidText, `detail header shows a GUID: ${guidText}`).toMatch(
      /\d+-\d+-\d+/,
    );

    // Description write then readback on detail and in the catalog.
    await page.locator('[data-testid="developer-comm-description-input"]').fill(description);
    await page.locator('[data-testid="developer-comm-description-save"]').click();
    await expect(page.locator('[data-testid="developer-comm-detail-notice"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-comm-description"]')).toHaveText(
      description,
    );
    await page.locator('[data-testid="developer-comm-back"]').click();
    const row = page.locator(`tr:has(${openSel})`);
    await expect(row).toContainText(description, { timeout: 20_000 });

    // REST cleanup: resolve the GUID then DELETE the bulk GuidList.
    const detail = await inPageJson(
      page,
      `/Rhythmyx/services/communities/${encodeURIComponent(name)}`,
      "GET",
    );
    expect(
      detail.status,
      `detail GET should be 200 (got ${detail.status}): ${detail.text.slice(0, 200)}`,
    ).toBe(200);
    const created = unwrapCommunity(detail.text);
    const createdGuid = guidStringValue(created);
    expect(createdGuid, `created guid missing: ${detail.text.slice(0, 200)}`).toMatch(
      /\d+-\d+-\d+/,
    );
    await expect(page.locator('[data-testid="developer-comm-detail-title"]')).toHaveCount(0);

    const del = await inPageJson(
      page,
      "/Rhythmyx/services/communities/bulk",
      "DELETE",
      { GuidList: [created.guid || { stringValue: createdGuid }] },
      { ignoredependencies: "false" },
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openCommunitiesCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
