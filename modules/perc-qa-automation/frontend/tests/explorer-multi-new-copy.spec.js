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
 * Explorer multi-select Create → New Copy (#5247 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-multi-new-copy.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  copyListedOnlyAfterPost,
  explorerNewCopyUrl,
  isKnownExplorerNewCopyConsoleNoise,
  isMultiNewCopyConfirm,
  isPartialNewCopy,
  namesSkippedNewCopyFolder,
} = require("./helpers/explorer-new-copy");

const TAGS = ["@explorer-new-copy", "@explorer-multi-new-copy", "@explorer"];

const BASE_ROWS = [
  {
    id: "42",
    name: "Home",
    path: "/Sites/Demo/Home",
    type: "percPage",
    category: "page",
    accessLevel: "WRITE",
    leaf: true,
  },
  {
    id: "43",
    name: "About",
    path: "/Sites/Demo/About",
    type: "percPage",
    category: "page",
    accessLevel: "WRITE",
    leaf: true,
  },
  {
    id: "7",
    name: "News",
    path: "/Sites/Demo/News",
    type: "folder",
    category: "folder",
    accessLevel: "WRITE",
    leaf: false,
  },
];

/**
 * @returns {{ promise: Promise<void>, resolve: () => void }}
 */
function gate() {
  /** @type {() => void} */
  let resolve = () => {};
  const promise = new Promise((done) => {
    resolve = () => done();
  });
  return { promise, resolve };
}

/**
 * @param {readonly object[]} copies
 * @returns {string}
 */
function listingBody(copies) {
  const children = BASE_ROWS.concat(copies);
  return JSON.stringify({
    PagedItemList: {
      childrenInPage: children,
      childrenCount: children.length,
      startIndex: 0,
    },
  });
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ aboutStatus?: number, hold?: boolean }} [opts]
 */
