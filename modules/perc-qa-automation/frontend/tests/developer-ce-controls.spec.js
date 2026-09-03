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
 * Developer CE Controls user write H2 (#4215 UI-01 / parent #1690).
 *
 * Admin create lists the row; PUT display name is visible; DELETE is 204 and
 * following GET is 404; system controls stay immutable; delete uses the in-app
 * CatalogConfirmDialog (not window.confirm).
 *
 * Consumes SPA chrome from #4213/#4214 (stack those PRs when still open).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   python docker/scripts/perc-devctl.py qa-deploy-webui
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-ce-controls.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");
const {
  confirmDeveloperCatalogDelete,
} = require("./helpers/developer-catalog-confirm");
const {
  uniqueControlName,
  ceControlPath,
  isCeControlsResponse,
} = require("./helpers/developer-ce-controls");

function developerControlsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "ce-controls",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openControlsCatalog(page) {
  await page.goto(developerControlsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ctl-panel"]');
  const empty = page.locator('[data-testid="developer-ctl-empty"]');
  const listError = page.locator('[data-testid="developer-ctl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer CE controls catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ctl-new"]')).toBeVisible();
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

/**
 * Same-origin GET/DELETE/PUT status for a CE control (session + CSRF).
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} controlName
 * @param {string} method
 * @param {object | null} body
 * @returns {Promise<number>}
 */
async function inPageControlStatus(page, controlName, method, body) {
  const url = ceControlPath(controlName);
  return page.evaluate(
    async ({ path, httpMethod, jsonBody }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = { Accept: "application/json" };
      if (token) {
        headers[headerName] = token;
      }
      const init = { method: httpMethod, credentials: "same-origin", headers };
      if (jsonBody != null) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify({ ControlDef: jsonBody });
      }
      const res = await fetch(path, init);
      return res.status;
    },
    { path: url, httpMethod: method, jsonBody: body || null },
  );
}

test.describe("Developer CE control write H2 (#4215 / UI-01)", () => {
  test("Admin create listed, PUT visible, DELETE 204/GET 404, no window.confirm", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const nativeDialogs = [];
    page.on("dialog", (dialog) => {
      nativeDialogs.push(dialog.type());
      void dialog.dismiss();
    });

    await loginAsAdmin(page);
    await openControlsCatalog(page);

    const controlName = uniqueControlName("qa4215");
    const createdLabel = `${controlName} created`;
    const updatedLabel = `${controlName} updated`;

    const postWait = page.waitForResponse(
      (r) => isCeControlsResponse(r, "POST"),
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-ctl-new"]').click();
    await expect(page.locator('[data-testid="developer-ctl-create"]')).toBeVisible();
    await page.locator('[data-testid="developer-ctl-create-name"]').fill(controlName);
    await page.locator('[data-testid="developer-ctl-create-display"]').fill(createdLabel);
    await page.locator('[data-testid="developer-ctl-create-save"]').click();

    const postRes = await postWait;
    expect(postRes.status(), "POST /services/cecontrols").toBeGreaterThanOrEqual(200);
    expect(postRes.status(), "POST /services/cecontrols").toBeLessThan(300);

    const detail = page.locator('[data-testid="developer-ctl-detail"]');
    const saveError = page.locator('[data-testid="developer-ctl-create-error"]');
    await expect(detail.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-ctl-detail-name"]')).toHaveText(
      controlName,
    );
    await expect(page.locator('[data-testid="developer-ctl-detail-title"]')).toHaveText(
      createdLabel,
    );

    await page.locator('[data-testid="developer-ctl-back"]').click();
    await expect(
      page.locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName)),
    ).toBeVisible({
      timeout: 20_000,
    });

    await page
      .locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName))
      .click();
    await expect(page.locator('[data-testid="developer-ctl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ctl-save"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ctl-delete"]')).toBeVisible();

    const putWait = page.waitForResponse(
      (r) => isCeControlsResponse(r, "PUT", controlName),
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-ctl-edit-display"]').fill(updatedLabel);
    await page.locator('[data-testid="developer-ctl-save"]').click();
    const putRes = await putWait;
    expect(putRes.status(), "PUT /services/cecontrols/{name}").toBeGreaterThanOrEqual(
      200,
    );
    expect(putRes.status(), "PUT /services/cecontrols/{name}").toBeLessThan(300);
    await expect(page.locator('[data-testid="developer-ctl-detail-notice"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ctl-detail-title"]')).toHaveText(
      updatedLabel,
    );
    await expect(page.locator('[data-testid="developer-ctl-detail-error"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-ctl-back"]').click();
    const listedOpen = page.locator(
      catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName),
    );
    await expect(listedOpen).toBeVisible({ timeout: 20_000 });
    const listedRow = page.locator('[data-testid^="developer-ctl-row-"]').filter({
      has: listedOpen,
    });
    await expect(listedRow).toContainText(updatedLabel);

    await listedOpen.click();
    await expect(page.locator('[data-testid="developer-ctl-detail"]')).toBeVisible({
      timeout: 20_000,
    });

    const deleteWait = page.waitForResponse(
      (r) => isCeControlsResponse(r, "DELETE", controlName),
      { timeout: 30_000 },
    );
    await page.locator('[data-testid="developer-ctl-delete"]').click();
    const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAttribute("role", "dialog");
    await expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(nativeDialogs, "native window.confirm must not open").toEqual([]);

    await confirmDeveloperCatalogDelete(page);
    const deleteRes = await deleteWait;
    expect(deleteRes.status(), "DELETE /services/cecontrols/{name}").toBe(204);

    await expect(page.locator('[data-testid="developer-ctl-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", controlName)),
    ).toHaveCount(0);

    const getAfterDelete = await inPageControlStatus(page, controlName, "GET");
    expect(getAfterDelete, "GET after DELETE must be 404").toBe(404);
    expect(nativeDialogs, "native window.confirm must stay unused").toEqual([]);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system control stays immutable (no Edit/Delete chrome, REST 409)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const nativeDialogs = [];
    page.on("dialog", (dialog) => {
      nativeDialogs.push(dialog.type());
      void dialog.dismiss();
    });

    await loginAsAdmin(page);
    await openControlsCatalog(page);

    await page
      .locator(catalogOpenByExactName("developer-ctl-open", "data-ctl-name", "sys_EditBox"))
      .click();
    await expect(page.locator('[data-testid="developer-ctl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ctl-detail-name"]')).toHaveText(
      "sys_EditBox",
    );
    await expect(page.locator('[data-testid="developer-ctl-system-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ctl-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ctl-delete"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ctl-edit-display"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(
      0,
    );
    expect(nativeDialogs).toEqual([]);

    const putStatus = await inPageControlStatus(page, "sys_EditBox", "PUT", {
      name: "sys_EditBox",
      displayName: "must-not-write",
    });
    expect(putStatus, "PUT system control").toBe(409);
    const deleteStatus = await inPageControlStatus(page, "sys_EditBox", "DELETE");
    expect(deleteStatus, "DELETE system control").toBe(409);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
