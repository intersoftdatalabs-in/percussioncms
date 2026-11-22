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
 * Explorer publish/takedown/stage/unstage batch-helper failure literals
 * i18n (#7056, parent #4532).
 *
 * <p>The batch helpers in {@code contentExplorer/itemPublish.ts} (and the
 * custom-handler mirrors {@code runPublishBatch}/{@code runTakedownBatch} in
 * {@code actionDispatch.ts}) built per-item failure text from hardcoded
 * lowercase English ("publish failed", "not published", "missing content
 * id", …). Those literals are now catalog-routed through new
 * {@code EXPLORER_MSG.BATCH_*} keys with byte-identical en-us defaults; the
 * keyed-message Vitest ({@code itemPublishFallback.test.ts},
 * {@code actionDispatch.test.ts}) covers the routing. This spec is the
 * Playwright/catalog note: it proves the live H2 cell serves the new
 * {@code CmsUi.tmx} TUs through {@code tmx.jsp} (a malformed TMX edit would
 * break catalog delivery), and that the Explorer shell still loads cleanly
 * on the rebuilt bundle.</p>
 *
 * <p>Cell setup (once per C5 run, before this spec): copy the rebuilt
 * {@code CmsUi.tmx} into the cell and hot-deploy the rebuilt modern bundle —
 * no Jetty restart. The first test below then reloads the server i18n cache
 * over the remote console transport so {@code tmx.jsp} serves the new TUs:</p>
 * <pre>
 *   docker cp modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx \
 *     &lt;cell&gt;:/opt/Percussion/rxconfig/i18n/CmsUi.tmx
 *   python docker/scripts/perc-devctl.py qa-deploy-webui --container &lt;cell&gt;
 * </pre>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-batch-fallback-i18n.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

/** Catalog keys backing the batch-helper failure literals. */
const FALLBACK_KEYS = [
  "perc.ui.explorer@missing content id",
  "perc.ui.explorer@not published",
  "perc.ui.explorer@publish failed",
  "perc.ui.explorer@not taken down",
  "perc.ui.explorer@takedown failed",
  "perc.ui.explorer@not staged",
  "perc.ui.explorer@not removed",
];

/** Remote-console success for {@code reload i18nresources} (RCONSOLE_I18NRESOURCES_RELOADED). */
const I18N_RELOADED_RESULT_CODE = "1542";

test.describe("Explorer batch-helper fallback i18n (#7056)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "reload the i18n resource cache without a restart",
    {
      tag: ["@explorer", "@explorer-publish", "@i18n"],
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
    "TMX catalog serves the batch fallback keys",
    {
      tag: ["@explorer", "@explorer-publish", "@i18n"],
    },
    async ({ page }) => {
      const response = await page.request.get(
        `${BASE_URL}/Rhythmyx/tmx/tmx.jsp?mode=json&prefix=perc.ui.explorer@&sys_lang=en-us`,
      );
      expect(response.status()).toBe(200);
      const body = await response.json();
      const messages = body.tmxmessages ?? {};
      for (const key of FALLBACK_KEYS) {
        expect(
          messages[key],
          `catalog serves ${key}`,
        ).toBe(key.slice(key.indexOf("@") + 1));
      }
    },
  );

  test(
    "explorer shell loads cleanly on the keyed bundle",
    {
      tag: ["@explorer", "@explorer-publish", "@i18n"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 30_000 });
      await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible({
        timeout: 15_000,
      });
      await page.screenshot({
        path: "test-results/live-proof-7056-explorer-batch-fallback.png",
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
