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
 * Live H2 REST write of content-editor system definition (CD-16 residual #4037).
 *
 * Add a uniquely named field, PUT a property patch (including empty fields after
 * first save), GET/PUT control properties, DELETE, then catalog omits it.
 * Duplicate sys_title stays 409. SPA: Admin can edit and save at least one
 * control property on System definition.
 *
 * Surface:
 *   npm run test:surface -- --path tests/developer-system-def-writes.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

function uniqueFieldName() {
  return `qa4037f${Date.now().toString(36)}`;
}

function unwrapDetail(body) {
  if (!body || typeof body !== "object") {
    return body;
  }
  const detail = body.SystemDefDetail || body;
  const rawFields = detail.fields;
  let fields = [];
  if (Array.isArray(rawFields)) {
    fields = rawFields;
  } else if (rawFields && Array.isArray(rawFields.SystemDefField)) {
    fields = rawFields.SystemDefField;
  } else if (rawFields && rawFields.SystemDefField) {
    fields = [rawFields.SystemDefField];
  }
  return { ...detail, fields };
}

function fieldNames(detail) {
  return (detail.fields || [])
    .map((f) => (f && (f.name || f.Name)) || "")
    .filter(Boolean);
}

test.describe("System def REST durable writes (#4037 / CD-16)", () => {
  test("REST: empty PUT then add/patch/delete catalog omits field", async ({
    request,
  }) => {
    test.setTimeout(60_000);
    const headers = jsonHeaders();
    const url = `${BASE_URL}/Rhythmyx/services/systemdef`;
    const fieldName = uniqueFieldName();

    const firstEmpty = await request.put(url, {
      headers,
      data: { SystemDefDetail: { fields: [] } },
    });
    expect(
      firstEmpty.status(),
      `first empty PUT ${url} must be 200 (was NPE 500 after XML rewrite)`,
    ).toBe(200);

    const secondEmpty = await request.put(url, {
      headers,
      data: { SystemDefDetail: { fields: [] } },
    });
    expect(
      secondEmpty.status(),
      `second empty PUT ${url} must stay 200 after first no-op`,
    ).toBe(200);

    const dup = await request.post(`${url}/fields`, {
      headers,
      data: { SystemDefField: { name: "sys_title" } },
    });
    expect(dup.status(), "duplicate sys_title must stay 409").toBe(409);

    const added = await request.post(`${url}/fields`, {
      headers,
      data: { SystemDefField: { name: fieldName, searchable: true } },
    });
    expect(
      added.status(),
      `POST ${url}/fields ${fieldName} must be 200 (create CONTENTSTATUS column)`,
    ).toBe(200);
    const addedDetail = unwrapDetail(await added.json());
    expect(fieldNames(addedDetail).map((n) => n.toLowerCase())).toContain(
      fieldName.toLowerCase(),
    );

    const patched = await request.put(url, {
      headers,
      data: {
        SystemDefDetail: {
          fields: { SystemDefField: [{ name: fieldName, searchable: false }] },
        },
      },
    });
    expect(patched.status(), `PUT patch ${fieldName} must be 200`).toBe(200);

    const deleted = await request.delete(
      `${url}/fields/${encodeURIComponent(fieldName)}`,
      { headers },
    );
    expect(
      deleted.status(),
      `DELETE ${fieldName} must be 204 (missing column must not 500)`,
    ).toBe(204);

    const catalog = await request.get(url, { headers });
    expect(catalog.status(), "GET catalog after delete must be 200").toBe(200);
    const after = unwrapDetail(await catalog.json());
    expect(fieldNames(after).map((n) => n.toLowerCase())).not.toContain(
      fieldName.toLowerCase(),
    );
  });

  test("REST: GET/PUT field control properties round-trip and 404", async ({
    request,
  }) => {
    test.setTimeout(60_000);
    const headers = jsonHeaders();
    const url = `${BASE_URL}/Rhythmyx/services/systemdef`;
    const fieldName = uniqueFieldName();

    const added = await request.post(`${url}/fields`, {
      headers,
      data: { SystemDefField: { name: fieldName, searchable: true } },
    });
    expect(added.status(), `POST ${url}/fields ${fieldName}`).toBe(200);

    const cpUrl = `${url}/fields/${encodeURIComponent(fieldName)}/controlProperties`;
    const got = await request.get(cpUrl, { headers });
    expect(got.status(), `GET ${cpUrl}`).toBe(200);
    const gotBody = unwrapControlProperties(await got.json());
    expect(Array.isArray(gotBody.properties)).toBe(true);

    const put = await request.put(cpUrl, {
      headers,
      data: {
        SystemDefControlProperties: {
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

    const round = await request.get(cpUrl, { headers });
    expect(round.status(), `GET round-trip ${cpUrl}`).toBe(200);
    const roundBody = unwrapControlProperties(await round.json());
    expect(
      roundBody.properties.some(
        (p) => p && p.name === "width" && String(p.value) === "640",
      ),
      `GET round-trip properties ${JSON.stringify(roundBody.properties)}`,
    ).toBe(true);

    const missing = await request.get(
      `${url}/fields/${encodeURIComponent("noSuchSysField4440")}/controlProperties`,
      { headers },
    );
    expect(missing.status(), "unknown field GET must be 404").toBe(404);

    const unsafe = await request.get(
      `${url}/fields/${encodeURIComponent("..")}/controlProperties`,
      { headers },
    );
    expect(unsafe.status(), "unsafe field GET must be 404").toBe(404);

    const deleted = await request.delete(
      `${url}/fields/${encodeURIComponent(fieldName)}`,
      { headers },
    );
    expect(deleted.status(), `DELETE ${fieldName}`).toBe(204);
  });
});

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
    body.SystemDefControlProperties || body.systemDefControlProperties || body;
  const properties = flattenControlProperties(nested.properties);
  return { ...nested, properties };
}

function developerSystemDefUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "system-def",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

test.describe("System def SPA control properties (#4440 / CD-16)", () => {
  test("Admin can view and save a system-def control property", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await page.goto(developerSystemDefUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    const panel = page.locator('[data-testid="developer-sys-panel"]');
    const listError = page.locator('[data-testid="developer-sys-error"]');
    await expect(panel.or(listError).first()).toBeVisible({ timeout: 30_000 });
    if (await listError.isVisible()) {
      throw new Error(
        `Developer system def catalog error: ${(await listError.innerText()).trim()}`,
      );
    }

    const section = page.locator('[data-testid="developer-sys-control-props"]');
    await expect(section).toBeVisible();
    const fieldSelect = page.locator('[data-testid="developer-sys-cp-field"]');
    await expect(fieldSelect).toBeVisible();
    await fieldSelect.selectOption("sys_title");

    const empty = page.locator('[data-testid="developer-sys-cp-empty"]');
    const firstValue = page.locator('[data-testid="developer-sys-cp-value-0"]');
    await expect(empty.or(firstValue).first()).toBeVisible({ timeout: 20_000 });

    if (await empty.isVisible()) {
      await page.locator('[data-testid="developer-sys-cp-add-name"]').fill("qa4440w");
      await page.locator('[data-testid="developer-sys-cp-add-value"]').fill("111");
      await page.locator('[data-testid="developer-sys-cp-add"]').click();
    } else {
      const current = await firstValue.inputValue();
      await firstValue.fill(current === "240" ? "241" : "240");
    }

    const save = page.locator('[data-testid="developer-sys-cp-save"]');
    await expect(save).toBeEnabled();
    await save.click();
    await expect(page.locator('[data-testid="developer-sys-notice"]')).toContainText(
      /control properties saved/i,
      { timeout: 20_000 },
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
