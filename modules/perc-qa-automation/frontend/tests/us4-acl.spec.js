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
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function flattenPrincipals(raw) {
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw && Array.isArray(raw.Principal)) {
    return raw.Principal;
  }
  if (raw && raw.Principal) {
    return [raw.Principal];
  }
  return [];
}

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

  test("Explorer shell security toggle opens panel or folder-select hint (#2410)", async ({
    page,
  }) => {
    // Product path: spa.jsp?entry=explorer — ADMIN opens folder security
    // from view tools. Without a resolved folder id the shell shows a
    // polite hint; with a tree folder the panel mounts (folderProperties).
    const explorerUrl = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
    await page.goto(explorerUrl, { waitUntil: "networkidle" });
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="explorer-menu-view"]').click();
    const toggle = page.locator('[data-testid="explorer-toggle-security"]');
    await expect(toggle).toBeVisible({ timeout: 10_000 });
    await toggle.click();
    // Either the security panel region or the select-folder hint is shown.
    const panelOrHint = page.locator(
      '[data-testid="explorer-security-panel"], [data-testid="explorer-security-hint"]',
    );
    await expect(panelOrHint.first()).toBeVisible({ timeout: 15_000 });
  });

  test("Explorer shell security surface has no miller-column chrome (#2410)", async ({
    page,
  }) => {
    const explorerUrl = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
    await page.goto(explorerUrl, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="content-explorer-shell"]'),
    ).toBeVisible({ timeout: 15_000 });
    await page.locator('[data-testid="explorer-menu-view"]').click();
    await expect(page.locator('[data-testid="explorer-toggle-security"]')).toBeVisible({
      timeout: 10_000,
    });
    await page.locator('[data-testid="explorer-toggle-security"]').click();
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  /**
   * #2749 — when a real folderId is supplied on the pilot host, the panel
   * must not surface a server "validated object is null" hard failure as the
   * only outcome. Loading, ready panel, or a structured error (invalid id)
   * are all acceptable; the security panel region must remain mounted.
   */
  test("REST: Folders child folderProperties lists ROLE identities and locale (#3206)", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    const itemRes = await request.get(
      `${BASE_URL}/Rhythmyx/services/pathmanagement/path/item/Sites`,
      { headers },
    );
    const itemText = await itemRes.text();
    expect(
      itemRes.status(),
      `findItemByPath Sites: ${itemText.slice(0, 400)}`,
    ).toBe(200);
    const itemBody = JSON.parse(itemText);
    const item = itemBody.PathItem || itemBody;
    expect(item.id, itemText.slice(0, 400)).toBeTruthy();
    const propsRes = await request.get(
      `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folderProperties/${encodeURIComponent(item.id)}`,
      { headers },
    );
    const propsText = await propsRes.text();
    expect(
      propsRes.status(),
      `folderProperties: ${propsText.slice(0, 500)}`,
    ).toBe(200);
    expect(propsText).not.toContain("The validated object is null");
    const propsBody = JSON.parse(propsText);
    const props = propsBody.FolderProperties || propsBody;
    expect(props.name || props.id).toBeTruthy();
    const admins = flattenPrincipals(props.permission && props.permission.adminPrincipals);
    const names = admins.map((pr) => pr && pr.name).filter(Boolean);
    expect(
      names,
      `admin principals should include Admin role: ${propsText.slice(0, 800)}`,
    ).toContain("Admin");
  });

  test("Explorer Folder Security shows properties and Admin identity (#3206)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource/i.test(text)) {
        return;
      }
      pageErrors.push(text);
    });

    const explorerUrl = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
    await page.goto(explorerUrl, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="content-explorer-shell"]'),
    ).toBeVisible({ timeout: 20_000 });
    const tree = page.locator('[data-testid="explorer-tree"]');
    await expect(tree).toBeVisible({ timeout: 15_000 });

    const foldersNode = tree.locator(
      '[data-testid="tree-node-/Folders/"], [data-testid="tree-node-/Folders"]',
    );
    await expect(foldersNode.first()).toBeVisible({ timeout: 20_000 });
    await foldersNode.first().locator('[role="treeitem"]').click();
    const systemNode = tree.locator(
      '[data-testid="tree-node-/Folders/$/"], [data-testid="tree-node-/Folders/$"]',
    );
    const systemRow = page.locator('[data-testid="detail-row-16777215-101-4"]');
    if ((await systemNode.count()) > 0) {
      await systemNode.first().locator('[role="treeitem"]').click();
    } else if ((await systemRow.count()) > 0) {
      await systemRow.first().click();
    } else {
      const anyRow = page.locator('[data-testid^="detail-row-"]');
      await expect(anyRow.first()).toBeVisible({ timeout: 15_000 });
      await anyRow.first().click();
    }

    await page.locator('[data-testid="explorer-menu-view"]').click();
    const toggle = page.locator('[data-testid="explorer-toggle-security"]');
    await expect(toggle).toBeVisible({ timeout: 10_000 });
    await toggle.click();

    const panel = page.locator('[data-testid="folder-security-panel"]');
    await expect(panel).toBeVisible({ timeout: 25_000 });
    await expect(page.locator('[data-testid="folder-properties"]')).toBeVisible();
    await expect(page.locator('[data-testid="folder-props-locale"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="folder-security-list-adminPrincipals"]'),
    ).toBeVisible();
    const adminRemove = page.locator(
      '[data-testid="folder-security-list-adminPrincipals-remove-Admin"]',
    );
    await expect(adminRemove).toBeVisible();

    const locale = page.locator('[data-testid="folder-props-locale"]');
    const currentLocale = await locale.inputValue();
    const nextLocale = currentLocale || "en-us";
    await locale.fill(`${nextLocale}-x`);
    await expect(page.locator('[data-testid="folder-security-dirty"]')).toHaveText(
      "●",
    );
    await locale.fill(nextLocale);
    const save = page.locator('[data-testid="folder-security-save"]');
    await expect(save).toBeEnabled();
    await save.click();
    await expect(page.locator('[data-testid="folder-security-error"]')).toHaveCount(
      0,
    );
    await expect(page.locator('[data-testid="folder-security-dirty"]')).toHaveText(
      "○",
      { timeout: 20_000 },
    );
    expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test("FolderSecurityPanel with folderId stays mounted (no chrome crash) (#2749)", async ({
    page,
  }) => {
    // Use a plausible legacy guid shape; H2 QA may 400/500 for unknown ids —
    // assert mount + absence of miller-column, not a successful ACL payload.
    await page.goto(aclUrl("16777215-101-4"), { waitUntil: "networkidle" });
    const root = page.locator('[data-testid="perc-folder-security-root"]');
    await expect(root).toBeVisible({ timeout: 15_000 });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
    // Loading, ready panel, error, or no-access — any stable surface is OK.
    const surface = page.locator(
      [
        '[data-testid="folder-security-loading"]',
        '[data-testid="folder-security-panel"]',
        '[data-testid="folder-security-error"]',
        '[data-testid="folder-security-no-access"]',
      ].join(", "),
    );
    await expect(surface.first()).toBeVisible({ timeout: 20_000 });
  });
});
