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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Classic-UI jQuery-UI smoothness theme served from npm (issue #7381,
 * audit #5627).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-jquery-ui-theme-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: the stock 1.12.1 ThemeRoller
 * smoothness bundle checked in under the drifted filename
 * {@code cm/themes/smoothness/jquery-ui-1.8.9.custom.css} (not a custom
 * cut — same widget set as the npm dist, MIT) was deleted from
 * {@code cm/themes}, {@code cm/vendor/css/legacy}, and the {@code war/}
 * snapshot. The four legacy JSPs (+ pages twins, PercPageModel.js) now
 * load the Dependabot-tracked jquery-ui 1.14 dist theme at the corrected
 * URL {@code cm/themes/smoothness/jquery-ui.css}, served from
 * {@code node_modules/jquery-ui/dist/themes/smoothness/} via the
 * generated-webui overlay (STANDALONE_NPM_COPIES), with the sibling
 * {@code images/} copies providing image-path parity.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { proofScreenshot } = require("./helpers/proof-shot");

const THEME_CSS_URL = "/Rhythmyx/cm/themes/smoothness/jquery-ui.css";
const THEME_IMAGE_URL =
  "/Rhythmyx/cm/themes/smoothness/images/ui-icons_222222_256x240.png";
const RETIRED_THEME_URL =
  "/Rhythmyx/cm/themes/smoothness/jquery-ui-1.8.9.custom.css";

test.describe("Classic-UI jQuery-UI theme from npm (issue #7381)", () => {
  test(
    "dashboard loads npm smoothness theme with zero theme 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const themeErrors = [];
      const failedRequests = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        if (/themes\/smoothness/i.test(stack)) {
          themeErrors.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          /themes\/smoothness/i.test(msg.text())
        ) {
          themeErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (req.url().includes("/cm/themes/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/themes/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      // The page wires the corrected URL (not the retired lie filename).
      const themeLink = await page.evaluate(() => {
        const links = [...document.querySelectorAll('link[rel="stylesheet"]')].map(
          (el) => el.getAttribute("href"),
        );
        return {
          all: links,
          current: links.find((h) => h && h.includes("themes/smoothness/")) || null,
        };
      });
      expect(
        themeLink.current,
        `dashboard has no smoothness theme link: ${JSON.stringify(themeLink.all)}`,
      ).not.toBeNull();
      expect(themeLink.current).toContain("themes/smoothness/jquery-ui.css");
      expect(themeLink.current).not.toContain("1.8.9");

      // The served stylesheet is the npm 1.14 dist — never the 1.12.1 cut.
      const css = await page.request.get(`${BASE_URL}${THEME_CSS_URL}`);
      expect(css.status(), "npm theme CSS status").toBe(200);
      const cssText = await css.text();
      expect(cssText.slice(0, 2048), "npm theme banner").toMatch(
        /jQuery UI - v1\.14\./,
      );
      expect(cssText, "stale 1.12.1 vendored theme served").not.toMatch(
        /jQuery UI - v1\.12\.1/,
      );

      // A theme image the CSS references relatively must serve too —
      // otherwise every widget icon/background breaks.
      const img = await page.request.get(`${BASE_URL}${THEME_IMAGE_URL}`);
      expect(img.status(), "npm theme image status").toBe(200);

      // The retired lie filename must no longer resolve.
      const retired = await page.request.get(`${BASE_URL}${RETIRED_THEME_URL}`);
      expect(retired.status(), "retired lie-filename theme served").toBe(404);

      expect(
        failedRequests,
        `failed theme requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        themeErrors,
        `theme errors:\n${themeErrors.join("\n")}`,
      ).toEqual([]);

      // C5 live proof (issue #7381): dashboard chrome on the npm theme.
      await proofScreenshot(page, "issue-7381-jquery-ui-theme-dashboard.png");
    },
  );

  test(
    "retired lie-filename theme stays gone, users page on new URL @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      const retired = await page.request.get(`${BASE_URL}${RETIRED_THEME_URL}`);
      expect(
        retired.status(),
        `retired vendored file resurrected: ${RETIRED_THEME_URL}`,
      ).toBe(404);

      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/users.jsp`, {
        waitUntil: "networkidle",
      });
      const themeHref = await page.evaluate(() => {
        const el = document.querySelector(
          'link[rel="stylesheet"][href*="themes/smoothness/"]',
        );
        return el ? el.getAttribute("href") : null;
      });
      expect(themeHref, "users.jsp has no smoothness theme link").not.toBeNull();
      expect(themeHref).toContain("themes/smoothness/jquery-ui.css");
    },
  );
});
