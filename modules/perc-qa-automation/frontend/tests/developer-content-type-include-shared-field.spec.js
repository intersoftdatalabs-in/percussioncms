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
 * Developer Content Type include ONE shared field via the picker
 * (issue #5705, slice 124 / parent #1690).
 *
 * Including one shared field adds it with origin shared while the other
 * fields, the label, and the description stay. A missing held lock (409)
 * must not claim success, and cancelling the pick must not write.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-include-shared-field.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function asRecord(value) {
  return value != null && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function unwrapDetail(payload) {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  return (
    asRecord(root.ContentTypeDetail) ||
    asRecord(root.contentTypeDetail) ||
    root
  );
}

function unwrapFields(payload) {
  const body = unwrapDetail(payload);
  const nested =
    asRecord(body.SystemDefDetail) ||
    asRecord(body.systemDefDetail) ||
    asRecord(body.SharedFieldGroupDetail) ||
    asRecord(body.sharedFieldGroupDetail) ||
    body;
  const raw = nested.fields || nested.ContentTypeField || nested.SystemDefFieldSummary || [];
  if (Array.isArray(raw)) {
    return raw;
  }
  return raw ? [raw] : [];
}

function unwrapList(payload, keys) {
  if (Array.isArray(payload)) {
    return payload;
  }
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  for (const key of keys) {
    if (root[key] != null) {
      return Array.isArray(root[key]) ? root[key] : [root[key]];
    }
  }
  return [];
}

function fieldNameSet(fields) {
  return new Set(
    (fields || [])
      .map((f) => (f && f.name ? String(f.name).trim().toLowerCase() : ""))
      .filter(Boolean),
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

function expectConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
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
  if (await detailError.isVisible()) {
    throw new Error(
      `Content type detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-include"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
}

async function unlockIfHeld(page) {
  const unlock = page.locator('[data-testid="developer-ct-unlock"]');
  if ((await unlock.count()) && (await unlock.isEnabled())) {
    await unlock.click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
  }
}

async function lockType(page) {
  const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
  const status = page.locator('[data-testid="developer-ct-lock-status"]');
  const err = page.locator('[data-testid="developer-ct-detail-error"]');
  await lockBtn.click();
  await expect(status.or(err).first()).toBeVisible({ timeout: 20_000 });
  if ((await err.count()) && (await err.isVisible())) {
    throw new Error(`Lock failed: ${(await err.innerText()).trim()}`);
  }
  await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
}

async function getJson(page, path) {
  const res = await page.request.get(`${BASE_URL}/Rhythmyx/services/${path}`);
  expect(res.ok(), `GET ${path} HTTP ${res.status()}`).toBe(true);
  return res.json();
}

async function listSharedFieldNames(page) {
  const groups = unwrapList(await getJson(page, "sharedfields"), [
    "SharedFieldGroupSummary",
    "sharedFieldGroupSummary",
    "SharedFieldGroup",
    "sharedFieldGroup",
  ]);
  const names = [];
  for (const g of groups) {
    const groupName = g && g.name ? String(g.name).trim() : "";
    if (!groupName) {
      continue;
    }
    const detail = await getJson(
      page,
      `sharedfields/${encodeURIComponent(groupName)}`,
    );
    for (const f of unwrapFields(detail)) {
      const n = f && f.name ? String(f.name).trim() : "";
      if (n) {
        names.push(n);
      }
    }
  }
  return names;
}

/**
 * Find a content type (by detail name) that still has an unused shared
 * catalog field. Scans the preferred sample types in order.
 */
async function findTypeWithUnusedSharedField(page) {
  const sharedNames = await listSharedFieldNames(page);
  if (sharedNames.length === 0) {
    return null;
  }
  for (const candidate of ["percFileAsset", "percSimpleTextAsset", "percPage"]) {
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
    const existing = fieldNameSet(unwrapFields(payload));
    const unused = sharedNames.find((n) => !existing.has(n.toLowerCase()));
    if (unused) {
      const detail = unwrapDetail(payload);
      return {
        typeName: candidate,
        fieldName: unused,
        label: detail.label != null ? String(detail.label) : "",
        description: detail.description != null ? String(detail.description) : "",
        beforeNames: existing,
      };
    }
  }
  return null;
}

function escapeRegExp(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test.describe("Developer content type include one shared field (#5705 slice 124)", () => {
  test("missing held lock (409) claims no success", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const target = await findTypeWithUnusedSharedField(page);
    if (target == null) {
      test.skip(true, "H2 QA has no content type with an unused shared field");
    }

    await openContentTypeDetail(page, new RegExp(escapeRegExp(target.typeName)));
    try {
      await lockType(page);

      // Release the held lock out-of-band so the held lock is missing.
      const unlockRes = await page.request.post(
        `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(target.typeName)}/unlock`,
      );
      expect(unlockRes.ok(), `unlock HTTP ${unlockRes.status()}`).toBe(true);

      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const pick = page.locator('[data-testid="developer-ct-include-pick"]');
      const nameInput = page.locator('[data-testid="developer-ct-include-name"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      await origin.selectOption("shared");
      await expect(
        pick.locator(`option[value="${target.fieldName}"]`),
      ).toBeAttached({ timeout: 30_000 });
      await pick.selectOption(target.fieldName);
      await expect(nameInput).toHaveValue(target.fieldName);
      await expect(submit).toBeEnabled();

      const includeResp = page.waitForResponse(
        (res) =>
          res.url().includes("/fields/include") && res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      expect((await includeResp).status()).toBe(409);

      const err = page.locator('[data-testid="developer-ct-detail-error"]');
      const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
      await expect(err).toBeVisible({ timeout: 20_000 });
      await expect(err).toContainText(/include|lock/i);
      await expect(notice).toBeHidden();
      await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
        /Not locked/i,
        { timeout: 20_000 },
      );
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("include one shared field keeps origin shared; label, description, other fields stay", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const target = await findTypeWithUnusedSharedField(page);
    if (target == null) {
      test.skip(true, "H2 QA has no content type with an unused shared field");
    }

    await openContentTypeDetail(page, new RegExp(escapeRegExp(target.typeName)));
    try {
      await lockType(page);

      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const pick = page.locator('[data-testid="developer-ct-include-pick"]');
      const nameInput = page.locator('[data-testid="developer-ct-include-name"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      await expect(origin).toBeEnabled();
      await origin.selectOption("shared");
      await expect(
        pick.locator(`option[value="${target.fieldName}"]`),
      ).toBeAttached({ timeout: 30_000 });
      await pick.selectOption(target.fieldName);
      await expect(nameInput).toHaveValue(target.fieldName);
      await expect(submit).toBeEnabled();

      const includeResp = page.waitForResponse(
        (res) =>
          res.url().includes("/services/contenttypes/") &&
          res.url().includes("/fields/include") &&
          res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      const resp = await includeResp;
      expect(resp.ok(), `include HTTP ${resp.status()}`).toBe(true);

      const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
      const err = page.locator('[data-testid="developer-ct-detail-error"]');
      await expect(notice).toBeVisible({ timeout: 20_000 });
      await expect(notice).toContainText(/included/i);
      await expect(err).toBeHidden();

      const originCell = page.locator(
        `[data-testid="developer-ct-field-origin-${target.fieldName}"]`,
      );
      await expect(originCell).toHaveText(/shared/i);

      const after = unwrapFields(
        await getJson(page, `contenttypes/${encodeURIComponent(target.typeName)}`),
      );
      const afterDetail = unwrapDetail(
        await getJson(page, `contenttypes/${encodeURIComponent(target.typeName)}`),
      );
      const included = after.find(
        (f) => f && String(f.name || "").toLowerCase() === target.fieldName.toLowerCase(),
      );
      expect(included, `GET detail lists ${target.fieldName}`).toBeTruthy();
      expect(String(included.fieldType || "").toLowerCase()).toBe("shared");

      const afterNames = fieldNameSet(after);
      expect(afterNames.size).toBe(target.beforeNames.size + 1);
      for (const name of target.beforeNames) {
        expect(afterNames.has(name), `other field stays: ${name}`).toBe(true);
      }
      expect(
        afterDetail.label != null ? String(afterDetail.label) : "",
        "label stays",
      ).toBe(target.label);
      expect(
        afterDetail.description != null ? String(afterDetail.description) : "",
        "description stays",
      ).toBe(target.description);
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("cancelling the pick does not write", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    const target = await findTypeWithUnusedSharedField(page);
    if (target == null) {
      test.skip(true, "H2 QA has no content type with an unused shared field");
    }

    await openContentTypeDetail(page, new RegExp(escapeRegExp(target.typeName)));
    try {
      await lockType(page);

      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const pick = page.locator('[data-testid="developer-ct-include-pick"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      await origin.selectOption("shared");
      await expect(
        pick.locator(`option[value="${target.fieldName}"]`),
      ).toBeAttached({ timeout: 30_000 });
      await pick.selectOption(target.fieldName);
      await expect(submit).toBeEnabled();

      // Cancel the pick: back to the placeholder option.
      await pick.selectOption("");
      await expect(submit).toBeDisabled();

      let includePosts = 0;
      const onRequest = (req) => {
        if (
          req.url().includes("/fields/include") &&
          req.method() === "POST"
        ) {
          includePosts += 1;
        }
      };
      page.on("request", onRequest);
      await page.waitForTimeout(2_000);
      page.off("request", onRequest);
      expect(includePosts, "no POST .../fields/include after cancel").toBe(0);

      const after = unwrapFields(
        await getJson(page, `contenttypes/${encodeURIComponent(target.typeName)}`),
      );
      expect(
        fieldNameSet(after).has(target.fieldName.toLowerCase()),
        "cancelled field stays absent",
      ).toBe(false);
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });
});
