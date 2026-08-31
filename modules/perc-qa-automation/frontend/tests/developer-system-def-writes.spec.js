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
 * first save), DELETE, then catalog omits it. Duplicate sys_title stays 409.
 *
 * Surface:
 *   npm run test:surface -- --path tests/developer-system-def-writes.spec.js
 */

const { test, expect } = require("@playwright/test");
const { BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");

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
});
