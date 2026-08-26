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
 * Developer Content Type allowed-workflow associations chrome (CD-08 / #3782).
 *
 * Admin locks a type, replaces the allowed-workflow set, saves via
 * PUT .../allowedWorkflows, then GET lists the new set. Unlocked save is
 * disabled (no lock steal). Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-workflows.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function asWorkflowList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw.NamedObjectRef != null) {
    return [].concat(raw.NamedObjectRef);
  }
  if (typeof raw.empty === "boolean") {
    return [];
  }
  if (raw.name || raw.label) {
    return [raw];
  }
  return [];
}

function unwrapContentTypeDetail(body) {
  if (body == null || typeof body !== "object") {
    return {};
  }
  return body.ContentTypeDetail || body.contentTypeDetail || body;
}

async function getAllowedWorkflowState(request, typeName) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const url = `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`;
  const res = await request.get(url, { headers });
  expect(res.status(), `GET ${url}`).toBe(200);
  const detail = unwrapContentTypeDetail(await res.json());
  const names = asWorkflowList(detail.allowedWorkflows)
    .map((w) => w && w.name)
    .filter(Boolean);
  const defaultName =
    (detail.defaultWorkflow && detail.defaultWorkflow.name) || names[0] || "";
  return { names, defaultName };
}

async function openPercPageDetail(page) {
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
    throw new Error("No content types in catalog — cannot exercise workflow associations");
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-ct-row-"]').filter({
    hasText: /percPage/,
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
  await expect(page.locator('[data-testid="developer-ct-workflows"]')).toBeVisible();
  const nameEl = page.locator('[data-testid="developer-ct-detail-name"]');
  const typeName = ((await nameEl.count()) ? await nameEl.innerText() : "percPage").trim();
  return typeName || "percPage";
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
  return {
    assertClean() {
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
      );
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  };
}

test.describe("Developer content type allowed workflows (CD-08 / #3782)", () => {
  test("without lock, workflow editors and save stay disabled", async ({ page }) => {
    test.setTimeout(120_000);
    const guards = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openPercPageDetail(page);

    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const addName = page.locator('[data-testid="developer-ct-wf-add-name"]');
    const addBtn = page.locator('[data-testid="developer-ct-wf-add"]');
    await expect(saveBtn).toBeDisabled();
    await expect(addName).toBeDisabled();
    await expect(addBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
    );
    const remove0 = page.locator('[data-testid="developer-ct-wf-remove-0"]');
    if ((await remove0.count()) > 0) {
      await expect(remove0).toBeDisabled();
    }
    guards.assertClean();
  });

  test("Admin lock, PUT allowedWorkflows, GET lists the new set", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const putUrls = [];
    page.on("request", (req) => {
      if (req.method() === "PUT") {
        putUrls.push(req.url());
      }
    });

    await loginAsAdmin(page);
    const typeName = await openPercPageDetail(page);
    const original = await getAllowedWorkflowState(request, typeName);
    const originalNames = original.names;
    if (originalNames.length < 2) {
      throw new Error(
        `${typeName} needs at least two allowed workflows to replace (have: ${originalNames.join(", ") || "none"})`,
      );
    }
    const toRemove =
      originalNames.find((n) => n && n !== original.defaultName) ||
      originalNames[originalNames.length - 1];

    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(lockBtn).toBeEnabled();
    const lockResponsePromise = page.waitForResponse(
      (r) => r.request().method() === "POST" && /\/contenttypes\/[^/]+\/lock(?:\?|$)/.test(r.url()),
      { timeout: 20_000 },
    );
    await lockBtn.click();
    const lockRes = await lockResponsePromise;
    if (!lockRes.ok()) {
      const body = await lockRes.text();
      throw new Error(`Lock HTTP ${lockRes.status()} ${body}`);
    }
    if (await saveError.isVisible()) {
      throw new Error(`Lock failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-ct-wf-add-name"]')).toBeEnabled();

    const removeRow = page
      .locator('[data-testid^="developer-ct-wf-row-"]')
      .filter({ hasText: toRemove });
    await expect(removeRow).toHaveCount(1);
    await removeRow.locator('[data-testid^="developer-ct-wf-remove-"]').click();
    await expect(saveBtn).toBeEnabled();

    putUrls.length = 0;
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Workflow save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);
    await expect(status).toHaveText(/Locked by you/i);

    const workflowPuts = putUrls.filter((u) => /\/allowedWorkflows(?:\?|$)/.test(u));
    expect(
      workflowPuts.length,
      `expected PUT .../allowedWorkflows, got: ${putUrls.join(" | ") || "(none)"}`,
    ).toBeGreaterThan(0);

    const afterNames = (await getAllowedWorkflowState(request, typeName)).names;
    expect(afterNames, "GET after save should omit the removed workflow").not.toContain(toRemove);

    await page.locator('[data-testid="developer-ct-wf-add-name"]').fill(toRemove);
    await page.locator('[data-testid="developer-ct-wf-add"]').click();
    await expect(page.locator('[data-testid="developer-ct-workflows"]')).toContainText(toRemove);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Restore save failed: ${(await saveError.innerText()).trim()}`);
    }
    const restored = (await getAllowedWorkflowState(request, typeName)).names;
    expect(restored.sort()).toEqual([...originalNames].sort());

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-wf-add-name"]')).toBeDisabled();

    guards.assertClean();
  });
});
