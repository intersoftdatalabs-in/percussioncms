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
 * Architecture structure mutations smoke (#3096 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-mutations-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture (structure action bar when sites exist).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  architectureSpaUrl,
  siteListUrl,
  sectionTreeUrl,
  shouldRequireNavTree,
  firstSampleDemoSite,
  uniqueSectionTitle,
  uniqueSectionUrlName,
  uniqueLandingPageName,
  isCreateSiteSectionRequest,
  isSectionUpdateRequest,
  isSectionMoveRequest,
  isSectionDeleteRequest,
  sectionChildTitles,
  isKnownArchitectureConsoleNoise,
  missingNavTreeFailMessage,
  siteNamesFromPayload,
  isEmptyTreePayload,
} = require("./helpers/architecture-create-section");

test.describe("Architecture nav structure mutations (#3096)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("structure action bar is present with tree or empty states @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    const sitesResp = await page.request.get(siteListUrl(BASE_URL));
    let siteQuery = {};
    if (sitesResp.ok()) {
      const demoSite = firstSampleDemoSite(siteNamesFromPayload(await sitesResp.json()));
      if (demoSite) {
        siteQuery = { site: demoSite };
      }
    }
    await page.goto(architectureSpaUrl(BASE_URL, siteQuery), {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("nav-architecture")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("architecture-shell-title")).toContainText(
      /Navigation/i,
    );
    await expect(page.getByTestId("architecture-toolbar")).toBeVisible({
      timeout: 15_000,
    });

    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const picker = page.getByTestId("architecture-site-picker");
    const treePanel = page.getByTestId("architecture-tree-panel");
    const emptyState = page.getByTestId("architecture-empty-state");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty-sites";
          if (await sitesError.isVisible().catch(() => false)) return "sites-error";
          if (await picker.isVisible().catch(() => false)) return "picker";
          if (await emptyState.isVisible().catch(() => false)) return "no-site";
          return "pending";
        },
        { timeout: 25_000 },
      )
      .not.toBe("pending");

    if (await picker.isVisible().catch(() => false)) {
      await expect(page.getByTestId("architecture-site-select")).toBeVisible();
      await expect(treePanel.or(emptyState)).toBeVisible({ timeout: 15_000 });
      if (await treePanel.isVisible().catch(() => false)) {
        await expect(page.getByTestId("architecture-nav-tree")).toBeVisible();
        await expect(
          page.getByTestId("architecture-structure-actions"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-create"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-rename"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move-up"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-move-down"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-action-delete"),
        ).toBeVisible();
        await expect(
          page.getByTestId("architecture-structure-note"),
        ).toBeVisible();
        // Read-only note must be gone
        await expect(
          page.getByTestId("architecture-readonly-note"),
        ).toHaveCount(0);

        // #3350 / #3155: when a NavTree is present, Create section is enabled
        // (root or selected regular section). Dialog is modal; Escape closes
        // and returns focus to the opener. No forced save against the live site.
        const treeItems = page.locator(
          '[data-testid="architecture-nav-tree"] [role="treeitem"]',
        );
        if (shouldRequireNavTree()) {
          await expect(treeItems.first()).toBeVisible({ timeout: 20_000 });
        }
        const itemCount = await treeItems.count();
        const createBtn = page.getByTestId("architecture-action-create");
        const extBtn = page.getByTestId(
          "architecture-action-create-external-link",
        );
        const linkBtn = page.getByTestId(
          "architecture-action-create-section-link",
        );
        if (itemCount > 0) {
          await treeItems.first().click();
          await expect(createBtn).toBeEnabled();
          await expect(extBtn).toBeEnabled();
          await expect(linkBtn).toBeEnabled();
          await createBtn.click();
          const createDialog = page.getByTestId("architecture-create-dialog");
          await expect(createDialog).toBeVisible({ timeout: 10_000 });
          await expect(createDialog.locator('[role="dialog"]')).toHaveAttribute(
            "aria-modal",
            "true",
          );
          await page.keyboard.press("Escape");
          await expect(createDialog).toHaveCount(0);
          await expect(createBtn).toBeFocused();

          await extBtn.click();
          const extDialog = page.getByTestId(
            "architecture-external-link-dialog",
          );
          await expect(extDialog).toBeVisible({ timeout: 10_000 });
          await expect(extDialog).toHaveAttribute("role", "dialog");
          await page.keyboard.press("Escape");
          await expect(extDialog).toHaveCount(0);
          await expect(extBtn).toBeFocused();

          await linkBtn.click();
          const linkDialog = page.getByTestId(
            "architecture-section-link-dialog",
          );
          await expect(linkDialog).toBeVisible({ timeout: 10_000 });
          await expect(linkDialog).toHaveAttribute("role", "dialog");
          await page.keyboard.press("Escape");
          await expect(linkDialog).toHaveCount(0);
          await expect(linkBtn).toBeFocused();

          const renameBtn = page.getByTestId("architecture-action-rename");
          await expect(renameBtn).toBeEnabled();
          await renameBtn.click();
          const renameDialog = page.getByTestId("architecture-rename-dialog");
          await expect(renameDialog).toBeVisible({ timeout: 10_000 });
          await expect(renameDialog.locator('[role="dialog"]')).toHaveAttribute(
            "aria-modal",
            "true",
          );
          await page.keyboard.press("Escape");
          await expect(renameDialog).toHaveCount(0);
          await expect(renameBtn).toBeFocused();
        } else if (shouldRequireNavTree()) {
          expect(itemCount, missingNavTreeFailMessage()).toBeGreaterThan(0);
        } else if (await createBtn.isEnabled().catch(() => false)) {
          await createBtn.click();
          await expect(
            page.getByTestId("architecture-create-dialog"),
          ).toBeVisible({ timeout: 10_000 });
          await page.keyboard.press("Escape");
          await expect(
            page.getByTestId("architecture-create-dialog"),
          ).toHaveCount(0);
        } else {
          test.info().annotations.push({
            type: "note",
            description:
              "No treeitems — Create stays disabled (empty NavTree). Dialog Escape covered by Vitest + cells with #3352 seed.",
          });
        }
      }
    }

    // Zero uncaught page errors; ignore common network 404 console noise
    // (favicon, optional assets) that is not feature-related.
    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            e,
          ),
      ),
    ).toEqual([]);
  });

  test("rename persists and move up/down changes sibling order (#3797) @smoke @ui @architecture-nav-mutations", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    expect(
      shouldRequireNavTree(),
      "this surface is the H2 no-skip gate; set TEST_DB_TYPE=h2",
    ).toBe(true);

    const sitesResp = await page.request.get(siteListUrl(BASE_URL));
    expect(sitesResp.status(), "site list must be HTTP 200").toBe(200);
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(
      demoSite,
      `QA cell must list a #3352 sample site; got ${JSON.stringify(names)}`,
    ).toBeTruthy();

    const treeResp = await page.request.get(
      sectionTreeUrl(BASE_URL, demoSite),
    );
    expect(treeResp.status(), `tree GET for ${demoSite}`).toBe(200);
    expect(
      isEmptyTreePayload(await treeResp.text()),
      missingNavTreeFailMessage(),
    ).toBe(false);

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    const treeItems = page.locator(
      `[data-testid="${TEST_IDS.navTree}"] [role="treeitem"]`,
    );
    await expect(treeItems.first()).toBeVisible({ timeout: 20_000 });
    await treeItems.first().click();

    const stamp = Date.now();
    const titleA = uniqueSectionTitle(stamp, "QA3797A");
    const titleB = uniqueSectionTitle(stamp + 1, "QA3797B");
    const renamed = `${titleA}Ren`;

    async function createSection(title) {
      const createBtn = page.getByTestId(TEST_IDS.actionCreate);
      await expect(createBtn).toBeEnabled();
      await createBtn.click();
      const createDialog = page.getByTestId(TEST_IDS.createDialog);
      await expect(createDialog).toBeVisible({ timeout: 10_000 });
      await page.getByTestId(TEST_IDS.createTitle).fill(title);
      await page.getByTestId(TEST_IDS.createUrl).fill(uniqueSectionUrlName(title));
      await page
        .getByTestId(TEST_IDS.createPageName)
        .fill(uniqueLandingPageName(title));
      await expect
        .poll(
          async () =>
            (await page
              .getByTestId(TEST_IDS.createTemplatesLoading)
              .isVisible()
              .catch(() => false))
              ? "loading"
              : "ready",
          { timeout: 20_000 },
        )
        .toBe("ready");
      const templateSelect = page.getByTestId(TEST_IDS.createTemplate);
      await expect(templateSelect).toBeEnabled({ timeout: 20_000 });
      await templateSelect.selectOption({ index: 0 });
      const postPromise = page.waitForRequest(
        (req) => isCreateSiteSectionRequest(req.url(), req.method()),
        { timeout: 30_000 },
      );
      await page.getByTestId(TEST_IDS.createSubmit).click();
      const postReq = await postPromise;
      const postResp = await postReq.response();
      expect(
        postResp && postResp.status(),
        "POST /section/create must be HTTP 200",
      ).toBe(200);
      await expect(createDialog).toHaveCount(0, { timeout: 30_000 });
      await expect(
        page.getByRole("treeitem", { name: new RegExp(title, "i") }),
      ).toBeVisible({ timeout: 20_000 });
    }

    await createSection(titleA);
    await treeItems.first().click();
    await createSection(titleB);

    const itemA = page.getByRole("treeitem", {
      name: new RegExp(titleA, "i"),
    });
    await itemA.click();
    const renameBtn = page.getByTestId(TEST_IDS.actionRename);
    await expect(renameBtn).toBeEnabled();
    await renameBtn.click();
    const renameDialog = page.getByTestId(TEST_IDS.renameDialog);
    await expect(renameDialog).toBeVisible({ timeout: 10_000 });
    await page.getByTestId(TEST_IDS.renameTitle).fill(renamed);
    const updatePromise = page.waitForRequest(
      (req) => isSectionUpdateRequest(req.url(), req.method()),
      { timeout: 30_000 },
    );
    await page.getByTestId(TEST_IDS.renameSubmit).click();
    const updateReq = await updatePromise;
    const updateResp = await updateReq.response();
    expect(
      updateResp && updateResp.status(),
      "POST /section/update rename must be HTTP 200 (not rollback-only 500)",
    ).toBe(200);
    await expect(renameDialog).toHaveCount(0, { timeout: 20_000 });
    await expect(
      page.getByRole("treeitem", { name: new RegExp(renamed, "i") }),
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.getByTestId("architecture-mutation-error")).toHaveCount(0);

    const treeAfterRename = await page.request.get(
      sectionTreeUrl(BASE_URL, demoSite),
    );
    expect(treeAfterRename.status()).toBe(200);
    const titlesAfterRename = sectionChildTitles(await treeAfterRename.json());
    expect(
      titlesAfterRename.some((t) => t.includes(renamed)),
      `GET tree after rename must include ${renamed}; got ${JSON.stringify(titlesAfterRename)}`,
    ).toBe(true);

    const itemB = page.getByRole("treeitem", {
      name: new RegExp(titleB, "i"),
    });
    await itemB.click();
    const moveUp = page.getByTestId(TEST_IDS.actionMoveUp);
    await expect(moveUp).toBeEnabled();
    const movePromise = page.waitForRequest(
      (req) => isSectionMoveRequest(req.url(), req.method()),
      { timeout: 30_000 },
    );
    await moveUp.click();
    const moveReq = await movePromise;
    const moveResp = await moveReq.response();
    expect(
      moveResp && moveResp.status(),
      "POST /section/move must be HTTP 200",
    ).toBe(200);
    await expect(page.getByTestId("architecture-mutation-error")).toHaveCount(0);

    await expect
      .poll(
        async () => {
          const resp = await page.request.get(
            sectionTreeUrl(BASE_URL, demoSite),
          );
          if (!resp.ok()) {
            return "http-error";
          }
          const titles = sectionChildTitles(await resp.json());
          const idxB = titles.findIndex((t) => t.includes(titleB));
          const idxA = titles.findIndex((t) => t.includes(renamed));
          if (idxB < 0 || idxA < 0) {
            return "missing";
          }
          return idxB < idxA ? "reordered" : "same";
        },
        { timeout: 25_000 },
      )
      .toBe("reordered");

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    const labels = await page
      .locator(`[data-testid="${TEST_IDS.navTree}"] [role="treeitem"]`)
      .allTextContents();
    const uiIdxB = labels.findIndex((t) => t.includes(titleB));
    const uiIdxA = labels.findIndex((t) => t.includes(renamed));
    expect(uiIdxB, "B visible after reload").toBeGreaterThanOrEqual(0);
    expect(uiIdxA, "renamed A visible after reload").toBeGreaterThanOrEqual(0);
    expect(uiIdxB, "Move up must survive reload").toBeLessThan(uiIdxA);

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });

  test("create non-root section enables Delete; cancel keeps; confirm removes (#3821) @smoke @ui @architecture-nav-mutations", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    expect(
      shouldRequireNavTree(),
      "this surface is the H2 no-skip gate; set TEST_DB_TYPE=h2",
    ).toBe(true);

    const sitesResp = await page.request.get(siteListUrl(BASE_URL));
    expect(sitesResp.status(), "site list must be HTTP 200").toBe(200);
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(
      demoSite,
      `QA cell must list a #3352 sample site; got ${JSON.stringify(names)}`,
    ).toBeTruthy();

    const treeResp = await page.request.get(sectionTreeUrl(BASE_URL, demoSite));
    expect(treeResp.status(), `tree GET for ${demoSite}`).toBe(200);
    expect(
      isEmptyTreePayload(await treeResp.text()),
      missingNavTreeFailMessage(),
    ).toBe(false);

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    const treeItems = page.locator(
      `[data-testid="${TEST_IDS.navTree}"] [role="treeitem"]`,
    );
    await expect(treeItems.first()).toBeVisible({ timeout: 20_000 });
    await treeItems.first().click();

    const deleteBtn = page.getByTestId(TEST_IDS.actionDelete);
    await expect(deleteBtn).toBeDisabled();

    const title = uniqueSectionTitle(Date.now(), "QA3821");
    const createBtn = page.getByTestId(TEST_IDS.actionCreate);
    await expect(createBtn).toBeEnabled();
    await createBtn.click();
    const createDialog = page.getByTestId(TEST_IDS.createDialog);
    await expect(createDialog).toBeVisible({ timeout: 10_000 });
    await page.getByTestId(TEST_IDS.createTitle).fill(title);
    await page.getByTestId(TEST_IDS.createUrl).fill(uniqueSectionUrlName(title));
    await page
      .getByTestId(TEST_IDS.createPageName)
      .fill(uniqueLandingPageName(title));
    await expect
      .poll(
        async () =>
          (await page
            .getByTestId(TEST_IDS.createTemplatesLoading)
            .isVisible()
            .catch(() => false))
            ? "loading"
            : "ready",
        { timeout: 20_000 },
      )
      .toBe("ready");
    const templateSelect = page.getByTestId(TEST_IDS.createTemplate);
    await expect(templateSelect).toBeEnabled({ timeout: 20_000 });
    await templateSelect.selectOption({ index: 0 });
    const postPromise = page.waitForRequest(
      (req) => isCreateSiteSectionRequest(req.url(), req.method()),
      { timeout: 30_000 },
    );
    await page.getByTestId(TEST_IDS.createSubmit).click();
    const postReq = await postPromise;
    const postResp = await postReq.response();
    expect(
      postResp && postResp.status(),
      "POST /section/create must be HTTP 200",
    ).toBe(200);
    await expect(createDialog).toHaveCount(0, { timeout: 30_000 });
    const createdItem = page.getByRole("treeitem", {
      name: new RegExp(title, "i"),
    });
    await expect(createdItem).toBeVisible({ timeout: 20_000 });

    // Auto-select after Create — do not click the new node first (#3821).
    await expect(deleteBtn).toBeEnabled({ timeout: 20_000 });

    await treeItems.first().click();
    await expect(deleteBtn).toBeDisabled();
    await createdItem.click();
    await expect(deleteBtn).toBeEnabled();

    page.once("dialog", (dialog) => {
      void dialog.dismiss();
    });
    await deleteBtn.click();
    await expect(createdItem).toBeVisible();
    const treeAfterCancel = await page.request.get(
      sectionTreeUrl(BASE_URL, demoSite),
    );
    expect(treeAfterCancel.status()).toBe(200);
    expect(
      sectionChildTitles(await treeAfterCancel.json()).some((t) =>
        t.includes(title),
      ),
      `cancel must keep ${title}`,
    ).toBe(true);

    const deletePromise = page.waitForRequest(
      (req) => isSectionDeleteRequest(req.url(), req.method()),
      { timeout: 30_000 },
    );
    page.once("dialog", (dialog) => {
      void dialog.accept();
    });
    await deleteBtn.click();
    const deleteReq = await deletePromise;
    const deleteResp = await deleteReq.response();
    const deleteStatus = deleteResp && deleteResp.status();
    expect(
      deleteStatus,
      "DELETE /section/{id} must be HTTP 2xx (200 or 204)",
    ).toBeGreaterThanOrEqual(200);
    expect(deleteStatus).toBeLessThan(300);
    await expect(createdItem).toHaveCount(0, { timeout: 20_000 });
    await expect(page.getByTestId("architecture-mutation-error")).toHaveCount(0);

    await expect
      .poll(
        async () => {
          const resp = await page.request.get(
            sectionTreeUrl(BASE_URL, demoSite),
          );
          if (!resp.ok()) {
            return "http-error";
          }
          const titles = sectionChildTitles(await resp.json());
          return titles.some((t) => t.includes(title)) ? "present" : "gone";
        },
        { timeout: 25_000 },
      )
      .toBe("gone");

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.getByRole("treeitem", { name: new RegExp(title, "i") }),
    ).toHaveCount(0);

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });
});
