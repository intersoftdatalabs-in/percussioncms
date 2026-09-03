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
 * Regression: editor toolbar icon 404s leave chrome stuck loading (GH-3332 / parent #3329).
 *
 * Human QA reported:
 *   GET /cm/pages/app/images/icons/editor/delete.png 404
 *   GET /cm/pages/app/images/icons/editor/edit.png 404
 *
 * Those URLs must 200 (remap onto /cm/images/...) and Explorer/Editor chrome
 * must finish loading.
 *
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=... TEST_DB_TYPE=h2 \
 *     npm run test:surface -- --path tests/bugs/bug-3332-editor-icons.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("../helpers/auth");

const ICON_NAMES = ["delete.png", "edit.png"];

function explorerUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

function editorUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=editor&_=${Date.now()}`;
}

function canonicalIconUrl(name) {
  return `${BASE_URL}/Rhythmyx/cm/images/icons/editor/${name}`;
}

test.describe("GH-3332 editor icons + chrome not stuck loading", () => {
  test("REST: canonical editor icons are 200 PNG", async ({ request }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    for (const name of ICON_NAMES) {
      const url = canonicalIconUrl(name);
      const res = await request.get(url, { headers });
      expect(res.status(), `${url} must be 200 (not 404).`).toBe(200);
      const ctype = (res.headers()["content-type"] || "").toLowerCase();
      expect(
        ctype,
        `${url} content-type should be an image, got ${ctype}`,
      ).toMatch(/image|octet-stream|png/);
    }
  });

  test("UI: Explorer shell finishes loading without editor-icon 404s", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    const icon404s = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("response", (res) => {
      const url = res.url();
      if (
        res.status() === 404 &&
        /\/images\/icons\/editor\/(delete|edit)\.png(?:\?|$)/i.test(url)
      ) {
        icon404s.push(url);
      }
    });

    await loginAsAdmin(page);
    await page.goto(explorerUrl(), { waitUntil: "domcontentloaded" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="explorer-route-loading"]')).toHaveCount(
      0,
    );
    await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(icon404s, `editor icon 404s: ${icon404s.join(", ")}`).toEqual([]);
    const unexpected = pageErrors.filter(
      (t) =>
        !/Failed to load resource/i.test(t) &&
        !/ResizeObserver/i.test(t),
    );
    expect(unexpected, unexpected.join("\n")).toEqual([]);
  });

  test("UI: Editor view does not stay on loading overlay after icon chrome", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const icon404s = [];
    page.on("response", (res) => {
      const url = res.url();
      if (
        res.status() === 404 &&
        /\/images\/icons\/editor\/(delete|edit)\.png(?:\?|$)/i.test(url)
      ) {
        icon404s.push(url);
      }
    });

    await loginAsAdmin(page);
    await page.goto(editorUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("editor-host")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page).not.toHaveURL(/view=editor/);
    expect(icon404s, `editor icon 404s: ${icon404s.join(", ")}`).toEqual([]);
  });
});
