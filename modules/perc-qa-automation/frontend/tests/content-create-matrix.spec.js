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
 * US1 content-creation matrix (#6239 / parent #6238): every pinned built-in
 * content type gets a create + save/reopen round-trip case plus a negative
 * case (blank required field shows a message, nothing is persisted).
 *
 * <p>Tags: {@code @content-create} {@code @explorer} plus one
 * {@code @content-create-&lt;type&gt;} tag per type.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/content-create-matrix.js} (closed inventory, 54
 * types). CMS REST APIs are stubbed per type, following the
 * {@code assembly-*-field} / {@code editor-host-*} family; the CMS-only cell
 * serves the SPA shell and login. {@code afterAll} assembles one
 * results-matrix row per case, validates every row against
 * {@code contracts/results-matrix.schema.json}, and writes the matrix to
 * {@code test-results/content-create-matrix.json}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/content-create-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Single type:
 * {@code npm run test:surface -- --path tests/content-create-matrix.spec.js --grep percPage}.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS: CREATE_IDS,
  editorSpaUrl: createSpaUrl,
} = require("./helpers/editor-host-create-item");
const {
  TEST_IDS: FIELD_IDS,
  editorSpaUrl: editorSpaUrlFor,
  isItemFieldsPutUrl,
} = require("./helpers/editor-host-required-fields");
const {
  BUILT_IN_TYPES,
  MATRIX_SPEC,
  CASE_CREATE,
  CASE_NEGATIVE,
  buildResultRow,
  sampleValueFor,
  tagForType,
  validateResultRow,
} = require("./helpers/content-create-matrix");

const caseOutcomes = new Map();

function recordCase(entry, caseKey) {
  const current = caseOutcomes.get(entry.name) || {};
  current[caseKey] = "pass";
  caseOutcomes.set(entry.name, current);
}

function fieldLabel(name) {
  return String(name)
    .replace(/^sys_/, "")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/**
 * Minimal type detail for one matrix entry: only the fields the case drives.
 * Every stubbed field is a required single-line text input on the live cell,
 * so the generic editor renders each as a fillable input.
 */
function detailFor(entry) {
  const names = [];
  for (const name of ["sys_title", entry.roundTripField, entry.negativeField]) {
    if (!names.includes(name)) {
      names.push(name);
    }
  }
  return {
    ContentTypeDetail: {
      name: entry.name,
      fields: names.map((name) => ({
        name,
        label: fieldLabel(name),
        control: "sys_EditBox",
        required: true,
      })),
    },
  };
}

function fieldsBody(entry, contentId, values, revision) {
  const rows = [];
  for (const name of ["sys_title", entry.roundTripField, entry.negativeField]) {
    if (!rows.some((row) => row.name === name)) {
      rows.push({ name, value: values[name] ?? "" });
    }
  }
  return {
    ItemEditorFields: {
      contentId: String(contentId),
      contentType: entry.name,
      name: `Matrix ${entry.name}`,
      checkoutUser: "admin",
      revision,
      fields: rows,
    },
  };
}

function filledValues(entry) {
  const values = { sys_title: `Matrix ${entry.name}` };
  values[entry.roundTripField] = sampleValueFor(entry);
  values[entry.negativeField] = `Matrix ${entry.name} ${entry.negativeField}`;
  return values;
}

function rowsFromPutBody(bodyText) {
  try {
    const body = JSON.parse(bodyText || "{}");
    const payload = body.ItemEditorFields || body.itemEditorFields || body;
    return payload.fields || payload.Fields || [];
  } catch {
    return [];
  }
}

function fieldValue(rows, name) {
  const row = (rows || []).find((item) => (item.name || item.Name) === name);
  return row ? String(row.value ?? row.Value ?? "") : undefined;
}

function unwrapCreateBody(bodyText) {
  try {
    const body = JSON.parse(bodyText || "{}");
    return body.ItemCreateRequest || body.itemCreateRequest || body;
  } catch {
    return {};
  }
}

async function stubMatrixApis(page, entry, contentId, initialValues) {
  const state = { values: { ...initialValues }, revision: 1 };
  const creates = [];
  const puts = [];

  await page.route("**/services/contenttypes", async (route) => {
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ContentTypeList: {
          ContentType: BUILT_IN_TYPES.map((item) => ({
            name: item.name,
            label: item.label,
          })),
        },
      }),
    });
  });
  await page.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(detailFor(entry)),
    });
  });
  await page.route("**/services/itemmanagement/item/create**", async (route) => {
    creates.push(unwrapCreateBody(route.request().postData()));
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemCreateResult: {
          itemId: String(contentId),
          folderPath: entry.folder,
          name: `Matrix ${entry.name}`,
          contentType: entry.name,
        },
      }),
    });
  });
  const quiet = { status: 200, contentType: "application/json", body: "{}" };
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/itemmanagement/workflow/checkIn/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/rest/editor/items/**/checkin", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: String(contentId),
          stateName: "Draft",
          transitionTriggers: ["Submit"],
        },
      }),
    }),
  );
  await page.route("**/rest/content-explorer/translations/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/pathmanagement/path/item/id/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/assembly/slot-relationships/**", (route) =>
    route.fulfill(quiet),
  );
  await page.route("**/services/itemmanagement/item/siteimpact/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
    }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      const rows = rowsFromPutBody(route.request().postData());
      for (const row of rows) {
        const name = row.name || row.Name;
        if (name) {
          state.values[name] = String(row.value ?? row.Value ?? "");
        }
      }
      puts.push(route.request().url());
      state.revision += 1;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(fieldsBody(entry, contentId, state.values, state.revision)),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(entry, contentId, state.values, state.revision)),
    });
  });
  return { creates, puts };
}

