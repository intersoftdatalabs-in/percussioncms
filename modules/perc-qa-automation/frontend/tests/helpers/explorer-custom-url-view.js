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

"use strict";

/**
 * Helpers for the unmocked Explorer custom-URL view surface (#4834).
 */

function unwrapViewDefs(payload) {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload !== "object") return [];
  const raw = payload.ViewDef ?? payload.viewDef ?? payload.ViewDefList;
  if (raw == null) return [];
  if (Array.isArray(raw)) return raw;
  if (typeof raw === "object") {
    const nested = raw.ViewDef ?? raw.viewDef;
    if (Array.isArray(nested)) return nested;
    if (nested != null && typeof nested === "object") return [nested];
  }
  return [raw];
}

function isNonInboxCustom(def) {
  if (!def || def.customView !== true) return false;
  const name = String(def.name || "").trim().toLowerCase();
  const label = String(def.label || "").trim().toLowerCase();
  if (name === "inbox" || label === "inbox") return false;
  return name.length > 0;
}

function pickNonInboxCustomView(defs) {
  const list = Array.isArray(defs) ? defs : [];
  return (
    list.find((d) => isNonInboxCustom(d) && /^outbox$/i.test(String(d.name || ""))) ||
    list.find((d) => isNonInboxCustom(d)) ||
    null
  );
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * True only for POST /services/views/{exactName}/execute.
 * A longer sibling name must not match.
 *
 * @param {string} url
 * @param {string} viewName
 * @returns {boolean}
 */
function isNamedViewExecute(url, viewName) {
  const name = String(viewName || "").trim();
  if (!name) return false;
  let decoded = String(url || "");
  try {
    decoded = decodeURIComponent(decoded);
  } catch {
    return false;
  }
  const re = new RegExp(
    `/services/views/${escapeRegExp(name)}/execute(?:\\?|$)`,
    "i",
  );
  return re.test(decoded);
}

module.exports = {
  unwrapViewDefs,
  isNonInboxCustom,
  pickNonInboxCustomView,
  isNamedViewExecute,
};
