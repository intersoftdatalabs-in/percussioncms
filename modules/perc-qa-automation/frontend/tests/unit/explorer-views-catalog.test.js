/**
 * Unit tests for Explorer Views catalog smoke helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  PATH_VIEWS,
  explorerEntryUrl,
  viewsCatalogUrl,
  unwrapViewDefs,
  viewDefKey,
  isCustomUrlView,
  pickRunnableView,
} = require("../helpers/explorer-views-catalog");

describe("explorer-views-catalog helpers (#3116)", () => {
  it("exports stable test ids used by Views tree / results", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.viewsTree, "explorer-views-tree");
    assert.equal(TEST_IDS.group(1), "explorer-views-group-1");
    assert.equal(TEST_IDS.leaf("View_All"), "explorer-views-leaf-View_All");
    assert.equal(PATH_VIEWS, "/Rhythmyx/services/views");
  });

  it("explorerEntryUrl builds spa.jsp explorer entry with cache-buster", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
  });

  it("viewsCatalogUrl joins origin + /Rhythmyx/services/views", () => {
    assert.equal(
      viewsCatalogUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/Rhythmyx/services/views",
    );
  });

  it("unwrapViewDefs handles arrays and Jackson wrappers", () => {
    assert.deepEqual(unwrapViewDefs(null), []);
    assert.deepEqual(unwrapViewDefs([{ name: "A" }]), [{ name: "A" }]);
    assert.deepEqual(unwrapViewDefs({ ViewDef: { name: "B" } }), [{ name: "B" }]);
  });

  it("pickRunnableView skips custom URL views", () => {
    assert.equal(
      pickRunnableView([
        { name: "Inbox", customView: true },
        { name: "View_All", standardView: true },
      ]).name,
      "View_All",
    );
    assert.equal(isCustomUrlView({ customView: true }), true);
    assert.equal(viewDefKey({ name: "  X  " }), "X");
  });
});
