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
 * Developer → Sites catalog bind (#3198 / parent #3090).
 *
 * GET /services/sites must be HTTP 200. When the payload contains Site entries,
 * the SPA table must list at least one name (never silent empty). Empty JSON
 * list may show the empty state.
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/bugs/bug-3198-developer-sites-list.spec.js
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

function unwrapSiteList(body) {
  if (body == null) {
    return [];
  }
  if (Array.isArray(body)) {
    return body;
  }
  const nested = body.SiteList ?? body.siteList ?? body.Site ?? body.site;
  if (Array.isArray(nested)) {
    return nested.map((row) =>
      row && row.Site && typeof row.Site === "object" ? row.Site : row,
    );
  }
  if (nested && typeof nested === "object") {
    const inner = nested.Site ?? nested.site;
    if (Array.isArray(inner)) {
      return inner;
    }
    if (inner && typeof inner === "object") {
      return [inner];
    }
    if (nested.name || nested.baseUrl) {
      return [nested];
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
  if (row.guid && typeof row.guid.stringValue === "string") {
    return row.guid.stringValue.trim();
  }
  return "";
}

test.describe("Developer Sites list bind (#3198)", () => {
  test("REST: GET /services/sites is 200 with array or SiteList wrap", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
    };
    const url = `${BASE_URL}/Rhythmyx/services/sites`;
    const res = await request.get(url, { headers });
    expect(res.status(), `GET ${url} must be 2xx`).toBeGreaterThanOrEqual(200);
    expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

    const body = await res.json();
    const sites = unwrapSiteList(body);
    expect(Array.isArray(sites), `sites payload must unwrap to an array: ${JSON.stringify(body).slice(0, 400)}`).toBe(
      true,
    );
    if (sites.length > 0) {
      const firstName = siteName(sites[0]);
      expect(
        firstName,
        `first site must expose string name (not Optional bean): ${JSON.stringify(sites[0]).slice(0, 400)}`,
      ).toBeTruthy();
    }
  });

  test("SPA binds site rows when API has data; empty only when list is empty", async ({
    page,
  }) => {
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
      // Ignore static 404s (favicon / leftover hashed chunks) — not uncaught JS.
      if (/404|Failed to load resource/i.test(text) && /favicon|fonts|\.map$/i.test(loc + text)) {
        return;
      }
      if (/Failed to load resource: the server responded with a status of 404/i.test(text)) {
        return;
      }
      consoleErrors.push(loc ? `${text} (${loc})` : text);
    });

    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
    };
    const apiRes = await page.request.get(`${BASE_URL}/Rhythmyx/services/sites`, {
      headers,
    });
    expect(apiRes.status()).toBeGreaterThanOrEqual(200);
    expect(apiRes.status()).toBeLessThan(300);
    const apiSites = unwrapSiteList(await apiRes.json());
    const apiHasRows = apiSites.some((row) => siteName(row));

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
