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
 * Unknown file driver fallback i18n (#7106, residual from #7076).
 *
 * <p>{@code FileDriverFields.tsx} renders the unknown-driver note via
 * {@code message(MSG.PUBLISH_DELIVERY_DELIVERY_UNKNOWN_FILE_DRIVER,
 * [driver])}. The key ({@code perc.ui.publish.modern@Unknown file driver:
 * {0}}) had no {@code CmsUi.tmx} entry, and the WebUI fallback did not
 * substitute args — so the double-fault path (unknown driver + missing
 * catalog entry) showed a literal {@code {0}}. The Vitest
 * ({@code message.test.ts}, {@code fileDriverFields.test.tsx}) covers the
 * fallback interpolation routing; this spec is the Playwright/catalog note:
 * it proves the live H2 cell serves the new {@code CmsUi.tmx} TU through
 * {@code tmx.jsp} (a malformed TMX edit would break catalog delivery), and
 * that the Publishing shell still loads cleanly on the rebuilt bundle.</p>
 *
 * <p>Cell setup (once per C5 run, before this spec): copy the rebuilt
 * {@code CmsUi.tmx} into the cell and hot-deploy the rebuilt modern bundle —
 * no Jetty restart. The first test below then reloads the server i18n cache
 * over the remote console transport so {@code tmx.jsp} serves the new TU:</p>
 * <pre>
 *   docker cp modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx \
 *     &lt;cell&gt;:/opt/Percussion/rxconfig/i18n/CmsUi.tmx
 *   python docker/scripts/perc-devctl.py qa-deploy-webui --container &lt;cell&gt;
 * </pre>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/publishing-unknown-driver-fallback-i18n.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { publishingSitesUrl } = require("./helpers/publishing-item-schedule");

/** Catalog key backing the unknown file driver note. */
const FALLBACK_KEY = "perc.ui.publish.modern@Unknown file driver: {0}";

/** Remote-console success for {@code reload i18nresources} (RCONSOLE_I18NRESOURCES_RELOADED). */
const I18N_RELOADED_RESULT_CODE = "1542";

test.describe("Publishing unknown file driver fallback i18n (#7106)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "reload the i18n resource cache without a restart",
    {
      tag: ["@publishing", "@publishing-delivery", "@i18n"],
    },
    async ({ page }) => {
      // No-restart TMX pickup: POST the remote-console command the same way
      // PSRemoteConsole does (Designer servlet + PS-Request-Type: admin).
      const res = await page.request.post(`${BASE_URL}/Rhythmyx/Designer`, {
        data: "<PSXRemoteConsoleCommand><command>reload i18nresources</command></PSXRemoteConsoleCommand>",
        headers: {
          "Content-Type": "text/xml",
          "PS-Request-Type": "admin",
        },
      });
      expect(res.status()).toBe(200);
      const text = await res.text();
      expect(text).toContain(
        `<resultCode>${I18N_RELOADED_RESULT_CODE}</resultCode>`,
      );
    },
  );

  test(
    "TMX catalog serves the unknown file driver fallback key",
    {
      tag: ["@publishing", "@publishing-delivery", "@i18n"],
    },
    async ({ page }) => {
      const response = await page.request.get(
        `${BASE_URL}/Rhythmyx/tmx/tmx.jsp?mode=json&prefix=perc.ui.publish.modern@&sys_lang=en-us`,
      );
      expect(response.status()).toBe(200);
      const body = await response.json();
      const messages = body.tmxmessages ?? {};
      expect(
        messages[FALLBACK_KEY],
        `catalog serves ${FALLBACK_KEY}`,
      ).toBe("Unknown file driver: {0}");
    },
  );

  test(
    "publishing shell loads cleanly on the keyed bundle",
    {
      tag: ["@publishing", "@publishing-delivery", "@i18n"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.goto(publishingSitesUrl(BASE_URL), { waitUntil: "networkidle" });
      const shell = page.locator('[data-testid="publishing-shell"]');
      await expect(shell).toBeVisible({ timeout: 30_000 });
      await page.screenshot({
        path: "test-results/live-proof-7106-publishing-unknown-driver-fallback.png",
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
