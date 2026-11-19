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
 * Closed inventory + results-matrix row helpers for the US3 metadata
 * index-search matrix (#6560 / parent #6241): prove the metadata
 * index-search round-trip against the in-cell DTS metadata service — a
 * self-seeded probe index row is retrievable through the index query
 * ({@code POST <context>/metadata/get}), and removing it takes it back
 * out of the query results.
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code indexQueryAnswers} — the query surface:
 *   {@code POST <context>/metadata/get} with an explicit quoted
 *   {@code site = '...'} criterion answers 200 with a result list shape
 *   (the fixed behavior — non-null bodies used to 302 to the error
 *   page, #6343). No seeding needed: an empty index still answers the
 *   shape.</li>
 *   <li>{@code seededEntryRetrievable} — the index surface: one probe
 *   row seeded through the extractor
 *   ({@code POST <context-level>/indexer/entry<path>} as
 *   {@code deliverymanager}) is contained in the re-queried results,
 *   then removed so the disposable cell stays clean.</li>
 *   <li>{@code cleanupRemoves} — the removal surface: a second probe
 *   row, seeded then removed
 *   ({@code DELETE <context-level>/indexer/entry<path>}), no longer
 *   appears in the re-queried results.</li>
 * </ul>
 *
 * <p>Each Playwright case seeds its own unique probe row
 * (timestamp-derived page path, see {@link probeEntry}) under the fixed
 * probe site, so cases stay independent under parallel workers and
 * reruns never collide. The extractor resource lives at the
 * context-level {@code /indexer} root, not under the {@code /metadata}
 * JAX-RS root used by the query endpoints.</p>
 *
 * <p>Skip contract (mirrors the US3 polls matrix): without
 * {@code DTS_BASE_URL} every case skips naming the missing cell (#6237,
 * green on the CMS-only cell); with a base URL each case probes
 * liveness ({@code HEAD <context>/metadata/csrf} — the service lives
 * under its Tomcat deployment context, issue #6342) before touching
 * the service. Writer calls (the index seed and cleanup) ride the
 * published DTS HTTPS origin (see {@link dtsHttpsBaseUrl} — the
 * extractor is CONFIDENTIAL, #6375) through an
 * untrusted-chain-allowed request context; a cell that publishes no
 * HTTPS origin skips the seeding cases naming {@code DTS_HTTPS_URL}
 * instead of failing. A cell whose HTTPS writes are auth-gated skips
 * the seeding cases with a named reason instead of failing (see
 * {@link skipUnprovisioned}).</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "metadata-index-matrix";
const MATRIX_SPEC = "tests/metadata-index-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-metadata-index.example.json";

const CASE_QUERY = "indexQueryAnswers";
const CASE_SEEDED = "seededEntryRetrievable";
const CASE_CLEANUP = "cleanupRemoves";
const CASE_NAMES = Object.freeze([CASE_QUERY, CASE_SEEDED, CASE_CLEANUP]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Probe site shared by every case of this suite (rows keyed by unique page path). */
const PROBE_SITE = "qamatrix-index";

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the cell serves no DTS HTTPS origin (default H2 QA
 * cell and pre-#6375 cells). The extractor seed path is CONFIDENTIAL,
 * so host-side self-seed writes ride the published HTTPS origin from
 * the {@code qa-up --with-dts} banner ({@code DTS_HTTPS_URL});
 * plain-HTTP seeds 302 to the unpublished HTTPS port instead of
 * writing (#6375).
 */
const SKIP_NO_DTS_HTTPS =
  "needs DTS_HTTPS_URL=(in-cell DTS HTTPS origin from the qa-up --with-dts banner) " +
  "or DTS_HTTPS_HOST_PORT=(published host port for the in-cell 8443); " +
  "plain-HTTP seeds 302 to the unpublished HTTPS port (#6375)";

/**
 * Skip reason when the DTS origin answers but the metadata service
 * does not. A 401/403 still counts as "served" (the service is up, the
 * caller lacks the role); only unreachable / 404 probes land here. The
 * probed endpoint is the deployment context path (war name) plus the
 * JAX-RS root — the service is served under a Tomcat context (see
 * {@link METADATA_ENTRY}), not at the DTS origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `metadata index (Metadata): no liveness answer at ` +
    `${baseUrl}/${METADATA_ENTRY.context}${METADATA_ENTRY.csrf}; the DTS cell does not serve ` +
    `${METADATA_ENTRY.context}${METADATA_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the service is up but the cell cannot persist the
 * probe index row. Each skip names the exact gap so the cell slice can
 * close it without touching the spec.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipUnprovisioned(detail) {
  return (
    `metadata index (Metadata): ${detail}; ` +
    `the cell must serve a writable metadata index (deliverymanager role ` +
    `on the extractor plus a reachable DTS HTTPS port for the ` +
    `CONFIDENTIAL seed path) to turn these cases green (parent #6241, ` +
    `delivery credentials via DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD)`
  );
}

/**
 * The pinned metadata service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code perc-metadata-services});
 * {@code root} is the JAX-RS root; {@code csrf} the {@code HEAD}
 * liveness probe. Index rows ride the extractor at the context-level
 * {@code /indexer} root (see {@link indexerServiceUrl}).
 */
const METADATA_ENTRY = Object.freeze({
  name: "metadata",
  label: "Metadata",
  context: "perc-metadata-services",
  root: "/metadata",
  csrf: "/metadata/csrf",
  widget: null,
});

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
 * In-cell DTS HTTPS origin for the CONFIDENTIAL extractor writer paths
 * (the context-level {@code /indexer} seed and cleanup), or null when
 * the cell publishes no HTTPS origin. Resolution mirrors the freeport
 * contract: explicit {@code DTS_HTTPS_URL} wins, else
 * {@code DTS_HTTPS_HOST_PORT} / {@code DTS_HTTPS_PORT} build
 * {@code https://127.0.0.1:<port>} — the same loopback convention as
 * {@link dtsBaseUrl}. The cell TLS identity is shipped/self-signed, so
 * callers must allow the untrusted chain (Playwright
 * {@code ignoreHTTPSErrors}, same as
 * {@code tests/helpers/published-widget-surfaces.js#dtsHttpsBaseUrl}).
 *
 * @returns {URL|null} in-cell DTS HTTPS origin for host-side writer calls
 */
function dtsHttpsBaseUrl() {
  const explicit = process.env.DTS_HTTPS_URL || "";
  if (explicit) {
    return new URL(explicit);
  }
  const port =
    process.env.DTS_HTTPS_HOST_PORT || process.env.DTS_HTTPS_PORT || "";
  if (!port) {
    return null;
  }
  return new URL(`https://127.0.0.1:${port}`);
}

/**
 * Deployment context URL for the metadata service: the DTS origin
 * plus the deployment context path (war name), without the JAX-RS root.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${METADATA_ENTRY.context}`;
}

/**
 * Service base URL for the metadata index cases: the DTS
 * origin plus the deployment context path (war name) plus the JAX-RS
 * root, mirroring how the metadata consumers address the service.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${METADATA_ENTRY.root}`;
}

/**
 * Metadata extractor (indexer) service root for the probe index rows:
 * the DTS origin plus the deployment context path (war name) plus the
 * context-level {@code /indexer} root — the extractor resource lives
 * at the context level, not under the {@code /metadata} JAX-RS root
 * used by the query endpoints.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} indexer service root (no trailing slash)
 */
function indexerServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}/indexer`;
}

/**
 * Extractor entry URL for one probe page path: the indexer root plus
 * {@code /entry/} plus the page path with its leading slash kept, so
 * the request line carries the double slash
 * ({@code /indexer/entry//qamatrix-index/...}). The extractor binds
 * {@code /entry/{pagePath:.*}} and the DAO matches {@code pagepath}
 * verbatim including the leading slash, so the single-slash form
 * ({@code /indexer/entry/qamatrix-index/...}) binds a slashless path
 * and DELETEs nothing while answering 204 — the silent-litter trap
 * (#6557 trap 2). The single choke point keeps seed and cleanup on the
 * supported contract (same as
 * {@code docker/scripts/qa_seed_widget_fixtures.py#entry_url}).
 *
 * @param {string} indexerSvc indexer service root from {@link indexerServiceUrl}
 * @param {string} pagePath probe page path with its leading slash (see {@link probeEntry})
 * @returns {string} entry URL with the doubled slash
 */
function entryUrl(indexerSvc, pagePath) {
  return `${indexerSvc}/entry/${pagePath}`;
}

/**
 * BASIC credentials for the role-protected DTS extractor endpoints
 * (the context-level {@code /indexer/entry/...} surface,
 * {@code deliverymanager} role). Defaults are the shipped DTS
 * development defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties}
 * and the installed {@code conf/tomcat-users.xml}); override with env
 * when the cell provisions its own. Note the shipped {@code web.xml}
 * marks the extractor {@code CONFIDENTIAL}, so plain-HTTP cells answer
 * the seed with a redirect to the (usually unpublished) HTTPS port and
 * the seed block skips by name. The credential lives in the helper and
 * the spec sends it only on the writer calls — the index queries send
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
 * Unique self-seeded probe index row for one index round-trip. The
 * page path is timestamp-derived so parallel workers and reruns never
 * collide; the site is the fixed suite probe site so every case
 * queries the same namespace.
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {{ path: string, title: string, abstract: string, site: string }} probe entry
 */
function probeEntry(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  return {
    path: `/qamatrix-index/matrix-${at}.html`,
    title: `QA matrix probe entry ${at}`,
    abstract: `QA matrix probe index row (#6560, ${at}).`,
    site: PROBE_SITE,
  };
}

/**
 * {@code POST /indexer/entry<path>} body (shape:
 * {@code PSMetadataEntry}) for the probe row. Property values ride on
 * {@code value} (not {@code stringvalue}): the live services
 * deserialize through {@code JacksonXmlBindJsonProvider}, under which
 * {@code PSMetadataProperty.setStringvalue} is {@code @XmlTransient}
 * so a {@code stringvalue} field is silently dropped and the row
 * indexes with null properties (learning from #6341).
 *
 * @param {{ path: string, title: string, abstract: string, site: string }} entry probe entry from {@link probeEntry}
 * @returns {object} index entry POST body
 */
function entryPayload(entry) {
  const name = entry.path.slice(entry.path.lastIndexOf("/") + 1);
  // URL path (always `/`), never a host filesystem path.
  const folder = `${entry.path.slice(0, entry.path.lastIndexOf("/") + 1)}`;
  return {
    name,
    folder,
    pagepath: entry.path,
    linktext: entry.title,
    type: "page",
    site: entry.site,
    properties: [
      { name: "dcterms:title", value: entry.title },
      { name: "dcterms:abstract", value: entry.abstract },
    ],
  };
}

/**
 * {@code POST /metadata/get} body limiting rows to the probe site.
 * Criteria need an explicit operator with quoted string values
 * ({@code PSCriteriaElement}); the legacy "site:xyz" shorthand is
 * malformed.
 *
 * @returns {object} index query POST body
 */
function siteQueryBody() {
  return {
    criteria: [`site = '${PROBE_SITE}'`],
    maxResults: 25,
    startIndex: 0,
  };
}

/**
 * Builds the one results-matrix row for the pinned metadata entry.
 * The row shape mirrors {@code contracts/results-matrix.schema.json};
 * see {@link validateIndexRow}.
 *
 * @param {{ indexQueryAnswers: string, seededEntryRetrievable: string, cleanupRemoves: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildIndexRow(caseResults, options = {}) {
  const status = caseResults[CASE_QUERY] === "fail"
    || caseResults[CASE_SEEDED] === "fail"
    || caseResults[CASE_CLEANUP] === "fail"
    ? "fail"
    : caseResults[CASE_QUERY] === "skip"
      || caseResults[CASE_SEEDED] === "skip"
      || caseResults[CASE_CLEANUP] === "skip"
      ? "skip"
      : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: METADATA_ENTRY.name,
    label: METADATA_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_QUERY]: caseResults[CASE_QUERY],
      [CASE_SEEDED]: caseResults[CASE_SEEDED],
      [CASE_CLEANUP]: caseResults[CASE_CLEANUP],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one metadata index
 * results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateIndexRowHeader(row) {
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
  if (row.contentType !== METADATA_ENTRY.name) {
    errors.push(`contentType must be '${METADATA_ENTRY.name}'`);
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
 * Validates the per-case block of one metadata index
 * results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateIndexRowCases(cases) {
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
 * Validates one metadata index results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateIndexRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateIndexRowHeader(row), ...validateIndexRowCases(row.cases)];
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
  CASE_QUERY,
  CASE_SEEDED,
  CASE_CLEANUP,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  PROBE_SITE,
  SKIP_NO_DTS_BASE,
  SKIP_NO_DTS_HTTPS,
  skipServiceDown,
  skipUnprovisioned,
  METADATA_ENTRY,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  dtsHttpsBaseUrl,
  entryUrl,
  deliveryAuthHeader,
  probeEntry,
  entryPayload,
  siteQueryBody,
  buildIndexRow,
  validateIndexRow,
  schemaPathFor,
};
