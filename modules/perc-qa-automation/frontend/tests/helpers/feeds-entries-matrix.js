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
 * Closed inventory + results-matrix row helpers for the US3 feeds
 * entries matrix (#6560 / parent #6241): prove the feeds entries
 * round-trip against the in-cell DTS feeds service — a self-seeded
 * probe descriptor is created ({@code PUT <context>/rss/descriptors}),
 * the generated feed serves the seeded entries
 * ({@code GET <context>/rss/{site}/{feed}/localhost}), and the
 * documented unseeded-name outcome holds (302 to the feeds error page).
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code descriptorCreates} — the descriptor surface:
 *   {@code PUT <context>/rss/descriptors} with a fresh probe site and
 *   feed name answers 2xx ({@code PSFeedService#saveDescriptors} is
 *   {@code deliverymanager}-gated, so the probe carries the delivery
 *   credentials and the write is scoped to the probe site — existing
 *   descriptors for other sites are untouched).</li>
 *   <li>{@code entriesServe} — the entries surface: two index rows
 *   seeded under the probe site ({@code POST
 *   perc-metadata-services/indexer/entry<path>}) then
 *   {@code GET <context>/rss/{site}/{feed}/localhost} answers 200
 *   {@code text/xml} carrying the probe descriptor title plus both
 *   seeded entry titles and abstracts.</li>
 *   <li>{@code unseededRedirects} — the unseeded-name contract: a feed
 *   URL for a name that was never seeded answers 302 to the feeds
 *   error page (the documented outcome, no seeding needed).</li>
 * </ul>
 *
 * <p>Each Playwright case seeds its own unique probe descriptor
 * (timestamp-derived site + feed name, see {@link probeFeed}), so cases
 * stay independent under parallel workers and reruns never collide.
 * The descriptor PUT replaces descriptors per site, which is why the
 * site (not just the feed name) is unique per case. Descriptors expose
 * no single-delete endpoint, so the probe descriptor remains against
 * its emptied query after the case removes its index rows — the unique
 * probe site keeps that residue invisible to every other test.</p>
 *
 * <p>Skip contract (mirrors the US3 polls matrix): without
 * {@code DTS_BASE_URL} every case skips naming the missing cell (#6237,
 * green on the CMS-only cell); with a base URL each case probes
 * liveness ({@code HEAD <context>/rss/csrf} — the service lives under
 * its Tomcat deployment context, issue #6342) before touching the
 * service. A cell whose feeds/metadata writes are auth-gated or
 * CONFIDENTIAL-redirected (plain-HTTP cell without provisioned DTS
 * deliverymanager credentials or a reachable DTS HTTPS port) skips the
 * seeding cases with a named reason instead of failing (see
 * {@link skipUnprovisioned}).</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "feeds-entries-matrix";
const MATRIX_SPEC = "tests/feeds-entries-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-feeds.example.json";

const CASE_DESCRIPTOR = "descriptorCreates";
const CASE_ENTRIES = "entriesServe";
const CASE_UNSEEDED = "unseededRedirects";
const CASE_NAMES = Object.freeze([CASE_DESCRIPTOR, CASE_ENTRIES, CASE_UNSEEDED]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the DTS origin answers but the feeds service does
 * not. A 401/403 still counts as "served" (the service is up, the
 * caller lacks the role); only unreachable / 404 probes land here. The
 * probed endpoint is the deployment context path (war name) plus the
 * JAX-RS root — the service is served under a Tomcat context (see
 * {@link FEEDS_ENTRY}), not at the DTS origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `feeds entries (Feeds): no liveness answer at ` +
    `${baseUrl}/${FEEDS_ENTRY.context}${FEEDS_ENTRY.csrf}; the DTS cell does not serve ` +
    `${FEEDS_ENTRY.context}${FEEDS_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the service is up but the cell cannot persist the
 * probe descriptor or index rows. Each skip names the exact gap so the
 * cell slice can close it without touching the spec.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipUnprovisioned(detail) {
  return (
    `feeds entries (Feeds): ${detail}; ` +
    `the cell must serve writable feeds descriptors (deliverymanager role) ` +
    `and a writable metadata index on the DTS services to turn these cases ` +
    `green (parent #6241, delivery credentials via DTS_DELIVERY_USER / ` +
    `DTS_DELIVERY_PASSWORD)`
  );
}

/**
 * The pinned feeds service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code feeds}); {@code root} is the JAX-RS
 * root; {@code csrf} the {@code HEAD} liveness probe. Index rows ride
 * the metadata extractor at the context-level {@code /indexer} root
 * (see {@link indexerServiceUrl}).
 */
const FEEDS_ENTRY = Object.freeze({
  name: "feeds",
  label: "Feeds",
  context: "feeds",
  root: "/rss",
  csrf: "/rss/csrf",
  widget: "percRss",
});

/** DTS Tomcat context path (war name) of the metadata services. */
const METADATA_CONTEXT = "perc-metadata-services";

/**
 * @returns {URL|null} DTS-providing cell origin, or null when the cell is
 * CMS-only (default H2 QA cell: every case skips named)
 */
function dtsBaseUrl() {
  const baseUrl = process.env.DTS_BASE_URL || "";
  if (!baseUrl) {
    return null;
  }
  return new URL(baseUrl);
}

/**
 * Deployment context URL for the feeds service: the DTS origin
 * plus the deployment context path (war name), without the JAX-RS root.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${FEEDS_ENTRY.context}`;
}

/**
 * Service base URL for the feeds entries cases: the DTS
 * origin plus the deployment context path (war name) plus the JAX-RS
 * root, mirroring how the published feed widget addresses the service.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${FEEDS_ENTRY.root}`;
}

/**
 * Metadata extractor (indexer) service root for the feeds index rows:
 * the DTS origin plus the metadata deployment context path (war name)
 * plus the context-level {@code /indexer} root — the extractor
 * resource lives at the context level, not under the {@code /metadata}
 * JAX-RS root used by the query endpoints.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} indexer service root (no trailing slash)
 */
function indexerServiceUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${METADATA_CONTEXT}/indexer`;
}

/**
 * Extractor entry URL for one probe page path: the indexer root plus
 * {@code /entry/} plus the page path with its leading slash kept, so
 * the request line carries the double slash
 * ({@code /indexer/entry//qamatrix-feed-.../...}). The extractor binds
 * {@code /entry/{pagePath:.*}} and the DAO matches {@code pagepath}
 * verbatim including the leading slash, so the single-slash form
 * binds a slashless path and DELETEs nothing while answering 204 —
 * the silent-litter trap (#6557 trap 2). The single choke point keeps
 * seed and cleanup on the supported contract (same as
 * {@code docker/scripts/qa_seed_widget_fixtures.py#entry_url}).
 *
 * @param {string} indexerSvc indexer service root from {@link indexerServiceUrl}
 * @param {string} pagePath probe page path with its leading slash (see {@link probeFeed})
 * @returns {string} entry URL with the doubled slash
 */
function entryUrl(indexerSvc, pagePath) {
  return `${indexerSvc}/entry/${pagePath}`;
}

/**
 * BASIC credentials for the role-protected DTS writer endpoints (the
 * feeds {@code PUT /rss/descriptors} surface and the context-level
 * {@code /indexer/entry/...} extractor surface, both
 * {@code deliverymanager} role). Defaults are the shipped DTS
 * development defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties}
 * and the installed {@code conf/tomcat-users.xml}); override with env
 * when the cell provisions its own. The credential lives in the helper
 * and the spec sends it only on the writer calls — the feed reads send
 * no auth.
 *
 * @returns {{ Authorization: string }} BASIC auth header value
 */
function deliveryAuthHeader() {
  const user = process.env.DTS_DELIVERY_USER || "ps_manager";
  const password =
    process.env.DTS_DELIVERY_PASSWORD || "f2c57870308dc87f432e5912d4de6f8e322721ba";
  return {
    Authorization: `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}`,
  };
}

/**
 * Unique self-seeded probe feed for one entries round-trip. The site
 * and feed name are timestamp-derived so parallel workers and reruns
 * never collide; the descriptor PUT scopes writes per site, so the
 * site (not just the name) must be unique per case. The query limits
 * rows to the probe site so the generated feed carries exactly the
 * probe entries.
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {{ site: string, name: string, title: string, description: string, link: string, entries: Array<{ path: string, title: string, abstract: string }> }} probe feed
 */
function probeFeed(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  const site = `qamatrix-feed-${at}`;
  return {
    site,
    name: `qamatrix-entries-${at}`,
    title: `QA matrix probe feed ${at}`,
    description: `Disposable QA-cell probe feed for the feeds entries matrix (#6560).`,
    link: `http://localhost/${site}`,
    entries: [
      {
        path: `/${site}/matrix-entry-1.html`,
        title: `QA matrix probe item 1 ${at}`,
        abstract: `QA matrix probe entry 1 (#6560, ${at}).`,
      },
      {
        path: `/${site}/matrix-entry-2.html`,
        title: `QA matrix probe item 2 ${at}`,
        abstract: `QA matrix probe entry 2 (#6560, ${at}).`,
      },
    ],
  };
}

