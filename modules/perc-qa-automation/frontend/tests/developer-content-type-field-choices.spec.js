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
 * Developer Content Type field choice catalog chrome (CD-07 / #4046).
 *
 * Admin locks a type, sets/clears the field choice catalog via PUT
 * .../fields/{field}/controlProperties (held lock). Properties-only save
 * omits choices so the catalog is not wiped. Type none clears.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-field-choices.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = "4046";

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
  await expect(page.locator('[data-testid="developer-ct-ch-type"]')).toBeDisabled({
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

test.describe("Developer content type field choice catalog (CD-07 / #4046)", () => {
  test("unlocked choice editors stay blocked; 409 lock is not stolen", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const typeSelect = page.locator('[data-testid="developer-ct-ch-type"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-choices"]')).toBeVisible();
    await expect(typeSelect).toBeDisabled();
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
    await expect(typeSelect).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, set local choices, properties-only save keeps catalog, type none clears", async ({
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
    if ((await fieldSelect.inputValue()) !== fieldName) {
      await fieldSelect.selectOption(fieldName);
    }

    const restoreChoices = original.choices
      ? original.choices
      : { type: "none" };

    await expect(typeSelect).toBeDisabled();
    await expect(lockBtn).toBeEnabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(typeSelect).toBeEnabled();
    await expect(unlockBtn).toBeEnabled();

    const saveControlProps = async (label, expectChoicesInBody) => {
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
      if (expectChoicesInBody) {
        expect(putBody, `${label} PUT includes choices`).toContain('"choices"');
      } else {
        expect(putBody, `${label} PUT omits choices`).not.toContain('"choices"');
      }
      await expect(notice).toContainText(/saved/i);
      return getControlPropertiesViaRest(page, typeName, fieldName);
    };

    try {
      await typeSelect.selectOption("local");
      await page.locator('[data-testid="developer-ct-ch-entry-add-value"]').fill(MARKER);
      await page.locator('[data-testid="developer-ct-ch-entry-add-label"]').fill(`Issue${MARKER}`);
      await page.locator('[data-testid="developer-ct-ch-entry-add"]').click();
      await expect(page.locator('[data-testid="developer-ct-ch-entry-0"]')).toBeVisible();

      const afterChoices = await saveControlProps("Set local choices failed", true);
      expect(choiceType(afterChoices.choices)).toBe("local");
      const values = ((afterChoices.choices && afterChoices.choices.entries) || []).map((e) =>
        String((e && e.value) || ""),
      );
      expect(values, "GET includes marker entry").toContain(MARKER);

      const valueInput = page.locator('[data-testid="developer-ct-cp-value-0"]');
      if (await valueInput.count()) {
        const originalValue = await valueInput.inputValue();
        const nextValue = originalValue.includes(MARKER) ? originalValue : `${originalValue}${MARKER}`;
        await valueInput.fill(nextValue);
        const afterProps = await saveControlProps("Properties-only save failed", false);
        expect(choiceType(afterProps.choices)).toBe("local");
        const kept = ((afterProps.choices && afterProps.choices.entries) || []).map((e) =>
          String((e && e.value) || ""),
        );
        expect(kept, "properties-only save must not wipe choices").toContain(MARKER);
        await valueInput.fill(originalValue);
        await saveControlProps("Restore property value failed", false);
      } else {
        await page.locator('[data-testid="developer-ct-cp-add-name"]').fill("height");
        await page.locator('[data-testid="developer-ct-cp-add-value"]').fill(MARKER);
        await page.locator('[data-testid="developer-ct-cp-add"]').click();
        const afterProps = await saveControlProps("Add property save failed", false);
        expect(choiceType(afterProps.choices)).toBe("local");
        await page.locator('[data-testid="developer-ct-cp-remove-0"]').click();
        await saveControlProps("Remove added property failed", false);
      }

      await typeSelect.selectOption("none");
      const afterNone = await saveControlProps("Clear choices failed", true);
      expect(afterNone.choices == null || choiceType(afterNone.choices) === "none").toBeTruthy();
    } finally {
      const restore = await putControlPropertiesViaRest(
        page,
        typeName,
        fieldName,
        original.properties,
        restoreChoices,
      );
      if (restore.status !== 200) {
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
});
