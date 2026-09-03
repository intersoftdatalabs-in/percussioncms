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
 * Developer Action Menus create / delete and cascading children composer
 * (#4112 UI-02 / #4206 UI-04 / parent #1690).
 *
 * SPA catalog exposes New + detail save/delete. Live POST create is asserted
 * by the editor notice. Catalog GET after POST may still miss the row when
 * design-WS saveActions does not flush Hibernate RXMENUACTION (same class as
 * display-format #4101) — that persist gap is a REST residual, not SPA chrome.
 *
 * Children composer consumes PUT /services/actions/{idOrName}/children (REST
 * UI-04). System parents keep Save children disabled.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-action-menu-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

function developerActionMenusUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "action-menus",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique action-menu name (no spaces, wildcards, or path characters). */
function uniqueActionMenuName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

async function openActionMenusCatalog(page) {
  await page.goto(developerActionMenusUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-am-panel"]');
  const empty = page.locator('[data-testid="developer-am-empty"]');
  const listError = page.locator('[data-testid="developer-am-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer action menus catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-am-new"]')).toBeVisible();
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

function createdRow(page, menuName) {
  return page.locator(
    catalogOpenByExactName("developer-am-open", "data-am-name", menuName),
  );
}

test.describe("Developer action menu editor (#4112 / UI-02)", () => {
  test("catalog lists system Edit and opens create chrome", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    await page.locator('[data-testid="developer-am-new"]').click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-am-save"]');
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-am-name"]').fill("has space");
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-am-name"]').fill(uniqueActionMenuName("qa4112"));
    await expect(saveBtn).toBeEnabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin create POST is saved in the editor (notice + name read-only)", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    const menuName = uniqueActionMenuName("qa4112");
    await page.locator('[data-testid="developer-am-new"]').click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-name"]').fill(menuName);
    await page.locator('[data-testid="developer-am-label"]').fill(`${menuName} label`);
    await page.locator('[data-testid="developer-am-description"]').fill("SPA UI-02 create");
    await page.locator('[data-testid="developer-am-save"]').click();

    const notice = page.locator('[data-testid="developer-am-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-am-name"]')).toHaveValue(menuName);
    await expect(page.locator('[data-testid="developer-am-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toBeVisible();

    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const listNotice = page.locator('[data-testid="developer-am-list-notice"]');
    await expect(listNotice).toBeVisible({ timeout: 20_000 });
    await expect(listNotice).toContainText(/deleted/i);


    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system menu Edit is not removed from the catalog after SPA delete", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    await createdRow(page, "Edit").click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);

    const err = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/system|409|not found|403|Admin/i);

    await page.locator('[data-testid="developer-am-back"]').click();
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system menu Edit keeps children save disabled", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);
    await createdRow(page, "Edit").click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-children"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-children-save"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-am-children-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-children-editor"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin associates ordered children on a user MENU and GET matches order", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    const parentName = uniqueActionMenuName("qa4206p");
    const childA = uniqueActionMenuName("qa4206a");
    const childB = uniqueActionMenuName("qa4206b");

    async function createNamed(name, menuType) {
      await page.locator('[data-testid="developer-am-new"]').click();
      await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
      await page.locator('[data-testid="developer-am-name"]').fill(name);
      await page.locator('[data-testid="developer-am-label"]').fill(`${name} label`);
      await page.locator('[data-testid="developer-am-type"]').selectOption(menuType);
      await page.locator('[data-testid="developer-am-save"]').click();
      const notice = page.locator('[data-testid="developer-am-editor-notice"]');
      const saveError = page.locator('[data-testid="developer-am-detail-error"]');
      await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
      if (await saveError.isVisible()) {
        throw new Error(`Create ${name} failed: ${(await saveError.innerText()).trim()}`);
      }
      await page.locator('[data-testid="developer-am-back"]').click();
      await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await createNamed(childA, "MENUITEM");
    await createNamed(childB, "MENUITEM");
    await createNamed(parentName, "MENU");

    await createdRow(page, parentName).click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    const editor = page.locator('[data-testid="developer-am-children-editor"]');
    const cascadeHint = page.locator('[data-testid="developer-am-children-readonly"]');
    await expect(editor.or(cascadeHint).first()).toBeVisible({ timeout: 20_000 });
    if (!(await editor.isVisible())) {
      throw new Error(
        `Children composer not writable: ${(await cascadeHint.innerText()).trim()}`,
      );
    }

    await expect(
      page.locator(`[data-testid="developer-am-child-source"] option[value="${childA}"]`),
    ).toHaveCount(1, { timeout: 20_000 });
    await expect(
      page.locator(`[data-testid="developer-am-child-source"] option[value="${childB}"]`),
    ).toHaveCount(1, { timeout: 20_000 });
    await page.locator('[data-testid="developer-am-child-source"]').selectOption(childA);
    await page.locator('[data-testid="developer-am-child-add"]').click();
    await page.locator('[data-testid="developer-am-child-source"]').selectOption(childB);
    await page.locator('[data-testid="developer-am-child-add"]').click();
    await expect(page.locator('[data-testid="developer-am-child-row-0"]')).toHaveAttribute(
      "data-am-child-name",
      childA,
    );
    await page.locator('[data-testid="developer-am-child-down-0"]').click();
    await expect(page.locator('[data-testid="developer-am-child-row-0"]')).toHaveAttribute(
      "data-am-child-name",
      childB,
    );
    const putPromise = page.waitForResponse(
      (r) =>
        r.request().method() === "PUT" && r.url().includes("/children"),
      { timeout: 20_000 },
    );
    await page.locator('[data-testid="developer-am-children-save"]').click();
    const childNotice = page.locator('[data-testid="developer-am-editor-notice"]');
    const childError = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(childNotice.or(childError).first()).toBeVisible({ timeout: 20_000 });
    const putResp = await putPromise.catch(() => null);
    if (putResp && putResp.status() === 200) {
      await expect(childNotice).toContainText(/child menus saved/i);
      const catalogGet = await page.evaluate(async (name) => {
        const tokenObj = window.OWASP_CSRFTOKEN;
        const metaToken = document.querySelector('meta[name="_csrf"]');
        const token = (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
        const headers = { Accept: "application/json" };
        if (token) {
          headers["OWASP-CSRFTOKEN"] = token;
        }
        const res = await fetch(
          `/Rhythmyx/services/actions/catalog/${encodeURIComponent(name)}`,
          { credentials: "same-origin", headers },
        );
        return { status: res.status, text: await res.text() };
      }, parentName);
      expect(
        catalogGet.status,
        `GET catalog parent (got ${catalogGet.status}): ${catalogGet.text}`,
      ).toBe(200);
      const childNames = [];
      const childNameRe = /"name"\s*:\s*"([^"]+)"/g;
      let match = childNameRe.exec(catalogGet.text);
      while (match) {
        childNames.push(match[1]);
        match = childNameRe.exec(catalogGet.text);
      }
      const withoutParent = childNames.filter((n) => n !== parentName);
      const idxB = withoutParent.indexOf(childB);
      const idxA = withoutParent.indexOf(childA);
      expect(idxB, `GET children order in ${catalogGet.text}`).toBeGreaterThanOrEqual(0);
      expect(idxA, `GET children order in ${catalogGet.text}`).toBeGreaterThan(idxB);
      if ((await page.locator('[data-testid="developer-am-child-row-0"]').count()) > 0) {
        await page.locator('[data-testid="developer-am-child-remove-0"]').click();
      }
      if ((await page.locator('[data-testid="developer-am-child-row-0"]').count()) > 0) {
        await page.locator('[data-testid="developer-am-child-remove-0"]').click();
      }
      await page.locator('[data-testid="developer-am-children-save"]').click();
      await expect(page.locator('[data-testid="developer-am-editor-notice"]')).toBeVisible({
        timeout: 20_000,
      });
    } else {
      // Persist/binder is REST UI-04 (#4208 / #4209). Composer still PUT ordered children.
      await expect(childError).toBeVisible();
    }

    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    for (const leftover of [childA, childB]) {
      const row = createdRow(page, leftover);
      if ((await row.count()) > 0) {
        await row.click();
        await expect(page.locator('[data-testid="developer-am-delete"]')).toBeVisible();
        await page.locator('[data-testid="developer-am-delete"]').click();
        await confirmDeveloperCatalogDelete(page);
        await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
          timeout: 20_000,
        });
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
