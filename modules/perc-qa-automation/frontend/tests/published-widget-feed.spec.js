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
 * The shell case renders the published fixture page and the liveness case
 * gates the service; the entries case consumes the cell-seeded fixture
 * ({@code FEED_FIXTURE} in {@code ./helpers/published-widget-surfaces},
 * seeded by {@code python docker/scripts/perc-devctl.py
 * qa-seed-widget-fixtures} on the {@code --with-dts} cell, #6341) and
 * asserts the generated feed carries the fixture descriptor title plus
 * both fixture entries. The credential lives in the fixture step, never
 * in this spec: the entries case sends no auth and skips by name when
 * the fixture is absent (unseeded cell). The unseeded-name probe stays
 * as the documented error-page contract for names that were never
 * seeded.</p>
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
 * {@code DTS_URL} the service cases skip; without the seeded fixture
 * the entries case skips (run {@code qa-seed-widget-fixtures} first).</p>
 */

const { test, expect } = require("@playwright/test");
const {
  SURFACES,
  FEED_FIXTURE,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  skipNoFixture,
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

  test(
    "feed endpoint serves seeded entries from the live index",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      // Fixture consumption (no credentials in this spec): the
      // cell-side `qa-seed-widget-fixtures` step seeds the
      // `FEED_FIXTURE` descriptor plus index rows and the seeder itself
      // verifies the feed serves them, so this case only asserts the
      // served contract — skipping by name when the fixture is absent.
      const feedUrl =
        `${svc}/${FEED_FIXTURE.site}/${FEED_FIXTURE.name}/localhost`;
      const feed = await request.get(feedUrl, {
        headers: { Accept: "text/xml" },
        maxRedirects: 0,
      });
      test.skip(
        feed.status() === 302 ||
          feed.status() === 404 ||
          feed.status() === 503,
        skipNoFixture(
          entry,
          `fixture feed '${FEED_FIXTURE.name}' answered HTTP ${feed.status()}; ` +
            `run qa-seed-widget-fixtures on the --with-dts cell first (#6341)`,
        ),
      );
      expect(
        feed.ok(),
        `feeds: the seeded feed '${FEED_FIXTURE.name}' must answer 200 (HTTP ${feed.status()})`,
      ).toBe(true);
      const xml = await feed.text();
      expect(
        xml,
        "feeds: the generated feed must carry the seeded descriptor title",
      ).toContain(FEED_FIXTURE.title);
      for (const item of FEED_FIXTURE.entries) {
        expect(
          xml,
          `feeds: the generated feed must carry the seeded entry '${item.title}'`,
        ).toContain(item.title);
        expect(
          xml,
          `feeds: the generated feed must carry the seeded abstract '${item.abstract}'`,
        ).toContain(item.abstract);
      }
    },
  );
});
