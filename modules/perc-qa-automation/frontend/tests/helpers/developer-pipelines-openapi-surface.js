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
 * Developer Pipelines OpenAPI surface helpers (#4384 / parent #1690).
 * Catalog tokens only — not filesystem paths. No live CMS.
 */

"use strict";

const OPENAPI_CANDIDATE_LIMIT = 8;

const TEST_IDS = Object.freeze({
  panel: "developer-pipe-panel",
  table: "developer-pipe-table",
  open: "developer-pipe-open",
  detail: "developer-pipe-detail",
  back: "developer-pipe-back",
  ir: "developer-pipe-ir",
  irEmpty: "developer-pipe-ir-empty",
  irResources: "developer-pipe-ir-resources",
  openApi: "developer-pipe-openapi",
  openApiDoc: "developer-pipe-openapi-doc",
  openApiError: "developer-pipe-openapi-error",
  openApiLoading: "developer-pipe-openapi-loading",
  openApiView: "developer-pipe-openapi-view",
  openApiDownload: "developer-pipe-openapi-download",
});

/**
 * Rank catalog names so IR/execute apps are tried before empty-IR first-8
 * rows such as sys_ActionPage (#4384 Cycle Verify).
 *
 * @param {string[]} names
 * @returns {string[]}
 */
function rankPipelineNamesForOpenApi(names) {
  if (!Array.isArray(names)) {
    throw new TypeError("names must be an array of catalog tokens");
  }
  const unique = [];
  const seen = new Set();
  for (const raw of names) {
    if (typeof raw !== "string") continue;
    const name = raw.trim();
    if (!name) continue;
    const key = name.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    unique.push(name);
  }
  function score(n) {
    if (/^sys_cmp/i.test(n)) return 0;
    if (/^sys_cx/i.test(n)) return 1;
    if (/actionpage/i.test(n)) return 90;
    if (/^sys_/i.test(n)) return 20;
    return 10;
  }
  return unique.slice().sort((a, b) => {
    const d = score(a) - score(b);
    if (d !== 0) return d;
    return a.localeCompare(b);
  });
}

/**
 * Cap ranked names for live catalog walks (PIPELINE_APP_NAME is exclusive).
 *
 * @param {string[]} names
 * @param {string} [preferred]
 * @param {number} [limit]
 * @returns {string[]}
 */
function openApiCandidateNames(names, preferred, limit) {
  const want = (preferred || "").trim();
  if (want) {
    return [want];
  }
  const cap =
    Number.isInteger(limit) && limit > 0 ? limit : OPENAPI_CANDIDATE_LIMIT;
  return rankPipelineNamesForOpenApi(names).slice(0, cap);
}

/**
 * True when OpenAPI text documents a resource execute path.
 *
 * @param {string} body
 * @returns {boolean}
 */
function openApiHasResourceExecutePath(body) {
  return /\/pipelines\/.+\/resources\/.+\/execute/.test(String(body || ""));
}

/**
 * Match GET /services/pipelines/{app}/openapi for one catalog token.
 *
 * @param {string} url
 * @param {string} appName
 * @returns {boolean}
 */
function isPipelineOpenApiGetUrl(url, appName) {
  const name = (appName || "").trim();
  if (!name) return false;
  const encoded = encodeURIComponent(name);
  const re = new RegExp(
    `/services/pipelines/(?:${encoded}|${name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})/openapi(?:\\?|$)`,
    "i",
  );
  return re.test(String(url || ""));
}

/**
 * Absolute REST URL for OpenAPI YAML. Uses URL origin + services root
 * (not filesystem joins).
 *
 * @param {string} pageOrBaseUrl spa or TEST_CMS_URL
 * @param {string} appName catalog token
 * @returns {string}
 */
function pipelineOpenApiRestUrl(pageOrBaseUrl, appName) {
  const name = (appName || "").trim();
  if (!name) {
    throw new TypeError("appName must be a non-empty catalog token");
  }
  const raw = String(pageOrBaseUrl || "").trim();
  if (!raw) {
    throw new TypeError("pageOrBaseUrl must be a non-empty URL");
  }
  const abs = /^[a-z][a-z0-9+.-]*:/i.test(raw) ? raw : `http://${raw}`;
  const u = new URL(abs);
  const servicesRoot = u.pathname.startsWith("/Rhythmyx")
    ? "/Rhythmyx/services"
    : "/services";
  const path = `${servicesRoot}/pipelines/${encodeURIComponent(name)}/openapi`;
  return new URL(path, u.origin).toString() + "?format=yaml";
}

function staleOpenApiChromeMessage(tried) {
  const list = (tried || []).join(", ") || "(none)";
  return (
    "OpenAPI chrome (developer-pipe-openapi) missing on tried catalog apps: " +
    list +
    ". Deploy Slice C SPA (perc-devctl qa-deploy-webui of current WebUI " +
    "cm/modern); skip-image-build cells without that copy omit the section."
  );
}

function noExecutePathMessage(tried) {
  const list = (tried || []).join(", ") || "(none)";
  return (
    "No catalog pipeline returned OpenAPI with a resource execute path " +
    "(tried " +
    list +
    "). Set PIPELINE_APP_NAME to a sys_cmp* IR/execute app."
  );
}

module.exports = {
  OPENAPI_CANDIDATE_LIMIT,
  TEST_IDS,
  rankPipelineNamesForOpenApi,
  openApiCandidateNames,
  openApiHasResourceExecutePath,
  isPipelineOpenApiGetUrl,
  pipelineOpenApiRestUrl,
  staleOpenApiChromeMessage,
  noExecutePathMessage,
};
