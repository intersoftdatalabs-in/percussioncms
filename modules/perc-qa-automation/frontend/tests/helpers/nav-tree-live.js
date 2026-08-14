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
 * Parsers for live Navigation tree Playwright (#3218).
 * No I/O — unit-tested without a CMS.
 */

function siteNamesFromPayload(payload) {
  if (payload == null || typeof payload !== "object") {
    return [];
  }
  const raw = payload.SiteSummary || payload.siteSummary || payload;
  const list = Array.isArray(raw) ? raw : raw ? [raw] : [];
  return list
    .map((row) => (row && (row.name || row.Name || row.id)) || "")
    .map((name) => String(name).trim())
    .filter(Boolean);
}

/** Seeded H2 / installer demo site names that must have a NavTree (#3352). */
const SAMPLE_DEMO_SITE_NAMES = [
  "Corporate_Investments",
  "Enterprise_Investments",
];

function isSampleDemoSite(name) {
  const key = String(name || "").trim().toLowerCase();
  if (!key) {
    return false;
  }
  return SAMPLE_DEMO_SITE_NAMES.some((n) => n.toLowerCase() === key);
}

function isEmptyTreePayload(bodyText) {
  if (bodyText == null || String(bodyText).trim() === "") {
    return true;
  }
  let json;
  try {
    json = JSON.parse(bodyText);
  } catch {
    return false;
  }
  if (!json || typeof json !== "object" || Array.isArray(json)) {
    return false;
  }
  const node = json.SectionNode || json.sectionNode;
  if (!node || typeof node !== "object" || Array.isArray(node)) {
    return false;
  }
  const rawId = node.id;
  const id = Array.isArray(rawId)
    ? String(rawId[0] || "").trim()
    : rawId != null
      ? String(rawId).trim()
      : "";
  const children = node.childNodes;
  const noChildren =
    children == null ||
    children === "" ||
    (Array.isArray(children) && children.length === 0);
  return !id && noChildren;
}

module.exports = {
  SAMPLE_DEMO_SITE_NAMES,
  siteNamesFromPayload,
  isSampleDemoSite,
  isEmptyTreePayload,
};
