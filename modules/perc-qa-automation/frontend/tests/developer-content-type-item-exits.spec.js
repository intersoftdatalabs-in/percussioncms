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
 * Developer Content Type item-level exits persist (CD-09 / #3905 / leftover #3902).
 *
 * After a held design lock, PUT /contenttypes/{id}/itemExits must persist
 * reconstructed GET rows plus a valid item-level input translation (IPSItemInputTransformer).
 * Field UDFs such as sys_ToUpperCase are not valid at item-level.
 * Unlocked PUT is 409. Pipe pre/post exits are omitted so percPage is unchanged.
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

/** Item-level IPSItemInputTransformer present on H2 sample types — not sys_ToUpperCase. */
const SAMPLE_FQN = "Java/global/percussion/content/sys_cleanReservedHtmlClasses";
const SAMPLE_PARAM = "sys_title";
const SAMPLE_TYPES = ["percRawHtmlAsset", "percPage", "percFileAsset"];

function unwrapItemExits(payload) {
  if (payload == null || typeof payload !== "object") {
    return {
      inputTranslations: [],
      outputTranslations: [],
      validations: [],
    };
  }
  const nested =
    payload.ContentTypeItemExits && typeof payload.ContentTypeItemExits === "object"
      ? payload.ContentTypeItemExits
      : payload.contentTypeItemExits && typeof payload.contentTypeItemExits === "object"
        ? payload.contentTypeItemExits
        : payload;
  const asList = (raw, itemKey) => {
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw;
    }
    if (typeof raw !== "object") {
      return [];
    }
    if (Array.isArray(raw[itemKey])) {
      return raw[itemKey];
    }
    if (raw[itemKey] && typeof raw[itemKey] === "object") {
      return [raw[itemKey]];
    }
    if (typeof raw.empty === "boolean" && Object.keys(raw).every((k) => k === "empty")) {
      return [];
    }
    if (raw.extension || raw.name || "value" in raw) {
      return [raw];
    }
    return [];
  };
  const normalizeParams = (raw) =>
    asList(raw, "ContentTypeItemExitParam").map((p) => {
      const out = {};
      if (p && typeof p.name === "string") {
        out.name = p.name;
      }
      out.value = p && p.value != null ? String(p.value) : "";
      return out;
    });
  const normalizeExits = (raw) =>
    asList(raw, "ContentTypeItemExit").map((item) => ({
      extension: exitExtension(item),
      parameters: normalizeParams(item && item.parameters),
    }));
  return {
    inputTranslations: normalizeExits(nested.inputTranslations),
    outputTranslations: normalizeExits(nested.outputTranslations),
    validations: normalizeExits(nested.validations),
    maxErrorsToStopValidation: nested.maxErrorsToStopValidation,
  };
}

function exitExtension(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  return String(item.extension || item.name || "").trim();
}

function firstParamValue(item) {
  if (!item || !Array.isArray(item.parameters) || item.parameters.length === 0) {
    return "";
  }
  const v = item.parameters[0] && item.parameters[0].value;
  return v != null ? String(v) : "";
}

function exitKey(item) {
  return `${exitExtension(item).toLowerCase()}|${firstParamValue(item)}`;
}

const SAMPLE_KEY = `${SAMPLE_FQN.toLowerCase()}|${SAMPLE_PARAM}`;

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

function itemExitsUrl(typeName) {
  return `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/itemExits`;
}

function lockUrl(typeName) {
  return `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/lock`;
}

function unlockUrl(typeName) {
  return `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/unlock`;
}

async function getJson(page, url) {
  const res = await page.request.get(url, {
    headers: { Accept: "application/json" },
  });
  const text = await res.text();
  let json = {};
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    throw new Error(`Non-JSON GET ${res.status()} ${url}: ${String(text).slice(0, 400)}`);
  }
  return { status: res.status(), json, text };
}

async function postJson(page, url) {
  const res = await page.request.post(url, {
    headers: { Accept: "application/json" },
  });
  const text = await res.text();
  return { status: res.status(), text };
}

async function putItemExits(page, typeName, envelope) {
  const wrapRes = await page.request.put(itemExitsUrl(typeName), {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    data: { ContentTypeItemExits: envelope },
  });
  return { status: wrapRes.status(), text: await wrapRes.text() };
}

