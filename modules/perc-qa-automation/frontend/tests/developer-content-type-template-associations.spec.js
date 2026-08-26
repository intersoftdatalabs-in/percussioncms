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
 * Developer Content Type allowed-template associations chrome (CD-12 / #3783).
 *
 * Admin locks a type, replaces the allowed-template set (add/remove existing
 * ids), saves via PUT .../allowedTemplates, then GET lists the new set.
 * Unlocked save is blocked; lock is not stolen.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-template-associations.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapNamedObjectRefs(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload;
  }
  if (typeof payload !== "object") {
    return [];
  }
  if (Array.isArray(payload.NamedObjectRefList)) {
    return payload.NamedObjectRefList;
  }
  if (payload.NamedObjectRefList && typeof payload.NamedObjectRefList === "object") {
    return unwrapNamedObjectRefs(payload.NamedObjectRefList);
  }
  if (payload.NamedObjectRef) {
    return unwrapNamedObjectRefs(payload.NamedObjectRef);
  }
  if (typeof payload.empty === "boolean" && Object.keys(payload).every((k) => k === "empty")) {
    return [];
  }
  if (payload.name || payload.label || payload.guid) {
    return [payload];
  }
  return [];
}

function refName(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  return String(item.name || item.label || "").trim();
}

async function openContentTypeDetail(page, namePattern) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const empty = page.locator('[data-testid="developer-ct-empty"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    test.skip(true, "No content types in catalog — cannot exercise template associations");
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = namePattern
    ? table.locator('[data-testid^="developer-ct-row-"]').filter({ hasText: namePattern })
    : table.locator('[data-testid^="developer-ct-row-"]').filter({ hasText: /percPage/ });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-ct-row", 0));
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
  }
}

async function getAllowedTemplatesViaRest(page, typeName) {
  const result = await page.evaluate(async (name) => {
    const res = await fetch(
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/allowedTemplates`,
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    const text = await res.text();
    return { status: res.status, text };
  }, typeName);
  expect(
    result.status,
    `GET allowedTemplates ${result.status} ${String(result.text).slice(0, 400)}`,
  ).toBe(200);
  let json = {};
  try {
    json = JSON.parse(result.text);
  } catch {
    throw new Error(`GET allowedTemplates non-JSON: ${String(result.text).slice(0, 400)}`);
  }
  return unwrapNamedObjectRefs(json).map(refName).filter(Boolean);
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
  expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
}

test.describe("Developer content type template associations (CD-12 / #3783)", () => {
  test("unlocked template editors and save stay blocked; 409 lock is not stolen", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const addName = page.locator('[data-testid="developer-ct-tpl-add-name"]');
    const addBtn = page.locator('[data-testid="developer-ct-tpl-add"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-templates"]')).toBeVisible();
    await expect(addName).toBeDisabled();
    await expect(addBtn).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    await page.route("**/services/contenttypes/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(addName).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, replace allowed templates, GET lists the new set", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percPage/);

    const addName = page.locator('[data-testid="developer-ct-tpl-add-name"]');
    const addBtn = page.locator('[data-testid="developer-ct-tpl-add"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const original = await getAllowedTemplatesViaRest(page, typeName);

    await expect(addName).toBeDisabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(addName).toBeEnabled();
    await expect(unlockBtn).toBeEnabled();

    const rows = page.locator('[data-testid^="developer-ct-tpl-row-"]');
    const originalRowCount = await rows.count();
    expect(originalRowCount, "UI rows vs GET allowedTemplates").toBe(original.length);

    const saveTemplates = async (label) => {
      const putWait = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" &&
          /\/contenttypes\/[^/?]+\/allowedTemplates(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
      await expect(saveBtn).toBeEnabled();
      await saveBtn.click();
      const putRes = await putWait;
      const putBody = putRes.request().postData() || "";
      await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
      if (await saveError.isVisible()) {
        throw new Error(
          `${label}: ${(await saveError.innerText()).trim()} PUT ${putRes.status()} ${putBody}`,
        );
      }
      expect(putRes.status(), `${label} PUT allowedTemplates ${putBody}`).toBe(200);
      expect(putBody, `${label} PUT wrap`).toContain("NamedObjectRefList");
      await expect(notice).toContainText(/saved/i);
      return getAllowedTemplatesViaRest(page, typeName);
    };

    try {
      if (original.length > 0) {
        const removedName = original[original.length - 1];
        await page.locator(`[data-testid="developer-ct-tpl-remove-${original.length - 1}"]`).click();
        const afterRemove = await saveTemplates("Remove template save failed");
        const restGet = await getAllowedTemplatesViaRest(page, typeName);
        expect(restGet).toEqual(afterRemove);
        if (removedName) {
          expect(restGet).not.toContain(removedName);
        }
        expect(restGet.length).toBe(original.length - 1);

        if (removedName) {
          await addName.fill(removedName);
          await addBtn.click();
        }
      } else {
        await addName.fill("perc.page");
        await addBtn.click();
        const afterAdd = await saveTemplates("Add perc.page save failed");
        const restGet = await getAllowedTemplatesViaRest(page, typeName);
        expect(restGet).toEqual(afterAdd);
        expect(restGet).toContain("perc.page");
        await page.locator('[data-testid="developer-ct-tpl-remove-0"]').click();
      }

      const restoredUi = await saveTemplates("Restore template save failed");
      const restored = await getAllowedTemplatesViaRest(page, typeName);
      expect(restored).toEqual(restoredUi);
      expect(restored.sort()).toEqual([...original].sort());
    } finally {
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    await expect(addName).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    assertConsoleClean(pageErrors, consoleErrors);
  });
});
