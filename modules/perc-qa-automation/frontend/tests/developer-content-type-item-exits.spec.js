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
 * Developer Content Type item-level exits chrome (CD-09 / #3895 / parent #1690).
 *
 * Admin locks a type, edits item-level input translations, saves via
 * PUT .../itemExits, then GET lists the new set. Unlocked save is blocked;
 * lock is not stolen.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-item-exits.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const SAMPLE_FQN = "Java/global/percussion/generic/sys_ToUpperCase";
const SAMPLE_PARAM = "sys_title";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapItemExits(payload) {
  if (payload == null || typeof payload !== "object") {
    return {
      inputTranslations: [],
      outputTranslations: [],
      validations: [],
      preExits: [],
      postExits: [],
    };
  }
  const nested =
    payload.ContentTypeItemExits && typeof payload.ContentTypeItemExits === "object"
      ? payload.ContentTypeItemExits
      : payload.contentTypeItemExits && typeof payload.contentTypeItemExits === "object"
        ? payload.contentTypeItemExits
        : payload;
  const asList = (raw) => {
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw;
    }
    if (typeof raw !== "object") {
      return [];
    }
    if (Array.isArray(raw.ContentTypeItemExit)) {
      return raw.ContentTypeItemExit;
    }
    if (raw.ContentTypeItemExit && typeof raw.ContentTypeItemExit === "object") {
      return [raw.ContentTypeItemExit];
    }
    if (typeof raw.empty === "boolean" && Object.keys(raw).every((k) => k === "empty")) {
      return [];
    }
    if (raw.extension || raw.name) {
      return [raw];
    }
    return [];
  };
  return {
    inputTranslations: asList(nested.inputTranslations),
    outputTranslations: asList(nested.outputTranslations),
    validations: asList(nested.validations),
    preExits: asList(nested.preExits),
    postExits: asList(nested.postExits),
    maxErrorsToStopValidation: nested.maxErrorsToStopValidation,
  };
}

function exitExtension(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  return String(item.extension || item.name || "").trim();
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
    throw new Error(
      "No content types in catalog — fail closed (H2 QA must include sample types)",
    );
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-ct-row-"]').filter({
    hasText: namePattern || /percPage/,
  });
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
    throw new Error(
      `Content type detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-item-exits"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-ie-in-add-fqn"]')).toBeDisabled({
    timeout: 15_000,
  });
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-detail-loading"]')).toBeHidden({
    timeout: 15_000,
  });
  return detail;
}

async function getItemExitsViaRest(page, typeName) {
  const result = await page.evaluate(async (name) => {
    const res = await fetch(
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/itemExits`,
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    const text = await res.text();
    return { status: res.status, text };
  }, typeName);
  expect(
    result.status,
    `GET itemExits ${result.status} ${String(result.text).slice(0, 400)}`,
  ).toBe(200);
  let json = {};
  try {
    json = JSON.parse(result.text);
  } catch {
    throw new Error(`GET itemExits non-JSON: ${String(result.text).slice(0, 400)}`);
  }
  return unwrapItemExits(json);
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

test.describe("Developer content type item-level exits (CD-09 / #3895)", () => {
  test("unlocked item-exit editors and save stay blocked; 409 lock is not stolen", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const addFqn = page.locator('[data-testid="developer-ct-ie-in-add-fqn"]');
    const addBtn = page.locator('[data-testid="developer-ct-ie-in-add"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-item-exits"]')).toBeVisible();
    await expect(addFqn).toBeDisabled();
    await expect(addFqn).toHaveAttribute("readonly", "");
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
    await expect(addFqn).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, edit item-level exits, GET reflects values", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percRawHtmlAsset/);

    const addFqn = page.locator('[data-testid="developer-ct-ie-in-add-fqn"]');
    const addParam = page.locator('[data-testid="developer-ct-ie-in-add-param"]');
    const addBtn = page.locator('[data-testid="developer-ct-ie-in-add"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(page.locator('[data-testid="developer-ct-detail-name"]')).toBeVisible({
      timeout: 30_000,
    });
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const original = await getItemExitsViaRest(page, typeName);
    const originalFqns = original.inputTranslations.map(exitExtension).filter(Boolean);
    const alreadyHasSample = originalFqns.some(
      (n) => n.toLowerCase() === SAMPLE_FQN.toLowerCase(),
    );

    await expect(addFqn).toBeDisabled();
    await expect(lockBtn).toBeEnabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(addFqn).toBeEnabled();
    await expect(addFqn).toBeEditable();
    await expect(unlockBtn).toBeEnabled();

    const saveItemExits = async (label) => {
      const putWait = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" &&
          /\/contenttypes\/[^/?]+\/itemExits(?:\?|$)/.test(res.url()),
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
      expect(putRes.status(), `${label} PUT itemExits ${putBody}`).toBe(200);
      expect(putBody, `${label} PUT wrap`).toContain("ContentTypeItemExits");
      await expect(notice).toContainText(/saved/i);
      return getItemExitsViaRest(page, typeName);
    };

    try {
      if (alreadyHasSample) {
        const idx = originalFqns.findIndex(
          (n) => n.toLowerCase() === SAMPLE_FQN.toLowerCase(),
        );
        await page.locator(`[data-testid="developer-ct-ie-in-remove-${idx}"]`).click();
        const afterRemove = await saveItemExits("Remove item-exit save failed");
        const afterFqns = afterRemove.inputTranslations.map(exitExtension);
        expect(afterFqns.map((n) => n.toLowerCase())).not.toContain(SAMPLE_FQN.toLowerCase());

        await addFqn.fill(SAMPLE_FQN);
        await addParam.fill(SAMPLE_PARAM);
        await addBtn.click();
      } else {
        await addFqn.fill(SAMPLE_FQN);
        await addParam.fill(SAMPLE_PARAM);
        await addBtn.click();
        const afterAdd = await saveItemExits("Add item-exit save failed");
        const afterFqns = afterAdd.inputTranslations.map(exitExtension);
        expect(afterFqns.map((n) => n.toLowerCase())).toContain(SAMPLE_FQN.toLowerCase());

        const rowCount = await page.locator('[data-testid^="developer-ct-ie-in-row-"]').count();
        await page.locator(`[data-testid="developer-ct-ie-in-remove-${rowCount - 1}"]`).click();
      }

      const restored = await saveItemExits("Restore item-exit save failed");
      const restoredFqns = restored.inputTranslations.map(exitExtension).filter(Boolean);
      expect([...restoredFqns].sort()).toEqual([...originalFqns].sort());
    } finally {
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    await expect(addFqn).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    assertConsoleClean(pageErrors, consoleErrors);
  });
});
