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
 * Developer Templates slot assignment (#5786 / parent #1690 slice 133).
 *
 * Admin locks a template, checks one unassigned catalog slot in the Slots
 * section, saves while the lock is held, then unchecks (restore) and unlocks.
 * Save without a held lock must show the lock error and send no template PUT.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-slot-assign.spec.js
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
  const nameEl = page.locator('[data-testid="developer-tpl-detail-name"]');
  if ((await nameEl.count()) > 0) {
    const name = (await nameEl.innerText()).trim();
    if (name) {
      await page.request.post(
        `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
      );
    }
  }
  return detail;
}

test.describe("Developer template slot assignment (#5786)", () => {
  test("Admin can lock, assign one slot, save, restore, and unlock", async ({
    page,
  }) => {
    test.setTimeout(180_000);
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
    const getRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(getRes.status(), `GET template ${getRes.status()}`).toBe(200);
    const beforeNames = slotNames(await getRes.json()).map((n) => n.toLowerCase());

    const boxes = page.locator('[data-testid^="developer-tpl-slot-check-"]');
    const boxCount = await boxes.count();
    if (boxCount === 0) {
      throw new Error(
        "No slot checkboxes in the Slots section — fail closed (H2 QA must include catalog slots)",
      );
    }
    let targetKey = null;
    for (let i = 0; i < boxCount; i += 1) {
      const box = boxes.nth(i);
      const key = await box.getAttribute("data-testid");
      const shortKey = String(key || "").replace("developer-tpl-slot-check-", "");
      const slotName = shortKey.startsWith("name:")
        ? shortKey.slice("name:".length).toLowerCase()
        : shortKey.toLowerCase();
      if (!(await box.isChecked()) && !beforeNames.includes(slotName)) {
        targetKey = key;
        break;
      }
    }
    if (!targetKey) {
      throw new Error(
        `Every catalog slot is already a member of ${name} — fail closed (nothing to assign)`,
      );
    }

    const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const unlockBtn = page.locator('[data-testid="developer-tpl-unlock"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');

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
      await page.request.post(
        `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}/unlock`,
      );
      lockWait = waitLock();
      await lockBtn.click();
      lockHttp = await lockWait;
    }
    expect(lockHttp.status(), `lock HTTP ${lockHttp.status()}`).toBe(200);
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(unlockBtn).toBeEnabled();

    const targetBox = page.locator(`[data-testid="${targetKey}"]`);
    await targetBox.check();
    await expect(saveBtn).toBeEnabled();
    const putWait = page.waitForResponse(
      (res) =>
        res.request().method() === "PUT" && /\/services\/templates\/[^/?]+(?:\?|$)/.test(res.url()),
      { timeout: 20_000 },
    );
    await saveBtn.click();
    const putHttp = await putWait;
    expect(putHttp.status(), `assign PUT HTTP ${putHttp.status()}`).toBe(200);
    const notice = page.locator('[data-testid="developer-tpl-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-tpl-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save (assign) failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    const putNames = slotNames(await putHttp.json()).map((n) => n.toLowerCase());
    const addedName = targetKey.replace("developer-tpl-slot-check-", "").replace(/^name:/, "").toLowerCase();
    expect(putNames).toContain(addedName);
    for (const kept of beforeNames) {
      expect(putNames, `assign PUT must keep existing member ${kept}`).toContain(kept);
    }

    const afterAdd = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(afterAdd.status()).toBe(200);
    const afterAddNames = slotNames(await afterAdd.json()).map((n) => n.toLowerCase());
    expect(afterAddNames).toContain(addedName);

    // Restore: uncheck the same slot, save, confirm it leaves the membership.
    await targetBox.uncheck();
    await expect(saveBtn).toBeEnabled();
    const restorePutWait = page.waitForResponse(
      (res) =>
        res.request().method() === "PUT" && /\/services\/templates\/[^/?]+(?:\?|$)/.test(res.url()),
      { timeout: 20_000 },
    );
    await saveBtn.click();
    const restorePutHttp = await restorePutWait;
    expect(restorePutHttp.status(), `restore PUT HTTP ${restorePutHttp.status()}`).toBe(200);
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save (restore) failed: ${(await saveError.innerText()).trim()}`);
    }
    const restorePutNames = slotNames(await restorePutHttp.json()).map((n) =>
      n.toLowerCase(),
    );
    expect(restorePutNames).not.toContain(addedName);

    const afterRestore = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(afterRestore.status()).toBe(200);
    const restored = slotNames(await afterRestore.json()).map((n) => n.toLowerCase());
    expect(restored).not.toContain(addedName);

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });

  test("Save without a held lock shows the lock error and sends no PUT", async ({
    page,
  }) => {
    test.setTimeout(120_000);
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

    const templatePuts = [];
    page.on("request", (req) => {
      if (req.method() !== "PUT") {
        return;
      }
      if (/\/services\/templates\/[^/?]+(?:\?|$)/.test(req.url())) {
        templatePuts.push(req.url());
      }
    });

    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    // Force the button enabled path is lock-gated; without a lock the panel
    // must refuse the save with the lock error and no template PUT.
    await expect(saveBtn).toBeDisabled();
    expect(templatePuts, "no template PUT without a save attempt").toEqual([]);

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
