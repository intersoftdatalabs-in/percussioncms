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
 * Developer Shared Fields create / save / delete chrome (#4029 CD-15 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-shared-fields-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerSharedFieldsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "shared-fields",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique group name (no spaces or path characters). */
function uniqueGroupName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4029${suffix || "x"}`;
}

async function openSharedFieldsCatalog(page) {
  await page.goto(developerSharedFieldsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-sf-panel"]');
  const empty = page.locator('[data-testid="developer-sf-empty"]');
  const listError = page.locator('[data-testid="developer-sf-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer shared fields catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-sf-new"]')).toBeVisible();
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

test.describe("Developer shared field group editor (#4029 / CD-15)", () => {
  test("Admin can create, save, and delete a shared field group", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    const groupName = uniqueGroupName();
    const savedFilename = `${groupName}saved.xml`;

    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-sf-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-sf-name"]').fill(groupName);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-sf-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(notice).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-sf-name"]')).toHaveValue(groupName);

    await page.locator('[data-testid="developer-sf-filename"]').fill(savedFilename);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-sf-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-sf-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-sf-name="${groupName}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate group name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sf-name"]').fill("shared");
    await page.locator('[data-testid="developer-sf-save"]').click();

    const err = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

function uniqueNestedGroupName() {
  const uuid = crypto.randomUUID().replace(/-/g, "").slice(0, 12);
  return `qa4439g${uuid}`.slice(0, 24);
}

function uniqueFieldName() {
  const uuid = crypto.randomUUID().replace(/-/g, "").slice(0, 12);
  return `qa4439f${uuid}`.slice(0, 24);
}

function flattenFields(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.flatMap(flattenFields);
  }
  if (typeof raw !== "object") {
    return [];
  }
  for (const key of [
    "SharedField",
    "sharedField",
    "SharedFieldSummary",
    "sharedFieldSummary",
    "fields",
  ]) {
    if (raw[key] != null) {
      return flattenFields(raw[key]);
    }
  }
  if (raw.name || raw.Name) {
    return [raw];
  }
  return [];
}

function unwrapGroupDetail(body) {
  if (!body || typeof body !== "object") {
    return { fields: [] };
  }
  const detail = body.SharedFieldGroupDetail || body.sharedFieldGroupDetail || body;
  return { ...detail, fields: flattenFields(detail.fields) };
}

function fieldNames(detail) {
  return (detail.fields || [])
    .map((f) => (f && (f.name || f.Name)) || "")
    .filter(Boolean);
}

function flattenControlProperties(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.flatMap(flattenControlProperties);
  }
  if (typeof raw !== "object") {
    return [];
  }
  if (raw.ContentTypeControlProperty != null) {
    return flattenControlProperties(raw.ContentTypeControlProperty);
  }
  if (raw.contentTypeControlProperty != null) {
    return flattenControlProperties(raw.contentTypeControlProperty);
  }
  const name = raw.name || raw.Name;
  const value = raw.value != null ? raw.value : raw.Value;
  if (name != null || value != null) {
    return [{ name, value }];
  }
  return [];
}

function unwrapControlProperties(body) {
  if (!body || typeof body !== "object") {
    return { properties: [] };
  }
  const nested =
    body.SharedFieldControlProperties || body.sharedFieldControlProperties || body;
  const properties = flattenControlProperties(nested.properties);
  return { ...nested, properties };
}

test.describe("Developer shared nested field editor (#4439 / CD-15)", () => {
  test("REST: add nested field, PUT control properties, delete, 409/404", async ({
    request,
  }) => {
    test.setTimeout(90_000);
    const headers = jsonHeaders();
    const groupName = uniqueNestedGroupName();
    const fieldName = uniqueFieldName();
    const groupsUrl = `${BASE_URL}/Rhythmyx/services/sharedfields`;

    const created = await request.post(groupsUrl, {
      headers,
      data: { SharedFieldGroupDetail: { name: groupName } },
    });
    expect(created.status(), `POST group ${groupName}`).toBe(200);

    const fieldsUrl = `${groupsUrl}/${encodeURIComponent(groupName)}/fields`;
    const added = await request.post(fieldsUrl, {
      headers,
      data: { SharedField: { name: fieldName, dataType: "text", occurrence: "optional" } },
    });
    expect(added.status(), `POST nested field ${fieldName}`).toBe(200);
    let addedDetail = unwrapGroupDetail(await added.json());
    if (fieldNames(addedDetail).length === 0) {
      const gotGroup = await request.get(
        `${groupsUrl}/${encodeURIComponent(groupName)}`,
        { headers },
      );
      expect(gotGroup.status(), "GET group after add").toBe(200);
      addedDetail = unwrapGroupDetail(await gotGroup.json());
    }
    expect(fieldNames(addedDetail).map((n) => n.toLowerCase())).toContain(
      fieldName.toLowerCase(),
    );

    const dup = await request.post(fieldsUrl, {
      headers,
      data: { SharedField: { name: fieldName } },
    });
    expect(dup.status(), "duplicate nested field must be 409").toBe(409);

    const cpUrl = `${fieldsUrl}/${encodeURIComponent(fieldName)}/controlProperties`;
    const got = await request.get(cpUrl, { headers });
    expect(got.status(), `GET ${cpUrl}`).toBe(200);

    const put = await request.put(cpUrl, {
      headers,
      data: {
        SharedFieldControlProperties: {
          properties: [{ name: "width", value: "640" }],
        },
      },
    });
    expect(put.status(), `PUT ${cpUrl}`).toBe(200);
    const putBody = unwrapControlProperties(await put.json());
    expect(
      putBody.properties.some((p) => p && p.name === "width" && String(p.value) === "640"),
      `PUT properties ${JSON.stringify(putBody.properties)}`,
    ).toBe(true);

    const missingField = await request.get(
      `${fieldsUrl}/${encodeURIComponent("noSuchSharedField4439")}/controlProperties`,
      { headers },
    );
    expect(missingField.status(), "unknown field GET must be 404").toBe(404);

    const missingGroup = await request.get(
      `${groupsUrl}/${encodeURIComponent("noSuchSharedGroup4439")}`,
      { headers },
    );
    expect(missingGroup.status(), "unknown group GET must be 404").toBe(404);

    const deletedField = await request.delete(
      `${fieldsUrl}/${encodeURIComponent(fieldName)}`,
      { headers },
    );
    expect(deletedField.status(), `DELETE nested field ${fieldName}`).toBe(204);

    const catalog = await request.get(
      `${groupsUrl}/${encodeURIComponent(groupName)}`,
      { headers },
    );
    expect(catalog.status()).toBe(200);
    const after = unwrapGroupDetail(await catalog.json());
    expect(fieldNames(after).map((n) => n.toLowerCase())).not.toContain(
      fieldName.toLowerCase(),
    );

    const deletedGroup = await request.delete(
      `${groupsUrl}/${encodeURIComponent(groupName)}`,
      { headers },
    );
    expect(deletedGroup.status(), `DELETE group ${groupName}`).toBe(204);
  });

  test("Admin can add a nested field, save control properties, and delete it", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    const groupName = uniqueNestedGroupName();
    const fieldName = uniqueFieldName();

    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sf-name"]').fill(groupName);
    await page.locator('[data-testid="developer-sf-save"]').click();
    const notice = page.locator('[data-testid="developer-sf-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create group failed: ${(await saveError.innerText()).trim()}`);
    }

    const addBtn = page.locator('[data-testid="developer-sf-add-btn"]');
    await expect(addBtn).toBeDisabled();
    await page.locator('[data-testid="developer-sf-new-name"]').fill(fieldName);
    await page.locator('[data-testid="developer-sf-new-datatype"]').selectOption("text");
    await page.locator('[data-testid="developer-sf-new-occurrence"]').selectOption("optional");
    await expect(addBtn).toBeEnabled();
    await addBtn.click();
    await expect(page.locator(`[data-sf-field="${fieldName}"]`)).toBeVisible({
      timeout: 20_000,
    });

    const section = page.locator('[data-testid="developer-sf-control-props"]');
    await expect(section).toBeVisible();
    const fieldSelect = page.locator('[data-testid="developer-sf-cp-field"]');
    await expect(fieldSelect).toBeVisible();
    await fieldSelect.selectOption(fieldName);

    const empty = page.locator('[data-testid="developer-sf-cp-empty"]');
    const firstValue = page.locator('[data-testid="developer-sf-cp-value-0"]');
    await expect(empty.or(firstValue).first()).toBeVisible({ timeout: 20_000 });
    if (await empty.isVisible()) {
      await page.locator('[data-testid="developer-sf-cp-add-name"]').fill("qa4439w");
      await page.locator('[data-testid="developer-sf-cp-add-value"]').fill("111");
      await page.locator('[data-testid="developer-sf-cp-add"]').click();
    } else {
      const current = await firstValue.inputValue();
      await firstValue.fill(current === "240" ? "241" : "240");
    }

    const saveCp = page.locator('[data-testid="developer-sf-cp-save"]');
    await expect(saveCp).toBeEnabled();
    await saveCp.click();
    await expect(page.locator('[data-testid="developer-sf-editor-notice"]')).toContainText(
      /control properties saved/i,
      { timeout: 20_000 },
    );

    await page
      .locator(`[data-sf-field="${fieldName}"] [data-testid="developer-sf-field-delete"]`)
      .click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator(`[data-sf-field="${fieldName}"]`)).toHaveCount(0, {
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-sf-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-sf-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
