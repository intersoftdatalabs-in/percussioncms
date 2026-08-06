/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Spanish locale smoke residual (#2094 / parent #961).
 *
 * <p>After TMX residual (#2092) + Finder display wiring (#2105), log in with a
 * product Spanish locale (prefer {@code es-es}) and assert:</p>
 * <ol>
 *   <li>Finder root <strong>display</strong> labels resolve to Spanish
 *       (Sitios / Activos / Diseño / Buscar / Reciclaje) while repository
 *       path identity stays English ({@code /Sites/}, …).</li>
 *   <li>Default Dashboard gadgets Welcome / Process Monitor / Pages By Status
 *       show non-English (Spanish) chrome. License Monitor only when present.</li>
 * </ol>
 *
 * <p>Golden / QA mode (no {@code DEV_PERCUSSION_INSTALL}):</p>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; TEST_DB_TYPE=h2 \
 *     npm run test:surface -- --path tests/bugs/bug-2094-spanish-locale-finder-dashboard.spec.js
 * </pre>
 *
 * <p>Failure artifacts: {@code test-results/}, {@code playwright-report/}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  pickSpanishLoginLocale,
  BASE_URL,
} = require("../helpers/auth");

/** CmsUi.tmx Spanish residual (#2092) for finder.root + default gadgets. */
const ES_FINDER_ROOTS = {
  Sites: "Sitios",
  Assets: "Activos",
  Design: "Diseño",
  Search: "Buscar",
  Recycling: "Reciclaje",
};

const ES_GADGET_TITLES = {
  welcome: "BIENVENIDO",
  processMonitor: "MONITOREO DE PROCESO",
  pagesByStatus: "PÁGINAS POR ESTADO",
  licenseMonitor: "Monitor de licencia",
};

const FINDER_ROOT_KEYS = {
  Sites: "perc.ui.finder.root@Sites",
  Assets: "perc.ui.finder.root@Assets",
  Design: "perc.ui.finder.root@Design",
  Search: "perc.ui.finder.root@Search",
  Recycling: "perc.ui.finder.root@Recycling",
};

function explorerUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

function homeGadgetsUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&section=gadgets&_=${Date.now()}`;
}

/**
 * Read live I18N catalog messages after spa/tmx load.
 * @param {import("@playwright/test").Page} page
 * @param {string[]} keys
 * @returns {Promise<Record<string, string|null>>}
 */
async function readI18nMessages(page, keys) {
  return page.evaluate((messageKeys) => {
    const out = {};
    const i18n =
      typeof window !== "undefined" && window.I18N && window.I18N.message
        ? window.I18N
        : null;
    for (const k of messageKeys) {
      if (!i18n) {
        out[k] = null;
        continue;
      }
      try {
        out[k] = i18n.message(k);
      } catch {
        out[k] = null;
      }
    }
    return out;
  }, keys);
}

/**
 * Classic Finder display helper when the page loads perc_finder_root_display.js.
 * @param {import("@playwright/test").Page} page
 * @param {string[]} englishRoots
 * @returns {Promise<Record<string, string|null>>}
 */
async function readFinderDisplayLabels(page, englishRoots) {
  return page.evaluate((roots) => {
    const out = {};
    const api =
      typeof globalThis !== "undefined" && globalThis.percFinderRootDisplay
        ? globalThis.percFinderRootDisplay
        : typeof window !== "undefined" && window.percFinderRootDisplay
          ? window.percFinderRootDisplay
          : null;
    for (const name of roots) {
      if (
        api &&
        typeof api.displayLabelForFinderRoot === "function"
      ) {
        try {
          out[name] = api.displayLabelForFinderRoot(name);
        } catch {
          out[name] = null;
        }
      } else {
        out[name] = null;
      }
    }
    return out;
  }, englishRoots);
}

test.describe("Spanish locale smoke — Finder roots + default gadgets (#2094) @smoke @i18n", () => {
  test("Finder root display labels + path identity stay English paths @smoke @i18n", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const locale = await pickSpanishLoginLocale(page);
    expect(
      locale,
      "install must expose a Spanish login locale (es-es / es / es-mx)",
    ).toBeTruthy();

    await loginAsAdmin(page, { locale });

    await page.goto(explorerUrl(), { waitUntil: "networkidle" });

    // Session TMX must load Spanish residual keys (not English-only chrome).
    const keyList = Object.values(FINDER_ROOT_KEYS);
    await expect
      .poll(
        async () => {
          const msgs = await readI18nMessages(page, keyList);
          return msgs[FINDER_ROOT_KEYS.Sites];
        },
        { timeout: 30_000 },
      )
      .toBe(ES_FINDER_ROOTS.Sites);

    const msgs = await readI18nMessages(page, keyList);
    for (const [english, key] of Object.entries(FINDER_ROOT_KEYS)) {
      expect(
        msgs[key],
        `I18N.message(${key}) after Spanish login`,
      ).toBe(ES_FINDER_ROOTS[english]);
      expect(msgs[key]).not.toBe(english);
    }

    // Path identity: modern explorer tree-node testids use English paths.
    const shell = page.locator('[data-testid="content-explorer-shell"]');
    if ((await shell.count()) > 0) {
      await expect(shell).toBeVisible({ timeout: 30_000 });
      const tree = page.locator('[data-testid="explorer-tree"]');
      await expect(tree).toBeVisible({ timeout: 15_000 });

      const sitesNode = tree.locator(
        '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid^="tree-node-/Sites"]',
      );
      // Fresh H2 may still expose root children; path segment must stay English.
      if ((await sitesNode.count()) > 0) {
        await expect(sitesNode.first()).toBeVisible({ timeout: 15_000 });
        const testId = await sitesNode.first().getAttribute("data-testid");
        expect(testId, "path identity stays English Sites").toMatch(/Sites/i);
        expect(testId).not.toMatch(/Sitios/i);
      }
    }

    // Classic Finder display wiring (#2105): when percFinderRootDisplay is on
    // the page, display labels must use TMX (not English-only chrome).
    const displayLabels = await readFinderDisplayLabels(
      page,
      Object.keys(ES_FINDER_ROOTS),
    );
    const helperPresent = Object.values(displayLabels).some((v) => v != null);
    if (helperPresent) {
      for (const [english, spanish] of Object.entries(ES_FINDER_ROOTS)) {
        expect(
          displayLabels[english],
          `percFinderRootDisplay.displayLabelForFinderRoot(${english})`,
        ).toBe(spanish);
      }
    }

    // Classic miller-column DOM when still mounted on a residual surface.
    const classicNames = page.locator(".perc-finder-item-name");
    if ((await classicNames.count()) > 0) {
      const texts = (await classicNames.allTextContents()).map((t) =>
        t.trim(),
      );
      for (const spanish of Object.values(ES_FINDER_ROOTS)) {
        expect(
          texts,
          `classic Finder chrome should include ${spanish}`,
        ).toContain(spanish);
      }
      // English path segment names must not be the only chrome for roots.
      for (const english of Object.keys(ES_FINDER_ROOTS)) {
        expect(texts).not.toContain(english);
      }
    }
  });

  test("default Dashboard gadgets show Spanish chrome @smoke @i18n", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const locale = await pickSpanishLoginLocale(page);
    expect(locale).toBeTruthy();
    await loginAsAdmin(page, { locale });

    await page.goto(homeGadgetsUrl(), { waitUntil: "networkidle" });

    // Prefer modern Home gadgets section; fall back to /cm/app/home.
    const gadgetsSection = page.getByTestId("home-gadgets-section");
    if ((await gadgetsSection.count()) === 0) {
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/home`, {
        waitUntil: "networkidle",
      });
    }
    await expect(page.getByTestId("home-gadgets-section")).toBeVisible({
      timeout: 45_000,
    });

    // Welcome (no dedicated testid — match title chrome).
    await expect(
      page.getByText(ES_GADGET_TITLES.welcome, { exact: true }).first(),
    ).toBeVisible({ timeout: 30_000 });
    await expect(page.getByText("WELCOME", { exact: true })).toHaveCount(0);

    // Process Monitor + Pages By Status (default layout).
    const processWidget = page.getByTestId("process-monitor-widget");
    await expect(processWidget).toBeVisible({ timeout: 30_000 });
    await expect(processWidget).toContainText(ES_GADGET_TITLES.processMonitor);
    await expect(processWidget).not.toContainText(/^PROCESS MONITOR$/m);
    await expect(processWidget).not.toContainText(/^Process Monitor$/m);

    const workflowWidget = page.getByTestId("workflow-status-widget");
    await expect(workflowWidget).toBeVisible({ timeout: 30_000 });
    await expect(workflowWidget).toContainText(ES_GADGET_TITLES.pagesByStatus);
    await expect(workflowWidget).not.toContainText(/^PAGES BY STATUS$/m);

    // License Monitor only if still shipped on this install / layout.
    const licenseChrome = page.getByText(ES_GADGET_TITLES.licenseMonitor, {
      exact: false,
    });
    const englishLicense = page.getByText("License Monitor", { exact: true });
    if ((await licenseChrome.count()) > 0) {
      await expect(licenseChrome.first()).toBeVisible();
      await expect(englishLicense).toHaveCount(0);
    } else if ((await englishLicense.count()) > 0) {
      throw new Error(
        "License Monitor is present but still English — expected Spanish title when shipped",
      );
    }
    // else: not in default modern layout — skip (issue allows optional).
  });
});
