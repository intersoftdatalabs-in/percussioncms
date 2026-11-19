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
 * US3 feeds entries matrix (#6560 / parent #6241): prove the
 * descriptor-to-entries round-trip against the in-cell DTS feeds
 * service — a self-seeded probe descriptor is created by PUT, the
 * generated feed serves the seeded index entries, and the documented
 * unseeded-name outcome holds.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-feeds}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/feeds-entries-matrix.js} (closed inventory, 3
 * cases). {@code afterAll} assembles the one results-matrix row,
 * validates it against
 * {@code contracts/results-matrix.schema.json}, and writes it to
 * {@code test-results/feeds-entries-matrix.json}. Three-level skip
 * contract: without {@code DTS_BASE_URL} (or its {@code DTS_URL} banner
 * alias) every case skips naming the
 * cell (#6237, green on the CMS-only cell); with a base URL each case
 * probes liveness ({@code HEAD <context>/rss/csrf} — the service lives
 * under its Tomcat deployment context, issue #6342); writer calls
 * (descriptor PUT, index seed and cleanup) ride the published DTS
 * HTTPS origin ({@code DTS_HTTPS_URL}, #6375) because the writer
 * paths are CONFIDENTIAL — a cell that publishes no HTTPS origin
 * skips the seeding cases with a named reason instead of failing,
 * and auth-gated HTTPS writes skip naming the missing delivery
 * credentials. Each
 * Playwright case seeds its own probe descriptor under a unique site
 * so cases stay independent under parallel workers (the descriptor PUT
 * scopes writes per site). Index-row seed and cleanup ride the
 * supported extractor contract (helpers {@code entryUrl} +
 * {@code indexEntryPayload}: {@code value} shape, double-slash entry
 * URLs — the {@code stringvalue} / single-slash forms silently drop or
 * litter rows, #6557).</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_BASE_URL=http://<dts-cell-origin> (or the
 * {@code DTS_URL} banner alias)
 * DTS_HTTPS_URL=https://<dts-cell-origin>:<https-port>
 * DTS_DELIVERY_USER=<deliverymanager-user>
 * DTS_DELIVERY_PASSWORD=<deliverymanager-password>
 * npm run test:surface -- --path tests/feeds-entries-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} (or {@code DTS_URL}) every case skips with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect, request: baseRequest } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_DESCRIPTOR,
  CASE_ENTRIES,
  CASE_UNSEEDED,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  openDtsHttpsContext,
  entryUrl,
  deliveryAuthHeader,
  probeFeed,
  descriptorPayload,
  indexEntryPayload,
  feedUrl,
  buildEntriesRow,
  validateEntriesRow,
} = require("./helpers/feeds-entries-matrix");

const caseOutcomes = new Map();

function recordCase(name) {
  caseOutcomes.set(name, "pass");
}

function originOf(base) {
  return base.origin;
}

