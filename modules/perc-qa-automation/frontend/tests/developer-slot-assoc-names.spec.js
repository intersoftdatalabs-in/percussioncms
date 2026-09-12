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
 * Developer Slots content-type / template association names (#4462 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-slot-assoc-names.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

const CANDIDATE_TYPES = [
  "percImageAsset",
  "percFileAsset",
  "percPage",
  "rffBrief",
  "percTitleAsset",
];
const CANDIDATE_TEMPLATES = ["perc.page", "perc.pageXml", "rffSnTitle"];

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueSlotName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4462${suffix || "slot"}`;
}

function unwrapSlotDetail(payload) {
  if (payload == null) {
    return {};
  }
  if (payload.SlotDetail && typeof payload.SlotDetail === "object") {
    return payload.SlotDetail;
  }
  return payload;
}

function namesFromCatalog(payload, wrapKeys) {
  if (payload == null) {
    return [];
  }
  let list = payload;
  if (!Array.isArray(list) && typeof list === "object") {
    for (const k of wrapKeys) {
      if (list[k] != null) {
        list = list[k];
        break;
      }
    }
  }
  if (!Array.isArray(list) && list && typeof list === "object") {
    list = [list];
  }
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map((t) => String((t && (t.name || t.internalName)) || "").trim()).filter(Boolean);
}

function asAssocList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (typeof raw !== "object") {
    return [];
  }
  if (Array.isArray(raw.SlotAssociation)) {
    return raw.SlotAssociation;
  }
  if (raw.SlotAssociation && typeof raw.SlotAssociation === "object") {
    return [raw.SlotAssociation];
  }
  if (raw.contentTypeName || raw.templateName || raw.contentTypeGuid) {
    return [raw];
  }
  return [];
}

async function openSlotsCatalog(page) {
  await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-slot-panel"]');
  const empty = page.locator('[data-testid="developer-slot-empty"]');
  const listError = page.locator('[data-testid="developer-slot-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer slots catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-slot-new"]')).toBeVisible();
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

async function createSlot(page, name, label) {
  await page.locator('[data-testid="developer-slot-new"]').click();
  await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-slot-name"]').fill(name);
  await page.locator('[data-testid="developer-slot-label"]').fill(label);
  await page.locator('[data-testid="developer-slot-save"]').click();
  const notice = page.locator('[data-testid="developer-slot-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
  await expect(notice).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
}

async function lockSlot(page) {
  await page.locator('[data-testid="developer-slot-lock"]').click();
  await expect(page.locator('[data-testid="developer-slot-assoc-ct"]')).toBeEnabled({
    timeout: 20_000,
  });
}

async function deleteCurrentSlot(page, name) {
  const del = page.locator('[data-testid="developer-slot-delete"]');
  if (await del.isVisible()) {
    await del.click();
    await confirmDeveloperCatalogDelete(page);
    const panel = page.locator('[data-testid="developer-slot-panel"]');
    const err = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(panel.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      const unlock = page.locator('[data-testid="developer-slot-unlock"]');
      if (await unlock.isEnabled()) {
        await unlock.click();
        await page.locator('[data-testid="developer-slot-delete"]').click();
        await confirmDeveloperCatalogDelete(page);
        await expect(page.locator('[data-testid="developer-slot-panel"]')).toBeVisible({
          timeout: 20_000,
        });
      }
    }
    await expect(page.locator(`[data-slot-name="${name}"]`)).toHaveCount(0);
  }
}

async function pickCatalogNames(page) {
  const typesRes = await page.request.get(`${BASE_URL}/Rhythmyx/services/contenttypes`);
  expect(typesRes.ok(), `contenttypes ${typesRes.status()}`).toBeTruthy();
  const typesJson = await typesRes.json();
  const typeNames = namesFromCatalog(typesJson, [
    "ContentType",
    "ContentTypeSummary",
    "ContentTypeList",
  ]);
  const ct =
    CANDIDATE_TYPES.find((n) => typeNames.includes(n)) || typeNames[0];

  const tplRes = await page.request.get(`${BASE_URL}/Rhythmyx/services/templates`);
  expect(tplRes.ok(), `templates ${tplRes.status()}`).toBeTruthy();
  const tplJson = await tplRes.json();
  const tplNames = namesFromCatalog(tplJson, [
    "Template",
    "TemplateSummary",
    "TemplateList",
    "AssemblyTemplate",
  ]);
  const tpl =
    CANDIDATE_TEMPLATES.find((n) => tplNames.includes(n)) ||
    tplNames[0] ||
    "perc.page";

  if (!ct || !tpl) {
    throw new Error(
      `Need a content type and template on H2 QA (types=${typeNames.slice(0, 5)} templates=${tplNames.slice(0, 5)})`,
    );
  }
  return { ct, tpl };
}

test.describe("Developer slot association names (#4462 / AS-01)", () => {
  test("Admin can lock a slot and save associations by name", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    const { ct, tpl } = await pickCatalogNames(page);
    await openSlotsCatalog(page);

    const name = uniqueSlotName();
    const label = `QA 4462 ${name}`;
    await createSlot(page, name, label);

    await expect(page.locator('[data-testid="developer-slot-assoc-ct"]')).toBeDisabled();
    await lockSlot(page);

    await page.locator('[data-testid="developer-slot-assoc-ct"]').fill(ct);
    await page.locator('[data-testid="developer-slot-assoc-tpl"]').fill(tpl);
    await page.locator('[data-testid="developer-slot-assoc-add"]').click();
    await expect(page.locator('[data-testid="developer-slot-assoc-ct-name-0"]')).toHaveText(
      ct,
      { timeout: 10_000 },
    );
    await expect(page.locator('[data-testid="developer-slot-assoc-tpl-name-0"]')).toHaveText(
      tpl,
    );

    await page.locator('[data-testid="developer-slot-save"]').click();
    const notice = page.locator('[data-testid="developer-slot-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Association save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-slot-assoc-ct-name-0"]')).toHaveText(
      ct,
    );

    const getRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/slots/${encodeURIComponent(name)}`,
    );
    expect(getRes.ok(), `GET slot ${getRes.status()}`).toBeTruthy();
    const detail = unwrapSlotDetail(await getRes.json());
    const assocs = asAssocList(detail.associations);
    expect(assocs.length, "GET associations after save").toBeGreaterThan(0);
    const names = assocs.map((a) => ({
      ct: a.contentTypeName || a.contentTypeLabel,
      tpl: a.templateName || a.templateLabel,
    }));
    expect(
      names.some((n) => n.ct === ct && n.tpl === tpl),
      `expected ${ct}/${tpl} in ${JSON.stringify(names)}`,
    ).toBeTruthy();
    const gaps = detail.designGaps;
    const gapList = Array.isArray(gaps)
      ? gaps
      : gaps && gaps.DesignGap
        ? [].concat(gaps.DesignGap)
        : [];
    expect(
      gapList.every((g) => g.code !== "SLOT_ASSOC_GUIDS_ONLY"),
      `designGaps still has SLOT_ASSOC_GUIDS_ONLY: ${JSON.stringify(gapList)}`,
    ).toBeTruthy();

    await page.locator('[data-testid="developer-slot-assoc-ct"]').fill("qaNoSuchType4462");
    await page.locator('[data-testid="developer-slot-assoc-tpl"]').fill(tpl);
    await page.locator('[data-testid="developer-slot-assoc-add"]').click();
    await page.locator('[data-testid="developer-slot-save"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });

    if (await page.locator('[data-testid="developer-slot-unlock"]').isEnabled()) {
      await page.locator('[data-testid="developer-slot-unlock"]').click();
    }
    await deleteCurrentSlot(page, name);
    assertConsoleClean(pageErrors, consoleErrors);
  });
});