/**
 * {@code PUT /rss/descriptors} body (shape:
 * {@code PSFeedDescriptors}) for the probe feed. Mirrors the
 * {@code qa-seed-widget-fixtures} seeder payload
 * ({@code docker/scripts/qa_seed_widget_fixtures.py#feed_descriptor_payload},
 * #6341): the query is a JSON-encoded metadata query limiting rows to
 * the probe site.
 *
 * @param {{ site: string, name: string, title: string, description: string, link: string }} feed probe feed from {@link probeFeed}
 * @returns {object} descriptor PUT body
 */
function descriptorPayload(feed) {
  return {
    descriptors: [
      {
        name: feed.name,
        site: feed.site,
        description: feed.description,
        link: feed.link,
        title: feed.title,
        query: JSON.stringify({
          criteria: [`site = '${feed.site}'`],
          maxResults: 25,
          startIndex: 0,
        }),
        type: "RSS",
      },
    ],
    serviceUrl: "http://localhost/",
    serviceUser: "qamatrix",
    servicePass: "qamatrix",
    servicePassEncrypted: false,
    site: feed.site,
  };
}

/**
 * {@code POST /indexer/entry<path>} body (shape:
 * {@code PSMetadataEntry}) for one probe entry. Property values ride
 * on {@code value} (not {@code stringvalue}): the live services
 * deserialize through {@code JacksonXmlBindJsonProvider}, under which
 * {@code PSMetadataProperty.setStringvalue} is {@code @XmlTransient}
 * so a {@code stringvalue} field is silently dropped and the row
 * indexes with null properties (learning from #6341).
 *
 * @param {{ path: string, title: string, abstract: string }} entry probe entry from {@link probeFeed}
 * @param {string} site probe site from {@link probeFeed}
 * @returns {object} index entry POST body
 */
