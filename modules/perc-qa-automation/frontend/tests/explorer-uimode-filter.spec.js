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
 * Playwright surface: SPA shell UI-mode claim (#6619 / parent #4530).
 *
 * <p>{@code GET /Rhythmyx/services/actions/find} accepts the shell claim
 * as {@code ?uiMode=} (mode id or mode name). The shell claims the seeded
 * Content Explorer main-area mode ({@code CXMAIN} / {@code 4} per
 * installer {@code cmsTableData.xml} {@code RXMODE}); scoped menus survive
 * when the token matches, and menus scoped exclusively to other modes are
 * hidden (cascade). The first test locks seeded behavior live: the
 * CXNAV-only installer rows (e.g. {@code Create_New_Folder}) hide under
 * the shell claim while the unscoped catalog keeps them — the
 * {@code /find} tree carries design uiContexts via bulk overlay (#6824),
 * so the filter is enforced on the wire, not only in unit fixtures. A
 * second test seeds its own CXMAIN-scoped and IAMAIN-scoped menus (#6824;
 * REST create/PUT round-trips uiContexts through the H2 JDBC fallback) so
 * the claim itself is regression-locked live, then deletes both. The
 * enforcement logic is also locked by Vitest
 * ({@code menuVisibility.test.ts}) and Surefire
 * ({@code ActionMenuUiModeFilterTest},
 * {@code ActionMenuUiContextsMappingTest},
 * {@code ActionMenuAdaptorTreeUiContextsTest}).</p>
 *
 * <p>Tags: {@code @explorer-uimode} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-uimode-filter.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const FIND = `${BASE_URL}/Rhythmyx/services/actions/find`;

function asArray(payload) {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload === "object") {
    const raw =
      payload.ActionMenu ??
      payload.ActionMenuList ??
      payload.actionMenu ??
      payload.actionMenuList;
    if (raw == null) return [];
    return Array.isArray(raw) ? raw : [raw];
  }
  return [];
}

/**
 * Same-origin JSON fetch so OWASP CSRF + session cookies apply (#4119).
 * Used to seed and clean up scoped regression menus via the Admin
 * collection POST / DELETE.
 */
function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ url, httpMethod, payload }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { url: path, httpMethod: method, payload: body },
  );
}

/** REST-safe unique action-menu name (no spaces, wildcards, or path characters). */
function uniqueProbeMenuName(prefix) {
  const stamp = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-6);
  return `${prefix}${stamp || "x"}`;
}

test.describe("Explorer action catalog UI-mode filter (#6619)", () => {
  /** Uncaught page errors + unexpected console errors for C5. */
  let pageErrors = [];

  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") return;
      const text = msg.text();
      if (
        /Failed to load resource: the server responded with a status of (404|400)/i.test(
          text,
        )
      ) {
        return;
      }
      pageErrors.push(text);
    });
    await loginAsAdmin(page);
  });

  test.afterEach(() => {
    expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
      [],
    );
  });

  test(
    "uiMode claim filters seeded foreign-mode menus",
    { tag: ["@explorer-uimode", "@explorer", "@smoke"] },
    async ({ page }) => {
      const unscoped = await page.request.get(FIND);
      expect(unscoped.status()).toBe(200);
      const plainMenus = asArray(await unscoped.json());

      const scoped = await page.request.get(
        `${FIND}?uiMode=${encodeURIComponent("CXMAIN")}`,
      );
      expect(scoped.status()).toBe(200);
      const claimedMenus = asArray(await scoped.json());
      const claimedNames = claimedMenus.map((m) => m.name);

      // Seeded CXNAV-only installer rows (e.g. Create_New_Folder) hide
      // under the shell claim while the unscoped catalog keeps them (#6824:
      // the /find tree carries design uiContexts via bulk overlay).
      expect(plainMenus.map((m) => m.name)).toContain("Create_New_Folder");
      expect(claimedNames).not.toContain("Create_New_Folder");

      // Seeded-row id spelling of the same claim agrees too.
      const byId = await page.request.get(`${FIND}?uiMode=4`);
      expect(byId.status()).toBe(200);
      expect(asArray(await byId.json()).map((m) => m.name)).not.toContain(
        "Create_New_Folder",
      );
    },
  );

  test(
    "seeded CXMAIN/IAMAIN menus enforce the shell claim (#6824)",
    { tag: ["@explorer-uimode", "@explorer", "@smoke"] },
    async ({ page }) => {
      // The no-op test above cannot catch filter regressions: the H2 seed
      // serves no uiContext-scoped menus. Seed one CXMAIN-scoped and one
      // IAMAIN-scoped menu (installer RXMODE ids 4/6, Folder context 9),
      // assert the shell claim keeps the first and hides the second, then
      // delete both so the catalog returns to seed state.
      const cxmainName = uniqueProbeMenuName("qauimodecxmain");
      const iamainName = uniqueProbeMenuName("qauimodeiamain");
      const created = [];
      try {
        for (const [name, modeId, modeName] of [
          [cxmainName, "4", "CXMAIN"],
          [iamainName, "6", "IAMAIN"],
        ]) {
          const res = await inPageJson(page, "/Rhythmyx/services/actions", "POST", {
            ActionMenu: {
              name,
              label: `${name} label`,
              menuType: "MENUITEM",
              uiContexts: [
                {
                  modeId,
                  modeName,
                  contextId: "9",
                  contextName: "Folder",
                },
              ],
            },
          });
          expect(
            res.status,
            `POST seed ${name} (got ${res.status}): ${res.text}`,
          ).toBe(200);
          created.push(name);
        }

        const namesOf = async (url) => {
          const res = await page.request.get(url);
          expect(res.status(), `GET ${url}`).toBe(200);
          return asArray(await res.json()).map((m) => m.name);
        };

        // Unscoped catalog carries both seeds.
        const plain = await namesOf(FIND);
        expect(plain).toContain(cxmainName);
        expect(plain).toContain(iamainName);

        // Shell claim keeps the CXMAIN seed, hides the IAMAIN seed.
        const claimed = await namesOf(
          `${FIND}?uiMode=${encodeURIComponent("CXMAIN")}`,
        );
        expect(claimed).toContain(cxmainName);
        expect(claimed).not.toContain(iamainName);

        // Seeded-row id spelling of the same claim agrees.
        const byId = await namesOf(`${FIND}?uiMode=4`);
        expect(byId).toContain(cxmainName);
        expect(byId).not.toContain(iamainName);
      } finally {
        for (const name of created) {
          await inPageJson(
            page,
            `/Rhythmyx/services/actions/${encodeURIComponent(name)}`,
            "DELETE",
          );
        }
      }
    },
  );

  test(
    "explorer toolbar mounts with the scoped catalog load",
    { tag: ["@explorer-uimode", "@explorer", "@smoke"] },
    async ({ page }) => {
      const {
        explorerSpaUrl,
        TEST_IDS,
      } = require("./helpers/explorer-menu-bar");
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 15_000 });
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 15_000,
      });
      // T082b a11y gate: scoped catalog + role="status" empty hint (#6619).
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      // C5 live proof (passing-surface screenshot).
      await page.screenshot({
        path: "test-results/live-proof-6619-uimode.png",
      });
    },
  );
});
