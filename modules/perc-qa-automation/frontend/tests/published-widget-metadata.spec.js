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
 * Metadata service surface (#6244 / parent #6237, epic AC3 dynamic half):
 * the published {@code percMostReadBlogPosts} fixture page (the metadata
 * visit-tracking consumer) renders its shell, and the visit-tracking
 * round-trip plus the index query shape hold against the in-cell DTS.
 *
 * <p>Tags: {@code @published} {@code @surface-metadata}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * DTS_HTTPS_URL=https://<in-cell-dts-https-origin>
 * DTS_DELIVERY_USER=ps_manager DTS_DELIVERY_PASSWORD=<from-seeder>
 * npm run test:surface -- --path tests/published-widget-metadata.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code PUBLISHED_SITE_URL} the shell case skips; without
 * {@code DTS_URL} the interaction case skips; without
 * {@code DTS_HTTPS_URL} the extractor seed block skips.</p>
 *
 * <p>Seed contract (parent #6237, residuals #6343 / #6375 / #6557): the
 * extractor seed runs over the published DTS HTTPS origin
 * ({@code qa-up --with-dts} publishes the in-cell 8443, banner
 * {@code DTS_HTTPS_URL}) with the deliverymanager credential provisioned
 * by {@code qa-seed-widget-fixtures --delivery-password} (exported as
 * {@code DTS_DELIVERY_PASSWORD}). Property values ride on
 * {@code value}, never {@code stringvalue} — the live services
 * deserialize through {@code JacksonXmlBindJsonProvider}, under which
 * {@code PSMetadataProperty.setStringvalue} is {@code @XmlTransient} and
 * a {@code stringvalue} field is silently dropped (#6557 trap 1). Entry
 * URLs keep the page path's leading slash (double slash): single-slash
 * deletes match nothing while answering 204, so seeded rows would linger
 * (#6557 trap 2, same contract as the seeder's {@code entry_url}).</p>
 */

const { test, expect, request: baseRequest } = require("@playwright/test");
const {
  SURFACES,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  SKIP_NO_DTS_HTTPS,
  publishedSiteUrl,
  dtsBaseUrl,
  dtsHttpsBaseUrl,
  fixturePageUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
} = require("./helpers/published-widget-surfaces");

const entry = SURFACES.find((surface) => surface.name === "metadata");

/**
 * BASIC credentials for the role-protected DTS indexer endpoints (the
 * context-level `/indexer/entry/...` extractor surface, `deliverymanager`
 * role). The effective credential is the one provisioned by
 * `qa-seed-widget-fixtures --delivery-password` (exported as
 * `DTS_DELIVERY_USER` / `DTS_DELIVERY_PASSWORD`); the shipped
 * `tomcat-users.xml` carries a digest nobody can present, so the legacy
 * default below only ever 401s into the named skip (residuals #6343 /
 * #6375). Never commit a real secret here — read it from the env.
 */
function deliveryAuthHeader() {
  const user = process.env.DTS_DELIVERY_USER || "ps_manager";
  const password =
    process.env.DTS_DELIVERY_PASSWORD || "f2c57870308dc87f432e5912d4de6f8e322721ba";
  return {
    Authorization: `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}`,
  };
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

test.describe("published metadata widget surface", () => {
  test(
    "published most-read page renders the metadata shell with zero noise @published",
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
        `${entry.widget}: expected metadata shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "visit tracks and the index answers on the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      // The tracked visit targets the published fixture page itself, so
      // the surface ties the interaction to the page the shell case
      // renders.
      const site = publishedSiteUrl();
      const pagePath = site
        ? new URL(fixturePageUrl(site, entry)).pathname
        : "/widgets/percMostReadBlogPosts.html";

      const track = await request.post(`${svc}/trackblogpost`, {
        data: { pagePath: `${pagePath}?qasurface=${Date.now()}` },
      });
      expect(
        track.ok(),
        `metadata: tracking a visit to '${pagePath}' must be accepted (HTTP ${track.status()})`,
      ).toBe(true);

      const status = await request.get(`${svc}/visits/status`);
      expect(
        status.ok(),
        "metadata: the visit-tracking scheduler status must answer",
      ).toBe(true);

      const top = await request.post(`${svc}/topblogposts`, {
        data: {},
      });
      expect(
        top.ok(),
        "metadata: topblogposts must answer against the in-cell DTS",
      ).toBe(true);
      const ranked = await readJson(top);
      expect(
        Array.isArray(ranked),
        "metadata: topblogposts must return a result list shape",
      ).toBe(true);

      // The index query half (POST /metadata/get, issue #6343): the query
      // must answer 200 with a result list shape on every cell (this is the
      // fixed behavior — non-null bodies used to 302 to the error page).
      // Criteria need an explicit operator with quoted string values
      // (PSCriteriaElement); the legacy "site:xyz" shorthand is malformed.
      // Seeding one row below additionally proves non-empty answers where
      // the cell accepts the seed (see the skip contract).
      const seedSite = "qacell";
      const answered = await request.post(`${svc}/get`, {
        data: {
          criteria: [`site = '${seedSite}'`],
          maxResults: 25,
          startIndex: 0,
        },
      });
      expect(
        answered.ok(),
        `metadata: the index query must answer 200 (HTTP ${answered.status()})`,
      ).toBe(true);
      const searched = await readJson(answered);
      expect(
        searched && Array.isArray(searched.results),
        "metadata: the index query must return a result list shape",
      ).toBe(true);

      // Seed one entry through the indexer, prove the query returns the
      // seeded row, then remove it so the disposable cell stays clean.
      // NOTE: the extractor resource lives at the context-level /indexer root
      // (PSMetadataExtractorRestService @Path("/indexer")), not under the
      // /metadata JAX-RS root used by the query endpoints above.
      const stamp = Date.now();
      const seedPath = `/qacell/surface-${stamp}.html`;
      // The extractor is CONFIDENTIAL, so the seed runs over the published
      // DTS HTTPS origin (qa-up --with-dts publishes the in-cell 8443,
      // banner DTS_HTTPS_URL, #6375). The cell TLS identity is
      // shipped/self-signed, hence a dedicated request context with the
      // untrusted chain allowed. Skip cleanly when the cell publishes no
      // HTTPS origin, or when the origin never answers (pre-#6375 cell).
      const httpsBase = dtsHttpsBaseUrl();
      test.skip(!httpsBase, SKIP_NO_DTS_HTTPS);
      const httpsRequest = await baseRequest.newContext({
        ignoreHTTPSErrors: true,
      });
      const indexerSvc = `${httpsBase.origin}/${entry.context}/indexer`;
      // maxRedirects: 0 keeps a misconfigured-HTTPS answer as a plain
      // response so the skip below engages; following the redirect would
      // throw instead of skipping.
      // The seed/assert/cleanup sequence runs in try/finally so a failed
      // assertion never leaves the seeded row behind in the cell, and the
      // HTTPS context is always disposed.
      let seed = null;
      let cleanupDone = false;
      try {
        let httpsAnswered = false;
        try {
          await httpsRequest.get(`${httpsBase.origin}/`, {
            maxRedirects: 0,
          });
          httpsAnswered = true;
        } catch {
          httpsAnswered = false;
        }
        test.skip(
          !httpsAnswered,
          `${SKIP_NO_DTS_HTTPS} (no answer at '${httpsBase.origin}/'; ` +
            `the cell predates the qa-up --with-dts HTTPS publish, #6375)`,
        );
        seed = await httpsRequest.post(`${indexerSvc}/entry${seedPath}`, {
          maxRedirects: 0,
          headers: deliveryAuthHeader(),
          data: {
            name: `surface-${stamp}.html`,
            folder: "/qacell/",
            pagepath: seedPath,
            linktext: `QA surface probe ${stamp}`,
            type: "page",
            site: seedSite,
            // value shape, never stringvalue: the live services drop
            // stringvalue silently (@XmlTransient, #6557 trap 1 — the
            // seeder's index_entry_payload documents the same contract).
            properties: [
              { name: "dcterms:title", value: `QA surface probe ${stamp}` },
              { name: "dcterms:abstract", value: `QA surface probe ${stamp}` },
            ],
          },
        });
        // Skip only for the known cell limitations (no provisioned
        // credential, or a pre-#6375 cell answering over this origin);
        // any other failure (5xx, 404, …) means the seed path itself is
        // broken and must fail the test.
        test.skip(
          !seed.ok() && [401, 403, 302].includes(seed.status()),
          `surface metadata (index seeding): the cell rejected the seeded entry ` +
            `'${seedPath}' (HTTP ${seed.status()}); the shipped extractor is ` +
            `CONFIDENTIAL + deliverymanager-gated, so seed first with ` +
            `qa-seed-widget-fixtures --delivery-password and export ` +
            `DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD plus DTS_HTTPS_URL ` +
            `from the qa-up --with-dts banner (parent #6237, #6375)`,
        );
        expect(
          seed.ok(),
          `metadata: seeding the index entry '${seedPath}' must be accepted (HTTP ${seed.status()})`,
        ).toBe(true);

        const reseeded = await request.post(`${svc}/get`, {
          data: {
            criteria: [`site = '${seedSite}'`],
            maxResults: 25,
            startIndex: 0,
          },
        });
        expect(
          reseeded.ok(),
          `metadata: the index query must answer 200 after seeding (HTTP ${reseeded.status()})`,
        ).toBe(true);
        const research = await readJson(reseeded);
        expect(
          research && Array.isArray(research.results) && research.results.length > 0,
          "metadata: the index query must return non-empty results after seeding",
        ).toBe(true);
        expect(
          research.results.some((row) => row && row.pagepath === seedPath),
          `metadata: the index query must contain the seeded entry '${seedPath}'`,
        ).toBe(true);

        // Double slash after entry/: the DAO matches pagepath verbatim
        // including the leading slash, and the {pagePath:.*} parameter only
        // preserves it with the doubled slash (entry//qacell/...). The
        // single-slash form deletes nothing while answering 204 — the
        // silent-litter trap (#6557 trap 2, same as the seeder cleanup).
        const cleanup = await httpsRequest.delete(
          `${indexerSvc}/entry/${seedPath}`,
          {
            maxRedirects: 0,
            headers: deliveryAuthHeader(),
          },
        );
        expect(
          cleanup.ok(),
          `metadata: removing the seeded entry '${seedPath}' must be accepted (HTTP ${cleanup.status()})`,
        ).toBe(true);
        cleanupDone = true;
      } finally {
        // Best-effort removal when the assertions above did not finish it;
        // never masks the original failure (no assertions here).
        if (seed && seed.ok() && !cleanupDone) {
          const leftover = await httpsRequest
            .delete(`${indexerSvc}/entry/${seedPath}`, {
              maxRedirects: 0,
              headers: deliveryAuthHeader(),
            })
            .catch((cleanupError) => {
              // Best-effort only: the assertions above already ran. Note the
              // leftover row for the cell operator without failing the test.
              console.warn(
                `metadata: best-effort cleanup of '${seedPath}' threw: ` +
                  `${cleanupError?.message ?? cleanupError}`,
              );
              return null;
            });
          if (leftover && !leftover.ok()) {
            console.warn(
              `metadata: best-effort cleanup of '${seedPath}' answered HTTP ` +
                `${leftover.status()}; the seeded row may remain in the cell`,
            );
          }
        }
        await httpsRequest.dispose();
      }
    },
  );
});
