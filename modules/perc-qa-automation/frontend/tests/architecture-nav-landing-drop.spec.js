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
 * Architecture Finder-drop replace landing page (#3660 / parent #3092).
 *
 * When tree GET is HTTP 200 with a NavTree, this spec does not skip.
 * Page drop POSTs replaceLandingPage; folder / invalid MIME / Escape do not.
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-landing-drop.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* + TEST_DB_TYPE=h2 →
 * test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  architectureSpaUrl,
  siteListUrl,
  sectionTreeUrl,
  shouldRequireNavTree,
  firstSampleDemoSite,
  isKnownArchitectureConsoleNoise,
  missingNavTreeFailMessage,
  siteNamesFromPayload,
  isEmptyTreePayload,
} = require("./helpers/architecture-create-section");
const {
  FINDER_FOLDER_MIME,
  FINDER_PAGE_MIME,
  dispatchFinderDrop,
  serializeFinderItemDrag,
} = require("./helpers/architecture-landing-drop");

test.describe("Architecture Finder-drop landing page (#3660)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("page drop POSTs replaceLandingPage when tree GET is 200 @smoke @ui", async ({
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

    expect(
      shouldRequireNavTree(),
      "H2 Navigation drop gate; set TEST_DB_TYPE=h2",
    ).toBe(true);

    const sitesResp = await page.request.get(siteListUrl(BASE_URL));
    expect(sitesResp.status(), "site list must be HTTP 200").toBe(200);
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(
      demoSite,
      `QA cell must list a sample site; got ${JSON.stringify(names)}`,
    ).toBeTruthy();

    const treeResp = await page.request.get(sectionTreeUrl(BASE_URL, demoSite));
    expect(treeResp.status(), `tree GET for ${demoSite}`).toBe(200);
    expect(
      isEmptyTreePayload(await treeResp.text()),
      missingNavTreeFailMessage(),
    ).toBe(false);

    const replacePosts = [];
    await page.route("**/section/replaceLandingPage**", async (route) => {
      replacePosts.push(route.request().postData() || "");
      const body = route.request().postData() || "";
      let sectionId = "";
      try {
        const parsed = JSON.parse(body);
        const wrap =
          parsed.ReplaceLandingPage || parsed.replaceLandingPage || parsed;
        sectionId = String((wrap && wrap.sectionId) || "");
      } catch {
        sectionId = "";
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ReplaceLandingPage: {
            sectionId,
            newLandingPageId: "qa-drop-page",
            newLandingPageName: "Dropped Page",
            oldLandingPageName: "index",
          },
        }),
      });
    });

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("architecture-nav-tree-error")).toHaveCount(0);
    await expect(page.getByTestId("architecture-nav-tree-empty")).toHaveCount(0);
    await expect(page.getByTestId("architecture-landing-drop-hint")).toBeVisible();

    const target = page.locator('[data-testid^="nav-tree-item-"][data-landing-drop="true"]').first();
    await expect(target).toBeVisible({ timeout: 20_000 });
    await target.click();
    const testId = await target.getAttribute("data-testid");
    expect(testId).toBeTruthy();

    await dispatchFinderDrop(page, testId, {
      mime: FINDER_PAGE_MIME,
      payload: serializeFinderItemDrag({
        id: "qa-drop-page",
        name: "Dropped Page",
        path: `//Sites/${demoSite}/Dropped`,
        type: "page",
        category: "PAGE",
      }),
    });

    await expect
      .poll(() => replacePosts.length, { timeout: 15_000 })
      .toBe(1);
    expect(replacePosts[0]).toMatch(/qa-drop-page/);
    await expect(page.getByTestId("architecture-landing-current")).toContainText(
      /Dropped Page/,
      { timeout: 15_000 },
    );
    await expect(page.getByText(/HTTP 500/i)).toHaveCount(0);

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });

  test("folder drop, invalid MIME, and Escape do not POST @smoke @ui", async ({
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
    expect(sitesResp.status()).toBe(200);
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(demoSite).toBeTruthy();

    const treeResp = await page.request.get(sectionTreeUrl(BASE_URL, demoSite));
    expect(treeResp.status()).toBe(200);
    expect(isEmptyTreePayload(await treeResp.text())).toBe(false);

    const replacePosts = [];
    await page.route("**/section/replaceLandingPage**", async (route) => {
      replacePosts.push(route.request().postData() || "");
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ message: "should not POST" }),
      });
    });

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const target = page.locator('[data-testid^="nav-tree-item-"][data-landing-drop="true"]').first();
    await expect(target).toBeVisible({ timeout: 20_000 });
    await target.click();
    const testId = await target.getAttribute("data-testid");

    await dispatchFinderDrop(page, testId, {
      mime: FINDER_FOLDER_MIME,
      payload: serializeFinderItemDrag({
        id: "folder-1",
        name: "Folder",
        path: `//Sites/${demoSite}/Folder`,
        type: "folder",
        category: "folder",
      }),
    });

    await page.evaluate(
      ({ targetTestId }) => {
        const el = document.querySelector(`[data-testid="${targetTestId}"]`);
        const dt = new DataTransfer();
        dt.setData("text/html", "<div>nope</div>");
        el.dispatchEvent(
          new DragEvent("drop", {
            bubbles: true,
            cancelable: true,
            dataTransfer: dt,
          }),
        );
      },
      { targetTestId: testId },
    );

    await target.dispatchEvent("dragover");
    await page.keyboard.press("Escape");

    expect(replacePosts).toEqual([]);
    await expect(page.getByText(/HTTP 500/i)).toHaveCount(0);
    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });
});
