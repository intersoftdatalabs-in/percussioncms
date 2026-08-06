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
 * Playwright spec: US4 P-ACL — folder security panel (SC-004 / FR-014, FR-015, FR-016).
 *
 * <p>Asserts the modern React {@code FolderSecurityPanel} mounts on
 * <code>folderSecurityModern.jsp</code> and exposes the wired
 * surface. The dev CMS image has no installed folder ACL data by
 * default, so SC-004's "edit / save / second-user effect" acceptance
 * is documented as gated on a system-installed CMS — exactly the
 * same coverage pattern used for {@code tests/us3-menus.spec.js}.</p>
 *
 * <p>The Vitest suites cover the structural surface
 * ({@code aclLockout.test.ts}, {@code FolderSecurityPanel.test.tsx});
 * this Playwright spec exercises the wiring against the live CMS
 * shell.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us4-acl.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

/**
 * Build the pilot page URL with a per-test cache-buster so consecutive
 * tests don't share a stale bundle. The cache-buster is a per-call
 * helper (rather than a module-load-time Date.now()) because all tests
 * in this file share the same URL prefix; the helper returns a fresh
 * value per invocation (kilocode-bot PR #1397 thread 3614415917).
 */
function aclUrl(folderId) {
  const ts = `?folderId=${encodeURIComponent(folderId || "")}&_=${Date.now()}`;
  return `${BASE_URL}/Rhythmyx/cm/app/folderSecurityModern.jsp${ts}`;
}

test.describe("US4 P-ACL — folder security panel (SC-004)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("FolderSecurityPanel pilot page renders the no-folder placeholder when ?folderId is missing", async ({
    page,
  }) => {
    await page.goto(aclUrl(), { waitUntil: "networkidle" });
    const placeholder = page.locator(
      '[data-testid="perc-folder-security-no-folder"]',
    );
    await expect(placeholder).toBeVisible({ timeout: 15_000 });
  });

  test("FolderSecurityPanel pilot page mounts the panel root and waits for a folderId", async ({
    page,
  }) => {
    await page.goto(aclUrl(), { waitUntil: "networkidle" });
    // The mount root is always rendered; the no-folder placeholder is
    // surfaced once PercModernUI.mount() discovers the missing id.
    const root = page.locator('[data-testid="perc-folder-security-root"]');
    await expect(root).toBeVisible({ timeout: 15_000 });
  });

  test("legacy miller-column Finder chrome is NOT loaded on the ACL host", async ({
    page,
  }) => {
    await page.goto(aclUrl(), { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("the pilot page title advertises US4 P-ACL", async ({ page }) => {
    await page.goto(aclUrl(), { waitUntil: "networkidle" });
    const title = await page.title();
    expect(/Folder Security|folderSecurity/i.test(title)).toBeTruthy();
  });

  test("adding ?folderId= triggers the panel mount path with the loading placeholder", async ({
    page,
  }) => {
    // Pin an id of "0" — the dev CMS returns 500 for non-existent ids
    // and that triggers the error placeholder path. The point of this
    // spec is to prove the mount wiring, not to validate the error
    // payload against a system-installed CMS.
    await page.goto(aclUrl("0"), { waitUntil: "networkidle" });
    // The mount kicks off a fetchFolderProperties(0); the panel
    // either renders the loading state or the error state — both are
    // acceptable for the dev CMS.
    const root = page.locator('[data-testid="perc-folder-security-root"]');
    await expect(root).toBeVisible({ timeout: 15_000 });
    // The legacy miller-column Finder chrome should still be absent.
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("axe-core a11y gate — FolderSecurityPanel (T082b)", async ({ page }) => {
    await page.goto(aclUrl("1"), { waitUntil: "networkidle" });
    const root = page.locator('[data-testid="perc-folder-security-root"]');
    await expect(root).toBeVisible({ timeout: 15_000 });
    await expectNoSeriousA11yViolations(page, {
      scope:
        '[data-testid="perc-folder-security-root"], [data-testid="folder-security-panel"]',
    });
  });
});
