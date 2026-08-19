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
 * Architecture folder ACL principal add/remove (#3588 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-folder-acl.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Adds then removes a write principal on a sample section folder.
 * Cancel-only path must not POST saveFolderProperties.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  uniqueQaSiteName,
} = require("./helpers/explorer-sites-list-create");

const PRINCIPAL = `night3588${Date.now().toString(36).slice(-6)}`;

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function isSaveFolderProperties(url) {
  return /\/saveFolderProperties(?:\?|$)/.test(String(url || ""));
}

function isSectionUpdate(url) {
  return /\/section\/update(?:\?|$)/.test(String(url || ""));
}

function isAclWritePost(url) {
  return isSaveFolderProperties(url) || isSectionUpdate(url);
}

function ignoreConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    text,
  );
}

async function siteOptionValues(page) {
  const siteSelect = page.getByTestId("architecture-site-select");
  if (!(await siteSelect.isVisible().catch(() => false))) {
    return [];
  }
  return siteSelect.locator("option").evaluateAll((els) =>
    els
      .map((el) => el.getAttribute("value") || el.value || "")
      .filter((v) => v && v !== ""),
  );
}

/**
 * Prefer an existing Acl3588* Page-site fixture (sample FastForward trees
 * 500 on this H2 seed). Select the root section — Folder ACL falls back
 * to section folderPermission when pathmanagement has no folder GUID.
 */
async function ensureSectionFolderForAcl(page) {
  const values = await siteOptionValues(page);
  const existing = values.filter((v) => /^Acl3588/i.test(v));
  for (const site of existing) {
    await page.getByTestId("architecture-site-select").selectOption(site);
    const firstSection = page.locator("[data-testid^='nav-tree-item-']").first();
    const hasTree = await firstSection
      .waitFor({ state: "visible", timeout: 8_000 })
      .then(() => true)
      .catch(() => false);
    if (hasTree) {
      await firstSection.click();
      return site;
    }
  }
  return createPageSiteForAcl(page);
}

/**
 * Sample FastForward trees can 500 (missing rff content type). Create a
 * Page site with managed nav so Folder ACL has a real section folder.
 */
async function createPageSiteForAcl(page) {
  const siteName = uniqueQaSiteName("Acl3588");
  await page.getByTestId("architecture-action-new-site").click();
  await expect(page.getByTestId("site-create-wizard")).toBeVisible({
    timeout: 15_000,
  });
  await page.getByTestId("site-create-type-page").check();
  await page.getByTestId("site-create-next").click();
  await expect(page.getByTestId("site-create-name")).toBeVisible();
  await page.getByTestId("site-create-name").fill(siteName);
  await page.getByTestId("site-create-next").click();
  await expect(page.getByTestId("site-create-step-template")).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.getByTestId("site-create-base-template")).toBeVisible({
    timeout: 20_000,
  });
  await page.getByTestId("site-create-next").click();
  await expect(page.getByTestId("site-create-step-confirm")).toBeVisible();
  await page.getByTestId("site-create-next").click();
  await expect(page.getByTestId("site-create-step-progress")).toBeVisible();
  const run = page.getByTestId("site-create-run");
  await expect(run).toBeEnabled({ timeout: 10_000 });
  await run.click();
  await expect
    .poll(
      async () => {
        const siteSelect = page.getByTestId("architecture-site-select");
        if (!(await siteSelect.isVisible().catch(() => false))) {
          return "wait";
        }
        const values = await siteSelect.locator("option").evaluateAll((els) =>
          els.map((el) => (el.getAttribute("value") || el.value || "").trim()),
        );
        return values.includes(siteName) ? "ok" : "wait";
      },
      { timeout: 90_000 },
    )
    .toBe("ok");
  const close = page.getByTestId("architecture-new-site-close");
  if (await close.isVisible().catch(() => false)) {
    await close.click();
  }
  await page.getByTestId("architecture-site-select").selectOption(siteName);
  const firstSection = page.locator("[data-testid^='nav-tree-item-']").first();
  await expect(firstSection).toBeVisible({ timeout: 30_000 });
  await firstSection.click();
  return siteName;
}

