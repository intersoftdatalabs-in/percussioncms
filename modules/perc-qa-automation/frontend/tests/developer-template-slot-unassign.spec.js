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
 * Developer Templates slot unassignment (#6067 / parent #1690).
 *
 * Admin locks a template, opens the Unassign confirm for one assigned slot,
 * cancels (mapping kept, no PUT), confirms (PUT 200 removes only that slot),
 * reloads the detail (region renders unassigned), then restores the member.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-slot-unassign.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapTemplateDetail(payload) {
  if (payload == null) {
    return {};
  }
  if (payload.TemplateDetail && typeof payload.TemplateDetail === "object") {
    return payload.TemplateDetail;
  }
  return payload;
}

function asSlotList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (typeof raw !== "object") {
    return [];
  }
  const nested = raw.Slot ?? raw.TemplateSlot;
  if (Array.isArray(nested)) {
    return nested;
  }
  if (nested && typeof nested === "object") {
    return [nested];
  }
  if (raw.name || raw.label || raw.guid) {
    return [raw];
  }
  return [];
}

function slotNames(detail) {
  return asSlotList(unwrapTemplateDetail(detail).slots)
    .map((s) => String((s && (s.name || s.label)) || "").trim())
    .filter(Boolean);
}

async function releaseLeftoverTemplateLocks(page) {
  for (const name of ["perc.page", "perc.pageXml", "perc.pageDatabase"]) {
    await page.request.post(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
    );
  }
}

