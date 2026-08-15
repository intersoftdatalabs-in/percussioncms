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
 * Developer → Sites catalog bind residual (#3368 / parent #3129 / #3090).
 *
 * After #3198, GET /services/sites can still be HTTP 200 with a Jackson
 * ArrayList bean ({empty:false}) or an empty SiteList while
 * GET /sitemanage/site/ has SiteSummary rows. The SPA must list those sites.
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/bugs/bug-3368-developer-sites-list.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const { catalogRowsSelector } = require("../helpers/developer-catalog-selectors");

function developerSitesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "sites",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapSiteRows(body) {
  if (body == null) {
    return [];
  }
  if (Array.isArray(body)) {
    return body;
  }
  if (typeof body !== "object") {
    return [];
  }
  if (body.empty === true && Object.keys(body).length === 1) {
    return [];
  }
  const nested =
    body.SiteList ??
    body.siteList ??
    body.SiteSummary ??
    body.siteSummary ??
    body.Site ??
    body.site ??
    body.sites ??
    body.item;
  if (Array.isArray(nested)) {
    return nested.map((row) =>
      row && row.Site && typeof row.Site === "object" ? row.Site : row,
    );
  }
  if (nested && typeof nested === "object") {
    const inner =
      nested.Site ??
      nested.site ??
      nested.sites ??
      nested.SiteSummary ??
      nested.item;
    if (Array.isArray(inner)) {
      return inner;
    }
    if (inner && typeof inner === "object" && (inner.name || inner.baseUrl)) {
      return [inner];
    }
    if (nested.name || nested.baseUrl) {
      return [nested];
    }
    if (nested.empty === true) {
      return [];
    }
  }
  return [];
}

function siteName(row) {
  if (!row || typeof row !== "object") {
    return "";
  }
  if (typeof row.name === "string") {
    return row.name.trim();
  }
  if (row.name && typeof row.name.value === "string") {
    return row.name.value.trim();
  }
  if (row.guid && typeof row.guid.stringValue === "string") {
    return row.guid.stringValue.trim();
  }
  if (typeof row.guid === "string") {
    return row.guid.trim();
  }
  return "";
}

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
}

async function fetchList(request, path) {
  const url = `${BASE_URL}${path}`;
  const res = await request.get(url, { headers: jsonHeaders() });
  const contentType = res.headers()["content-type"] || "";
  let body = null;
  try {
    body = await res.json();
  } catch {
    body = await res.text();
  }
  return { url, status: res.status(), contentType, body, rows: unwrapSiteRows(body) };
}

test.describe("Developer Sites list bind (#3368)", () => {
  test("REST: /services/sites is array-shaped; not an empty-false bean", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const sites = await fetchList(request, "/Rhythmyx/services/sites");
    expect(sites.status, `GET ${sites.url} must be 2xx`).toBeGreaterThanOrEqual(200);
    expect(sites.status, `GET ${sites.url} must not be error`).toBeLessThan(300);
    const raw = JSON.stringify(sites.body).slice(0, 500);
    const asBean =
      sites.body &&
      typeof sites.body === "object" &&
      !Array.isArray(sites.body) &&
      (sites.body.empty === false ||
        (sites.body.SiteList &&
          typeof sites.body.SiteList === "object" &&
          !Array.isArray(sites.body.SiteList) &&
          sites.body.SiteList.empty === false));
    expect(asBean, `SiteList must not serialize as {empty:false} bean: ${raw}`).toBeFalsy();
    expect(
      Array.isArray(sites.rows),
      `sites payload must unwrap to an array: ${raw}`,
    ).toBe(true);
    if (sites.rows.length > 0) {
      expect(
        siteName(sites.rows[0]),
        `first site must expose string name: ${JSON.stringify(sites.rows[0]).slice(0, 400)}`,
      ).toBeTruthy();
    }
  });

  test("SPA lists sites when either REST or sitemanage has rows", async ({ page }) => {
    test.setTimeout(90_000);
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      const loc = msg.location() && msg.location().url ? msg.location().url : "";
      if (/404|Failed to load resource/i.test(text) && /favicon|fonts|\.map$/i.test(loc + text)) {
        return;
      }
      if (/Failed to load resource: the server responded with a status of 404/i.test(text)) {
        return;
      }
      consoleErrors.push(loc ? `${text} (${loc})` : text);
    });

    const restSites = await fetchList(page.request, "/Rhythmyx/services/sites");
    const smSites = await fetchList(page.request, "/Rhythmyx/services/sitemanage/site/");
    const restNamed = restSites.rows.filter((row) => siteName(row));
    const smNamed = smSites.rows.filter((row) => siteName(row));
    const apiHasRows = restNamed.length > 0 || smNamed.length > 0;

    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    if (apiHasRows) {
      await expect(page.locator('[data-testid="developer-site-panel"]')).toBeVisible();
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      expect(await rows.count()).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="developer-site-empty"]')).toHaveCount(0);
    } else {
      await expect(page.locator('[data-testid="developer-site-empty"]')).toBeVisible();
    }

    expect(consoleErrors, `JS console/page errors: ${consoleErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});
