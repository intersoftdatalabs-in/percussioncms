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
 * RSS feed widget surface (#6244 / parent #6237, epic AC3 dynamic half):
 * the published {@code percRss} fixture page renders the feed shell, the
 * feeds service answers liveness, and the feed endpoint follows its
 * documented contract for the fixture probe.
 *
 * <p>Feed entries render from the metadata index behind a seeded feed
 * descriptor ({@code PUT /rss/descriptors} as {@code deliverymanager}).
 * The {@code --with-dts} cell ships neither a descriptor nor indexed
 * fixture content, so the endpoint half asserts the documented unseeded
 * contract (service live, no descriptor: the cell's error-page contract
 * answers the fixture probe with a redirect at the feeds error page)
 * rather than pretending entries render — descriptor seeding is tracked
 * as a follow-on slice (see the residual linked from #6244). When that
 * slice lands, the probe case below is the place that turns the redirect
 * contract into an entries assertion.</p>
 *
 * <p>Tags: {@code @published} {@code @surface-feed}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-feed.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code PUBLISHED_SITE_URL} the shell case skips; without
 * {@code DTS_URL} the service cases skip.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  SURFACES,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  publishedSiteUrl,
  dtsBaseUrl,
  fixturePageUrl,
  dtsContextUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
} = require("./helpers/published-widget-surfaces");

const entry = SURFACES.find((surface) => surface.name === "feed");

test.describe("published RSS feed widget surface", () => {
  test(
    "published feed page renders the feed shell with zero noise @published",
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
        `${entry.widget}: expected feed shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "feeds service answers liveness on the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);

      const live = await request.head(`${dtsContextUrl(base, entry)}${entry.csrf}`);
      expect(
        live.ok(),
        `feeds: HEAD ${entry.context}${entry.csrf} must answer 2xx`,
      ).toBe(true);
    },
  );

  test(
    "feed endpoint reports the documented unseeded outcome for the fixture probe",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);

      // Unseeded fixture path: the service answers 404 (no descriptor),
      // which the cell's error-page contract surfaces as a redirect at the
      // service's error page. Assert the contract, not the bare code.
      const feedUrl =
        `${dtsServiceUrl(base, entry)}/qasurface/qasurface-feed-${Date.now()}/localhost`;
      const probe = await request.get(feedUrl, {
        headers: { Accept: "text/xml" },
        maxRedirects: 0,
      });
      expect(
        probe.status(),
        "feeds: unseeded fixture path must redirect at the documented error page",
      ).toBe(302);
      expect(
        String(probe.headers().location || ""),
        "feeds: unseeded fixture redirect must stay at the feeds error page",
      ).toMatch(/\/feeds\/error\.html$/);
    },
  );
});
