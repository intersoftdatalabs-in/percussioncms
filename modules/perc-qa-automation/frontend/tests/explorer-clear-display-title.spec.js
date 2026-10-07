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
 * Playwright surface: #5297 / parent #4530 — Explorer clear display title.
 *
 * Tags: @explorer-clear-display-title @explorer @item
 *
 * npm run test:surface -- --path tests/explorer-clear-display-title.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  ITEM_PROPS_TEST_IDS,
  explorerProductItemPropertiesUrl,
  itemPropertiesUrl,
  isItemPropertiesSaveUrl,
  wrapItemPropertiesRequest,
  hasRxFolderMutationsQuery,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-item-properties");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-clear-display-title", "@explorer", "@item"];

/** FastForward page whose content type stores displaytitle (rffHome). */
const PAGE_PATH = "/Sites/Corporate_Investments/Corporate Investments Home";
const PAGE_NAME = "Corporate Investments Home";

function encodeItemPath(itemPath) {
  return String(itemPath || "")
    .replace(/^\/+/, "")
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}

async function readItemProperties(request, itemPath) {
  const res = await request.get(
    `${itemPropertiesUrl(BASE_URL)}/${encodeItemPath(itemPath)}`,
    { headers: adminBasicAuthHeaders() },
  );
  const text = await res.text();
  let body = {};
  try {
    body = JSON.parse(text);
  } catch {
    body = {};
  }
  const rec = body.ItemProperties || body.itemProperties || body;
  return {
    status: res.status(),
    name: rec && rec.name != null ? String(rec.name) : "",
    displayTitle:
      rec && rec.displayTitle != null ? String(rec.displayTitle) : "",
  };
}

