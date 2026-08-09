/**
 * Unit tests for Explorer saved-search pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  PATH_SEARCHES,
  explorerEntryUrl,
  searchesCatalogUrl,
  searchesExecuteUrl,
  unwrapSearchDefs,
  searchDefKey,
  searchDefLabel,
  isCustomUrlSearch,
  pickRunnableSavedSearch,
  isCatalogSettled,
  noRunnableSearchSkipMessage,
  postExecuteRegionSelector,
  catalogSettledSelector,
} = require("../helpers/explorer-saved-search");

describe("explorer-saved-search helpers (#2507)", () => {
  it("exports stable test ids used by SearchPanel / shell", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.toggleSearch, "explorer-toggle-search");
    assert.equal(TEST_IDS.savedSelect, "search-panel-saved-select");
    assert.equal(TEST_IDS.savedRun, "search-panel-saved-run");
    assert.equal(TEST_IDS.resultsList, "search-panel-results");
    assert.equal(PATH_SEARCHES, "/Rhythmyx/services/searches");
  });

  it("explorerEntryUrl builds spa.jsp explorer entry with cache-buster", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    assert.equal(
      explorerEntryUrl("http://localhost:9992/", { cacheBuster: "a b" }),
      "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=a%20b",
    );
  });

  it("searchesCatalogUrl and searchesExecuteUrl encode idOrName", () => {
    assert.equal(
      searchesCatalogUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/Rhythmyx/services/searches",
    );
    assert.equal(
      searchesExecuteUrl("http://cms.example", "All Content"),
      "http://cms.example/Rhythmyx/services/searches/All%20Content/execute",
    );
  });

  it("unwrapSearchDefs handles arrays and Jackson wrappers", () => {
    assert.deepEqual(unwrapSearchDefs(null), []);
    assert.deepEqual(unwrapSearchDefs([{ name: "A" }]), [{ name: "A" }]);
    assert.deepEqual(unwrapSearchDefs({ SearchDef: { name: "B" } }), [
      { name: "B" },
    ]);
    assert.deepEqual(
      unwrapSearchDefs({ searchDef: [{ name: "C" }, { name: "D" }] }),
      [{ name: "C" }, { name: "D" }],
    );
  });

  it("searchDefKey prefers name then id", () => {
    assert.equal(searchDefKey(null), "");
    assert.equal(searchDefKey({ name: "All Content" }), "All Content");
    assert.equal(searchDefKey({ id: "42" }), "42");
    assert.equal(searchDefKey({ name: "  n  ", id: "1" }), "n");
  });

  it("searchDefLabel prefers label/displayName then key", () => {
    assert.equal(searchDefLabel({ name: "x", label: "Label X" }), "Label X");
    assert.equal(
      searchDefLabel({ name: "x", displayName: "Display X" }),
      "Display X",
    );
    assert.equal(searchDefLabel({ name: "fallback" }), "fallback");
  });

  it("isCustomUrlSearch detects customSearch flags", () => {
    assert.equal(isCustomUrlSearch({ customSearch: true }), true);
    assert.equal(isCustomUrlSearch({ customSearch: "true" }), true);
    assert.equal(isCustomUrlSearch({ customSearch: false }), false);
    assert.equal(isCustomUrlSearch({}), false);
  });

  it("pickRunnableSavedSearch skips custom URL entries", () => {
    assert.equal(pickRunnableSavedSearch([]), null);
    assert.equal(
      pickRunnableSavedSearch([{ name: "URL only", customSearch: true }]),
      null,
    );
    const picked = pickRunnableSavedSearch([
      { name: "Custom", customSearch: true },
      { name: "All Content", label: "All" },
      { name: "My Pages" },
    ]);
    assert.ok(picked);
    assert.equal(picked.key, "All Content");
    assert.equal(picked.label, "All");
  });

  it("isCatalogSettled and soft-skip message", () => {
    assert.equal(isCatalogSettled("loading"), false);
    assert.equal(isCatalogSettled("picker"), true);
    assert.equal(isCatalogSettled("empty"), true);
    assert.equal(isCatalogSettled("error"), true);
    const msg = noRunnableSearchSkipMessage({ empty: true, restStatus: 200 });
    assert.match(msg, /#2507/);
    assert.match(msg, /soft skip/i);
    assert.match(msg, /empty/i);
    assert.match(msg, /200/);
  });

  it("region selectors include key test ids", () => {
    assert.match(postExecuteRegionSelector(), /search-panel-results/);
    assert.match(postExecuteRegionSelector(), /search-panel-empty/);
    assert.match(catalogSettledSelector(), /search-panel-saved-picker/);
    assert.match(catalogSettledSelector(), /search-panel-saved-empty/);
  });
});
