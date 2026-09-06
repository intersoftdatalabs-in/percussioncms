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
 * Developer Problems panel surface helpers (#4345 / parent #1690).
 * No live CMS. Catalog tokens only (not filesystem paths).
 */

"use strict";

const TEST_IDS = Object.freeze({
  tab: "tab-developer-problems",
  panel: "developer-prob-panel",
  empty: "developer-prob-empty",
  error: "developer-prob-error",
  loading: "developer-prob-loading",
  table: "developer-prob-table",
  row: "developer-prob-row",
  message: "developer-prob-message",
  navigate: "developer-prob-navigate",
  contentTypesTab: "tab-developer-content-types",
});

const SAFE_ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;
const INVALID_SESSION_FIXTURE = "invalid-session";

/**
 * @param {string} baseUrl TEST_CMS_URL / BASE_URL
 * @returns {string}
 */
function developerProblemsUrl(baseUrl) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const q = new URLSearchParams({
    entry: "developer",
    section: "problems",
    _: String(Date.now()),
  });
  return `${base}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Admin Problems REST catalog URL. Context defaults to {@code Rhythmyx}.
 *
 * @param {string} baseUrl TEST_CMS_URL / BASE_URL (scheme+host[+port], no path)
 * @returns {string}
 */
function developerProblemsRestUrl(baseUrl) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  if (/\/services\/problems$/i.test(base)) {
    return base;
  }
  const raw = process.env.CMS_WEBAPP_CONTEXT;
  const ctx =
    raw === undefined || raw === null
      ? "Rhythmyx"
      : String(raw).replace(/^\/+|\/+$/g, "");
  if (!ctx) {
    return `${base}/services/problems`;
  }
  return `${base}/${ctx}/services/problems`;
}

/**
 * Unwrap Jackson list or `{ DesignProblem: [...] }` catalog payload.
 * @param {unknown} payload
 * @returns {Array<{ id: string, message?: string, navigateSection?: string }>}
 */
function unwrapDesignProblems(payload) {
  if (payload == null) return [];
  let raw = payload;
  if (!Array.isArray(raw) && typeof raw === "object") {
    const rec = /** @type {Record<string, unknown>} */ (raw);
    if (Array.isArray(rec.DesignProblem)) {
      raw = rec.DesignProblem;
    } else if (rec.DesignProblem && typeof rec.DesignProblem === "object") {
      raw = [rec.DesignProblem];
    } else if (Array.isArray(rec.designProblem)) {
      raw = rec.designProblem;
    } else if (typeof rec.id === "string") {
      raw = [rec];
    } else {
      return [];
    }
  }
  if (!Array.isArray(raw)) return [];
  const out = [];
  for (const item of raw) {
    if (!item || typeof item !== "object") continue;
    const id = typeof item.id === "string" ? item.id.trim() : "";
    if (!id || !SAFE_ID_RE.test(id)) continue;
    out.push(item);
  }
  return out;
}

/**
 * @param {string[]} consoleErrors
 * @returns {string[]}
 */
function unexpectedConsoleErrors(consoleErrors) {
  if (!Array.isArray(consoleErrors)) return [];
  return consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
}

module.exports = {
  TEST_IDS,
  SAFE_ID_RE,
  INVALID_SESSION_FIXTURE,
  developerProblemsUrl,
  developerProblemsRestUrl,
  unwrapDesignProblems,
  unexpectedConsoleErrors,
};