async function writeDisplayTitle(request, itemPath, name, displayTitle) {
  return request.post(itemPropertiesUrl(BASE_URL), {
    headers: {
      ...adminBasicAuthHeaders(),
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    data: wrapItemPropertiesRequest(itemPath, name, displayTitle),
  });
}

async function failNextSaves(page, status) {
  const pattern = "**/rest/folders/item-properties**";
  const handler = async (route) => {
    const req = route.request();
    if (req.method() === "POST" && isItemPropertiesSaveUrl(req.url())) {
      await route.fulfill({
        status,
        contentType: "application/json",
        body: "{}",
      });
      return;
    }
    await route.continue();
  };
  await page.route(pattern, handler);
  return async () => {
    await page.unroute(pattern, handler);
  };
}

test.describe("Explorer clear display title (#5297 / #4530)", () => {
  test(
    "UI: empty save reloads an empty title and the same name; cancel and HTTP 400/403/409 keep the previous title; folders are not offered clear",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          const text = msg.text();
          if (isKnownExplorerSitesConsoleNoise(text)) {
            return;
          }
          jsErrors.push(text);
        }
      });

      const stamp = Date.now();
      const seedTitle = `Clear seed ${stamp}`;
      const nextTitle = `Title ${stamp}`;
      let savePosts = 0;
      let originalTitle = "";
      let mutated = false;
      page.on("request", (req) => {
        if (req.method() === "POST" && isItemPropertiesSaveUrl(req.url())) {
          savePosts += 1;
        }
      });

      const before = await readItemProperties(request, PAGE_PATH);
      expect(before.status, "home page item properties").toBe(200);
      expect(before.name).toBe(PAGE_NAME);
      expect(before.displayTitle).not.toContain("PSTextValue@");
      originalTitle = before.displayTitle;

      const seeded = await writeDisplayTitle(
        request,
        PAGE_PATH,
        PAGE_NAME,
        seedTitle,
      );
      expect(seeded.ok(), "seed display title").toBeTruthy();
      mutated = true;

      try {
        await loginAsAdmin(page);
        const explorerUrl = explorerProductItemPropertiesUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });

        const shell = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.shell}"]`,
        );
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const sitesNode = treeRootLocator(page, "Sites").first();
        await expect(sitesNode).toBeVisible({ timeout: 20_000 });
        await sitesNode.click({ force: true });
        await expandExplorerTreeNode(sitesNode).catch(() => undefined);
        const list = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        const siteRow = list
          .locator(
            '[data-item-name="Corporate_Investments"], [data-item-name="Corporate Investments"]',
          )
          .first();
        await expect(siteRow).toBeVisible({ timeout: 20_000 });
        await siteRow.click();
        await page
          .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.toggle}"]`)
          .click();
        await expect(
          page.locator(`[data-testid="${ITEM_PROPS_TEST_IDS.hint}"]`),
        ).toBeVisible({ timeout: 15_000 });
        await expect(
          page.locator(
            `[data-testid="${ITEM_PROPS_TEST_IDS.clearDisplayTitle}"]`,
          ),
        ).toHaveCount(0);

        await page.locator('[data-testid="action-open"]').click();
        await page.waitForLoadState("networkidle").catch(() => undefined);
        const itemRow = list.locator(`[data-item-name="${PAGE_NAME}"]`).first();
        await expect(itemRow).toBeVisible({ timeout: 20_000 });
        await itemRow.click();

        const nameInput = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.name}"]`,
        );
        const titleInput = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.displayTitle}"]`,
        );
        const shown = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.shownDisplayTitle}"]`,
        );
        const clear = page.locator(
          `[data-testid="${ITEM_PROPS_TEST_IDS.clearDisplayTitle}"]`,
        );
        await expect(nameInput).toBeVisible({ timeout: 20_000 });
        await expect(nameInput).toHaveValue(PAGE_NAME);
        await expect(shown).toHaveText(seedTitle);
        await expect(clear).toBeVisible();

        const postsBeforeCancel = savePosts;
        await clear.click();
        await expect(titleInput).toHaveValue("");
        await expect(shown).toHaveText(seedTitle);
        await page
          .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.cancel}"]`)
          .click();
        await expect(titleInput).toHaveValue(seedTitle);
        await expect(shown).toHaveText(seedTitle);
        expect(savePosts).toBe(postsBeforeCancel);
        await expect(nameInput).toHaveValue(PAGE_NAME);

        for (const status of [400, 403, 409]) {
          const stop = await failNextSaves(page, status);
          try {
            await clear.click();
            await page
              .locator(`[data-testid="${ITEM_PROPS_TEST_IDS.save}"]`)
              .click();
            await expect(titleInput).toHaveValue(seedTitle, {
              timeout: 15_000,
            });
            await expect(shown).toHaveText(seedTitle);
            await expect(nameInput).toHaveValue(PAGE_NAME);
            await expect(
              page.locator('[data-testid="item-properties-status"]'),
            ).toBeVisible();
          } finally {
            await stop();
          }
        }

        const saveRespPromise = page.waitForResponse(
          (res) =>
            isItemPropertiesSaveUrl(res.url()) &&
            res.request().method() === "POST",
          { timeout: 30_000 },
        );
        await clear.click();
        await page.locator(`[data-testid="${ITEM_PROPS_TEST_IDS.save}"]`).click();
        const saveResp = await saveRespPromise;
        expect(saveResp.status(), "clear display title HTTP").toBe(200);
        const posted = saveResp.request().postDataJSON();
        expect(posted.ItemPropertiesRequest.name).toBe(PAGE_NAME);
        expect(posted.ItemPropertiesRequest.displayTitle).toBe("");
        await expect(shown).toHaveText("", { timeout: 20_000 });
        await expect(titleInput).toHaveValue("");
        await expect(nameInput).toHaveValue(PAGE_NAME);
        await expect(list.getByText(PAGE_NAME, { exact: true })).toBeVisible();

        const reloaded = await readItemProperties(request, PAGE_PATH);
        expect(reloaded.status).toBe(200);
        expect(reloaded.name).toBe(PAGE_NAME);
        expect(reloaded.displayTitle).toBe("");

        const nonemptyPromise = page.waitForResponse(
          (res) =>
            isItemPropertiesSaveUrl(res.url()) &&
            res.request().method() === "POST",
          { timeout: 30_000 },
        );
        await titleInput.fill(nextTitle);
        await page.locator(`[data-testid="${ITEM_PROPS_TEST_IDS.save}"]`).click();
        const nonemptyResp = await nonemptyPromise;
        expect(nonemptyResp.status(), "non-empty display title still saves").toBe(
          200,
        );
        const nonemptyPosted = nonemptyResp.request().postDataJSON();
        expect(nonemptyPosted.ItemPropertiesRequest.name).toBe(PAGE_NAME);
        expect(nonemptyPosted.ItemPropertiesRequest.displayTitle).toBe(
          nextTitle,
        );
        await expect(shown).toHaveText(nextTitle, { timeout: 20_000 });
        await expect(nameInput).toHaveValue(PAGE_NAME);

        await expectNoSeriousA11yViolations(page, {
          include: '[data-testid="content-explorer-shell"]',
        });
        expect(
          jsErrors,
          `console/pageerror must stay empty: ${jsErrors.join(" | ")}`,
        ).toEqual([]);
      } finally {
        if (mutated) {
          const restore = await writeDisplayTitle(
            request,
            PAGE_PATH,
            PAGE_NAME,
            originalTitle,
          ).catch(() => null);
          if (!restore || !restore.ok()) {
            throw new Error(
              `failed to restore display title on ${PAGE_PATH} status=${restore ? restore.status() : "error"}`,
            );
          }
        }
      }
    },
  );
});
