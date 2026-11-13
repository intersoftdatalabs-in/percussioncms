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
 * Developer workflow association dialog (#6062 / FR §5.5, parent #1690).
 *
 * FR §5.5 slice: associate ONE design object (Content Type ↔ Allowed Workflow)
 * via dialog; cancel + failed save leave edges unchanged. Disassociate restores
 * the staged draft without persisting. This spec performs NO server mutation:
 * every staged edge is removed before save, the lock is released, and the
 * wire-level 4xx check uses an unlocked PUT that the server must reject.
 *
 * Seed note: the QA H2 catalog currently ships a single workflow
 * ("Default Workflow"). When the opened type already has it, there is no
 * directly pickable candidate, so the associate leg runs as
 * remove-then-reassociate (disassociate the edge in the draft, then pick it
 * back in the dialog). Types whose design lock is unavailable (e.g. percPage
 * in this seed — the pre-existing description spec fails its lock step there
 * too) are skipped as candidates, never forced.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-workflow-association.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

async function inPageGetAllowedWorkflows(page, typeName) {
  return page.evaluate(async (name) => {
    const res = await fetch(
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}`,
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    if (!res.ok) {
      return { status: res.status, workflows: null };
    }
    const body = await res.json();
    const detail = body.ContentTypeDetail || body;
    const list = detail.allowedWorkflows || [];
    return {
      status: res.status,
      workflows: (Array.isArray(list) ? list : []).map((w) => w.name).sort(),
    };
  }, typeName);
}

async function inPageGetWorkflowCatalog(page) {
  return page.evaluate(async () => {
    const res = await fetch(
      "/Rhythmyx/services/workflowmanagement/workflows/metadata",
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    if (!res.ok) {
      return { status: res.status, workflows: null };
    }
    const body = await res.json();
    const list = body.Workflow || body.workflows || body;
    return {
      status: res.status,
      workflows: (Array.isArray(list) ? list : [])
        .map((w) => w.workflowName)
        .filter((n) => typeof n === "string" && n.length > 0)
        .sort(),
    };
  });
}

async function inPageUnlockedPutAllowedWorkflows(page, typeName, workflows) {
  return page.evaluate(
    async ({ name, wfs }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName = (metaHeader && metaHeader.content) || "OWASP_CSRFTOKEN";
      const headers = { Accept: "application/json", "Content-Type": "application/json" };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(
        `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/allowedWorkflows`,
        {
          method: "PUT",
          credentials: "same-origin",
          headers,
          body: JSON.stringify({ ContentTypeWorkflows: { allowedWorkflows: wfs } }),
        },
      );
      return res.status;
    },
    { name: typeName, wfs: workflows },
  );
}

async function workflowRowNames(section) {
  return section.locator('[data-testid^="developer-ct-wf-row-"]').allInnerTexts();
}

async function releaseLockIfHeld(page) {
  const status = page.locator('[data-testid="developer-ct-lock-status"]');
  if ((await status.count()) === 0) {
    return;
  }
  if (!/Locked by you/i.test((await status.innerText()).trim())) {
    return;
  }
  const unlock = page.locator('[data-testid="developer-ct-unlock"]');
  if ((await unlock.count()) === 0) {
    return;
  }
  await unlock.click();
  await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
}

test.describe("Developer workflow association dialog (#6062 / FR §5.5)", () => {
  test("unlocked 4xx PUT, dialog cancel, associate + disassociate leave edges unchanged", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    // Wait for the catalog to settle (New = loaded) before reading panel/empty.
    await expect(page.locator('[data-testid="developer-ct-new"]')).toBeVisible({
      timeout: 30_000,
    });
    const panel = page.locator('[data-testid="developer-ct-panel"]');
    const empty = page.locator('[data-testid="developer-ct-empty"]');
    if ((await panel.count()) === 0 && (await empty.count()) > 0) {
      test.skip(true, "no content types in the QA catalog");
      return;
    }
    await expect(panel).toBeVisible({ timeout: 30_000 });

    const catalogBefore = await inPageGetWorkflowCatalog(page);
    expect(catalogBefore.status, "workflow catalog GET should succeed").toBe(200);
    if (!catalogBefore.workflows || catalogBefore.workflows.length === 0) {
      test.skip(true, "no workflows in the QA catalog");
      return;
    }

    // Wire-level gate on percPage (no lock needed): an unlocked PUT must be
    // rejected (400/403/409) and the persisted edge set must be identical.
    const wireTarget = "percPage";
    const before = await inPageGetAllowedWorkflows(page, wireTarget);
    expect(before.status, `GET ${wireTarget} should succeed`).toBe(200);
    const putStatus = await inPageUnlockedPutAllowedWorkflows(page, wireTarget, []);
    expect(
      [400, 403, 409].includes(putStatus),
      `unlocked PUT should be 400/403/409 (got ${putStatus})`,
    ).toBe(true);
    const after = await inPageGetAllowedWorkflows(page, wireTarget);
    expect(after.status).toBe(200);
    expect(after.workflows, "failed PUT must leave persisted edges unchanged").toEqual(
      before.workflows,
    );

    // UI flow: first lockable candidate wins (percPage's design lock is
    // unavailable in this seed — see the description-spec precedent).
    const typeNames = await page
      .locator('[data-testid="developer-ct-open"]')
      .evaluateAll((els) =>
        els
          .map((e) => e.getAttribute("data-ct-name"))
          .filter((n) => typeof n === "string" && n.length > 0),
      );
    const candidates = ["percPage", ...typeNames.filter((n) => n !== "percPage")].slice(0, 4);
    let opened = null;
    for (const name of candidates) {
      const btn = page.locator(`[data-testid="developer-ct-open"][data-ct-name="${name}"]`);
      if ((await btn.count()) === 0) {
        continue;
      }
      await btn.click();
      await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
        timeout: 30_000,
      });
      await page.locator('[data-testid="developer-ct-lock"]').click();
      const locked = await expect(
        page.locator('[data-testid="developer-ct-lock-status"]'),
      )
        .toHaveText(/Locked by you/i, { timeout: 20_000 })
        .then(
          () => true,
          () => false,
        );
      if (locked) {
        opened = name;
        break;
      }
      await page.locator('[data-testid="developer-ct-back"]').click();
      await expect(panel).toBeVisible({ timeout: 30_000 });
    }
    if (opened === null) {
      test.skip(true, "no candidate type accepted a design lock in this cell");
      return;
    }

    const wfSection = page.locator('[data-testid="developer-ct-workflows"]');
    await expect(wfSection).toBeVisible();
    try {
      const initialRows = await workflowRowNames(wfSection);
      const initialNames = new Set(
        (await inPageGetAllowedWorkflows(page, opened)).workflows || [],
      );
      const pickable = catalogBefore.workflows.filter((n) => !initialNames.has(n));

      if (pickable.length === 0) {
        // Fully associated seed: disassociate one edge in the draft so the
        // dialog has something to re-associate. Nothing is saved.
        const victim = initialNames.values().next().value;
        expect(victim, "type has at least one workflow edge to stage with").toBeTruthy();
        const victimIndex = (await workflowRowNames(wfSection)).findIndex((row) =>
          row.includes(victim),
        );
        expect(victimIndex, `staged row shows ${victim}`).toBeGreaterThanOrEqual(0);
        await page.locator(`[data-testid="developer-ct-wf-remove-${victimIndex}"]`).click();
        const afterRemove = await workflowRowNames(wfSection);
        expect(afterRemove.length, "disassociate stages a removal").toBe(
          initialRows.length - 1,
        );

        await page.locator('[data-testid="developer-ct-wf-browse"]').click();
        const dialog = page.locator('[data-testid="developer-wf-assoc-dialog"]');
        await expect(dialog).toBeVisible({ timeout: 20_000 });
        const pick = dialog.locator(
          `[data-testid="developer-wf-assoc-pick-${victim}"]`,
        );
        await expect(pick).toBeVisible({ timeout: 30_000 });
        await pick.click();
        await expect(dialog.locator('[data-testid="developer-wf-assoc-confirm"]')).toBeEnabled();
        await dialog.locator('[data-testid="developer-wf-assoc-confirm"]').click();
        await expect(dialog).toHaveCount(0);
        expect(await workflowRowNames(wfSection), "re-associate restores edges").toEqual(
          initialRows,
        );
      } else {
        // Cancel path first: open, optionally pick, cancel — rows unchanged.
        await page.locator('[data-testid="developer-ct-wf-browse"]').click();
        const dialog = page.locator('[data-testid="developer-wf-assoc-dialog"]');
        await expect(dialog).toBeVisible({ timeout: 20_000 });
        const dialogRows = dialog.locator('[data-testid^="developer-wf-assoc-row-"]');
        const dialogError = dialog.locator('[data-testid="developer-wf-assoc-error"]');
        const dialogEmpty = dialog.locator('[data-testid="developer-wf-assoc-empty"]');
        await expect(
          dialogRows.first().or(dialogError).or(dialogEmpty).first(),
        ).toBeVisible({ timeout: 30_000 });
        if ((await dialogRows.count()) > 0) {
          await dialog.locator('[data-testid^="developer-wf-assoc-pick-"]').first().click();
        }
        await dialog.locator('[data-testid="developer-wf-assoc-cancel"]').click();
        await expect(dialog).toHaveCount(0);
        expect(await workflowRowNames(wfSection), "cancel must leave edges unchanged").toEqual(
          initialRows,
        );

        // Associate path: pick a workflow that is not already an edge.
        await page.locator('[data-testid="developer-ct-wf-browse"]').click();
        await expect(dialog).toBeVisible({ timeout: 20_000 });
        const wanted = pickable[0];
        const pick = dialog.locator(`[data-testid="developer-wf-assoc-pick-${wanted}"]`);
        await expect(pick).toBeVisible({ timeout: 30_000 });
        await pick.click();
        await dialog.locator('[data-testid="developer-wf-assoc-confirm"]').click();
        await expect(dialog).toHaveCount(0);
        const stagedRows = await workflowRowNames(wfSection);
        expect(stagedRows.length, "associate stages one edge").toBe(initialRows.length + 1);
        expect(
          stagedRows.some((row) => row.includes(wanted)),
          `staged edge shows ${wanted}`,
        ).toBe(true);

        // Disassociate path: remove the staged row — edges return to initial.
        const stagedIndex = stagedRows.findIndex((row) => row.includes(wanted));
        await page.locator(`[data-testid="developer-ct-wf-remove-${stagedIndex}"]`).click();
        expect(await workflowRowNames(wfSection), "disassociate restores edges").toEqual(
          initialRows,
        );
      }

      // No draft left behind: save stays disabled, nothing was persisted.
      await expect(page.locator('[data-testid="developer-ct-save"]')).toBeDisabled();
    } finally {
      await releaseLockIfHeld(page);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
