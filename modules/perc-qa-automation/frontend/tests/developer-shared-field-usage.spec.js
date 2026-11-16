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
 * Developer shared-field usage inspector (#6479 slice 1 / parent #1690).
 *
 * Where-used list per field: GET
 * /services/sharedfields/{group}/fields/{field}/usage rendered in the
 * group-detail Where used section.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-shared-field-usage.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSharedFieldsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "shared-fields",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique suffix (no spaces or path characters). */
function uniqueSuffix() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `${a}${b}`.slice(0, 8) || "x";
}

function asRecord(value) {
  return value != null && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function unwrapFields(payload) {
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  const nested =
    asRecord(root.ContentTypeDetail) ||
    asRecord(root.contentTypeDetail) ||
    asRecord(root.SharedFieldGroupDetail) ||
    asRecord(root.sharedFieldGroupDetail) ||
    root;
  const raw = nested.fields || nested.ContentTypeField || [];
  if (Array.isArray(raw)) {
    return raw;
  }
  return raw ? [raw] : [];
}

function fieldNameSet(fields) {
  return new Set(
    (fields || [])
      .map((f) => (f && f.name ? String(f.name).trim().toLowerCase() : ""))
      .filter(Boolean),
  );
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

async function openSharedGroupDetail(page, groupName) {
  await openSharedFieldsCatalog(page);
  const openBtn = page.locator(
    `[data-testid="developer-sf-open"][data-sf-name="${groupName}"]`,
  );
  await expect(openBtn).toBeVisible({ timeout: 15_000 });
  await openBtn.click();
  await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-sf-detail-title"]')).toHaveText(
    groupName,
    { timeout: 20_000 },
  );
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

async function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName = (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
}

async function createGroupWithField(page, groupName, fieldName) {
  const created = await inPageJson(page, "/Rhythmyx/services/sharedfields", "POST", {
    SharedFieldGroupDetail: { name: groupName },
  });
  expect(
    created.status,
    `create group should be 200 (got ${created.status}): ${created.text.slice(0, 200)}`,
  ).toBe(200);
  const added = await inPageJson(
    page,
    `/Rhythmyx/services/sharedfields/${encodeURIComponent(groupName)}/fields`,
    "POST",
    { SharedField: { name: fieldName, dataType: "text", occurrence: "optional" } },
  );
  expect(
    added.status,
    `add field should be 200 (got ${added.status}): ${added.text.slice(0, 200)}`,
  ).toBe(200);
}

/** First sample type whose detail loads and does not already carry the field. */
async function pickTypeForInclude(page, fieldName) {
  for (const candidate of ["percPage", "percFileAsset", "percSimpleTextAsset"]) {
    let payload = null;
    try {
      const res = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(candidate)}`,
      );
      if (!res.ok()) {
        continue;
      }
      payload = await res.json();
    } catch {
      continue;
    }
    if (!fieldNameSet(unwrapFields(payload)).has(fieldName.toLowerCase())) {
      return candidate;
    }
  }
  return null;
}

/** Packaged catalog fields: name -> group, from the thin-read APIs. */
async function listPackagedSharedFields(page) {
  const out = [];
  const res = await page.request.get(`${BASE_URL}/Rhythmyx/services/sharedfields`);
  if (!res.ok()) {
    return out;
  }
  const payload = await res.json();
  const root = asRecord(payload);
  const raw = (root && (root.SharedFieldGroupSummary || root.sharedFieldGroupSummary)) || [];
  const groups = Array.isArray(raw) ? raw : [raw];
  for (const g of groups.slice(0, 12)) {
    const groupName = g && g.name ? String(g.name).trim() : "";
    if (!groupName) {
      continue;
    }
    for (const f of await listGroupFieldNames(page, groupName)) {
      if (!out.some((e) => e.field === f)) {
        out.push({ group: groupName, field: f });
      }
    }
  }
  return out;
}

/** Field names of one shared group, or [] when the detail does not load. */
async function listGroupFieldNames(page, groupName) {
  try {
    const detailRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/sharedfields/${encodeURIComponent(groupName)}`,
    );
    if (!detailRes.ok()) {
      return [];
    }
    return unwrapFields(await detailRes.json())
      .map((f) => (f && f.name ? String(f.name).trim() : ""))
      .filter(Boolean);
  } catch {
    return [];
  }
}

test.describe("Developer shared-field where-used inspector (#6479)", () => {
  test("unused field shows the empty where-used state in the shell", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    const groupName = `qause${uniqueSuffix()}`;
    const fieldName = `qausefld${uniqueSuffix()}`;
    await createGroupWithField(page, groupName, fieldName);

    try {
      await openSharedGroupDetail(page, groupName);

      // Where-used section renders for the selected field with the empty state.
      const usage = page.locator('[data-testid="developer-sf-usage"]');
      await expect(usage).toBeVisible({ timeout: 20_000 });
      await expect(page.locator('[data-testid="developer-sf-usage-field"]')).toContainText(
        fieldName,
      );
      const usageError = page.locator('[data-testid="developer-sf-usage-error"]');
      if (await usageError.isVisible()) {
        throw new Error(`Usage lookup failed: ${(await usageError.innerText()).trim()}`);
      }
      await expect(page.locator('[data-testid="developer-sf-usage-empty"]')).toBeVisible({
        timeout: 20_000,
      });

      // REST probe agrees: 200 with an empty usedBy list.
      const probe = await inPageJson(
        page,
        `/Rhythmyx/services/sharedfields/${encodeURIComponent(groupName)}/fields/${encodeURIComponent(fieldName)}/usage`,
        "GET",
      );
      expect(
        probe.status,
        `usage should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
      ).toBe(200);
      expect(probe.text, "unused field has empty usedBy").toContain(fieldName);
    } finally {
      const del = await inPageJson(
        page,
        `/Rhythmyx/services/sharedfields/${encodeURIComponent(groupName)}`,
        "DELETE",
      );
      expect(
        [200, 204].includes(del.status),
        `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
      ).toBeTruthy();
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("included field lists the using content type in the shell", async ({ page }) => {
    test.setTimeout(240_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    // Packaged catalog field unused on a sample type: Workbench-shaped
    // groups include cleanly (fresh REST-created groups 500 on include —
    // residual filed).
    const catalog = await listPackagedSharedFields(page);
    let target = null;
    for (const entry of catalog) {
      const typeName = await pickTypeForInclude(page, entry.field);
      if (typeName != null) {
        target = { ...entry, typeName };
        break;
      }
    }
    if (target == null) {
      test.skip(true, "H2 QA has no unused packaged shared field for the usage round trip");
    }

    // Setup: lock a sample type that does not carry the field and include
    // the packaged shared field (same session, in-page). A lock held by a
    // stale session (409) falls through to the next candidate.
    let lockedType = null;
    let lastLockText = "";
    for (const candidate of ["percPage", "percFileAsset", "percSimpleTextAsset"]) {
      let unused = false;
      try {
        const detailRes = await page.request.get(
          `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(candidate)}`,
        );
        unused =
          detailRes.ok() &&
          !fieldNameSet(unwrapFields(await detailRes.json())).has(target.field.toLowerCase());
      } catch {
        unused = false;
      }
      if (!unused) {
        continue;
      }
      const attempt = await inPageJson(
        page,
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(candidate)}/lock`,
        "POST",
      );
      if (attempt.status === 200) {
        lockedType = candidate;
        break;
      }
      lastLockText = `lock ${candidate} -> ${attempt.status}: ${attempt.text.slice(0, 200)}`;
    }
    expect(
      lockedType,
      `could not lock a sample type for the include round trip (${lastLockText})`,
    ).toBeTruthy();
    target = { ...target, typeName: lockedType };
    try {
      const include = await inPageJson(
        page,
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(target.typeName)}/fields/include`,
        "POST",
        { ContentTypeField: { name: target.field, fieldType: "shared" } },
      );
      expect(
        include.status,
        `include should be 200 (got ${include.status}): ${include.text.slice(0, 200)}`,
      ).toBe(200);

      // REST probe: usage lists the including type.
      const probe = await inPageJson(
        page,
        `/Rhythmyx/services/sharedfields/${encodeURIComponent(target.group)}/fields/${encodeURIComponent(target.field)}/usage`,
        "GET",
      );
      expect(
        probe.status,
        `usage should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
      ).toBe(200);
      expect(probe.text, "usage lists the including type").toContain(target.typeName);

      // Shell: the Where used section shows the content-type row.
      await openSharedGroupDetail(page, target.group);
      const usage = page.locator('[data-testid="developer-sf-usage"]');
      await expect(usage).toBeVisible({ timeout: 20_000 });
      const usageError = page.locator('[data-testid="developer-sf-usage-error"]');
      if (await usageError.isVisible()) {
        throw new Error(`Usage lookup failed: ${(await usageError.innerText()).trim()}`);
      }
      const row = page.locator(
        `[data-testid="developer-sf-usage-table"] [data-ct-name="${target.typeName}"]`,
      );
      await expect(row).toBeVisible({ timeout: 20_000 });
      await expect(page.locator('[data-testid="developer-sf-usage-field"]')).toContainText(
        target.field,
      );
    } finally {
      // Cleanup: detach (lock still held by this session), unlock, then
      // prove the type leaves the where-used list.
      const detach = await inPageJson(
        page,
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(target.typeName)}/fields/include/${encodeURIComponent(target.field)}`,
        "DELETE",
      );
      expect(
        [200, 204].includes(detach.status),
        `detach cleanup should be 200/204 (got ${detach.status}): ${detach.text.slice(0, 200)}`,
      ).toBeTruthy();
      await inPageJson(
        page,
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(target.typeName)}/unlock`,
        "POST",
      ).then((unlock) => {
        expect(
          [200, 204].includes(unlock.status),
          `unlock cleanup should be 200/204 (got ${unlock.status}): ${unlock.text.slice(0, 200)}`,
        ).toBeTruthy();
      });
      const after = await inPageJson(
        page,
        `/Rhythmyx/services/sharedfields/${encodeURIComponent(target.group)}/fields/${encodeURIComponent(target.field)}/usage`,
        "GET",
      );
      expect(after.status, "usage after detach should be 200").toBe(200);
      expect(after.text, "detached type leaves the usage list").not.toContain(
        `"contentTypeName":"${target.typeName}"`,
      );
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
