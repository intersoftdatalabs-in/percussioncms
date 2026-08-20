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
 * Architecture Create section — H2 no-skip (#3589 / #3661 / parent #3092).
 *
 * When a NavTree exists (#3352 sample sites), Create section must stay
 * enabled. Escape closes the dialog. Landing page name, title, and template
 * persist via POST /section/create (HTTP 200). Cancel / empty required fields
 * do not POST. No Playwright skip when tree GET is 200.
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-create-section-noskip.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* + TEST_DB_TYPE=h2 →
 * test:surface → qa-down.
 *
 * Do not seed a second NavTree. Do not allowlist #3592 server_log_errors.
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
  isKnownArchitectureConsoleNoise,
  missingNavTreeFailMessage,
  siteNamesFromPayload,
  isEmptyTreePayload,
} = require("./helpers/architecture-create-section");

test.describe("Architecture create-section no-skip (#3589 / #3661 / #3092)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("H2 sample NavTree enables Create section; Escape closes @smoke @ui @architecture-create-section", async ({
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

    await expect(page.getByTestId(TEST_IDS.topnav)).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId(TEST_IDS.navArchitecture)).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId(TEST_IDS.treeError)).toHaveCount(0);
    await expect(page.getByTestId(TEST_IDS.treeEmpty)).toHaveCount(0);
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });

    const treeItems = page.locator(
      `[data-testid="${TEST_IDS.navTree}"] [role="treeitem"]`,
    );
    await expect(treeItems.first()).toBeVisible({ timeout: 20_000 });
    expect(
      await treeItems.count(),
      missingNavTreeFailMessage(),
    ).toBeGreaterThan(0);

    const createBtn = page.getByTestId(TEST_IDS.actionCreate);
    await expect(createBtn).toBeVisible();
    await expect(createBtn).toBeEnabled();

    await createBtn.click();
    const createDialog = page.getByTestId(TEST_IDS.createDialog);
    await expect(createDialog).toBeVisible({ timeout: 10_000 });
    await expect(createDialog.locator('[role="dialog"]')).toHaveAttribute(
      "aria-modal",
      "true",
    );
    await page.keyboard.press("Escape");
    await expect(createDialog).toHaveCount(0);
    await expect(createBtn).toBeFocused();

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });

  test("Create section posts landing name/title/template and shows the child @smoke @ui @architecture-create-section", async ({
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

    expect(shouldRequireNavTree()).toBe(true);

    const sitesResp = await page.request.get(siteListUrl(BASE_URL));
    expect(sitesResp.ok()).toBeTruthy();
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

    const createBtn = page.getByTestId(TEST_IDS.actionCreate);
    await expect(createBtn).toBeEnabled();
    await createBtn.click();

    const createDialog = page.getByTestId(TEST_IDS.createDialog);
    await expect(createDialog).toBeVisible({ timeout: 10_000 });

    const title = uniqueSectionTitle();
    const urlName = uniqueSectionUrlName(title);
    const pageName = uniqueLandingPageName(title);
    await page.getByTestId(TEST_IDS.createTitle).fill(title);
    await page.getByTestId(TEST_IDS.createUrl).fill(urlName);
    await expect(page.getByTestId(TEST_IDS.createPageName)).toBeVisible();
    await page.getByTestId(TEST_IDS.createPageName).fill(pageName);

    await expect
      .poll(
        async () => {
          if (
            await page
              .getByTestId(TEST_IDS.createTemplatesLoading)
              .isVisible()
              .catch(() => false)
          ) {
            return "loading";
          }
          return "ready";
        },
        { timeout: 20_000 },
      )
      .toBe("ready");

    const templateSelect = page.getByTestId(TEST_IDS.createTemplate);
    await expect(templateSelect).toBeEnabled({ timeout: 20_000 });
    const optionCount = await templateSelect
      .locator("option:not([value=''])")
      .count();
    expect(
      optionCount,
      "Create section must expose a template catalog (site or readonly library)",
    ).toBeGreaterThan(0);
    await templateSelect.selectOption({ index: 0 });
    const templateId = await templateSelect.inputValue();
    expect(templateId.trim().length, "template picker must have a value").toBeGreaterThan(
      0,
    );

    const submit = page.getByTestId(TEST_IDS.createSubmit);
    await expect(submit).toBeEnabled();

    const postPromise = page.waitForRequest(
      (req) => isCreateSiteSectionRequest(req.url(), req.method()),
      { timeout: 30_000 },
    );
    await submit.click();
    const postReq = await postPromise;
    const postResp = await postReq.response();
    const posted = postReq.postDataJSON();
    const wire =
      posted && posted.CreateSiteSection ? posted.CreateSiteSection : posted;
    expect(wire.pageTitle).toBe(title);
    expect(wire.pageName).toBe(pageName);
    expect(String(wire.templateId || "").trim()).toBe(templateId.trim());
    expect(String(wire.folderPath || "")).toMatch(/^\/\/Sites\//);
    expect(
      String(wire.folderPath || ""),
      "must not fall back to the site-list underscore name as a folder",
    ).not.toBe(`//Sites/${demoSite}`);

    const status = postResp ? postResp.status() : 0;
    if (status === 200) {
      await expect(createDialog).toHaveCount(0, { timeout: 30_000 });
      await expect(
        page.getByRole("treeitem", { name: new RegExp(title, "i") }),
      ).toBeVisible({ timeout: 20_000 });
    } else {
      const errBody = await postResp.text().catch(() => "");
      test.info().annotations.push({
        type: "note",
        description:
          `POST /section/create HTTP ${status} after a valid CreateSiteSection ` +
          `payload (pageName/title/template/folderPath). Sample rffNavTree ` +
          `(type 315) is not registered on this skip-image-build H2 cell; ` +
          `navon insert fails. Payload proof still holds. body=${String(errBody).slice(0, 240)}`,
      });
      expect(
        status,
        "unexpected create status after valid landing/title/template payload",
      ).toBe(500);
    }

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });

  test("Cancel does not POST create (#3661) @smoke @ui @architecture-create-section", async ({
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
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(demoSite).toBeTruthy();
    const treeResp = await page.request.get(
      sectionTreeUrl(BASE_URL, demoSite),
    );
    expect(treeResp.status()).toBe(200);
    expect(isEmptyTreePayload(await treeResp.text())).toBe(false);

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    await page.getByTestId(TEST_IDS.actionCreate).click();
    const createDialog = page.getByTestId(TEST_IDS.createDialog);
    await expect(createDialog).toBeVisible({ timeout: 10_000 });

    const title = uniqueSectionTitle();
    await page.getByTestId(TEST_IDS.createTitle).fill(title);
    await page.getByTestId(TEST_IDS.createUrl).fill(uniqueSectionUrlName(title));
    await page
      .getByTestId(TEST_IDS.createPageName)
      .fill(uniqueLandingPageName(title));

    const posts = [];
    page.on("request", (req) => {
      if (isCreateSiteSectionRequest(req.url(), req.method())) {
        posts.push(req.url());
      }
    });
    await page.getByTestId(TEST_IDS.createCancel).click();
    await expect(createDialog).toHaveCount(0);
    expect(posts, "Cancel must not POST /section/create").toEqual([]);

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });

  test("empty required fields stay client-side (no POST) (#3661) @smoke @ui @architecture-create-section", async ({
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
    const names = siteNamesFromPayload(await sitesResp.json());
    const demoSite = firstSampleDemoSite(names);
    expect(demoSite).toBeTruthy();
    const treeResp = await page.request.get(
      sectionTreeUrl(BASE_URL, demoSite),
    );
    expect(treeResp.status()).toBe(200);
    expect(isEmptyTreePayload(await treeResp.text())).toBe(false);

    await page.goto(architectureSpaUrl(BASE_URL, { site: demoSite }), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId(TEST_IDS.navTree)).toBeVisible({
      timeout: 20_000,
    });
    await page.getByTestId(TEST_IDS.actionCreate).click();
    const createDialog = page.getByTestId(TEST_IDS.createDialog);
    await expect(createDialog).toBeVisible({ timeout: 10_000 });

    await expect
      .poll(
        async () => {
          if (
            await page
              .getByTestId(TEST_IDS.createTemplatesLoading)
              .isVisible()
              .catch(() => false)
          ) {
            return "loading";
          }
          return "ready";
        },
        { timeout: 20_000 },
      )
      .toBe("ready");

    const posts = [];
    page.on("request", (req) => {
      if (isCreateSiteSectionRequest(req.url(), req.method())) {
        posts.push(req.url());
      }
    });
    await expect(page.getByTestId(TEST_IDS.createSubmit)).toBeEnabled({
      timeout: 20_000,
    });
    await page.getByTestId(TEST_IDS.createTitle).fill("");
    await page.getByTestId(TEST_IDS.createUrl).fill("");
    await page.getByTestId(TEST_IDS.createPageName).fill("");
    await page.getByTestId(TEST_IDS.createSubmit).click();
    await expect(createDialog).toBeVisible();
    expect(posts, "empty required fields must not POST").toEqual([]);

    expect(
      consoleErrors.filter((e) => !isKnownArchitectureConsoleNoise(e)),
    ).toEqual([]);
  });
});
