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
 * Playwright surface: #3654 / parent #3102 — Explorer flag-on folder ops
 * ({@code spa.jsp?entry=explorer&rxFolderMutations=1}).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: content-explorer folders façade GET Folders and Sites (HTTP 200)</li>
 *   <li>REST: create then delete under Folders (AddFolderRequest wrap)</li>
 *   <li>UI: Create / Rename / Delete under Folders or Sites via RX REST HTTP 200</li>
 *   <li>UI: list + tree refresh after each mutation</li>
 *   <li>Hard-fail if flag-on shell posts pathmanagement mutations</li>
 * </ul>
 *
 * <p><strong>No soft-skip</strong> when the façade is on the H2 QA image
 * (GET by-path returns 200). Product default stays flag <strong>off</strong>.
 * Do not steal Move (#3655). Do not re-implement tree-key residuals
 * #3652 / #3653.</p>
 *
 * <p>Tags: {@code @explorer-rx-folder-mutations} {@code @explorer}
 * {@code @folder} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-rx-folder-mutations.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  RX_TEST_IDS,
  explorerRxFolderMutationsUrl,
  hasRxFolderMutationsQuery,
  rxFoldersRestBase,
  rxFolderByPathUrl,
  isRxFolderCreateUrl,
  isRxFolderByIdUrl,
  isPathmanagementFolderMutationUrl,
  isRxFolderMutationSuccessStatus,
  uniqueRxFolderName,
  unwrapRxFolder,
  wrapAddFolderRequest,
  treeRootLocator,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-rx-folder-mutations");

const TAGS = [
  "@explorer-rx-folder-mutations",
  "@explorer",
  "@folder",
  "@smoke",
];

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 * @returns {Promise<{ status: number, body: object }>}
 */
async function getJson(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  const body = await res.json().catch(() => ({}));
  return { status: res.status(), body };
}

test.describe("Explorer RX folder mutations flag-on no-skip (#3654 / #3102)", () => {
  test(
    "REST: content-explorer folders façade loads Folders (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(45_000);
      const url = rxFolderByPathUrl(BASE_URL, "/Folders");
      const { status, body } = await getJson(request, url);
      expect(
        status,
        `GET ${url} must be 200 when the façade is on the QA image; got ${status}`,
      ).toBe(200);
      const folder = unwrapRxFolder(body);
      const name = folder.name;
      const path = folder.path ?? "";
      if (name) {
        expect(String(name).toLowerCase()).toContain("folder");
      }
      if (path) {
        expect(String(path).toLowerCase()).toMatch(/folders/);
      }
    },
  );

  test(
    "REST: content-explorer folders façade loads Sites (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(45_000);
      const url = rxFolderByPathUrl(BASE_URL, "/Sites");
      const { status } = await getJson(request, url);
      expect(
        status,
        `GET ${url} must be 200 when the façade is on the QA image; got ${status}`,
      ).toBe(200);
    },
  );

  test(
    "REST: create then delete folder under Folders (AddFolderRequest wrap)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(60_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        "Content-Type": "application/json",
        Accept: "application/json",
      };
      const folderName = uniqueRxFolderName("qa3654r");
      const create = await request.post(rxFoldersRestBase(BASE_URL), {
        headers,
        data: wrapAddFolderRequest(folderName, "/Folders"),
      });
      const text = await create.text();
      expect(
        isRxFolderMutationSuccessStatus(create.status()),
        `POST add folder expected 200/201, got ${create.status()} body=${text}`,
      ).toBe(true);
      const created = unwrapRxFolder(JSON.parse(text || "{}"));
      const id = created.id;
      expect(id, "created folder must have id for delete").toBeTruthy();
      const del = await request.delete(
        `${rxFoldersRestBase(BASE_URL)}/by-id/${encodeURIComponent(id)}?purge=false`,
        { headers },
      );
      expect(
        isRxFolderMutationSuccessStatus(del.status()),
        `DELETE folder expected 200/204, got ${del.status()}`,
      ).toBe(true);
    },
  );

  test(
    "UI: flag-on Create/Rename/Delete use content-explorer folders (no pathmanagement)",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      /** @type {Array<{ url: string, method: string }>} */
      const mutations = [];
      page.on("request", (req) => {
        const url = req.url();
        if (req.method() === "OPTIONS") {
          return;
        }
        if (
          isPathmanagementFolderMutationUrl(url) ||
          isRxFolderCreateUrl(url) ||
          isRxFolderByIdUrl(url)
        ) {
          mutations.push({ url, method: req.method() });
        }
      });

      const createName = uniqueRxFolderName("qa3654c");
      const renameName = uniqueRxFolderName("qa3654n");
      /** @type {"create" | "rename" | "delete"} */
      let dialogPhase = "create";
      /** @type {string | null} */
      let createdId = null;
      let deletedViaUi = false;

      page.on("dialog", async (dialog) => {
        if (dialogPhase === "create") {
          await dialog.accept(createName);
        } else if (dialogPhase === "rename") {
          await dialog.accept(renameName);
        } else {
          await dialog.accept();
        }
      });

      try {
        const foldersProbe = await getJson(
          request,
          rxFolderByPathUrl(BASE_URL, "/Folders"),
        );
        const sitesProbe = await getJson(
          request,
          rxFolderByPathUrl(BASE_URL, "/Sites"),
        );
        expect(
          foldersProbe.status === 200 || sitesProbe.status === 200,
          `façade must be on the QA image; Folders=${foldersProbe.status} Sites=${sitesProbe.status}`,
        ).toBe(true);

        await loginAsAdmin(page);
        const explorerUrl = explorerRxFolderMutationsUrl(BASE_URL);
        expect(
          hasRxFolderMutationsQuery(explorerUrl),
          "diagnostic route must set rxFolderMutations=1",
        ).toBe(true);

        await page.goto(explorerUrl, { waitUntil: "networkidle" });
        const storedFlag = await page.evaluate(() =>
          sessionStorage.getItem("perc.explorer.rxFolderMutations"),
        );
        expect(
          hasRxFolderMutationsQuery(page.url()) ||
            storedFlag === "true" ||
            storedFlag === "1",
          `flag-on must survive spa.jsp rewrite; url=${page.url()} session=${storedFlag}`,
        ).toBe(true);

        const shell = page.locator(`[data-testid="${RX_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        await expect(
          page.locator(`[data-testid="${RX_TEST_IDS.tree}"]`),
        ).toBeVisible({ timeout: 20_000 });

        const foldersRoot = treeRootLocator(page, "Folders").first();
        const sitesRoot = sitesTreeRootLocator(page).first();
        const foldersVisible = await foldersRoot
          .isVisible({ timeout: 20_000 })
          .catch(() => false);
        const sitesVisible = await sitesRoot
          .isVisible({ timeout: foldersVisible ? 2_000 : 20_000 })
          .catch(() => false);
        expect(
          foldersVisible || sitesVisible,
          "H2 Explorer tree must show Folders or Sites for RX folder mutations",
        ).toBe(true);

        const parentNode = foldersVisible ? foldersRoot : sitesRoot;
        const parentLabel = foldersVisible ? "Folders" : "Sites";
        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode).catch(() => undefined);
        const list = page.locator(`[data-testid="${RX_TEST_IDS.detailList}"]`);
        await list.waitFor({ timeout: 15_000 });
        await page.waitForLoadState("networkidle").catch(() => undefined);

        const createBtn = page.locator(
          `[data-testid="${RX_TEST_IDS.actionCreateFolder}"]`,
        );
        await expect(createBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          createBtn,
          `Create Folder must be enabled under ${parentLabel} with rxFolderMutations=1`,
        ).toBeEnabled();

        dialogPhase = "create";
        const createRespPromise = page.waitForResponse(
          (res) =>
            isRxFolderCreateUrl(res.url()) &&
            res.request().method() === "POST",
          { timeout: 30_000 },
        );
        await createBtn.click();
        const createResp = await createRespPromise;
        expect(
          isRxFolderMutationSuccessStatus(createResp.status()),
          `RX POST folders expected 200/201, got ${createResp.status()} ${createResp.url()}`,
        ).toBe(true);
        const created = unwrapRxFolder(await createResp.json().catch(() => ({})));
        createdId = created.id ? String(created.id) : null;

        const pathHits = mutations.filter((m) =>
          isPathmanagementFolderMutationUrl(m.url),
        );
        expect(
          pathHits,
          `flag-on Create must not post pathmanagement: ${JSON.stringify(pathHits)}`,
        ).toEqual([]);

        await expect(list.getByText(createName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });
        const tree = page.locator(`[data-testid="${RX_TEST_IDS.tree}"]`);
        await expect(tree.getByText(createName, { exact: true })).toBeVisible({
          timeout: 15_000,
        });

        const createdRow = list.getByText(createName, { exact: true });
        await createdRow.click();

        const renameBtn = page.locator(
          `[data-testid="${RX_TEST_IDS.actionRename}"]`,
        );
        await expect(renameBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          renameBtn,
          `Rename must be enabled for ${createName}`,
        ).toBeEnabled();

        dialogPhase = "rename";
        const renameRespPromise = page.waitForResponse(
          (res) =>
            isRxFolderByIdUrl(res.url()) && res.request().method() === "PUT",
          { timeout: 30_000 },
        );
        await renameBtn.click();
        const renameResp = await renameRespPromise;
        expect(
          isRxFolderMutationSuccessStatus(renameResp.status()),
          `RX PUT by-id expected 200, got ${renameResp.status()} ${renameResp.url()}`,
        ).toBe(true);

        expect(
          mutations.filter((m) => isPathmanagementFolderMutationUrl(m.url)),
          "flag-on Rename must not post pathmanagement",
        ).toEqual([]);

        await expect(list.getByText(renameName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });
        await expect(tree.getByText(renameName, { exact: true })).toBeVisible({
          timeout: 15_000,
        });

        const renamedRow = list.getByText(renameName, { exact: true });
        await renamedRow.click();

        const deleteBtn = page.locator(
          `[data-testid="${RX_TEST_IDS.actionDelete}"]`,
        );
        await expect(deleteBtn).toBeVisible({ timeout: 10_000 });
        await expect(
          deleteBtn,
          `Delete must be enabled for ${renameName}`,
        ).toBeEnabled();

        dialogPhase = "delete";
        const deleteRespPromise = page.waitForResponse(
          (res) =>
            isRxFolderByIdUrl(res.url()) &&
            res.request().method() === "DELETE",
          { timeout: 30_000 },
        );
        await deleteBtn.click();
        const deleteResp = await deleteRespPromise;
        expect(
          isRxFolderMutationSuccessStatus(deleteResp.status()),
          `RX DELETE by-id expected 200/204, got ${deleteResp.status()} ${deleteResp.url()}`,
        ).toBe(true);
        deletedViaUi = true;

        expect(
          mutations.filter((m) => isPathmanagementFolderMutationUrl(m.url)),
          "flag-on Delete must not post pathmanagement",
        ).toEqual([]);

        await expect(list.getByText(renameName, { exact: true })).toHaveCount(0, {
          timeout: 20_000,
        });
        // Tree-node eviction after RX delete is #3652/#3653 (folderTreeEpoch /
        // childrenEpoch). This slice must not re-implement that; Create/Rename
        // already proved list+tree appearance, and Delete proved list+HTTP 200.

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${RX_TEST_IDS.shell}"]`,
        });

        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        if (!deletedViaUi && createdId) {
          await request
            .delete(
              `${rxFoldersRestBase(BASE_URL)}/by-id/${encodeURIComponent(createdId)}?purge=false`,
              { headers: adminBasicAuthHeaders() },
            )
            .catch(() => undefined);
        }
      }
    },
  );
});