function persistEnvelope(current, extraInput) {
  const inputs = [...(current.inputTranslations || [])];
  if (extraInput) {
    inputs.push(extraInput);
  }
  const envelope = {
    inputTranslations: inputs,
    outputTranslations: current.outputTranslations || [],
    validations: current.validations || [],
  };
  if (current.maxErrorsToStopValidation != null) {
    envelope.maxErrorsToStopValidation = current.maxErrorsToStopValidation;
  }
  return envelope;
}

async function resolveSampleType(page) {
  const failures = [];
  for (const name of SAMPLE_TYPES) {
    const got = await getJson(page, itemExitsUrl(name));
    if (got.status === 200) {
      return { typeName: name, current: unwrapItemExits(got.json) };
    }
    failures.push(`${name}:${got.status}:${String(got.text).slice(0, 180)}`);
  }
  throw new Error(
    `GET itemExits failed — fail closed: ${failures.join(" | ")}`,
  );
}

test.describe("Developer content type item-level exits (CD-09 / #3905)", () => {
  test("unlocked itemExits PUT is 409; lock is not stolen", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const { typeName } = await resolveSampleType(page);
    const put = await putItemExits(page, typeName, persistEnvelope({
      inputTranslations: [],
      outputTranslations: [],
      validations: [],
    }));
    expect(put.status, `unlocked PUT ${put.status} ${put.text}`).toBe(409);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, PUT reconstructed item-level exits, GET reflects values", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const { typeName, current } = await resolveSampleType(page);
    const originalFqns = current.inputTranslations.map(exitExtension).filter(Boolean);
    const originalKeys = current.inputTranslations.map(exitKey);
    const alreadyHasSample = originalKeys.includes(SAMPLE_KEY);

    const lock = await postJson(page, lockUrl(typeName));
    expect(lock.status, `POST lock ${lock.status} ${lock.text}`).toBe(200);

    const sample = {
      extension: SAMPLE_FQN,
      parameters: [{ value: SAMPLE_PARAM }],
    };

    try {
      const roundTrip = await putItemExits(page, typeName, persistEnvelope(current));
      expect(
        roundTrip.status,
        `Round-trip GET→PUT failed PUT ${roundTrip.status} ${roundTrip.text}`,
      ).toBe(200);

      if (alreadyHasSample) {
        const without = persistEnvelope({
          ...current,
          inputTranslations: current.inputTranslations.filter(
            (item) => exitKey(item) !== SAMPLE_KEY,
          ),
        });
        const removed = await putItemExits(page, typeName, without);
        expect(
          removed.status,
          `Remove item-exit save failed PUT ${removed.status} ${removed.text}`,
        ).toBe(200);
        const afterRemove = unwrapItemExits(JSON.parse(removed.text || "{}"));
        expect(afterRemove.inputTranslations.map(exitKey)).not.toContain(SAMPLE_KEY);

        const added = await putItemExits(page, typeName, persistEnvelope(afterRemove, sample));
        expect(
          added.status,
          `Add item-exit save failed PUT ${added.status} ${added.text}`,
        ).toBe(200);
      } else {
        const added = await putItemExits(page, typeName, persistEnvelope(current, sample));
        expect(
          added.status,
          `Add item-exit save failed PUT ${added.status} ${added.text}`,
        ).toBe(200);
        const afterAdd = unwrapItemExits(JSON.parse(added.text || "{}"));
        expect(afterAdd.inputTranslations.map(exitKey)).toContain(SAMPLE_KEY);
      }

      const restore = await putItemExits(page, typeName, persistEnvelope(current));
      expect(
        restore.status,
        `Restore item-exit save failed PUT ${restore.status} ${restore.text}`,
      ).toBe(200);
      const restoredGet = await getJson(page, itemExitsUrl(typeName));
      expect(restoredGet.status, `GET after restore ${restoredGet.text}`).toBe(200);
      const restoredFqns = unwrapItemExits(restoredGet.json)
        .inputTranslations.map(exitExtension)
        .filter(Boolean);
      expect([...restoredFqns].sort()).toEqual([...originalFqns].sort());
    } finally {
      const unlock = await postJson(page, unlockUrl(typeName));
      expect(
        unlock.status === 200 || unlock.status === 204,
        `POST unlock ${unlock.status} ${unlock.text}`,
      ).toBe(true);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
