/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
 * Live Admin GET/PUT /services/locales/auto-translations persist (#4039 / CD-18).
 *
 * No PUT intercept — hits the real design-lock + Hibernate path on H2 QA.
 * SPA table chrome (#4028 / #4044) is out of scope.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-auto-translations-persist.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const AT_PATH = "/Rhythmyx/services/locales/auto-translations";

function developerLocalesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "locales",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapRows(body) {
  if (Array.isArray(body)) {
    return body;
  }
  if (!body || typeof body !== "object") {
    return [];
  }
  if (Array.isArray(body.AutoTranslationRow)) {
    return body.AutoTranslationRow;
  }
  if (Array.isArray(body.autoTranslationRow)) {
    return body.autoTranslationRow;
  }
  return [];
}

function jsonHeaders() {
  return {
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

async function getAutoTranslations(page) {
  const res = await page.request.get(`${BASE_URL}${AT_PATH}`, {
    headers: { Accept: "application/json" },
  });
  const text = await res.text();
  let body = null;
  try {
    body = text ? JSON.parse(text) : [];
  } catch {
    throw new Error(
      `GET ${AT_PATH} ${res.status()} non-JSON: ${String(text).slice(0, 400)}`,
    );
  }
  return { status: res.status(), rows: unwrapRows(body), text };
}

async function putAutoTranslations(page, rows) {
  const url = `${BASE_URL}${AT_PATH}`;
  const payloads = [rows, { AutoTranslationRow: rows }];
  let last = { status: 0, text: "", rows: [] };
  for (const data of payloads) {
    const res = await page.request.put(url, { headers: jsonHeaders(), data });
    const text = await res.text();
    let body = null;
    try {
      body = text ? JSON.parse(text) : [];
    } catch {
      body = null;
    }
    last = {
      status: res.status(),
      text,
      rows: unwrapRows(body),
    };
    if (res.ok()) {
      return last;
    }
    if (res.status() !== 400) {
      return last;
    }
  }
  return last;
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

function firstName(list, keys) {
  if (!Array.isArray(list) || list.length === 0) {
    return null;
  }
  for (const key of keys) {
    const v = list[0] && list[0][key];
    if (v) {
      return String(v);
    }
  }
  return null;
}

async function catalogNames(page) {
  const localesRes = await page.request.get(`${BASE_URL}/Rhythmyx/services/locales`, {
    headers: { Accept: "application/json" },
  });
  const typesRes = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes`,
    { headers: { Accept: "application/json" } },
  );
  let locales = [];
  let types = [];
  try {
    const locBody = await localesRes.json();
    locales = Array.isArray(locBody)
      ? locBody
      : locBody?.LocaleSummary || locBody?.Locale || locBody?.locale || [];
  } catch {
    locales = [];
  }
  try {
    const typeBody = await typesRes.json();
    types = Array.isArray(typeBody)
      ? typeBody
      : typeBody?.ContentType || typeBody?.contentType || [];
  } catch {
    types = [];
  }
  const localeName =
    locales
      .map((l) => l.languageString || l.language || l.name)
      .map((s) => (s ? String(s) : ""))
      .find((s) => /^en-us$/i.test(s)) ||
    firstName(locales, ["languageString", "language", "name"]) ||
    "en-us";
  const typeName =
    types
      .map((t) => t.name || t.label)
      .map((s) => (s ? String(s) : ""))
      .find((s) => /^percPage$/i.test(s)) ||
    firstName(types, ["name", "label"]) ||
    "percPage";
  return { localeName, typeName };
}

test.describe("Developer auto-translation PUT persist (#4039)", () => {
  test("GET round-trips existing rows; PUT persists; empty PUT clears", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await page.goto(developerLocalesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });

    const original = await getAutoTranslations(page);
    expect(
      original.status,
      `GET ${AT_PATH} ${original.status}: ${original.text.slice(0, 400)}`,
    ).toBe(200);
    expect(Array.isArray(original.rows), "GET must unwrap to an array").toBe(
      true,
    );

    let persistBody = original.rows;
    if (persistBody.length === 0) {
      const { localeName, typeName } = await catalogNames(page);
      persistBody = [
        {
          locale: localeName,
          contentTypeName: typeName,
          workflowName: "Default Workflow",
          communityName: "Default",
        },
      ];
    }

    let restored = false;
    try {
      const putOne = await putAutoTranslations(page, persistBody);
      expect(
        putOne.status,
        `PUT persist ${putOne.status}: ${putOne.text.slice(0, 600)}`,
      ).toBe(200);
      expect(putOne.status, "lock conflict must be 409 not 500").not.toBe(500);

      const afterPut = await getAutoTranslations(page);
      expect(afterPut.status).toBe(200);
      expect(
        afterPut.rows.length,
        `GET after PUT must round-trip rows; rows=${JSON.stringify(afterPut.rows)}`,
      ).toBeGreaterThan(0);

      const putEmpty = await putAutoTranslations(page, []);
      expect(
        putEmpty.status,
        `empty PUT ${putEmpty.status}: ${putEmpty.text.slice(0, 600)}`,
      ).toBe(200);

      const afterClear = await getAutoTranslations(page);
      expect(afterClear.status).toBe(200);
      expect(
        afterClear.rows,
        `GET after empty PUT must be empty; rows=${JSON.stringify(afterClear.rows)}`,
      ).toEqual([]);

      const restore = await putAutoTranslations(page, original.rows);
      expect(
        restore.status,
        `restore PUT ${restore.status}: ${restore.text.slice(0, 600)}`,
      ).toBe(200);
      restored = true;
    } finally {
      if (!restored) {
        await putAutoTranslations(page, original.rows);
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
