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
 * Empty Recycling residual coverage (#2207 / parent #944 slice 3).
 *
 * <p><strong>Depends on:</strong></p>
 * <ul>
 *   <li>#2205 — {@code DELETE /pathmanagement/recycle/empty} (on main)</li>
 *   <li>#2206 — classic Finder Actions menu entry + confirm dialog
 *       ({@code data-testid="perc-finder-empty-recycling"})</li>
 * </ul>
 *
 * <p>Client unit coverage for {@code PercRecycleService.emptyRecycling} is
 * shipped with #2206 as {@code WebUI/src/test/js/percEmptyRecycling.test.js}.
 * This residual adds live-CMS Playwright (REST seed/empty + classic Finder
 * happy/cancel UI).</p>
 *
 * <h3>How to run</h3>
 * <pre>
 *   # QA mode (H2 docker — preferred unattended)
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/bugs/bug-2207-empty-recycling.spec.js
 *
 *   # Or focused:
 *   npm test -- tests/bugs/bug-2207-empty-recycling.spec.js
 *
 *   # Pure helpers (no live CMS):
 *   npm run test:unit
 * </pre>
 *
 * <p>Surface filter tag: {@code @empty-recycling}. Prefer i18n/stable
 * selectors ({@code data-testid}, confirm button ids) — no machine paths.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const {
  SELECTORS,
  listFolderChildren,
  seedRecycledFolder,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  isRecyclingListEmpty,
  recyclingHasName,
  RECYCLE_EMPTY_PATH,
} = require("../helpers/empty-recycling");

/**
 * Classic Finder shell that still includes finder_js + Actions menu
 * (webmgt residual; scripts load even when modern explorer is primary).
 */
function classicFinderUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp?_=${Date.now()}`;
}

/**
 * Open Actions menu and return Empty Recycling control when present.
 * @param {import("@playwright/test").Page} page
 */
async function openEmptyRecyclingAction(page) {
  const actionsBtn = page.locator(SELECTORS.actionsButton);
  await expect(
    actionsBtn,
    "classic Finder Actions button (#perc-finder-actions-button) should exist when #2206 finder scripts are loaded",
  ).toBeVisible({ timeout: 30_000 });

  await actionsBtn.click();
  const emptyBtn = page.locator(SELECTORS.emptyAction);
  // Menu may render disabled until path is Recycling; still must be present (#2206).
  await expect(
    emptyBtn,
    "Empty Recycling menu entry missing — deploy #2206 / PR #2222 WebUI (data-testid=perc-finder-empty-recycling)",
  ).toBeVisible({ timeout: 15_000 });
  return emptyBtn;
}

/**
 * Navigate classic Finder path summary to Recycling root.
 * @param {import("@playwright/test").Page} page
 */
async function navigateFinderToRecycling(page) {
  const expander = page.locator(SELECTORS.finderExpander);
  if ((await expander.count()) > 0) {
    // Expand collapsed finder body when present.
    const outer = page.locator(SELECTORS.finderOuter);
    if ((await outer.count()) > 0) {
      const collapsed = await outer.first().getAttribute("collapsed");
      if (collapsed === "true") {
        await expander
          .first()
          .click()
          .catch(() => {});
      }
    }
  }

  const pathInput = page.locator(SELECTORS.pathSummary);
  await expect(pathInput).toBeVisible({ timeout: 20_000 });
  await pathInput.fill("/Recycling");
  // Prefer hidden go action used by finder path bar.
  const go = page.locator(SELECTORS.pathGo);
  if ((await go.count()) > 0) {
    await go.click({ force: true }).catch(async () => {
      await pathInput.press("Enter");
    });
  } else {
    await pathInput.press("Enter");
  }
  // Allow path-changed listeners to re-enable Empty action.
  await page.waitForTimeout(750);
}

test.describe("Empty Recycling (#2207 / parent #944 slice 3) @empty-recycling", () => {
  test("REST: empty Recycling is idempotent for Admin @empty-recycling", async ({
    request,
  }) => {
    test.setTimeout(60_000);
    const headers = adminBasicAuthHeaders();

    // First empty may purge residual content; second should still be 2xx
    // (alreadyEmpty=true when no top-level children remain).
    const first = await emptyRecyclingViaApi(request, BASE_URL, headers);
    expect(
      first.status >= 200 && first.status < 300,
      emptyApiFailureMessage(first),
    ).toBe(true);

    const second = await emptyRecyclingViaApi(request, BASE_URL, headers);
    expect(
      second.status >= 200 && second.status < 300,
      emptyApiFailureMessage(second),
    ).toBe(true);
    // When the bin has no children, body may report alreadyEmpty=true.
    // Structural installs that re-materialize Sites/Assets under Recycling may
    // return another 2xx purge instead — both are valid (status asserted above).

    // Endpoint must remain registered (regression for #2205 wiring).
    expect(RECYCLE_EMPTY_PATH).toMatch(/recycle\/empty$/);
  });

  test("REST: seed recycle item then empty purges it @empty-recycling", async ({
    request,
  }) => {
    test.setTimeout(90_000);
    const headers = adminBasicAuthHeaders();

    const pre = await emptyRecyclingViaApi(request, BASE_URL, headers);
    expect(
      pre.status >= 200 && pre.status < 300,
      emptyApiFailureMessage(pre),
    ).toBe(true);

    const seeded = await seedRecycledFolder(request, BASE_URL, headers);
    const before = await listFolderChildren(
      request,
      BASE_URL,
      headers,
      "Recycling",
    );
    // Recycled folders often land under /Recycling/Assets/… or /Recycling/Sites/…
    // so search top-level and, if needed, known structural children.
    // Do not treat "any children exist" as seed present (structural roots).
    let found = recyclingHasName(before, seeded.name);
    if (!found) {
      for (const root of ["Assets", "Sites"]) {
        const nested = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          `Recycling/${root}`,
        ).catch(() => []);
        if (recyclingHasName(nested, seeded.name)) {
          found = true;
          break;
        }
      }
    }
    expect(
      found,
      `expected recycled seed ${seeded.name} under Recycling; top=${JSON.stringify(before)}`,
    ).toBe(true);

    const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
    expect(
      emptied.status >= 200 && emptied.status < 300,
      emptyApiFailureMessage(emptied),
    ).toBe(true);

    const afterTop = await listFolderChildren(
      request,
      BASE_URL,
      headers,
      "Recycling",
    );
    let stillThere = recyclingHasName(afterTop, seeded.name);
    if (!stillThere) {
      for (const root of ["Assets", "Sites"]) {
        const nested = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          `Recycling/${root}`,
        ).catch(() => []);
        if (recyclingHasName(nested, seeded.name)) {
          stillThere = true;
          break;
        }
      }
    }
    expect(
      stillThere,
      `seed ${seeded.name} should be gone after empty; after top=${JSON.stringify(afterTop)}`,
    ).toBe(false);
  });

  test("UI cancel: confirm dismiss leaves recycled content @empty-recycling", async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = adminBasicAuthHeaders();
    await emptyRecyclingViaApi(request, BASE_URL, headers);
    const seeded = await seedRecycledFolder(request, BASE_URL, headers);

    await loginAsAdmin(page);
    await page.goto(classicFinderUrl(), { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle").catch(() => {});

    await navigateFinderToRecycling(page);
    const emptyBtn = await openEmptyRecyclingAction(page);

    // Enablement: must not stay permanently disabled under Recycling for Admin.
    await expect
      .poll(
        async () => {
          const cls = (await emptyBtn.getAttribute("class")) || "";
          return cls.includes("ui-enabled") || !cls.includes("ui-disabled");
        },
        { timeout: 20_000 },
      )
      .toBe(true);

    await emptyBtn.click();
    const dialog = page.locator(SELECTORS.confirmDialog);
    await expect(dialog).toBeVisible({ timeout: 15_000 });
    // Prefer stable cancel id over i18n button text.
    await page.locator(SELECTORS.confirmCancel).click();
    await expect(dialog).toHaveCount(0, { timeout: 10_000 });

    const children = await listFolderChildren(
      request,
      BASE_URL,
      headers,
      "Recycling",
    );
    // Seed may land under structural Assets/Sites — require the seeded name,
    // not merely "Recycling has any children".
    let stillSeeded = recyclingHasName(children, seeded.name);
    if (!stillSeeded) {
      for (const root of ["Assets", "Sites"]) {
        const nested = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          `Recycling/${root}`,
        ).catch(() => []);
        if (recyclingHasName(nested, seeded.name)) {
          stillSeeded = true;
          break;
        }
      }
    }
    expect(
      stillSeeded,
      `cancel must leave recycled content; expected ${seeded.name} in ${JSON.stringify(children)}`,
    ).toBe(true);

    // Cleanup so the suite does not leave fixtures.
    await emptyRecyclingViaApi(request, BASE_URL, headers);
  });

  test("UI happy path: Empty Recycling confirm purges bin @empty-recycling", async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = adminBasicAuthHeaders();
    await emptyRecyclingViaApi(request, BASE_URL, headers);
    const seeded = await seedRecycledFolder(request, BASE_URL, headers);

    /** @type {string[]} */
    const emptyCalls = [];
    page.on("request", (req) => {
      if (
        req.method() === "DELETE" &&
        req.url().includes("/pathmanagement/recycle/empty")
      ) {
        emptyCalls.push(req.url());
      }
    });

    await loginAsAdmin(page);
    await page.goto(classicFinderUrl(), { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle").catch(() => {});

    await navigateFinderToRecycling(page);
    const emptyBtn = await openEmptyRecyclingAction(page);

    await expect
      .poll(
        async () => {
          const cls = (await emptyBtn.getAttribute("class")) || "";
          return cls.includes("ui-enabled") || !cls.includes("ui-disabled");
        },
        { timeout: 20_000 },
      )
      .toBe(true);

    await emptyBtn.click();
    const dialog = page.locator(SELECTORS.confirmDialog);
    await expect(dialog).toBeVisible({ timeout: 15_000 });
    // Confirm warning span uses i18n key text or resolved message.
    const warn = page.locator(SELECTORS.confirmWarn);
    if ((await warn.count()) > 0) {
      await expect(warn).toBeVisible();
    }

    await page.locator(SELECTORS.confirmOk).click();

    await expect
      .poll(() => emptyCalls.length > 0, { timeout: 30_000 })
      .toBe(true);

    await expect
      .poll(
        async () => {
          const children = await listFolderChildren(
            request,
            BASE_URL,
            headers,
            "Recycling",
          );
          return (
            isRecyclingListEmpty(children) ||
            !recyclingHasName(children, seeded.name)
          );
        },
        { timeout: 45_000 },
      )
      .toBe(true);
  });
});
