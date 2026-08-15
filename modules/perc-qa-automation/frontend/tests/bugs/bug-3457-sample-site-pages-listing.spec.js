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
 * GH-3457 / parent #2745: sample-site Pages (and Files) folders must list
 * children so Explorer is not stuck on "No items in this folder".
 *
 * REST gate uses the same demo-sites skip/enforce pattern as bug-1750.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const {
  pathItemNames,
  pagedItemListChildren,
  pagedItemListCount,
  isPageTypeChild,
  hasAnyExpectedSampleSite,
  shouldEnforceDemoSites,
  demoSitesSkipReason,
  normalizeSiteName,
} = require("../helpers/demo-sites");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;
const PATH_PAGED = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/paginatedFolder`;

function gateSampleSitesOrSkip(names) {
  if (hasAnyExpectedSampleSite(names)) {
    return;
  }
  if (!shouldEnforceDemoSites()) {
    test.skip(demoSitesSkipReason());
    return;
  }
  expect(
    names,
    `Sites must list a sample site after demo-sites; got [${names.join(", ")}]`,
  ).not.toEqual([]);
}

function firstSampleSiteName(names) {
  const corporate = names.find(
    (n) => normalizeSiteName(n) === "corporate investments",
  );
  return corporate || names[0];
}

test.describe("GH-3457 sample site Pages/Files listing", () => {
  test("REST: paginatedFolder Sites/<sample>/Pages has a page-type child", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    const sitesRes = await request.get(`${PATH_FOLDER}/Sites`, { headers });
    expect(sitesRes.status()).toBe(200);
    const siteNames = pathItemNames(await sitesRes.json());
    gateSampleSitesOrSkip(siteNames);

    const site = firstSampleSiteName(siteNames);
    const pagesUrl = `${PATH_PAGED}/Sites/${encodeURIComponent(site)}/Pages?startIndex=0&maxResults=50`;
    const res = await request.get(pagesUrl, { headers });
    expect(res.status(), `GET ${pagesUrl}`).toBe(200);
    const body = await res.json();
    expect(
      pagedItemListCount(body),
      `Pages listing for ${site} must have childrenCount >= 1`,
    ).toBeGreaterThanOrEqual(1);
    const kids = pagedItemListChildren(body);
    expect(
      kids.some(isPageTypeChild),
      `Pages listing for ${site} must include a page-type child; got ${JSON.stringify(kids.map((k) => k && k.name))}`,
    ).toBe(true);
  });

  test("UI: opening sample-site Pages shows detail rows", async ({ page }) => {
    test.setTimeout(90_000);
    const headers = adminBasicAuthHeaders();
    const probe = await page.request.get(`${PATH_FOLDER}/Sites`, { headers });
    expect(probe.status()).toBe(200);
    const siteNames = pathItemNames(await probe.json());
    gateSampleSitesOrSkip(siteNames);
    const site = firstSampleSiteName(siteNames);

    await loginAsAdmin(page);
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const list = page.locator('[data-testid="detail-list"]');
    const pagesUrl = `${PATH_PAGED}/Sites/${encodeURIComponent(site)}/Pages?startIndex=0&maxResults=50`;
    const pagesRes = await page.request.get(pagesUrl, { headers });
    expect(pagesRes.status()).toBe(200);
    const pagesBody = await pagesRes.json();
    expect(pagedItemListCount(pagesBody)).toBeGreaterThanOrEqual(1);

    const siteNode = page.locator(
      `[data-testid="tree-node-/Sites/${site}/"], [data-testid="tree-node-/Sites/${site}"]`,
    );
    if ((await siteNode.count()) > 0) {
      await siteNode.first().click();
    }
    const pagesNode = page.locator(
      '[data-testid*="/Pages"]',
    ).first();
    if ((await pagesNode.count()) > 0) {
      await pagesNode.click();
    }

    await expect(list).toBeVisible({ timeout: 20_000 });
    const empty = list.getByText(/No items in this folder/i);
    await expect(empty).toHaveCount(0);
    const rows = page.locator('[data-testid^="detail-row-"]');
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
  });
});
