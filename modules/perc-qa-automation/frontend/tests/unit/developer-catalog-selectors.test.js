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
  catalogOpenByExactName,
  catalogRowByExactName,
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

describe("catalogOpenByExactName (#3269)", () => {
  it("targets By_Author open without matching By_Author_And_Date substring", () => {
    const sel = catalogOpenByExactName(
      "developer-df-open",
      "data-df-name",
      "By_Author",
    );
    assert.equal(
      sel,
      '[data-testid="developer-df-open"][data-df-name="By_Author"]',
    );
    assert.ok(!sel.includes("hasText"));
    const peer = catalogOpenByExactName(
      "developer-df-open",
      "data-df-name",
      "By_Author_And_Date",
    );
    assert.notEqual(sel, peer);
  });

  it("rejects empty name or non data-* attr", () => {
    assert.throws(
      () => catalogOpenByExactName("developer-df-open", "data-df-name", ""),
      TypeError,
    );
    assert.throws(
      () => catalogOpenByExactName("developer-df-open", "df-name", "By_Author"),
      TypeError,
    );
  });
});

describe("catalogRowByExactName (#3269)", () => {
  it("targets the unique data-df-name row", () => {
    assert.equal(
      catalogRowByExactName("data-df-name", "By_Author"),
      'tr[data-df-name="By_Author"]',
    );
  });
});
