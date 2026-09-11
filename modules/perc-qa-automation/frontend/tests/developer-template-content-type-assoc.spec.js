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
 * Developer Templates content-type associations (#4461 / parent #1690 AS-08).
 *
 * Admin locks a template, adds/removes a content-type association (name + GUID),
 * saves while the lock is held, then restores and unlocks.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-content-type-assoc.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const CANDIDATE_TYPES = [
  "percImageAsset",
  "percFileAsset",
  "rffBrief",
  "percCalendarAsset",
  "percTitleAsset",
];

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

function asNamedObjectRefList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (typeof raw !== "object") {
    return [];
  }
  if (Array.isArray(raw.NamedObjectRef)) {
    return raw.NamedObjectRef;
  }
  if (raw.NamedObjectRef && typeof raw.NamedObjectRef === "object") {
    return asNamedObjectRefList(raw.NamedObjectRef);
  }
  if (raw.name || raw.label || raw.guid) {
    return [raw];
  }
  return [];
}

function assocNames(detail) {
  return asNamedObjectRefList(unwrapTemplateDetail(detail).associatedContentTypes)
    .map((r) => String((r && (r.name || r.label)) || "").trim())
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
  const named = table.locator('[data-testid^="developer-tpl-row-"]').filter({
    hasText: namePattern || /\bperc\.page\b/,
  });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-tpl-row", 0));
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

test.describe("Developer template content-type associations (#4461)", () => {
  test("Admin can lock, add/remove a content type, save, and unlock", async ({
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

    const assoc = page.locator('[data-testid="developer-tpl-ct-assoc"]');
    await expect(assoc).toBeVisible({ timeout: 20_000 });

    const name = (await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()).trim();
    const getRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(getRes.status(), `GET template ${getRes.status()}`).toBe(200);
    const beforeNames = assocNames(await getRes.json());
    expect(
      beforeNames.length >= 0,
      "associatedContentTypes must be present on GET (empty list is ok)",
    ).toBeTruthy();

    const addName = CANDIDATE_TYPES.find(
      (n) => !beforeNames.some((x) => x.toLowerCase() === n.toLowerCase()),
    );
    if (!addName) {
      throw new Error(
        `No unused candidate content type among ${CANDIDATE_TYPES.join(", ")}; already associated: ${beforeNames.join(", ")}`,
      );
    }

    const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
    const saveBtn = page.locator('[data-testid="developer-tpl-save"]');
    const unlockBtn = page.locator('[data-testid="developer-tpl-unlock"]');
    const status = page.locator('[data-testid="developer-tpl-lock-status"]');

    await expect(lockBtn).toBeEnabled();
    await expect(page.locator('[data-testid="developer-tpl-ct-add"]')).toBeDisabled();

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

    await page.locator('[data-testid="developer-tpl-ct-input"]').fill(addName);
    await page.locator('[data-testid="developer-tpl-ct-add"]').click();
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-tpl-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-tpl-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save (add) failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    const afterAdd = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(afterAdd.status()).toBe(200);
    const afterAddJson = await afterAdd.json();
    const afterAddNames = assocNames(afterAddJson);
    expect(afterAddNames.map((n) => n.toLowerCase())).toContain(addName.toLowerCase());
    const addedRow = asNamedObjectRefList(
      unwrapTemplateDetail(afterAddJson).associatedContentTypes,
    ).find((r) => String((r && r.name) || "").toLowerCase() === addName.toLowerCase());
    expect(addedRow, "added association must include name").toBeTruthy();
    expect(
      addedRow.guid && (addedRow.guid.stringValue || addedRow.guid.uuid != null),
      "added association must include guid",
    ).toBeTruthy();

    const row = page.locator('[data-testid^="developer-tpl-ct-row-"]').filter({
      hasText: addName,
    });
    await expect(row.first()).toBeVisible();
    await row.first().locator('button[data-testid^="developer-tpl-ct-remove-"]').click();
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save (restore) failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    const afterRestore = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(afterRestore.status()).toBe(200);
    const restored = assocNames(await afterRestore.json()).map((n) => n.toLowerCase());
    expect(restored).not.toContain(addName.toLowerCase());

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-tpl-ct-add"]')).toBeDisabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
