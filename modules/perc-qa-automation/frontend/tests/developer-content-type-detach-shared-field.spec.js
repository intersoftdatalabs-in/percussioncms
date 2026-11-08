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
 * Developer Content Type detach one shared field (#5787 / parent #1690).
 *
 * Admin locks a type, includes a shared catalog field (setup), then detaches
 * it through the row Detach confirm: the detached field leaves the type while
 * origin-local fields, other shared fields, label, and description stay.
 * Cancel does not write; an unlock-then-detach API call is 409.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-detach-shared-field.spec.js
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

function fieldNames(fields) {
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
  await expect(page.locator('[data-testid="developer-ct-fields-table"]')).toBeVisible({
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

async function findUnusedSharedTarget(page, typeName) {
  const typePayload = await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`);
  const existing = fieldNames(unwrapFields(typePayload));
  const groups = unwrapList(await getJson(page, "sharedfields"), [
    "SharedFieldGroupSummary",
    "sharedFieldGroupSummary",
    "SharedFieldGroup",
    "sharedFieldGroup",
  ]);
  for (const g of groups) {
    const groupName = g && g.name ? String(g.name).trim() : "";
    if (!groupName) {
      continue;
    }
    const groupPayload = await getJson(
      page,
      `sharedfields/${encodeURIComponent(groupName)}`,
    );
    const unusedShared = unwrapFields(groupPayload).find((f) => {
      const n = f && f.name ? String(f.name).trim() : "";
      return n && !existing.has(n.toLowerCase());
    });
    if (unusedShared && unusedShared.name) {
      return { name: String(unusedShared.name).trim(), group: groupName };
    }
  }
  return null;
}

test.describe("Developer content type detach one shared field (#5787)", () => {
  test("detach button is disabled until lock", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percFileAsset|percSimpleTextAsset/);

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const fields = unwrapFields(
      await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`),
    );
    const shared = fields.find(
      (f) => f && f.name && String(f.fieldType || "").toLowerCase() === "shared",
    );
    if (!shared) {
      test.skip(true, "H2 QA type has no included shared field for the disabled check");
    }
    await expect(
      page.locator(`[data-testid="developer-ct-field-detach-${shared.name}"]`),
    ).toBeDisabled();

    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("confirm detaches one shared field and keeps the rest; cancel does not write", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percFileAsset|percSimpleTextAsset/);

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const target = await findUnusedSharedTarget(page, typeName);
    if (target == null) {
      test.skip(true, "H2 QA has no unused shared catalog field for the detach round trip");
    }

    const labelBefore = (
      await page.locator('[data-testid="developer-ct-label"]').inputValue()
    ).trim();
    const fieldsBefore = unwrapFields(
      await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`),
    );
    const namesBefore = fieldNames(fieldsBefore);

    try {
      await lockType(page);

      // Setup: include one shared field so detach has a live target.
      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const nameInput = page.locator('[data-testid="developer-ct-include-name"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      await origin.selectOption("shared");
      await nameInput.fill(target.name);
      const includeResp = page.waitForResponse(
        (res) =>
          res.url().includes("/services/contenttypes/") &&
          res.url().includes("/fields/include") &&
          res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      expect((await includeResp).ok(), "include setup").toBe(true);
      const detachBtn = page.locator(
        `[data-testid="developer-ct-field-detach-${target.name}"]`,
      );
      await expect(detachBtn).toBeEnabled({ timeout: 20_000 });

      // Cancel path: no DELETE is fired and the field stays.
      let deleteFired = false;
      page.on("request", (req) => {
        if (
          req.method() === "DELETE" &&
          req.url().includes("/services/contenttypes/") &&
          req.url().includes("/fields/include/")
        ) {
          deleteFired = true;
        }
      });
      await detachBtn.click();
      const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator('[data-testid="developer-catalog-confirm-body"]'),
      ).toContainText(/Detach/i);
      await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
      await expect(dialog).toBeHidden({ timeout: 10_000 });
      expect(deleteFired, "cancel must not write").toBe(false);
      await expect(
        page.locator(`[data-testid="developer-ct-field-origin-${target.name}"]`),
      ).toHaveText(/shared/i);

      // Confirm path: DELETE 204, row leaves, everything else stays.
      await detachBtn.click();
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      const detachResp = page.waitForResponse(
        (res) =>
          res.url().includes("/services/contenttypes/") &&
          res.url().includes("/fields/include/") &&
          res.request().method() === "DELETE",
        { timeout: 30_000 },
      );
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const resp = await detachResp;
      expect(resp.status(), "detach HTTP").toBe(204);

      const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
      const err = page.locator('[data-testid="developer-ct-detail-error"]');
      await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
      if (await err.isVisible()) {
        throw new Error(`Detach failed: ${(await err.innerText()).trim()}`);
      }
      await expect(notice).toContainText(/detached/i);
      await expect(
        page.locator(`[data-testid="developer-ct-field-row"][data-field-name="${target.name}"]`),
      ).toHaveCount(0);

      const afterPayload = await getJson(
        page,
        `contenttypes/${encodeURIComponent(typeName)}`,
      );
      const after = unwrapFields(afterPayload);
      const afterNames = fieldNames(after);
      expect(afterNames.has(target.name.toLowerCase()), "detached field is gone").toBe(false);
      for (const kept of namesBefore) {
        expect(afterNames.has(kept), `pre-existing field stays: ${kept}`).toBe(true);
      }
      const detailBody = unwrapDetail(afterPayload);
      expect(String(detailBody.label || "").trim()).toBe(labelBefore);
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("detach without a held lock is 409 (no success claim)", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percFileAsset|percSimpleTextAsset/);

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const fields = unwrapFields(
      await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`),
    );
    const shared = fields.find(
      (f) => f && f.name && String(f.fieldType || "").toLowerCase() === "shared",
    );
    if (!shared) {
      test.skip(true, "H2 QA type has no included shared field for the 409 check");
    }
    const res = await page.request.delete(
      `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/fields/include/${encodeURIComponent(shared.name)}`,
    );
    expect(res.status(), "detach without lock").toBe(409);

    expectConsoleClean(pageErrors, consoleErrors);
  });
});
