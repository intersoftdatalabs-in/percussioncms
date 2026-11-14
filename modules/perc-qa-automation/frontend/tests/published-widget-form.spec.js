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
 * Form widget surface (#6244 / parent #6237, epic AC3 dynamic half): the
 * published {@code percForm} fixture page renders the form shell, and a
 * self-seeded form collects a submission against the in-cell DTS.
 *
 * <p>Tags: {@code @published} {@code @surface-form}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-form.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code PUBLISHED_SITE_URL} the shell case skips; without
 * {@code DTS_URL} the interaction case skips.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  SURFACES,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  publishedSiteUrl,
  dtsBaseUrl,
  fixturePageUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
} = require("./helpers/published-widget-surfaces");

const entry = SURFACES.find((surface) => surface.name === "form");

test.describe("published form widget surface", () => {
  test(
    "published form page renders the form shell with zero noise @published",
    { tag: ["@published", entry.tag] },
    async ({ page, request }) => {
      test.setTimeout(90_000);
      const site = publishedSiteUrl();
      test.skip(!site, SKIP_NO_SITE);

      const pageUrl = fixturePageUrl(site, entry);
      const probe = await request.get(pageUrl);
      expect(probe.status(), `expected 200 for ${entry.widget} fixture ${pageUrl}`).toBe(200);

      const noise = watchNoise(page);
      await page.goto(pageUrl, { waitUntil: "networkidle" });
      await expect(
        page.locator(entry.marker).first(),
        `${entry.widget}: expected form shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "self-seeded form submission collects against the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      // The real widget posts to the trailing-slash collect path (see the
      // form widget JS: action + "collect/"): that path is the openly
      // served submit contract, while the bare /forms/form/* tree stays
      // behind the deliverymanager role (admin retrieval surface, out of
      // scope for the visitor-facing widget surface).
      const formName = `qasurface-form-${Date.now()}`;
      const site = publishedSiteUrl();
      const collect = await request.post(`${svc}/form/collect/`, {
        maxRedirects: 0,
        form: {
          action: "submit",
          perc_formName: formName,
          perc_hostUrl: site ? site.origin : base.origin,
          qasurface: `qasurface-${Date.now()}`,
        },
      });
      expect(
        collect.status(),
        `forms: submit to self-seeded form '${formName}' must take the success redirect (HTTP ${collect.status()})`,
      ).toBe(302);
      expect(
        String(collect.headers().location || ""),
        `forms: submit to '${formName}' must redirect at the success page`,
      ).toMatch(/\/success\.html$/);
    },
  );
});
