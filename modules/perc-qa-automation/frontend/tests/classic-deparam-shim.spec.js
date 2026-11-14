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
 * jQuery BBQ 1.1 retirement (issue #5663).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-deparam-shim.spec.js}</p>
 *
 * <p>Covers the dead-upstream BBQ removal: the vendored
 * {@code jquery.bbq.js} copy is deleted and the stable jslib URL now serves
 * the first-party {@code jquery.deparam.js} shim. The spec proves the shim
 * bytes load (200), the BBQ bytes are gone (404), the classic dashboard
 * wires the deparam surface with no plugin-attributed errors, and the live
 * page round-trips the exact query-string shapes the classic navigation
 * code uses ({@code $.deparam.querystring()} reads and
 * {@code $.param.querystring(base, params)} URL building).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const SHIM_JS =
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.deparam.js";
const BBQ_JS =
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.bbq.js";

test.describe("jQuery BBQ retirement, first-party deparam shim (issue #5663)", () => {
  test(
    "classic dashboard serves the deparam shim with no bbq remnant @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pluginErrors = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        if (/deparam|jquery\.bbq|\$\.bbq/i.test(stack)) {
          pluginErrors.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          /deparam|jquery\.bbq/i.test(msg.text())
        ) {
          pluginErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (/deparam|jquery\.bbq/i.test(req.url())) {
          pluginErrors.push(
            `${req.url()} :: ${req.failure()?.errorText}`,
          );
        }
      });

      await loginAsAdmin(page);

      const shimResp = await page.request.get(`${BASE_URL}${SHIM_JS}`);
      expect(shimResp.status(), `expected 200 for ${SHIM_JS}`).toBe(200);
      const shimBytes = await shimResp.text();
      expect(shimBytes).toContain("$.deparam = deparam");
      expect(shimBytes).toContain("$.param.querystring = paramQuerystring");
      // The served file must be the first-party shim, not the retired copy:
      // BBQ's distinctive banner and hash-state/fragment internals are absent.
      expect(shimBytes).toContain("Intersoft Data Labs");
      expect(shimBytes).not.toContain("Back Button & Query Library");
      expect(shimBytes).not.toContain("jq_bbq");
      expect(shimBytes).not.toContain("get_fragment");
      expect(shimBytes).not.toContain("hashchange");

      const bbqResp = await page.request.get(`${BASE_URL}${BBQ_JS}`);
      expect(bbqResp.status(), `expected 404 for retired ${BBQ_JS}`).toBe(
        404,
      );

      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const probe = await page.evaluate(() => {
        const $ = window.jQuery;
        const parsed = $.deparam.querystring(
          "/cm/app/?site=Acme&view=arch&path=%2FSites%2FAcme",
        );
        return {
          hasShim:
            typeof $.deparam === "function" &&
            typeof $.deparam.querystring === "function" &&
            typeof $.param.querystring === "function",
          noBbqState: typeof $.bbq === "undefined",
          parsedSite: parsed.site,
          parsedPath: parsed.path,
          built: $.param.querystring("/cm/app/", {
            view: "arch",
            site: "Acme",
          }),
        };
      });
      expect(probe.hasShim, "deparam shim missing on classic page").toBe(true);
      expect(probe.noBbqState, "$.bbq state leaked onto the page").toBe(true);
      expect(probe.parsedSite).toBe("Acme");
      expect(probe.parsedPath).toBe("/Sites/Acme");
      expect(probe.built).toBe("/cm/app/?view=arch&site=Acme");
      expect(
        pluginErrors,
        `deparam plugin errors:\n${pluginErrors.join("\n")}`,
      ).toEqual([]);
    },
  );
});
