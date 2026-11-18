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
 * Pretty SPA routes survive direct load/reload (authenticated) (#6578 / parent #1690).
 *
 * Before the fix, `PSWebUiSpaFallbackFilter` (REQUEST-only `/cm/app/*` mapping plus
 * no tolerance for the unstripped `/Rhythmyx` prefix in root-context cells) never
 * rescued `/cm/app/<entry>/<section>`, so an authenticated reload or bookmark of a
 * pretty route 404'd with the server "This page cannot be found" page — even though
 * the entry load rewrote `spa.jsp?entry=…&section=…` to that same pretty URL.
 *
 * Proves, for Developer → Keywords (long-shipped section, same failure as roles and
 * the newer api-keys section):
 *  1. `spa.jsp?entry=developer&section=keywords` still rewrites to the pretty route
 *     (rewrite regression guard).
 *  2. `page.reload()` on the pretty route boots the app (no 404).
 *  3. A fresh direct `page.goto()` of the pretty route boots the app (bookmark /
 *     shared-link equivalent).
 *
 * Refs #6578, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function developerKeywordsEntryUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "keywords",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function prettyKeywordsUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/developer/keywords`;
}

async function expectKeywordsBooted(page) {
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="tab-developer-keywords"]')).toBeVisible({
    timeout: 15_000,
  });
  const panel = page.locator('[data-testid="developer-kw-panel"]');
  const empty = page.locator('[data-testid="developer-kw-empty"]');
  const error = page.locator('[data-testid="developer-kw-error"]');
  await expect(panel.or(empty).or(error).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await error.isVisible()) {
    throw new Error(`Keywords panel error: ${(await error.innerText()).trim()}`);
  }
  // The server 404 page carries no SPA chrome; the developer nav proves the
  // fallback filter forwarded to spa.jsp instead of the CMS 404 page.
  await expect(page.locator("text=This page cannot be found")).toHaveCount(0);
}

test.describe("Pretty SPA route direct load/reload (#6578)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("entry URL rewrites pretty, reload and direct pretty load boot the app", async ({
    page,
  }) => {
    const consoleErrors = [];
    const pageErrors = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });

    // 1. Entry URL boots and rewrites to the pretty route (rewrite regression).
    await page.goto(developerKeywordsEntryUrl(), { waitUntil: "networkidle" });
    await expectKeywordsBooted(page);
    await expect(page).toHaveURL(/\/cm\/app\/developer\/keywords/, {
      timeout: 15_000,
    });

    // 2. Reload on the pretty route boots the app (was HTTP 404 pre-fix).
    await page.reload({ waitUntil: "networkidle" });
    await expectKeywordsBooted(page);
    await expect(page).toHaveURL(/\/cm\/app\/developer\/keywords/, {
      timeout: 15_000,
    });

    // 3. Fresh direct navigation to the pretty route boots the app
    // (bookmark / shared-link equivalent).
    await page.goto(prettyKeywordsUrl(), { waitUntil: "networkidle" });
    await expectKeywordsBooted(page);

    await page.screenshot({
      path: "test-results/live-proof-6578-pretty-route.png",
    });

    expect(pageErrors).toEqual([]);
    expect(consoleErrors).toEqual([]);
  });
});