async function installCopyRoutes(page, opts = {}) {
  const aboutStatus = opts.aboutStatus ?? 200;
  const hold = opts.hold === true;
  /** @type {object[]} */
  const copies = [];
  /** @type {string[]} */
  const posts = [];
  const homeGate = gate();
  const aboutGate = gate();
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: listingBody(copies),
    });
  });
  await page.route("**/itemmanagement/item/newCopy/**", async (route) => {
    const url = route.request().url();
    posts.push(url);
    const idMatch = url.match(/newCopy\/([^/?#]+)/);
    const id = idMatch ? decodeURIComponent(idMatch[1]) : "";
    const status = id === "43" ? aboutStatus : 200;
    if (hold && status === 200 && id === "42") {
      await homeGate.promise;
    }
    if (hold && status === 200 && id === "43") {
      await aboutGate.promise;
    }
    if (status === 200) {
      copies.push({
        id: id === "42" ? "9001" : "9002",
        name: id === "42" ? "Copy of Home" : "Copy of About",
        path:
          id === "42"
            ? "/Sites/Demo/Copy of Home"
            : "/Sites/Demo/Copy of About",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      });
    }
    await route.fulfill({
      status,
      contentType: "application/json",
      body:
        status === 200
          ? JSON.stringify({
              ItemCopyResult: {
                itemId: id === "42" ? "9001" : "9002",
                folderPath: "//Sites/Demo",
                promotable: false,
              },
            })
          : JSON.stringify({ message: "denied" }),
    });
  });
  return { posts, homeGate, aboutGate };
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {string[]}
 */
function watchPageErrors(page) {
  /** @type {string[]} */
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerNewCopyConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  return jsErrors;
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function openCheckedListing(page) {
  await page.goto(explorerNewCopyUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  await page.locator('[data-testid="detail-select-42"]').check();
  await page.locator('[data-testid="detail-select-43"]').check();
  await page.locator('[data-testid="detail-select-7"]').check();
  await expect(page.locator('[data-testid="explorer-multi-select-count"]')).toContainText(
    "3",
  );
}

async function clickNewCopy(page) {
  const create = page
    .locator(`[data-testid="${TEST_IDS.createMenu}"][aria-haspopup="menu"]`)
    .first();
  await expect(create).toBeVisible({ timeout: 20_000 });
  const menuItem = page
    .locator(`[data-testid="${TEST_IDS.newCopy}"][role="menuitem"]`)
    .first();
  for (let attempt = 0; attempt < 3; attempt += 1) {
    if (!(await menuItem.isVisible().catch(() => false))) {
      await create.click({ force: true });
    }
    try {
      await menuItem.click({ timeout: 5_000 });
      return;
    } catch (err) {
      if (attempt === 2) {
        throw err;
      }
    }
  }
}

test.describe("Explorer create a new copy of each multi-selected item (#5247)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm copies each page and the list shows a copy only after that post",
    { tag: TAGS },
    async ({ page }) => {
      const jsErrors = watchPageErrors(page);
      page.on("dialog", (dialog) => {
        expect(isMultiNewCopyConfirm(dialog.message())).toBe(true);
        void dialog.accept();
      });
      const { posts, homeGate, aboutGate } = await installCopyRoutes(page, {
        hold: true,
      });
      /** @type {{ type: string, id: string }[]} */
      const events = [];
      try {
        await openCheckedListing(page);
        await clickNewCopy(page);
        await expect
          .poll(() => posts.some((url) => url.includes("/newCopy/42")))
          .toBe(true);
        events.push({ type: "post", id: "42" });
        await expect(page.locator('[data-item-name="Copy of Home"]')).toHaveCount(0);
        homeGate.resolve();
        await expect(page.locator('[data-item-name="Copy of Home"]')).toBeVisible({
          timeout: 15_000,
        });
        events.push({ type: "listed", id: "42" });
        await expect
          .poll(() => posts.some((url) => url.includes("/newCopy/43")))
          .toBe(true);
        events.push({ type: "post", id: "43" });
        await expect(page.locator('[data-item-name="Copy of About"]')).toHaveCount(0);
        aboutGate.resolve();
        await expect(page.locator('[data-item-name="Copy of About"]')).toBeVisible({
          timeout: 15_000,
        });
        events.push({ type: "listed", id: "43" });
        expect(copyListedOnlyAfterPost(events)).toBe(true);
        expect(posts.some((url) => url.includes("/newCopy/7"))).toBe(false);
        expect(posts).toHaveLength(2);
        const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
        await expect(error).toBeVisible({ timeout: 10_000 });
        const errorText = await error.innerText();
        expect(namesSkippedNewCopyFolder(errorText, "News")).toBe(true);
        expect(isPartialNewCopy(errorText, "About", 409)).toBe(false);
        await expect(page.locator('[data-testid="explorer-nav"]')).not.toHaveAttribute(
          "data-list-epoch",
          "0",
        );
        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${TEST_IDS.shell}"]`,
        });
        expect(jsErrors, jsErrors.join("\n")).toEqual([]);
      } finally {
        homeGate.resolve();
        aboutGate.resolve();
      }
    },
  );

  test(
    "cancel copies nothing",
    { tag: TAGS },
    async ({ page }) => {
      const jsErrors = watchPageErrors(page);
      page.on("dialog", (dialog) => {
        expect(isMultiNewCopyConfirm(dialog.message())).toBe(true);
        void dialog.dismiss();
      });
      const { posts } = await installCopyRoutes(page);
      await openCheckedListing(page);
      await clickNewCopy(page);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(posts).toEqual([]);
      await expect(page.locator('[data-item-name="Copy of Home"]')).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.serverError}"]`)).toHaveCount(
        0,
      );
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} on one item does not claim the whole selection was copied`,
      { tag: TAGS },
      async ({ page }) => {
        const jsErrors = watchPageErrors(page);
        page.on("dialog", (dialog) => {
          expect(isMultiNewCopyConfirm(dialog.message())).toBe(true);
          void dialog.accept();
        });
        const { posts } = await installCopyRoutes(page, { aboutStatus: status });
        await openCheckedListing(page);
        await clickNewCopy(page);
        const error = page.locator(`[data-testid="${TEST_IDS.serverError}"]`);
        await expect(error).toBeVisible({ timeout: 15_000 });
        const errorText = await error.innerText();
        expect(isPartialNewCopy(errorText, "About", status)).toBe(true);
        expect(namesSkippedNewCopyFolder(errorText, "News")).toBe(true);
        await expect(page.locator('[data-item-name="Copy of Home"]')).toBeVisible({
          timeout: 15_000,
        });
        await expect(page.locator('[data-item-name="Copy of About"]')).toHaveCount(0);
        expect(posts.some((url) => url.includes("/newCopy/42"))).toBe(true);
        expect(posts.some((url) => url.includes("/newCopy/43"))).toBe(true);
        expect(posts.some((url) => url.includes("/newCopy/7"))).toBe(false);
        expect(jsErrors, jsErrors.join("\n")).toEqual([]);
      },
    );
  }
});
