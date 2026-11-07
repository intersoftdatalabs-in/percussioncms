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
 * Developer Content Type field choice add/reorder on one field (CD-07 / #5706).
 *
 * Admin locks a type, adds two local entries, reorders them with the entry
 * Move up / Move down buttons, and saves via PUT
 * .../fields/{field}/controlProperties (held lock). A following GET lists the
 * new order. Unlock without saving (cancel) does not write. A choices save
 * without the held lock (409) shows the error and never claims success.
 * Other fields on the type stay unchanged.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-field-choices-reorder.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER_A = "5706a";
const MARKER_B = "5706b";
const MARKER_C = "5706c";

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
    return { properties: [], choices: null };
  }
  const nested =
    payload.ContentTypeFieldControlProperties &&
    typeof payload.ContentTypeFieldControlProperties === "object"
      ? payload.ContentTypeFieldControlProperties
      : payload.contentTypeFieldControlProperties &&
          typeof payload.contentTypeFieldControlProperties === "object"
        ? payload.contentTypeFieldControlProperties
        : payload;
  function asItemArray(raw, wrapKeys) {
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw;
    }
    if (typeof raw !== "object") {
      return [];
    }
    for (const key of wrapKeys) {
      if (raw[key] != null) {
        return Array.isArray(raw[key]) ? raw[key] : [raw[key]];
      }
    }
    if (typeof raw.empty === "boolean") {
      return [];
    }
    return [raw];
  }

  const raw = asItemArray(nested.properties, [
    "ContentTypeControlProperty",
    "contentTypeControlProperty",
  ]);
  let choices = nested.choices && typeof nested.choices === "object" ? nested.choices : null;
  if (choices) {
    choices = {
      ...choices,
      entries: asItemArray(choices.entries, [
        "ContentTypeChoiceEntry",
        "contentTypeChoiceEntry",
      ]),
    };
  }
  return {
    fieldName: nested.fieldName,
    control: nested.control,
    properties: raw.map((p) => ({
      name: String((p && p.name) || ""),
      value: p && p.value == null ? "" : String(p.value),
    })),
    choices,
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
  await expect(page.locator('[data-testid="developer-ct-choices"]')).toBeVisible({
    timeout: 30_000,
  });
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

async function putControlPropertiesViaRest(page, typeName, fieldName, properties, choices) {
  const result = await page.evaluate(
    async ({ name, field, properties: props, choices: cat }) => {
      const body = { ContentTypeFieldControlProperties: { properties: props } };
      if (cat !== undefined) {
        body.ContentTypeFieldControlProperties.choices = cat;
      }
      const res = await fetch(
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/fields/${encodeURIComponent(field)}/controlProperties`,
        {
          method: "PUT",
          credentials: "same-origin",
          headers: { Accept: "application/json", "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
      );
      const text = await res.text();
      return { status: res.status, text };
    },
    { name: typeName, field: fieldName, properties, choices },
  );
  return result;
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

function choiceType(choices) {
  const t = choices && choices.type ? String(choices.type).trim() : "";
  return t || "none";
}

function entryValues(choices) {
  return ((choices && choices.entries) || []).map((e) => String((e && e.value) || ""));
}

test.describe("Developer content type field choice add/reorder (CD-07 / #5706)", () => {
  test("add two entries, reorder, save persists order; unlock does not write; other fields stay", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percPage/);

    const fieldSelect = page.locator('[data-testid="developer-ct-cp-field"]');
    const typeSelect = page.locator('[data-testid="developer-ct-ch-type"]');
    const sortSelect = page.locator('[data-testid="developer-ct-ch-sort"]');
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

    let fieldName = fieldOptions[0];
    let original = await getControlPropertiesViaRest(page, typeName, fieldName);
    for (const candidate of fieldOptions) {
      const loaded = await getControlPropertiesViaRest(page, typeName, candidate);
      if (!loaded.choices || choiceType(loaded.choices) === "none") {
        fieldName = candidate;
        original = loaded;
        break;
      }
    }
    const otherField = fieldOptions.find((f) => f !== fieldName);
    const otherBefore =
      otherField != null
        ? await getControlPropertiesViaRest(page, typeName, otherField)
        : null;
    if ((await fieldSelect.inputValue()) !== fieldName) {
      await fieldSelect.selectOption(fieldName);
    }

    const restoreChoices = original.choices ? original.choices : { type: "none" };

    await expect(lockBtn).toBeEnabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(typeSelect).toBeEnabled();

    const saveChoices = async (label) => {
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
      expect(putBody, `${label} PUT includes choices`).toContain('"choices"');
      await expect(notice).toContainText(/saved/i);
      return getControlPropertiesViaRest(page, typeName, fieldName);
    };

    const addEntry = async (value, label) => {
      await page.locator('[data-testid="developer-ct-ch-entry-add-value"]').fill(value);
      await page.locator('[data-testid="developer-ct-ch-entry-add-label"]').fill(label);
      await page.locator('[data-testid="developer-ct-ch-entry-add"]').click();
    };

    try {
      await typeSelect.selectOption("local");
      await sortSelect.selectOption("user");
      await addEntry(MARKER_A, `Alpha${MARKER_A}`);
      await addEntry(MARKER_B, `Beta${MARKER_B}`);
      await expect(page.locator('[data-testid="developer-ct-ch-entry-1"]')).toBeVisible();

      const afterAdd = await saveChoices("Add two entries failed");
      expect(choiceType(afterAdd.choices)).toBe("local");
      expect(entryValues(afterAdd.choices), "GET keeps added order").toEqual([
        MARKER_A,
        MARKER_B,
      ]);

      // Reorder: move the first entry down; bounds buttons stay disabled.
      await expect(
        page.locator('[data-testid="developer-ct-ch-entry-move-up-0"]'),
      ).toBeDisabled();
      await expect(
        page.locator('[data-testid="developer-ct-ch-entry-move-down-1"]'),
      ).toBeDisabled();
      await page.locator('[data-testid="developer-ct-ch-entry-move-down-0"]').click();
      await expect(page.locator('[data-testid="developer-ct-ch-entry-value-0"]')).toHaveValue(
        MARKER_B,
      );
      await expect(page.locator('[data-testid="developer-ct-ch-entry-value-1"]')).toHaveValue(
        MARKER_A,
      );

      const afterReorder = await saveChoices("Reorder save failed");
      expect(entryValues(afterReorder.choices), "GET keeps reordered entries").toEqual([
        MARKER_B,
        MARKER_A,
      ]);

      // Cancel path: edit without saving, then unlock — nothing is written.
      await addEntry(MARKER_C, `Gamma${MARKER_C}`);
      await expect(page.locator('[data-testid="developer-ct-ch-entry-2"]')).toBeVisible();
      await unlockBtn.click();
      await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      const afterCancel = await getControlPropertiesViaRest(page, typeName, fieldName);
      expect(
        entryValues(afterCancel.choices),
        "unlock without save must not write",
      ).toEqual([MARKER_B, MARKER_A]);

      // Other fields on the type stay unchanged.
      if (otherField != null && otherBefore != null) {
        const otherAfter = await getControlPropertiesViaRest(page, typeName, otherField);
        expect(
          JSON.stringify(otherAfter.choices),
          "other field choices unchanged",
        ).toBe(JSON.stringify(otherBefore.choices));
        expect(
          JSON.stringify(otherAfter.properties),
          "other field properties unchanged",
        ).toBe(JSON.stringify(otherBefore.properties));
      }

      // Restore the original catalog while holding the lock again.
      await lockBtn.click();
      await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
      if ((await fieldSelect.inputValue()) !== fieldName) {
        await fieldSelect.selectOption(fieldName);
      }
      await typeSelect.selectOption(
        restoreChoices && restoreChoices.type ? String(restoreChoices.type) : "none",
      );
      if (!restoreChoices || choiceType(restoreChoices) === "none") {
        const putWait = page.waitForResponse(
          (res) =>
            res.request().method() === "PUT" &&
            /\/contenttypes\/[^/?]+\/fields\/[^/?]+\/controlProperties(?:\?|$)/.test(
              res.url(),
            ),
          { timeout: 20_000 },
        );
        await expect(saveBtn).toBeEnabled();
        await saveBtn.click();
        await putWait;
        await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
      }
    } finally {
      const restore = await putControlPropertiesViaRest(
        page,
        typeName,
        fieldName,
        original.properties,
        restoreChoices,
      );
      if (restore.status !== 200 && restore.status !== 409) {
        throw new Error(
          `restore PUT ${restore.status} ${String(restore.text).slice(0, 400)}`,
        );
      }
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    await expect(typeSelect).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("choices save without the held lock (409) shows the error, never success", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percPage/);

    const fieldSelect = page.locator('[data-testid="developer-ct-cp-field"]');
    const typeSelect = page.locator('[data-testid="developer-ct-ch-type"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(fieldSelect).toBeEnabled({ timeout: 30_000 });
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const fieldOptions = await fieldSelect.locator("option").evaluateAll((opts) =>
      opts.map((o) => String(o.value || "")).filter(Boolean),
    );
    let fieldName = fieldOptions[0];
    let original = await getControlPropertiesViaRest(page, typeName, fieldName);
    for (const candidate of fieldOptions) {
      const loaded = await getControlPropertiesViaRest(page, typeName, candidate);
      if (!loaded.choices || choiceType(loaded.choices) === "none") {
        fieldName = candidate;
        original = loaded;
        break;
      }
    }
    if ((await fieldSelect.inputValue()) !== fieldName) {
      await fieldSelect.selectOption(fieldName);
    }
    const restoreChoices = original.choices ? original.choices : { type: "none" };

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await typeSelect.selectOption("local");
    await page.locator('[data-testid="developer-ct-ch-entry-add-value"]').fill(MARKER_A);
    await page.locator('[data-testid="developer-ct-ch-entry-add-label"]').fill("Alpha409");
    await page.locator('[data-testid="developer-ct-ch-entry-add"]').click();
    await expect(page.locator('[data-testid="developer-ct-ch-entry-0"]')).toBeVisible();

    try {
      await page.route("**/services/contenttypes/**/fields/**/controlProperties", async (route) => {
        if (route.request().method() === "PUT") {
          await route.fulfill({
            status: 409,
            contentType: "application/json",
            body: JSON.stringify({ message: "Lock required" }),
          });
        } else {
          await route.continue();
        }
      });

      await expect(saveBtn).toBeEnabled();
      await saveBtn.click();
      await expect(saveError).toBeVisible({ timeout: 20_000 });
      await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      await expect(notice).toBeHidden();
      await expect(typeSelect).toBeDisabled();
      await expect(saveBtn).toBeDisabled();
    } finally {
      await page.unroute("**/services/contenttypes/**/fields/**/controlProperties");
      // The intercepted PUT never reached the server, so the real lock is
      // still ours: release it, then restore the original catalog.
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
      await lockBtn.click();
      await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
      const restore = await putControlPropertiesViaRest(
        page,
        typeName,
        fieldName,
        original.properties,
        restoreChoices,
      );
      expect(restore.status, `restore PUT ${String(restore.text).slice(0, 400)}`).toBe(200);
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