async function probeLiveness(request, base) {
  // Liveness hangs off the deployment context (war name) plus the JAX-RS
  // root — the service is not served at the DTS origin root (issue #6342).
  const url = `${dtsContextUrl(base)}/rss/csrf`;
  let response = null;
  try {
    response = await request.head(url);
  } catch (error) {
    test.skip(
      true,
      `${skipServiceDown(originOf(base))} ` +
        `(probe unreachable: ${String((error && error.message) || error).slice(0, 120)})`,
    );
  }
  test.skip(!response.ok(), skipServiceDown(originOf(base)));
  return response;
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/**
 * HTTPS writer context for the CONFIDENTIAL writer paths (the
 * descriptor PUT plus the index seed and cleanup): the writers answer
 * plain-HTTP calls with a 302 to the in-cell HTTPS port, so seeding
 * rides the published DTS HTTPS origin (shared
 * {@code tests/helpers/dts-cell.js#openDtsHttpsContext}, #6375) through
 * an untrusted-chain-allowed context — the cell TLS identity is
 * shipped/self-signed. Skips by name when the cell publishes no HTTPS
 * origin or the origin never answers (default H2 QA cell, pre-#6375
 * cell).
 *
 * <p>The caller disposes the returned context (try/finally) so a
 * passing run leaves no request state behind.</p>
 *
 * @returns {Promise<{ httpsBase: URL, httpsRequest: object }>} HTTPS origin + context
 */
async function httpsWriterContext() {
  return openDtsHttpsContext(test, baseRequest, {
    label: "feeds entries (Feeds)",
  });
}

/**
 * Seeds the probe descriptor ({@code PUT /rss/descriptors} as
 * {@code deliverymanager}) plus its index rows
 * ({@code POST /indexer/entry<path>}), skipping by name on the known
 * cell limitations (auth-gated or CONFIDENTIAL-redirected writes) and
 * failing on any other rejection. Removal of the index rows stays with
 * the caller (try/finally) so the cell never keeps probe rows behind.
 * Entry URLs keep the page path's leading slash (double slash,
 * {@link entryUrl}): the single-slash form binds a slashless
 * {@code pagePath} and DELETEs nothing while answering 204, so seeded
 * rows would linger (#6557 trap 2).
 */
async function seedProbe(httpsRequest, httpsSvc, httpsIndexerSvc, feed) {
  const put = await httpsRequest.put(`${httpsSvc}/descriptors`, {
    maxRedirects: 0,
    headers: deliveryAuthHeader(),
    data: descriptorPayload(feed),
  });
  // Skip only for the known cell limitation (auth-gated seed); any
  // other failure (5xx, 404, …) means the descriptor surface itself
  // is broken and must fail the test. The CONFIDENTIAL redirect is
  // gone by construction: writers always ride the HTTPS origin.
  test.skip(
    !put.ok() && [401, 403].includes(put.status()),
    skipUnprovisioned(
      `PUT /rss/descriptors for site '${feed.site}' answered HTTP ${put.status()}; ` +
        `the shipped feeds writer is deliverymanager-gated, so cells without provisioned ` +
        `DTS deliverymanager credentials (DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD) cannot seed`,
    ),
  );
  expect(
    put.ok(),
    `feeds: creating the probe descriptor for site '${feed.site}' must be accepted (HTTP ${put.status()})`,
  ).toBe(true);

  for (const entry of feed.entries) {
    const seed = await httpsRequest.post(entryUrl(httpsIndexerSvc, entry.path), {
      maxRedirects: 0,
      headers: deliveryAuthHeader(),
      data: indexEntryPayload(entry, feed.site),
    });
    test.skip(
      !seed.ok() && [401, 403].includes(seed.status()),
      skipUnprovisioned(
        `POST /indexer/entry for '${entry.path}' answered HTTP ${seed.status()}; ` +
          `the shipped extractor is deliverymanager-gated, so cells without provisioned ` +
          `credentials cannot seed`,
      ),
    );
    expect(
      seed.ok(),
      `feeds: seeding the probe index row '${entry.path}' must be accepted (HTTP ${seed.status()})`,
    ).toBe(true);
  }
}

/**
 * Removes the probe index rows (best-effort, never masks the
 * assertion that already ran — no assertions inside). The probe
 * descriptor has no single-delete endpoint and remains against its
 * emptied query; the unique probe site keeps that residue invisible
 * to every other test. The row DELETEs go through {@link entryUrl}
 * (double slash, #6557 trap 2) so they actually remove the rows.
 */
async function removeProbeRows(httpsRequest, httpsIndexerSvc, feed) {
  for (const entry of feed.entries) {
    const cleanup = await httpsRequest
      .delete(entryUrl(httpsIndexerSvc, entry.path), {
        maxRedirects: 0,
        headers: deliveryAuthHeader(),
      })
      .catch((cleanupError) => {
        console.warn(
          `feeds: best-effort cleanup of '${entry.path}' threw: ` +
            `${cleanupError?.message ?? cleanupError}`,
        );
        return null;
      });
    if (cleanup && !cleanup.ok()) {
      console.warn(
        `feeds: best-effort cleanup of '${entry.path}' answered HTTP ` +
          `${cleanup.status()}; the seeded row may remain in the cell`,
      );
    }
  }
}

test.describe("feeds entries matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned feeds entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildEntriesRow(
      {
        [CASE_DESCRIPTOR]: caseOutcomes.get(CASE_DESCRIPTOR) || "skip",
        [CASE_ENTRIES]: caseOutcomes.get(CASE_ENTRIES) || "skip",
        [CASE_UNSEEDED]: caseOutcomes.get(CASE_UNSEEDED) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateEntriesRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "feeds-entries-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "descriptor creates: self-seeded PUT answers 2xx for a fresh probe site @dts",
    { tag: ["@dts", "@dts-feeds"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `feeds entries (Feeds): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const feed = probeFeed();
      const { httpsBase, httpsRequest } = await httpsWriterContext();
      try {
        await seedProbe(
          httpsRequest,
          dtsServiceUrl(httpsBase),
          indexerServiceUrl(httpsBase),
          feed,
        );
      } finally {
        await removeProbeRows(
          httpsRequest,
          indexerServiceUrl(httpsBase),
          feed,
        );
        await httpsRequest.dispose();
      }
      recordCase(CASE_DESCRIPTOR);
    },
  );

  test(
    "entries serve: the generated feed carries the probe descriptor title plus both entries @dts",
    { tag: ["@dts", "@dts-feeds"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `feeds entries (Feeds): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const feed = probeFeed();
      const svc = dtsServiceUrl(base);
      const { httpsBase, httpsRequest } = await httpsWriterContext();
      const httpsIndexerSvc = indexerServiceUrl(httpsBase);
      try {
        await seedProbe(
          httpsRequest,
          dtsServiceUrl(httpsBase),
          httpsIndexerSvc,
          feed,
        );

        const generated = await request.get(feedUrl(svc, feed), {
          headers: { Accept: "text/xml" },
          maxRedirects: 0,
        });
        expect(
          generated.ok(),
          `feeds: the generated feed for probe site '${feed.site}' must answer 200 (HTTP ${generated.status()})`,
        ).toBe(true);
        const contentType = generated.headers()["content-type"] || "";
        expect(
          /xml/i.test(contentType),
          `feeds: the generated feed for '${feed.site}/${feed.name}' must render XML (content-type=${contentType})`,
        ).toBe(true);
        const xml = await generated.text();
        expect(
          xml,
          `feeds: the generated feed must carry the probe descriptor title '${feed.title}'`,
        ).toContain(feed.title);
        for (const entry of feed.entries) {
          expect(
            xml,
            `feeds: the generated feed must carry the seeded entry '${entry.title}'`,
          ).toContain(entry.title);
          expect(
            xml,
            `feeds: the generated feed must carry the seeded abstract '${entry.abstract}'`,
          ).toContain(entry.abstract);
        }
      } finally {
        await removeProbeRows(httpsRequest, httpsIndexerSvc, feed);
        await httpsRequest.dispose();
      }
      recordCase(CASE_ENTRIES);
    },
  );

  test(
    "unseeded name redirects: the documented error-page outcome holds with no seeding @dts",
    { tag: ["@dts", "@dts-feeds"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `feeds entries (Feeds): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);

      // Unseeded fixture path: the service answers 404 (no descriptor),
      // which the cell's error-page contract surfaces as a redirect at
      // the service's error page. Assert the contract, not the bare code.
      const feedUrlProbe =
        `${svc}/qamatrix-unseeded/qamatrix-unseeded-feed-${Date.now()}/localhost`;
      const probe = await request.get(feedUrlProbe, {
        headers: { Accept: "text/xml" },
        maxRedirects: 0,
      });
      expect(
        probe.status(),
        "feeds: an unseeded feed name must redirect at the documented error page",
      ).toBe(302);
      expect(
        String(probe.headers().location || ""),
        "feeds: the unseeded feed redirect must stay at the feeds error page",
      ).toMatch(/\/feeds\/error\.html$/);
      recordCase(CASE_UNSEEDED);
    },
  );
});
