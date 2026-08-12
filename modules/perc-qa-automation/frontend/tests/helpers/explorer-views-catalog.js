/**
 * Explorer Views catalog Playwright helpers (#3116 / parent #3110).
 *
 * Compact C5 smoke helpers only. Full Views surface + a11y suite is #3117.
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  nav: "explorer-nav",
  viewsTree: "explorer-views-tree",
  viewsRoot: "explorer-views-root",
  group: (n) => `explorer-views-group-${n}`,
  groupRow: (n) => `explorer-views-group-${n}-row`,
  leaf: (key) => `explorer-views-leaf-${key}`,
  inbox: "explorer-views-inbox",
  inboxLeaf: "explorer-views-leaf-Inbox",
  inboxIcon: "explorer-views-inbox-icon",
  results: "explorer-view-results",
  resultsList: "explorer-view-results-list",
  resultsEmpty: "explorer-view-results-empty",
  resultsError: "explorer-view-results-error",
  resultsLoading: "explorer-view-results-loading",
});

const PATH_VIEWS = "/Rhythmyx/services/views";

function explorerEntryUrl(baseUrl, opts = {}) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const bust =
    opts.cacheBuster != null ? String(opts.cacheBuster) : String(Date.now());
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${encodeURIComponent(bust)}`;
}

function viewsCatalogUrl(baseUrl) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  return `${root}${PATH_VIEWS}`;
}

function unwrapViewDefs(payload) {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload === "object") {
    const raw = payload.ViewDef ?? payload.viewDef ?? payload.ViewDefList;
    if (raw == null) return [];
    return Array.isArray(raw) ? raw : [raw];
  }
  return [];
}

function viewDefKey(def) {
  if (def == null || typeof def !== "object") return "";
  const name = typeof def.name === "string" ? def.name.trim() : "";
  if (name) return name;
  if (def.id != null && String(def.id).trim()) return String(def.id).trim();
  return "";
}

function isCustomUrlView(def) {
  return !!(def && def.customView === true);
}

function isInboxView(def) {
  if (def == null || typeof def !== "object") return false;
  const name = typeof def.name === "string" ? def.name.trim() : "";
  const label = typeof def.label === "string" ? def.label.trim() : "";
  if (name.toLowerCase() === "inbox" || label.toLowerCase() === "inbox") {
    return true;
  }
  const key = viewDefKey(def).replace(/\\/g, "/");
  return /\/\/Views\/\/MyContent\/Inbox$/i.test(key);
}

function pickRunnableView(defs) {
  const list = Array.isArray(defs) ? defs : [];
  return list.find((d) => viewDefKey(d) && !isCustomUrlView(d)) || null;
}

module.exports = {
  TEST_IDS,
  PATH_VIEWS,
  explorerEntryUrl,
  viewsCatalogUrl,
  unwrapViewDefs,
  viewDefKey,
  isCustomUrlView,
  isInboxView,
  pickRunnableView,
};