function watchNoise(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertQuiet(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const serious = consoleErrors.filter(
    (text) =>
      !/favicon|ResizeObserver|net::ERR|404 \(Not Found\)|status of 400|status of 403|status of 409/i.test(
        text,
      ),
  );
  expect(serious, `JS console errors: ${serious.join(" | ")}`).toEqual([]);
}

function editorUrl(contentId) {
  return editorSpaUrlFor(BASE_URL, `contentId=${contentId}&mode=edit`);
}

test.describe("content creation matrix", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test.afterAll(async () => {
    // One row per pinned type. A case that never ran (grep-filtered run) or
    // never recorded (its test failed) is reported as skipped; a red run is
    // still red because the failing test itself fails.
    const rows = BUILT_IN_TYPES.map((entry) => {
      const outcomes = caseOutcomes.get(entry.name) || {};
      return buildResultRow(
        entry,
        {
          [CASE_CREATE]: outcomes[CASE_CREATE] || "skip",
          [CASE_NEGATIVE]: outcomes[CASE_NEGATIVE] || "skip",
        },
        { spec: MATRIX_SPEC },
      );
    });
    const problems = [];
    for (const row of rows) {
      for (const error of validateResultRow(row)) {
        problems.push(`${row.contentType}: ${error}`);
      }
    }
    if (problems.length > 0) {
      throw new Error(`matrix rows fail schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "content-create-matrix.json"),
      `${JSON.stringify(rows, null, 2)}\n`,
      "utf8",
    );
  });

  BUILT_IN_TYPES.forEach((entry, index) => {
    const contentId = String(7100 + index);
    const itemName = `Matrix ${entry.name}`;
    const value = sampleValueFor(entry);

    test(
      `${entry.name}: creates, saves and reopens with values round-tripped`,
      { tag: ["@content-create", tagForType(entry.name), "@explorer"] },
      async ({ page }) => {
        const { pageErrors, consoleErrors } = watchNoise(page);
        const { creates, puts } = await stubMatrixApis(page, entry, contentId, filledValues(entry));

        await page.goto(createSpaUrl(BASE_URL));
        await expect(page.getByTestId(CREATE_IDS.panel)).toBeVisible({ timeout: 20_000 });
        await page.getByTestId(CREATE_IDS.type).selectOption(entry.name);
        await page.getByTestId(CREATE_IDS.folder).fill(entry.folder);
        await page.getByTestId(CREATE_IDS.name).fill(itemName);
        await page.getByTestId(CREATE_IDS.submit).click();
        await expect(page.getByTestId(CREATE_IDS.contentId)).toContainText(contentId, {
          timeout: 15_000,
        });
        expect(creates.length).toBeGreaterThan(0);
        expect(creates[creates.length - 1].contentType).toBe(entry.name);
        expect(creates[creates.length - 1].folderPath).toBe(entry.folder);

        await page.goto(editorUrl(contentId));
        await expect(page.locator(`[data-testid="${FIELD_IDS.host}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(page.locator(`[data-testid="${FIELD_IDS.form}"]`)).toBeVisible();
        const field = page.locator(`[data-testid="editor-field-${entry.roundTripField}"]`);
        await expect(field).toBeVisible({ timeout: 15_000 });
        await field.fill(`${value} updated`);
        await page.locator(`[data-testid="${FIELD_IDS.save}"]`).click();
        await expect.poll(() => puts.length, { timeout: 15_000 }).toBeGreaterThan(0);
        const fieldPuts = puts.filter((url) => isItemFieldsPutUrl(url));
        expect(fieldPuts.length).toBeGreaterThan(0);
        await expect(page.locator(`[data-testid="${FIELD_IDS.saved}"]`)).toBeVisible({
          timeout: 15_000,
        });

        await page.goto(editorUrl(contentId));
        await expect(page.locator(`[data-testid="${FIELD_IDS.form}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(
          page.locator(`[data-testid="editor-field-${entry.roundTripField}"]`),
        ).toHaveValue(`${value} updated`, { timeout: 15_000 });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${FIELD_IDS.host}"]`,
        });
        assertQuiet(pageErrors, consoleErrors);
        recordCase(entry, CASE_CREATE);
      },
    );

    test(
      `${entry.name}: blank required field shows a message and writes nothing`,
      { tag: ["@content-create", tagForType(entry.name), "@explorer", "@validation"] },
      async ({ page }) => {
        const { pageErrors, consoleErrors } = watchNoise(page);
        const blank = filledValues(entry);
        blank[entry.negativeField] = "";
        const { puts } = await stubMatrixApis(page, entry, contentId, blank);

        await page.goto(editorUrl(contentId));
        await expect(page.locator(`[data-testid="${FIELD_IDS.form}"]`)).toBeVisible({
          timeout: 20_000,
        });
        const target = page.locator(`[data-testid="editor-field-${entry.negativeField}"]`);
        await expect(target).toBeVisible({ timeout: 15_000 });
        await expect(target).toHaveValue("");
        await page.locator(`[data-testid="${FIELD_IDS.save}"]`).click();
        await expect(
          page.locator(`[data-testid="editor-field-error-${entry.negativeField}"]`),
        ).toBeVisible({ timeout: 15_000 });
        await expect(page.locator(`[data-testid="${FIELD_IDS.saveError}"]`)).toContainText(
          /required fields before saving/i,
          { timeout: 10_000 },
        );
        expect(puts.filter((url) => isItemFieldsPutUrl(url))).toEqual([]);

        await page.goto(editorUrl(contentId));
        await expect(page.locator(`[data-testid="${FIELD_IDS.form}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(
          page.locator(`[data-testid="editor-field-${entry.negativeField}"]`),
        ).toHaveValue("", { timeout: 15_000 });

        assertQuiet(pageErrors, consoleErrors);
        recordCase(entry, CASE_NEGATIVE);
      },
    );
  });
});