async function openTemplateDetail(page, namePattern) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });

  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const empty = page.locator('[data-testid="developer-tpl-empty"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "No templates in catalog — fail closed (H2 QA must include sample templates)",
    );
  }

  const table = page.locator('[data-testid="developer-tpl-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const wantName = namePattern || "perc.page";
  const byNameAttr = table.locator(`[data-tpl-name="${wantName}"]`);
  const named =
    (await byNameAttr.count()) > 0
      ? byNameAttr
      : table.locator('[data-testid^="developer-tpl-row-"]').filter({
          has: page.locator("td", {
            hasText: new RegExp(
              `^${wantName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`,
            ),
          }),
        });
  if ((await named.count()) === 0) {
    throw new Error(
      `Template ${wantName} not in Developer catalog — fail closed (do not fall back to row 0)`,
    );
  }
  const targetRow = named.first();
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-tpl-detail"]');
  const detailError = page.locator('[data-testid="developer-tpl-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Template detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-tpl-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
}

test.describe("Developer template slot unassignment (#6067)", () => {
  test("Cancel keeps mapping; confirm unassigns one slot, restore keeps cell clean", async ({
    page,
  }) => {
    test.setTimeout(240_000);
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

    await loginAsAdmin(page);
    await releaseLeftoverTemplateLocks(page);
    await openTemplateDetail(page);

    const slots = page.locator('[data-testid="developer-tpl-slots"]');
    await expect(slots).toBeVisible({ timeout: 20_000 });

    const name = (await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()).trim();
    const templateUrl = `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`;
    const getRes = await page.request.get(templateUrl);
    expect(getRes.status(), `GET template ${getRes.status()}`).toBe(200);
    const beforeNames = slotNames(await getRes.json()).map((n) => n.toLowerCase());

    const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const unlockBtn = page.locator('[data-testid="developer-tpl-unlock"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');
    const notice = page.locator('[data-testid="developer-tpl-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-tpl-detail-error"]');

    async function lockHeld() {
      const waitLock = () =>
        page.waitForResponse(
          (res) =>
            res.request().method() === "POST" && /\/templates\/[^/?]+\/lock(?:\?|$)/.test(res.url()),
          { timeout: 20_000 },
        );
      let lockWait = waitLock();
      await lockBtn.click();
      let lockHttp = await lockWait;
      if (lockHttp.status() === 409) {
        await page.request.post(`${templateUrl}/unlock`);
        lockWait = waitLock();
        await lockBtn.click();
        lockHttp = await lockWait;
      }
      expect(lockHttp.status(), `lock HTTP ${lockHttp.status()}`).toBe(200);
      await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    }

    async function putTemplate() {
      return page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" && /\/services\/templates\/[^/?]+(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
    }

    const boxes = page.locator('[data-testid^="developer-tpl-slot-check-"]');
    const boxCount = await boxes.count();
    if (boxCount === 0) {
      throw new Error(
        "No slot checkboxes in the Slots section — fail closed (H2 QA must include catalog slots)",
      );
    }

    // Ensure one assigned member: prefer an existing member, else seed by assigning.
    let targetKey = null;
    for (let i = 0; i < boxCount; i += 1) {
      const box = boxes.nth(i);
      const key = await box.getAttribute("data-testid");
      const shortKey = String(key || "").replace("developer-tpl-slot-check-", "");
      const slotName = shortKey.startsWith("name:")
        ? shortKey.slice("name:".length).toLowerCase()
        : shortKey.toLowerCase();
      if ((await box.isChecked()) && beforeNames.includes(slotName)) {
        targetKey = key;
        break;
      }
    }
    let seeded = false;
    if (!targetKey) {
      await lockHeld();
      let seedKey = null;
      for (let i = 0; i < boxCount; i += 1) {
        const box = boxes.nth(i);
        if (!(await box.isChecked())) {
          seedKey = await box.getAttribute("data-testid");
          break;
        }
      }
      if (!seedKey) {
        throw new Error(
          "No unassigned catalog slot to seed — fail closed (nothing to unassign)",
        );
      }
      await page.locator(`[data-testid="${seedKey}"]`).check();
      const seedWait = putTemplate();
      await saveBtn.click();
      const seedPut = await seedWait;
      expect(seedPut.status(), `seed PUT HTTP ${seedPut.status()}`).toBe(200);
      await expect(notice).toContainText(/saved/i);
      targetKey = seedKey;
      seeded = true;
    } else {
      await lockHeld();
    }

    const shortTarget = String(targetKey).replace("developer-tpl-slot-check-", "");
    const targetName = shortTarget.startsWith("name:")
      ? shortTarget.slice("name:".length).toLowerCase()
      : shortTarget.toLowerCase();
    const unassignBtn = page.locator(`[data-testid="developer-tpl-slot-unassign-${shortTarget}"]`);
    await expect(unassignBtn).toBeVisible({ timeout: 20_000 });
    const targetBox = page.locator(`[data-testid="${targetKey}"]`);
    await expect(targetBox).toBeChecked();

    // Cancel keeps the mapping and sends no PUT.
    let templatePuts = 0;
    const putCounter = (req) => {
      if (
        req.method() === "PUT" &&
        /\/services\/templates\/[^/?]+(?:\?|$)/.test(req.url())
      ) {
        templatePuts += 1;
      }
    };
    page.on("request", putCounter);
    await unassignBtn.click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible({
      timeout: 10_000,
    });
    // The region still renders assigned while the confirm is open.
    await expect(targetBox).toBeChecked();
    await page.screenshot({ path: "test-results/live-proof-6067-confirm-open.png" });
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeHidden({
      timeout: 10_000,
    });
    await expect(targetBox).toBeChecked();
    expect(templatePuts, "cancel must send no template PUT").toEqual(0);
    page.off("request", putCounter);

    // Confirm unassigns exactly that slot.
    await unassignBtn.click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible({
      timeout: 10_000,
    });
    const confirmWait = putTemplate();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const putHttp = await confirmWait;
    expect(putHttp.status(), `unassign PUT HTTP ${putHttp.status()}`).toBe(200);
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save (unassign) failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);
    // Region renders unassigned only after success.
    await expect(targetBox).not.toBeChecked({ timeout: 20_000 });
    await page.screenshot({ path: "test-results/live-proof-6067-unassigned.png" });
    await expect(
      page.locator(`[data-testid="developer-tpl-slot-unassign-${shortTarget}"]`),
    ).toHaveCount(0);

    const putNames = slotNames(await putHttp.json()).map((n) => n.toLowerCase());
    expect(putNames).not.toContain(targetName);
    for (const kept of beforeNames) {
      if (kept === targetName) continue;
      expect(putNames, `unassign PUT must keep existing member ${kept}`).toContain(kept);
    }

    const afterUnassign = await page.request.get(templateUrl);
    expect(afterUnassign.status()).toBe(200);
    const afterNames = slotNames(await afterUnassign.json()).map((n) => n.toLowerCase());
    expect(afterNames).not.toContain(targetName);

    // Reload the detail: the region still renders unassigned.
    await page.locator('[data-testid="developer-tpl-back"]').click();
    await openTemplateDetail(page);
    const reloadedBox = page.locator(`[data-testid="${targetKey}"]`);
    await expect(reloadedBox).toBeVisible({ timeout: 20_000 });
    await expect(reloadedBox).not.toBeChecked();

    if (!seeded) {
      // Restore the member so the cell is left as found.
      await lockHeld();
      await reloadedBox.check();
      const restoreWait = putTemplate();
      await saveBtn.click();
      const restorePut = await restoreWait;
      expect(restorePut.status(), `restore PUT HTTP ${restorePut.status()}`).toBe(200);
      await expect(notice).toContainText(/saved/i);
      const restoredNames = slotNames(await restorePut.json()).map((n) => n.toLowerCase());
      expect(restoredNames).toContain(targetName);
      await unlockBtn.click();
      await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    } else {
      // Seeded member removed again: the template is back to its found state.
      const finalGet = await page.request.get(templateUrl);
      expect(finalGet.status()).toBe(200);
      expect(
        slotNames(await finalGet.json()).map((n) => n.toLowerCase()),
      ).not.toContain(targetName);
    }

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
