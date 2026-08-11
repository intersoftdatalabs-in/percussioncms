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
 * Regression: Content Explorer root folder tree empty (GH-1622 / encodePath).
 *
 * <p>encodePath used to preserve leading slashes on CMS paths like
 * {@code /} and {@code /Sites/}, producing {@code folder//} and
 * {@code folder//Sites} which the pathmanagement service rejects with
 * HTTP 400. The React tree then rendered with no children.</p>
 *
 * <p>Coverage (issue #1695 / parent #1690):</p>
 * <ul>
 *   <li>REST: root {@code path/folder/} returns 200 with well-known roots</li>
 *   <li>REST: double-slash {@code path/folder//Sites} is rejected (400) —
 *       the SPA must not emit this shape</li>
 *   <li>UI: explorer tree is non-empty after shell mount</li>
 *   <li>UI network: SPA pathmanagement requests never use {@code folder//}</li>
 *   <li>UI: if load fails, error chrome is human-readable (not
 *       {@code [object Object]} — formatApiError #1691)</li>
 * </ul>
 *
 * <p>Run against H2 qa-up or a host install with current WebUI (#1680):</p>
 * <pre>
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \\
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=... \\
 *     npm test -- tests/bugs/bug-1622-explorer-root-folders.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const {
  isDoubleSlashPathmanagementUrl,
  isHumanReadableErrorText,
  EXPECTED_ROOT_FOLDER_NAMES,
} = require("../helpers/pathmanagement-url");

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
      `GET ${PATH_FOLDER}/ should be 200 (double-slash form is 400)`,
    ).toBe(200);

    const body = await root.json();
    const items = Array.isArray(body?.PathItem)
      ? body.PathItem
      : Array.isArray(body)
        ? body
        : [];
    const names = items.map((it) => it?.name).filter(Boolean);
    for (const expected of EXPECTED_ROOT_FOLDER_NAMES) {
      expect(
        names,
        `root folder list should include ${expected}; got [${names.join(", ")}]`,
      ).toContain(expected);
    }

    const sites = await request.get(`${PATH_FOLDER}/Sites`, { headers });
    // Sites may be empty on a fresh install, but the path must be valid.
    expect(
      sites.status(),
      `GET ${PATH_FOLDER}/Sites must not 400 (folder//Sites was the bug)`,
    ).toBe(200);

    // Classic //Folders root (#3044): must resolve like Sites/Assets/Design.
    const folders = await request.get(`${PATH_FOLDER}/Folders`, { headers });
    expect(
      folders.status(),
      `GET ${PATH_FOLDER}/Folders should be 200 (classic Folders root #3044)`,
    ).toBe(200);

    const bad = await request.get(`${PATH_FOLDER}//Sites`, { headers });
    // Document the server contract the SPA must avoid.
    expect(bad.status()).toBe(400);
  });

  test("UI: Content Explorer tree is not empty at root (no folder// network)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);

    /** @type {string[]} */
    const pathFolderRequests = [];
    page.on("request", (req) => {
      const url = req.url();
      if (url.includes("/pathmanagement/path/")) {
        pathFolderRequests.push(url);
      }
    });

    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tree = page.locator('[data-testid="explorer-tree"]');
    await expect(tree).toBeVisible({ timeout: 15_000 });

    // Surface API/load failures explicitly (install lag still has folder// → 400).
    // Prefer data-testid after WebUI fix; also match older alert chrome.
    const treeErr = page.locator(
      '[data-testid="explorer-tree-error"], [data-testid="explorer-tree"] [role="alert"]',
    );
    if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
      const text = await treeErr.first().innerText();
      // #1691: formatApiError must yield a human string, not [object Object].
      expect(
        isHumanReadableErrorText(text),
        `Explorer tree error must be human-readable, got: ${JSON.stringify(text)}`,
      ).toBe(true);
      throw new Error(
        `Explorer tree failed to load: ${text}. If the network URL was path/folder//, redeploy WebUI with encodePath fix (#1680).`,
      );
    }

    // Root children render as tree-node-* rows (paths like /Sites/, /Assets/).
    const nodes = tree.locator('[data-testid^="tree-node-"]');
    await expect(nodes.first()).toBeVisible({ timeout: 20_000 });
    const count = await nodes.count();
    expect(count, "explorer tree should list root folders").toBeGreaterThan(0);

    // Prefer well-known roots when present (stock CMS).
    const nodeTestIds = await nodes.evaluateAll((els) =>
      els.map((el) => el.getAttribute("data-testid") || ""),
    );
    const joined = nodeTestIds.join(" ");
    for (const expected of EXPECTED_ROOT_FOLDER_NAMES) {
      const hasRoot =
        joined.includes(`tree-node-/${expected}/`) ||
        joined.includes(`tree-node-/${expected}`) ||
        nodeTestIds.some((id) =>
          id.toLowerCase().includes(expected.toLowerCase()),
        );
      expect(
        hasRoot,
        `expected root node for ${expected}; testids=${JSON.stringify(nodeTestIds)}`,
      ).toBe(true);
    }

    // Network contract: SPA must never call pathmanagement with // after resource.
    const doubleSlash = pathFolderRequests.filter(
      isDoubleSlashPathmanagementUrl,
    );
    expect(
      doubleSlash,
      `SPA must not request double-slash pathmanagement URLs (encodePath #1680). Seen: ${JSON.stringify(pathFolderRequests)}`,
    ).toEqual([]);

    // At least one root folder/ request should have been issued (encodePath → folder/).
    const folderGets = pathFolderRequests.filter((u) =>
      /\/pathmanagement\/path\/folder(\/|$|\?)/.test(u),
    );
    expect(
      folderGets.length,
      `expected SPA to call path/folder…; captured=${JSON.stringify(pathFolderRequests)}`,
    ).toBeGreaterThan(0);
    for (const u of folderGets) {
      expect(u, `folder URL must not contain folder//: ${u}`).not.toContain(
        "folder//",
      );
    }
  });
});