function indexEntryPayload(entry, site) {
  const name = entry.path.slice(entry.path.lastIndexOf("/") + 1);
  // URL path (always `/`), never a host filesystem path.
  const folder = `${entry.path.slice(0, entry.path.lastIndexOf("/") + 1)}`;
  return {
    name,
    folder,
    pagepath: entry.path,
    linktext: entry.title,
    type: "page",
    site,
    properties: [
      { name: "dcterms:title", value: entry.title },
      { name: "dcterms:abstract", value: entry.abstract },
    ],
  };
}

/**
 * Generated-feed URL for the probe feed, mirroring how the published
 * feed widget addresses the service
 * ({@code /rss/{site}/{feed}/localhost}).
 *
 * @param {string} serviceUrl feeds service base URL from {@link dtsServiceUrl}
 * @param {{ site: string, name: string }} feed probe feed from {@link probeFeed}
 * @returns {string} generated-feed URL
 */
function feedUrl(serviceUrl, feed) {
  return `${serviceUrl}/${feed.site}/${feed.name}/localhost`;
}

/**
 * Builds the one results-matrix row for the pinned feeds entry.
 * The row shape mirrors {@code contracts/results-matrix.schema.json};
 * see {@link validateEntriesRow}.
 *
 * @param {{ descriptorCreates: string, entriesServe: string, unseededRedirects: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildEntriesRow(caseResults, options = {}) {
  const status = caseResults[CASE_DESCRIPTOR] === "fail"
    || caseResults[CASE_ENTRIES] === "fail"
    || caseResults[CASE_UNSEEDED] === "fail"
    ? "fail"
    : caseResults[CASE_DESCRIPTOR] === "skip"
      || caseResults[CASE_ENTRIES] === "skip"
      || caseResults[CASE_UNSEEDED] === "skip"
      ? "skip"
      : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: FEEDS_ENTRY.name,
    label: FEEDS_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_DESCRIPTOR]: caseResults[CASE_DESCRIPTOR],
      [CASE_ENTRIES]: caseResults[CASE_ENTRIES],
      [CASE_UNSEEDED]: caseResults[CASE_UNSEEDED],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one feeds entries
 * results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateEntriesRowHeader(row) {
  const errors = [];
  const allowed = ["suite", "spec", "contentType", "label", "cell", "cases", "status", "updated"];
  for (const key of Object.keys(row)) {
    if (!allowed.includes(key)) {
      errors.push(`unexpected property '${key}'`);
    }
  }
  if (row.suite !== MATRIX_SUITE) {
    errors.push(`suite must be '${MATRIX_SUITE}'`);
  }
  if (typeof row.spec !== "string" || row.spec.length === 0) {
    errors.push("spec must be a non-empty string");
  }
  if (row.contentType !== FEEDS_ENTRY.name) {
    errors.push(`contentType must be '${FEEDS_ENTRY.name}'`);
  }
  if (typeof row.label !== "string" || row.label.length === 0) {
    errors.push("label must be a non-empty string");
  }
  if (!MATRIX_CELLS.includes(row.cell)) {
    errors.push(`cell must be one of ${MATRIX_CELLS.join(", ")}`);
  }
  return errors;
}

/**
 * Validates the per-case block of one feeds entries
 * results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateEntriesRowCases(cases) {
  if (cases == null || typeof cases !== "object" || Array.isArray(cases)) {
    return ["cases must be an object"];
  }
  const errors = [];
  for (const key of Object.keys(cases)) {
    if (!CASE_NAMES.includes(key)) {
      errors.push(`unexpected case '${key}'`);
    }
  }
  for (const key of CASE_NAMES) {
    if (!RESULT_STATUSES.includes(cases[key])) {
      errors.push(`cases.${key} must be one of ${RESULT_STATUSES.join(", ")}`);
    }
  }
  return errors;
}

/**
 * Validates one feeds entries results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateEntriesRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateEntriesRowHeader(row), ...validateEntriesRowCases(row.cases)];
  if (!RESULT_STATUSES.includes(row.status)) {
    errors.push(`status must be one of ${RESULT_STATUSES.join(", ")}`);
  }
  if (typeof row.updated !== "string" || !DATE_PATTERN.test(row.updated)) {
    errors.push("updated must be a YYYY-MM-DD date");
  }
  return errors;
}

/**
 * @param {string} moduleDir module root (has {@code contracts/} and {@code frontend/})
 * @returns {string} absolute path of the results-matrix JSON schema
 */
function schemaPathFor(moduleDir) {
  return path.join(String(moduleDir), "contracts", SCHEMA_FILE);
}

module.exports = {
  MATRIX_SUITE,
  MATRIX_SPEC,
  SCHEMA_FILE,
  EXAMPLE_FILE,
  CASE_DESCRIPTOR,
  CASE_ENTRIES,
  CASE_UNSEEDED,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  FEEDS_ENTRY,
  METADATA_CONTEXT,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  entryUrl,
  deliveryAuthHeader,
  probeFeed,
  descriptorPayload,
  indexEntryPayload,
  feedUrl,
  buildEntriesRow,
  validateEntriesRow,
  schemaPathFor,
};
