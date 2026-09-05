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
 * Developer Workflow allowed content types (SY-06 / #4296 / parent #1690).
 *
 * Admin opens a workflow detail, replaces the allowed content-type set via
 * PUT .../workflows/{id}/allowedContentTypes (no client-held design lock;
 * server locks affected CTs), then GET lists the new set. Peer of CD-08
 * developer-content-type-workflows.spec.js (CT → workflow side).
 *
 * Consumes REST/SPA tips #4298 / #4299.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-content-types.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

/** Prefer Default Workflow (rich H2 associations); fall back to Simple Workflow. */
const PREFERRED_WORKFLOWS = ["Default Workflow", "Simple Workflow"];

/** Stock content type that ships on H2 when the workflow list is empty. */
const FALLBACK_CT = "percPage";

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function asNamedObjectRefList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw.NamedObjectRef != null) {
    return [].concat(raw.NamedObjectRef);
  }
  if (raw.NamedObjectRefList != null) {
    return asNamedObjectRefList(raw.NamedObjectRefList);
  }
  if (typeof raw.empty === "boolean") {
    return [];
  }
  if (raw.name || raw.label) {
    return [raw];
  }
  return [];
}

function unwrapNamedObjectRefPayload(body) {
  if (body == null || typeof body !== "object") {
    return [];
  }
  if (body.NamedObjectRefList != null) {
    return asNamedObjectRefList(body.NamedObjectRefList);
  }
  return asNamedObjectRefList(body);
}

/** Content-type names often ship as {@code rx:percPage}; SPA add accepts bare {@code percPage}. */
function normalizeCtName(name) {
  const s = String(name || "").trim();
  return s.replace(/^rx:/i, "");
}

async function getAllowedContentTypes(request, workflowName) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const url = `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(workflowName)}/allowedContentTypes`;
  const res = await request.get(url, { headers });
  expect(res.status(), `GET ${url}`).toBe(200);
  const names = unwrapNamedObjectRefPayload(await res.json())
    .map((r) => r && r.name)
    .filter(Boolean)
    .map(normalizeCtName);
  return names;
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

/**
 * Open Developer → Workflows catalog and open a preferred workflow detail.
 * @returns {Promise<string>} workflow name opened
 */
