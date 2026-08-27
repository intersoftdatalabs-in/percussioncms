/**
 * Unit tests for Explorer Translations row-id helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  guidShapedIdFromText,
  translationsRowIdFromAttrs,
  isPreferredContentRowName,
  foldedNamesEqual,
} = require("../helpers/explorer-translations-row");

describe("explorer-translations-row helpers (#3871)", () => {
  it("accepts host-type-uuid and detail-row-<guid>", () => {
    assert.equal(guidShapedIdFromText("16777215-101-551"), "16777215-101-551");
    assert.equal(
      guidShapedIdFromText("detail-row-16777215-101-551"),
      "16777215-101-551",
    );
    assert.equal(guidShapedIdFromText("1-101-708"), "1-101-708");
  });

  it("does not treat numeric, slug, or path ids as GUIDs", () => {
    assert.equal(guidShapedIdFromText("551"), "");
    assert.equal(guidShapedIdFromText("detail-row-551"), "");
    assert.equal(guidShapedIdFromText("ci-home"), "");
    assert.equal(
      guidShapedIdFromText(
        "detail-row-/Sites/16777215-101-703/Pages/Corporate Investments Home",
      ),
      "",
    );
    assert.equal(guidShapedIdFromText(""), "");
  });

  it("prefers data-item-id over data-testid", () => {
    assert.equal(
      translationsRowIdFromAttrs({
        itemId: "16777215-101-551",
        testId: "detail-row-ci-home",
      }),
      "16777215-101-551",
    );
    assert.equal(
      translationsRowIdFromAttrs({
        itemId: "551",
        testId: "detail-row-16777215-101-551",
      }),
      "16777215-101-551",
    );
  });

  it("matches Corporate Investments Home aliases", () => {
    assert.equal(
      isPreferredContentRowName("Corporate Investments Home", ""),
      true,
    );
    assert.equal(
      isPreferredContentRowName(
        "Corporate Investments Home Page (Section Image).jpg",
        "",
      ),
      false,
    );
    assert.equal(isPreferredContentRowName("Pages", "Pages folder"), false);
  });

  it("folds site folder names across spaces and underscores", () => {
    assert.equal(
      foldedNamesEqual("Corporate_Investments", "Corporate Investments"),
      true,
    );
    assert.equal(
      foldedNamesEqual("CorporateInvestments", "Corporate_Investments"),
      true,
    );
    assert.equal(foldedNamesEqual("Pages", "Assets"), false);
  });
});
