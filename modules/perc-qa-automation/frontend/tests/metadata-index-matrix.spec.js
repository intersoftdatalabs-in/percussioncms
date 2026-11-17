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
 * US3 metadata index-search matrix (#6560 / parent #6241): prove the
 * index-search round-trip against the in-cell DTS metadata service — a
 * self-seeded probe index row is retrievable through the index query,
 * and removing it takes it back out of the results.
 *
 * <p>Tags: {@code @dts} plus {@code @dts-metadata}.</p>
 *
 * <p>The suite is data-driven from
 * {@code tests/helpers/metadata-index-matrix.js} (closed inventory, 3
 * cases). {@code afterAll} assembles the one results-matrix row,
 * validates it against
 * {@code contracts/results-matrix.schema.json}, and writes it to
 * {@code test-results/metadata-index-matrix.json}. Three-level skip
 * contract: without {@code DTS_BASE_URL} every case skips naming the
 * cell (#6237, green on the CMS-only cell); with a base URL each case
 * probes liveness ({@code HEAD <context>/metadata/csrf} — the service
 * lives under its Tomcat deployment context, issue #6342); a cell
 * whose extractor seed is auth-gated or CONFIDENTIAL-redirected skips
 * the seeding cases with a named reason instead of failing. Each
 * Playwright case seeds its own probe row under the fixed probe site
 * so cases stay independent under parallel workers. Seed and cleanup
 * ride the supported extractor contract (helpers {@code entryUrl} +
 * {@code entryPayload}: {@code value} shape, double-slash entry URLs
 * — the {@code stringvalue} / single-slash forms silently drop or
 * litter rows, #6557).</p>
 *
 * <p>Run (needs the DTS-providing cell):
 * {@code DTS_BASE_URL=https://<dts-cell-origin>
 * npm run test:surface -- --path tests/metadata-index-matrix.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code DTS_BASE_URL} every case skips with the named reason.</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const {
  MATRIX_SPEC,
  CASE_QUERY,
  CASE_SEEDED,
  CASE_CLEANUP,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  entryUrl,
  deliveryAuthHeader,
  probeEntry,
  entryPayload,
  siteQueryBody,
  buildIndexRow,
  validateIndexRow,
} = require("./helpers/metadata-index-matrix");

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
  const url = `${dtsContextUrl(base)}/metadata/csrf`;
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

async function querySite(request, svc) {
  const answered = await request.post(`${svc}/get`, {
    data: siteQueryBody(),
  });
  expect(
    answered.ok(),
    `metadata: the index query for the probe site must answer 200 (HTTP ${answered.status()})`,
  ).toBe(true);
  const body = await readJson(answered);
  expect(
    body && Array.isArray(body.results),
    "metadata: the index query must return a result list shape",
  ).toBe(true);
  return body.results;
}

/**
 * Seeds the probe row through the extractor
 * ({@code POST /indexer/entry<path>} as {@code deliverymanager}),
 * skipping by name on the known cell limitations (auth-gated or
 * CONFIDENTIAL-redirected seed) and failing on any other rejection.
 * NOTE: the extractor resource lives at the context-level
 * {@code /indexer} root, not under the {@code /metadata} JAX-RS root
 * used by the query endpoints. The entry URL keeps the page path's
 * leading slash (double slash, {@link entryUrl}): the single-slash
 * form binds a slashless {@code pagePath} and DELETEs nothing while
 * answering 204, so seeded rows would linger (#6557 trap 2).
 *
 * @returns {Promise<boolean>} true when the seed landed (never skips)
 */
async function seedProbe(request, indexerSvc, entry) {
  const seed = await request.post(entryUrl(indexerSvc, entry.path), {
    maxRedirects: 0,
    headers: deliveryAuthHeader(),
    data: entryPayload(entry),
  });
  // Skip only for the known cell limitations (auth-gated or redirected
  // seed); any other failure (5xx, 404, …) means the seed path itself
  // is broken and must fail the test.
  test.skip(
    !seed.ok() && [401, 403, 302].includes(seed.status()),
    skipUnprovisioned(
      `the cell rejected the seeded entry '${entry.path}' (HTTP ${seed.status()}); ` +
        `the shipped extractor is CONFIDENTIAL + deliverymanager-gated, so plain-HTTP cells ` +
        `without provisioned DTS deliverymanager credentials (DTS_DELIVERY_USER / ` +
        `DTS_DELIVERY_PASSWORD) and a reachable DTS HTTPS port cannot seed`,
    ),
  );
  expect(
    seed.ok(),
    `metadata: seeding the probe index row '${entry.path}' must be accepted (HTTP ${seed.status()})`,
  ).toBe(true);
  return true;
}

async function removeProbe(request, indexerSvc, entry) {
  // Double slash via entryUrl (#6557 trap 2): the DAO matches pagepath
  // verbatim including the leading slash, and the {pagePath:.*} binding
  // only preserves it with the doubled slash. The single-slash form
  // deletes nothing while answering 204.
  const cleanup = await request.delete(entryUrl(indexerSvc, entry.path), {
    maxRedirects: 0,
    headers: deliveryAuthHeader(),
  });
  expect(
    cleanup.ok(),
    `metadata: removing the probe index row '${entry.path}' must be accepted (HTTP ${cleanup.status()})`,
  ).toBe(true);
}

function rowHasPath(rows, entryPath) {
  return rows.some((row) => row && row.pagepath === entryPath);
}

test.describe("metadata index-search matrix", () => {
  test.afterAll(async () => {
    // One row for the pinned metadata entry. A case that never ran
    // (grep-filtered run) or never recorded (its test failed) is reported
    // as skipped; a red run is still red because the failing test itself
    // fails.
    const row = buildIndexRow(
      {
        [CASE_QUERY]: caseOutcomes.get(CASE_QUERY) || "skip",
        [CASE_SEEDED]: caseOutcomes.get(CASE_SEEDED) || "skip",
        [CASE_CLEANUP]: caseOutcomes.get(CASE_CLEANUP) || "skip",
      },
      { spec: MATRIX_SPEC },
    );
    const problems = validateIndexRow(row);
    if (problems.length > 0) {
      throw new Error(`matrix row fails schema validation: ${problems.join(" | ")}`);
    }
    const dir = path.join(__dirname, "..", "test-results");
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(
      path.join(dir, "metadata-index-matrix.json"),
      `${JSON.stringify([row], null, 2)}\n`,
      "utf8",
    );
  });

  test(
    "index query answers: POST /metadata/get returns a result list shape @dts",
    { tag: ["@dts", "@dts-metadata"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `metadata index (Metadata): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      await querySite(request, dtsServiceUrl(base));
      recordCase(CASE_QUERY);
    },
  );

  test(
    "seeded entry is retrievable: the seeded probe row appears in the re-queried results @dts",
    { tag: ["@dts", "@dts-metadata"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `metadata index (Metadata): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const indexerSvc = indexerServiceUrl(base);
      const entry = probeEntry();
      // The seed/assert/cleanup sequence runs in try/finally so a failed
      // assertion never leaves the seeded row behind in the cell.
      let seeded = false;
      try {
        seeded = await seedProbe(request, indexerSvc, entry);
        const rows = await querySite(request, svc);
        expect(
          rows.length > 0,
          "metadata: the index query must return non-empty results after seeding",
        ).toBe(true);
        expect(
          rowHasPath(rows, entry.path),
          `metadata: the index query must contain the seeded entry '${entry.path}'`,
        ).toBe(true);
      } finally {
        if (seeded) {
          await removeProbe(request, indexerSvc, entry);
        }
      }
      recordCase(CASE_SEEDED);
    },
  );

  test(
    "cleanup removes: the deleted probe row no longer appears in the re-queried results @dts",
    { tag: ["@dts", "@dts-metadata"] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, `metadata index (Metadata): ${SKIP_NO_DTS_BASE}`);

      await probeLiveness(request, base);
      const svc = dtsServiceUrl(base);
      const indexerSvc = indexerServiceUrl(base);
      const entry = probeEntry();
      await seedProbe(request, indexerSvc, entry);
      await removeProbe(request, indexerSvc, entry);
      const rows = await querySite(request, svc);
      expect(
        rowHasPath(rows, entry.path),
        `metadata: the index query must no longer contain the removed entry '${entry.path}'`,
      ).toBe(false);
      recordCase(CASE_CLEANUP);
    },
  );
});
