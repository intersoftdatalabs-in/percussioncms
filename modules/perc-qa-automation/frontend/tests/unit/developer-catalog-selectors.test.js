/**
 * Unit tests for Developer catalog indexed row selectors (#2186).
 * Aligns Playwright with WebUI SimpleCatalogTable / Vitest contract.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  catalogRowsSelector,
  catalogRowSelector,
} = require("../helpers/developer-catalog-selectors");

describe("catalogRowsSelector", () => {
  it("matches WebUI indexed content-type rows (not bare developer-ct-row)", () => {
    assert.equal(
      catalogRowsSelector("developer-ct-row"),
      '[data-testid^="developer-ct-row-"]',
    );
  });

  it("matches WebUI indexed template rows", () => {
    assert.equal(
      catalogRowsSelector("developer-tpl-row"),
      '[data-testid^="developer-tpl-row-"]',
    );
  });

  it("rejects empty base", () => {
    assert.throws(() => catalogRowsSelector(""), TypeError);
    assert.throws(() => catalogRowsSelector("   "), TypeError);
  });
});

describe("catalogRowSelector", () => {
  it("targets first row index used by Vitest (developer-ct-row-0)", () => {
    assert.equal(
      catalogRowSelector("developer-ct-row", 0),
      '[data-testid="developer-ct-row-0"]',
    );
  });

  it("targets template first row open control parent", () => {
    assert.equal(
      catalogRowSelector("developer-tpl-row", 0),
      '[data-testid="developer-tpl-row-0"]',
    );
  });

  it("rejects negative or non-integer index", () => {
    assert.throws(() => catalogRowSelector("developer-ct-row", -1), TypeError);
    assert.throws(() => catalogRowSelector("developer-ct-row", 1.5), TypeError);
  });
});
