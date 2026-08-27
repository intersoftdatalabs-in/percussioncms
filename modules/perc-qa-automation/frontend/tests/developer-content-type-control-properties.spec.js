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
 * Developer Content Type control property values chrome (CD-07 / #3894).
 *
 * Admin locks a type, edits field control property values (not names-only),
 * saves via PUT .../fields/{field}/controlProperties, then GET lists the
 * new values. Unlocked save is blocked; lock is not stolen.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-control-properties.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = "3894";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapControlProperties(payload) {
  if (payload == null || typeof payload !== "object") {
    return { properties: [] };
  }
  const nested =
    payload.ContentTypeFieldControlProperties &&
    typeof payload.ContentTypeFieldControlProperties === "object"
      ? payload.ContentTypeFieldControlProperties
      : payload.contentTypeFieldControlProperties &&
          typeof payload.contentTypeFieldControlProperties === "object"
        ? payload.contentTypeFieldControlProperties
        : payload;
  let raw = nested.properties;
  if (raw == null) {
    raw = [];
  } else if (!Array.isArray(raw)) {
    if (raw.ContentTypeControlProperty) {
      raw = Array.isArray(raw.ContentTypeControlProperty)
        ? raw.ContentTypeControlProperty
        : [raw.ContentTypeControlProperty];
    } else if (typeof raw.empty === "boolean") {
      raw = [];
    } else if (raw.name || raw.value) {
      raw = [raw];
    } else {
      raw = [];
    }
  }
  return {
    fieldName: nested.fieldName,
    control: nested.control,
    properties: raw.map((p) => ({
      name: String((p && p.name) || ""),
      value: p && p.value == null ? "" : String(p.value),
    })),
  };
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
  await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-control-props"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-cp-add-name"]')).toBeDisabled({
    timeout: 15_000,
  });
  try {
    await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
      timeout: 30_000,
    });
  } catch (err) {
    if (await detailError.isVisible()) {
      throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
    }
    throw err;
  }
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
  }
  return detail;
}

async function getControlPropertiesViaRest(page, typeName, fieldName) {
  const result = await page.evaluate(
    async ({ name, field }) => {
      const res = await fetch(
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/fields/${encodeURIComponent(field)}/controlProperties`,
        { credentials: "same-origin", headers: { Accept: "application/json" } },
      );
      const text = await res.text();
      return { status: res.status, text };
    },
    { name: typeName, field: fieldName },
  );
  expect(
    result.status,
    `GET controlProperties ${result.status} ${String(result.text).slice(0, 400)}`,
  ).toBe(200);
  let json = {};
  try {
    json = JSON.parse(result.text);
  } catch {
    throw new Error(`GET controlProperties non-JSON: ${String(result.text).slice(0, 400)}`);
  }
  return unwrapControlProperties(json);
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

test.describe("Developer content type control property values (CD-07 / #3894)", () => {
  test("unlocked value editors and save stay blocked; 409 lock is not stolen", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const addName = page.locator('[data-testid="developer-ct-cp-add-name"]');
    const addValue = page.locator('[data-testid="developer-ct-cp-add-value"]');
    const addBtn = page.locator('[data-testid="developer-ct-cp-add"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-control-props"]')).toBeVisible();
    await expect(addName).toBeDisabled();
    await expect(addValue).toBeDisabled();
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

  test("Admin lock, edit control property values, GET reflects values", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percPage/);

    const fieldSelect = page.locator('[data-testid="developer-ct-cp-field"]');
    const addName = page.locator('[data-testid="developer-ct-cp-add-name"]');
    const addValue = page.locator('[data-testid="developer-ct-cp-add-value"]');
    const addBtn = page.locator('[data-testid="developer-ct-cp-add"]');
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

    await expect(fieldSelect).toBeEnabled({ timeout: 30_000 });
    const fieldOptions = await fieldSelect.locator("option").evaluateAll((opts) =>
      opts.map((o) => String(o.value || "")).filter(Boolean),
    );
    expect(fieldOptions.length, "content type has fields").toBeGreaterThan(0);

    let fieldName = "";
    let original = { properties: [] };
    for (const candidate of fieldOptions) {
      const loaded = await getControlPropertiesViaRest(page, typeName, candidate);
      if (loaded.properties.length > 0 || loaded.control) {
        fieldName = candidate;
        original = loaded;
        if (loaded.properties.length > 0) {
          break;
        }
      }
    }
    if (!fieldName) {
      fieldName = fieldOptions[0];
      original = await getControlPropertiesViaRest(page, typeName, fieldName);
    }
    if ((await fieldSelect.inputValue()) !== fieldName) {
      await fieldSelect.selectOption(fieldName);
    }
    expect(fieldName.length, "selected field name").toBeGreaterThan(0);
    await expect(
      page
        .locator('[data-testid="developer-ct-cp-empty"]')
        .or(page.locator('[data-testid="developer-ct-cp-row-0"]')),
    ).toBeVisible({ timeout: 20_000 });

    await expect(addName).toBeDisabled();
    await expect(lockBtn).toBeEnabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(addName).toBeEnabled();
    await expect(addName).toBeEditable();
    await expect(unlockBtn).toBeEnabled();

    const saveControlProps = async (label) => {
      const putWait = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" &&
          /\/contenttypes\/[^/?]+\/fields\/[^/?]+\/controlProperties(?:\?|$)/.test(res.url()),
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
      expect(putRes.status(), `${label} PUT controlProperties ${putBody}`).toBe(200);
      expect(putBody, `${label} PUT wrap`).toContain("ContentTypeFieldControlProperties");
      expect(putBody, `${label} PUT omits choices`).not.toContain('"choices"');
      await expect(notice).toContainText(/saved/i);
      return getControlPropertiesViaRest(page, typeName, fieldName);
    };

    try {
      const rows = page.locator('[data-testid^="developer-ct-cp-row-"]');
      if ((await rows.count()) > 0) {
        const valueInput = page.locator('[data-testid="developer-ct-cp-value-0"]');
        const originalValue = await valueInput.inputValue();
        const nextValue = originalValue.includes(MARKER)
          ? originalValue
          : `${originalValue}${MARKER}`;
        await valueInput.fill(nextValue);
        const afterEdit = await saveControlProps("Edit property save failed");
        expect(afterEdit.properties.length).toBeGreaterThan(0);
        expect(afterEdit.properties[0].value).toBe(nextValue);

        await valueInput.fill(originalValue);
      } else {
        await addName.fill("height");
        await addValue.fill(MARKER);
        await addBtn.click();
        const afterAdd = await saveControlProps("Add property save failed");
        const height = afterAdd.properties.find((p) => p.name === "height");
        expect(height, "GET includes added height").toBeTruthy();
        expect(height.value).toBe(MARKER);
        await page.locator('[data-testid="developer-ct-cp-remove-0"]').click();
      }

      const restored = await saveControlProps("Restore property save failed");
      expect(restored.properties.map((p) => `${p.name}=${p.value}`).sort()).toEqual(
        original.properties.map((p) => `${p.name}=${p.value}`).sort(),
      );
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
