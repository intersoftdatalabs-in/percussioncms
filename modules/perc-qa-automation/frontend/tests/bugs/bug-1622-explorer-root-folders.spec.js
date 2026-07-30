/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Regression: Content Explorer root folder tree empty (GH-1622 / encodePath).
 *
 * <p>encodePath used to preserve leading slashes on CMS paths like
 * {@code /} and {@code /Sites/}, producing {@code folder//} and
 * {@code folder//Sites} which the pathmanagement service rejects with
 * HTTP 400. The React tree then rendered with no children.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: root {@code path/folder/} returns 200 with well-known roots</li>
 *   <li>REST: double-slash {@code path/folder//Sites} must not be the
 *       client contract (assert correct Sites URL works)</li>
 *   <li>UI: explorer tree is non-empty after shell mount (requires WebUI
 *       with encodePath fix deployed to the CMS install)</li>
 * </ul>
 *
 * <p>Run against a live CMS (e.g. {@code C:\Installs\8.2-july-29} or docker):</p>
 * <pre>
 *   cd modules/perc-qa-automation/frontend
 *   npm test -- tests/bugs/bug-1622-explorer-root-folders.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;

test.describe("GH-1622 explorer root folders (encodePath / no double-slash)", () => {
  test("REST: root folder/ returns well-known children (not folder//)", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    const root = await request.get(`${PATH_FOLDER}/`, { headers });
    expect(
      root.status(),
      `GET ${PATH_FOLDER}/ should be 200 (double-slash form is 400)`
    ).toBe(200);

    const sites = await request.get(`${PATH_FOLDER}/Sites`, { headers });
    // Sites may be empty on a fresh install, but the path must be valid.
    expect(
      sites.status(),
      `GET ${PATH_FOLDER}/Sites must not 400 (folder//Sites was the bug)`
    ).toBe(200);

    const bad = await request.get(`${PATH_FOLDER}//Sites`, { headers });
    // Document the server contract the SPA must avoid.
    expect(bad.status()).toBe(400);
  });

  test("UI: Content Explorer tree is not empty at root", async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 15_000 });

    const tree = page.locator('[data-testid="explorer-tree"]');
    await expect(tree).toBeVisible({ timeout: 15_000 });

    // Surface API/load failures explicitly (install lag still has folder// → 400).
    // Prefer data-testid after WebUI fix; also match older alert chrome.
    const treeErr = page.locator(
      '[data-testid="explorer-tree-error"], [data-testid="explorer-tree"] [role="alert"]'
    );
    if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
      const text = await treeErr.first().innerText();
      throw new Error(
        `Explorer tree failed to load: ${text}. If the network URL was path/folder//, redeploy WebUI with encodePath fix (#1680).`
      );
    }

    // Root children render as tree-node-* rows (paths like /Sites/, /Assets/).
    const nodes = tree.locator('[data-testid^="tree-node-"]');
    await expect(nodes.first()).toBeVisible({ timeout: 15_000 });
    const count = await nodes.count();
    expect(count, "explorer tree should list root folders").toBeGreaterThan(0);
  });
});