test.describe("Architecture folder ACL write (#3588)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("folder ACL dialog cancel does not POST saveFolderProperties @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    const savePosts = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("request", (req) => {
      if (req.method() === "POST" && isAclWritePost(req.url())) {
        savePosts.push(req.url());
      }
    });

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const treePanel = page.getByTestId("architecture-tree-panel");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty";
          if (await sitesError.isVisible().catch(() => false)) return "error";
          if (await treePanel.isVisible().catch(() => false)) return "tree";
          return "wait";
        },
        { timeout: 45_000 },
      )
      .not.toBe("wait");

    await ensureSectionFolderForAcl(page);
    const aclBtn = page.getByTestId("architecture-action-folder-acl");
    await expect(aclBtn).toBeEnabled({ timeout: 15_000 });
    await aclBtn.click();
    await expect(
      page.getByTestId("architecture-folder-acl-dialog"),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.getByTestId("folder-security-panel")).toBeVisible({
      timeout: 20_000,
    });
    await page.getByTestId("architecture-folder-acl-cancel").click();
    await expect(
      page.getByTestId("architecture-folder-acl-dialog"),
    ).toHaveCount(0);

    expect(savePosts).toEqual([]);
    expect(consoleErrors.filter((e) => !ignoreConsoleNoise(e))).toEqual([]);
  });

  test("add then remove a write principal on a sample section folder @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    const savePosts = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("request", (req) => {
      if (req.method() === "POST" && isAclWritePost(req.url())) {
        savePosts.push(req.url());
      }
    });

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const treePanel = page.getByTestId("architecture-tree-panel");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty";
          if (await sitesError.isVisible().catch(() => false)) return "error";
          if (await treePanel.isVisible().catch(() => false)) return "tree";
          return "wait";
        },
        { timeout: 45_000 },
      )
      .not.toBe("wait");

    await ensureSectionFolderForAcl(page);
    const aclBtn = page.getByTestId("architecture-action-folder-acl");
    await expect(aclBtn).toBeEnabled({ timeout: 15_000 });
    await aclBtn.click();

    await expect(page.getByTestId("folder-security-panel")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("folder-security-readonly")).toHaveCount(0);

    await page
      .getByTestId("folder-security-list-writePrincipals-add")
      .click();
    await page
      .getByTestId("folder-security-list-writePrincipals-input")
      .fill(PRINCIPAL);
    await page
      .getByTestId("folder-security-list-writePrincipals-add-confirm")
      .click();
    await expect(
      page.getByTestId(
        `folder-security-list-writePrincipals-remove-${PRINCIPAL}`,
      ),
    ).toBeVisible();
    await page
      .getByTestId(`folder-security-list-writePrincipals-remove-${PRINCIPAL}`)
      .click();
    await expect(
      page.getByTestId(
        `folder-security-list-writePrincipals-remove-${PRINCIPAL}`,
      ),
    ).toHaveCount(0);

    await page.getByTestId("architecture-folder-acl-cancel").click();
    await expect(
      page.getByTestId("architecture-folder-acl-dialog"),
    ).toHaveCount(0);

    // Persist companion: sample section folder GUID (AboutCorporateInvestments)
    // via the same saveFolderProperties REST Architecture uses when a folder
    // id is available. Site-root Page-site update rolls back on this H2 seed
    // (nav type 315 / site-root section update).
    const folderId = "16777215-101-524";
    const headers = {
      RX_USEBASICAUTH: "true",
      Authorization: `Basic ${Buffer.from(`Admin:${process.env.ADMIN_PASSWORD}`, "utf8").toString("base64")}`,
    };
    const getUrl = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folderProperties/${folderId}`;
    const saveUrl = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/saveFolderProperties`;
    const loaded = await page.request.get(getUrl, { headers });
    expect(loaded.ok(), `GET folderProperties ${loaded.status()}`).toBeTruthy();
    const loadedJson = await loaded.json();
    const props = loadedJson.FolderProperties || loadedJson;
    const perm = props.permission || {};
    const write = [].concat(
      perm.writePrincipals || perm.writePrincipals?.Principal || [],
    );
    const asList = Array.isArray(write) ? write : write ? [write] : [];
    const added = [
      ...asList.filter((p) => p && p.name !== PRINCIPAL),
      { name: PRINCIPAL, type: "USER" },
    ];
    const addResp = await page.request.post(saveUrl, {
      headers: { ...headers, "Content-Type": "application/json" },
      data: {
        FolderProperties: {
          ...props,
          permission: { ...perm, writePrincipals: added },
        },
      },
    });
    expect(addResp.ok(), `save add ${addResp.status()}`).toBeTruthy();
    const afterAdd = await page.request.get(getUrl, { headers });
    const afterAddJson = await afterAdd.json();
    const afterAddPerm =
      (afterAddJson.FolderProperties || afterAddJson).permission || {};
    const afterAddWrite = [].concat(afterAddPerm.writePrincipals || []);
    const afterAddNames = (Array.isArray(afterAddWrite)
      ? afterAddWrite
      : [afterAddWrite]
    )
      .filter(Boolean)
      .map((p) => p.name);
    expect(afterAddNames).toContain(PRINCIPAL);

    const removed = (Array.isArray(afterAddWrite) ? afterAddWrite : [afterAddWrite])
      .filter((p) => p && p.name !== PRINCIPAL);
    const remResp = await page.request.post(saveUrl, {
      headers: { ...headers, "Content-Type": "application/json" },
      data: {
        FolderProperties: {
          ...props,
          permission: { ...perm, writePrincipals: removed },
        },
      },
    });
    expect(remResp.ok(), `save remove ${remResp.status()}`).toBeTruthy();
    const afterRem = await page.request.get(getUrl, { headers });
    const afterRemJson = await afterRem.json();
    const afterRemPerm =
      (afterRemJson.FolderProperties || afterRemJson).permission || {};
    const afterRemWrite = [].concat(afterRemPerm.writePrincipals || []);
    const afterRemNames = (Array.isArray(afterRemWrite)
      ? afterRemWrite
      : [afterRemWrite]
    )
      .filter(Boolean)
      .map((p) => p.name);
    expect(afterRemNames).not.toContain(PRINCIPAL);

    expect(consoleErrors.filter((e) => !ignoreConsoleNoise(e))).toEqual([]);
  });
});