async function openPreferredWorkflowDetail(page) {
  await page.goto(developerWorkflowsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(
    page.locator('[data-testid="tab-developer-workflows"]'),
  ).toBeVisible({ timeout: 15_000 });

  const panel = page.locator('[data-testid="developer-wf-panel"]');
  const empty = page.locator('[data-testid="developer-wf-empty"]');
  const listError = page.locator('[data-testid="developer-wf-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer workflows catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("No workflows in catalog — cannot exercise SY-06 content types");
  }

  await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible();

  const openButtons = page.locator(
    '[data-testid="developer-wf-table"] button[aria-label^="Open "]',
  );
  const count = await openButtons.count();
  expect(count, "workflow catalog should have at least one open button").toBeGreaterThan(
    0,
  );

  const names = [];
  for (let i = 0; i < count; i++) {
    const label = (await openButtons.nth(i).getAttribute("aria-label")) || "";
    names.push(label.replace(/^Open\s+/i, "").trim());
  }
  const preferred = PREFERRED_WORKFLOWS.find((n) => names.includes(n));
  const target = preferred || names[0];

  const openBtn = page.locator(
    catalogOpenByExactName("developer-wf-open", "data-wf-name", target),
  );
  await expect(openBtn).toBeVisible();
  await openBtn.click();

  const detail = page.locator('[data-testid="developer-wf-detail"]');
  const detailError = page.locator('[data-testid="developer-wf-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 20_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Workflow detail failed for "${target}": ${(await detailError.innerText()).trim()}`,
    );
  }

  await expect(page.locator('[data-testid="developer-wf-content-types"]')).toBeVisible({
    timeout: 30_000,
  });
  const ctLoading = page.locator('[data-testid="developer-wf-ct-loading"]');
  const ctError = page.locator('[data-testid="developer-wf-ct-error"]');
  // Wait until loading clears (or error/empty/rows appear).
  await expect(ctLoading).toHaveCount(0, { timeout: 30_000 }).catch(() => undefined);
  if (await ctError.isVisible()) {
    throw new Error(
      `Allowed content types load failed: ${(await ctError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-wf-ct-add-name"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-wf-ct-save"]')).toBeVisible();

  return target;
}

test.describe("Developer workflow allowed content types (SY-06 / #4296)", () => {
  test("save stays disabled until the allowed content type set is dirty", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const guards = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openPreferredWorkflowDetail(page);

    const saveBtn = page.locator('[data-testid="developer-wf-ct-save"]');
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-wf-ct-add"]')).toBeDisabled();
    guards.assertClean();
  });

  test("Admin replaces allowed content types from workflow detail and GET matches", async ({
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
    const workflowName = await openPreferredWorkflowDetail(page);
    const originalNames = await getAllowedContentTypes(request, workflowName);

    const saveBtn = page.locator('[data-testid="developer-wf-ct-save"]');
    const addName = page.locator('[data-testid="developer-wf-ct-add-name"]');
    const addBtn = page.locator('[data-testid="developer-wf-ct-add"]');
    const notice = page.locator('[data-testid="developer-wf-ct-notice"]');
    const ctError = page.locator('[data-testid="developer-wf-ct-error"]');

    let mutatedName;
    if (originalNames.length > 0) {
      mutatedName = originalNames[originalNames.length - 1];
      const removeRow = page
        .locator('[data-testid^="developer-wf-ct-row-"]')
        .filter({ hasText: mutatedName });
      await expect(removeRow).toHaveCount(1);
      await removeRow.locator('[data-testid^="developer-wf-ct-remove-"]').click();
    } else {
      mutatedName = FALLBACK_CT;
      await addName.fill(mutatedName);
      await expect(addBtn).toBeEnabled();
      await addBtn.click();
      await expect(page.locator('[data-testid="developer-wf-content-types"]')).toContainText(
        mutatedName,
      );
    }

    await expect(saveBtn).toBeEnabled();
    putUrls.length = 0;
    const putWait = page.waitForResponse(
      (r) =>
        /\/services\/workflows\/[^/]+\/allowedContentTypes(?:\?|$)/i.test(r.url()) &&
        r.request().method() === "PUT",
      { timeout: 30_000 },
    );
    await saveBtn.click();
    const putRes = await putWait;
    await expect(notice.or(ctError).first()).toBeVisible({ timeout: 20_000 });
    if (await ctError.isVisible()) {
      const putText = await putRes.text().catch(() => "");
      throw new Error(
        `Content-type save failed: ${(await ctError.innerText()).trim()} | PUT ${putRes.status()} ${putText}`,
      );
    }
    await expect(notice).toContainText(/saved/i);
    expect(putRes.ok(), `PUT status ${putRes.status()}`).toBeTruthy();

    const assocPuts = putUrls.filter((u) =>
      /\/allowedContentTypes(?:\?|$)/i.test(u),
    );
    expect(
      assocPuts.length,
      `expected PUT .../allowedContentTypes, got: ${putUrls.join(" | ") || "(none)"}`,
    ).toBeGreaterThan(0);

    const afterNames = await getAllowedContentTypes(request, workflowName);
    if (originalNames.length > 0) {
      expect(
        afterNames,
        "GET after save should omit the removed content type",
      ).not.toContain(mutatedName);
    } else {
      expect(
        afterNames,
        "GET after save should include the added content type",
      ).toContain(mutatedName);
    }

    // Restore original set.
    if (originalNames.length > 0) {
      await addName.fill(mutatedName);
      await addBtn.click();
      await expect(page.locator('[data-testid="developer-wf-content-types"]')).toContainText(
        mutatedName,
      );
    } else {
      const removeRow = page
        .locator('[data-testid^="developer-wf-ct-row-"]')
        .filter({ hasText: mutatedName });
      await expect(removeRow).toHaveCount(1);
      await removeRow.locator('[data-testid^="developer-wf-ct-remove-"]').click();
    }

    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(ctError).first()).toBeVisible({ timeout: 20_000 });
    if (await ctError.isVisible()) {
      throw new Error(
        `Restore save failed: ${(await ctError.innerText()).trim()}`,
      );
    }

    const restored = await getAllowedContentTypes(request, workflowName);
    expect(restored.sort()).toEqual([...originalNames].sort());
    await expect(saveBtn).toBeDisabled();

    guards.assertClean();
  });
});
