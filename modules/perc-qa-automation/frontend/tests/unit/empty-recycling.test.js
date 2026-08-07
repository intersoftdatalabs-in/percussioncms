/**
 * Unit tests for Empty Recycling pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  normalizePathItems,
  isAlreadyEmptyResult,
  purgedTotal,
  uniqueSeedFolderName,
  cmsUrl,
  isRecyclingListEmpty,
  recyclingHasName,
  emptyApiFailureMessage,
  SELECTORS,
  RECYCLE_EMPTY_PATH,
} = require("../helpers/empty-recycling");

describe("empty-recycling helpers", () => {
  it("exposes stable selectors and empty API path", () => {
    assert.equal(SELECTORS.emptyAction, '[data-testid="perc-finder-empty-recycling"]');
    assert.equal(SELECTORS.confirmDialog, "#perc-finder-empty-recycling-confirm");
    assert.equal(SELECTORS.confirmOk, "#perc-confirm-generic-ok");
    assert.equal(SELECTORS.confirmCancel, "#perc-confirm-generic-cancel");
    assert.match(RECYCLE_EMPTY_PATH, /\/pathmanagement\/recycle\/empty$/);
  });

  it("normalizePathItems handles array, PathItem wrapper, and empty", () => {
    assert.deepEqual(normalizePathItems(null), []);
    assert.deepEqual(normalizePathItems([]), []);
    assert.deepEqual(
      normalizePathItems([{ name: "a" }, null, { name: "b" }]).map((x) => x.name),
      ["a", "b"],
    );
    assert.deepEqual(
      normalizePathItems({ PathItem: [{ name: "Sites" }, { name: "Assets" }] }).map(
        (x) => x.name,
      ),
      ["Sites", "Assets"],
    );
    assert.deepEqual(normalizePathItems({ PathItem: { name: "only" } })[0].name, "only");
  });

  it("isAlreadyEmptyResult reads root and EmptyRecycleResult wrapper", () => {
    assert.equal(isAlreadyEmptyResult(null), false);
    assert.equal(isAlreadyEmptyResult({ alreadyEmpty: true }), true);
    assert.equal(
      isAlreadyEmptyResult({ EmptyRecycleResult: { alreadyEmpty: true } }),
      true,
    );
    assert.equal(isAlreadyEmptyResult({ alreadyEmpty: false }), false);
  });

  it("purgedTotal sums folder + item counts", () => {
    assert.equal(purgedTotal(null), 0);
    assert.equal(
      purgedTotal({ purgedFolderCount: 2, purgedItemCount: 3 }),
      5,
    );
    assert.equal(
      purgedTotal({
        EmptyRecycleResult: { purgedFolderCount: 1, purgedItemCount: 0 },
      }),
      1,
    );
  });

  it("uniqueSeedFolderName is ASCII-safe and unique", () => {
    const a = uniqueSeedFolderName("qa");
    const b = uniqueSeedFolderName("qa");
    assert.match(a, /^qa-/);
    assert.notEqual(a, b);
    assert.doesNotMatch(uniqueSeedFolderName("x y!"), /[!\s]/);
    assert.match(uniqueSeedFolderName("bad name!"), /^badname-/);
  });

  it("cmsUrl joins base and path without double slash", () => {
    assert.equal(
      cmsUrl("http://127.0.0.1:9993/", "/Rhythmyx/services/x"),
      "http://127.0.0.1:9993/Rhythmyx/services/x",
    );
    assert.equal(
      cmsUrl("http://localhost:9992", "Rhythmyx/y"),
      "http://localhost:9992/Rhythmyx/y",
    );
  });

  it("recycling list empty / has-name helpers", () => {
    assert.equal(isRecyclingListEmpty([]), true);
    assert.equal(isRecyclingListEmpty([{ name: "Sites" }]), false);
    assert.equal(
      recyclingHasName([{ name: "qa-folder-1" }, { path: "/Recycling/other" }], "qa-folder-1"),
      true,
    );
    assert.equal(recyclingHasName([{ name: "other" }], "missing"), false);
    // Exact match only — substring must not count as a hit.
    assert.equal(
      recyclingHasName([{ name: "qa-empty-recycl-xyz" }], "qa-empty-recycl-abc"),
      false,
    );
    assert.equal(
      recyclingHasName([{ path: "/Recycling/Assets/qa-folder-1" }], "qa-folder-1"),
      true,
    );
  });

  it("emptyApiFailureMessage points at #2205 for 404", () => {
    const msg = emptyApiFailureMessage({
      status: 404,
      body: "HTTP 404 Not Found",
    });
    assert.match(msg, /#2205/);
    assert.match(msg, /recycle\/empty/);
  });
});
