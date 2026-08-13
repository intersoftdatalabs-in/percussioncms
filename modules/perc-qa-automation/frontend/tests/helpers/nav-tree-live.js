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
  const node = json.SectionNode || json.sectionNode || json;
  if (!node || typeof node !== "object") {
    return true;
  }
  const id = node.id != null ? String(node.id).trim() : "";
  const children = node.childNodes;
  const noChildren =
    children == null ||
    children === "" ||
    (Array.isArray(children) && children.length === 0);
  return !id && noChildren;
}

module.exports = {
  siteNamesFromPayload,
  isEmptyTreePayload,
};
