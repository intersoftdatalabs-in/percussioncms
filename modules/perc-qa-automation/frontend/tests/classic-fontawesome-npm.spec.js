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
 * Classic-UI FontAwesome served from npm (issue #6321, audit #5627).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-fontawesome-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: the mismatched tree (all.css 5.6.1 vs
 * fontawesome-all.js + v4-shims.js 5.3.1) under cm/jslib,
 * cm/vendor/js/legacy, and war/jslib was deleted. Classic pages
 * (dashboard/editTemplate/importTemplate/pageNotFound/users/webmgt.jsp, plus
 * PercMobilePreview.js) load only all.css at the same stable versionless
 * jslib URL, now served out of the single npm 5.15.4
 * (@fortawesome/fontawesome-free, exact-pinned, Dependabot-tracked in
 * {@code WebUI/src/main/frontend/package.json}) via the generated-webui
 * overlay. The vendored JS pair had zero in-repo consumers and is retired
 * with no mapping. All in-repo icon usage is FA5-native
 * ({@code fas fa-*}), so no v4-shim coverage is needed.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { proofScreenshot } = require("./helpers/proof-shot");

const FA_CSS_URL =
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/fontawesome/css/all.css";
const FA_WOFF2_URL =
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/fontawesome/webfonts/fa-solid-900.woff2";

test.describe("Classic-UI FontAwesome from npm (issue #6321)", () => {
  test(
    "dashboard loads npm FontAwesome with zero jslib 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const faErrors = [];
      const failedRequests = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        // Fail only on errors arising from the migrated FontAwesome files.
        // The dashboard emits pre-existing cell errors (never-shipped
        // cm/gadgets/container/* probes, a PercDashboard I18N.meesage typo,
        // dashboard.jsp set_label) that sibling classic specs fail on
        // identically in this cell — unrelated to #6321.
        if (/fontawesome|font-awesome|fa-solid|fa-brands|fa-regular/i.test(stack)) {
          faErrors.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          /fontawesome|font-awesome|fa-solid|fa-brands|fa-regular/i.test(msg.text())
        ) {
          faErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (req.url().includes("/cm/jslib/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/jslib/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      // The served stylesheet is the single npm 5.15.4 — neither deleted
      // banner (5.6.1 CSS / 5.3.1 JS) may come back.
      const css = await page.request.get(`${BASE_URL}${FA_CSS_URL}`);
      expect(css.status(), "npm FontAwesome CSS status").toBe(200);
      const cssText = await css.text();
      expect(cssText.slice(0, 4096), "npm CSS banner").toMatch(
        /Font Awesome Free 5\.15\.4/,
      );
      expect(cssText, "stale 5.6.1 vendored CSS served").not.toMatch(
        /Font Awesome Free 5\.6\.1/,
      );
      expect(cssText, "stale 5.3.1 vendored JS banner served").not.toMatch(
        /Font Awesome Free 5\.3\.1/,
      );

      // A webfont the CSS references relatively must serve too — otherwise
      // every icon renders as a tofu box.
      const woff2 = await page.request.get(`${BASE_URL}${FA_WOFF2_URL}`);
      expect(woff2.status(), "npm webfont status").toBe(200);

      // The icon font itself must be usable by the page (not just served):
      // dashboard gadget icons are `fas` (solid, weight 900).
      const fontReady = await page.evaluate(async () => {
        try {
          await document.fonts.load('900 16px "Font Awesome 5 Free"');
        } catch (e) {
          return `load threw: ${e}`;
        }
        return document.fonts.check('900 16px "Font Awesome 5 Free"')
          ? "ready"
          : "not-available";
      });
      expect(fontReady, "Font Awesome 5 Free solid face unusable").toBe(
        "ready",
      );

      const iconBox = await page.evaluate(() => {
        const el = document.querySelector("i.fas, span.fas");
        if (!el) {
          return null;
        }
        const rect = el.getBoundingClientRect();
        const family = getComputedStyle(el).fontFamily || "";
        return { width: rect.width, height: rect.height, family };
      });
      expect(iconBox, "no rendered fas icon found on dashboard").not.toBeNull();
      expect(iconBox.width, "fas icon has zero width").toBeGreaterThan(0);
      expect(iconBox.height, "fas icon has zero height").toBeGreaterThan(0);
      expect(iconBox.family, "fas icon not set in Font Awesome").toMatch(
        /Font Awesome/,
      );

      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(faErrors, `fontawesome errors:\n${faErrors.join("\n")}`).toEqual([]);

      // C5 live proof (issue #6321): dashboard with FA5-native icons
      // rendered from the single npm 5.15.4 webfonts.
      await proofScreenshot(page, "issue-6321-fontawesome-dashboard.png");
    },
  );

  test(
    "retired vendored FontAwesome JS pair stays gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of [
        "/Rhythmyx/cm/jslib/profiles/3x/libraries/fontawesome/js/fontawesome-all.js",
        "/Rhythmyx/cm/jslib/profiles/3x/libraries/fontawesome/js/v4-shims.js",
      ]) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `retired vendored file resurrected: ${asset}`,
        ).toBe(404);
      }
    },
  );
});
