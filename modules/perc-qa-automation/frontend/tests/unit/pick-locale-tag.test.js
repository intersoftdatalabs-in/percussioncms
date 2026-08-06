/**
 * Unit tests for login locale pick helpers (no live CMS).
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  pickPreferredLocaleTag,
  localeLanguageFamily,
  DEFAULT_PREFERRED_NON_ENGLISH,
} = require("../helpers/pick-locale-tag");

describe("pickPreferredLocaleTag", () => {
  it("prefers de-de when present", () => {
    assert.equal(
      pickPreferredLocaleTag(["en-us", "de-de", "hi-in", "es"]),
      "de-de",
    );
  });

  it("falls back to de when de-de missing", () => {
    assert.equal(pickPreferredLocaleTag(["en-us", "de", "es"]), "de");
  });

  it("falls back to hi-in then hi", () => {
    assert.equal(pickPreferredLocaleTag(["en-us", "hi-in", "es"]), "hi-in");
    assert.equal(pickPreferredLocaleTag(["en-us", "hi", "es"]), "hi");
  });

  it("falls back to es when only Spanish non-English is available", () => {
    assert.equal(pickPreferredLocaleTag(["en-us", "es"]), "es");
  });

  it("returns null when only English is available", () => {
    assert.equal(pickPreferredLocaleTag(["en-us", "en"]), null);
  });

  it("returns null for empty / null available", () => {
    assert.equal(pickPreferredLocaleTag([]), null);
    assert.equal(pickPreferredLocaleTag(null), null);
    assert.equal(pickPreferredLocaleTag(undefined), null);
  });

  it("honors custom preferred order", () => {
    assert.equal(
      pickPreferredLocaleTag(["de-de", "es", "hi"], ["es", "de-de"]),
      "es",
    );
  });

  it("exports a non-empty default preferred list", () => {
    assert.ok(DEFAULT_PREFERRED_NON_ENGLISH.length >= 3);
    assert.ok(DEFAULT_PREFERRED_NON_ENGLISH.includes("de-de"));
  });
});

describe("localeLanguageFamily", () => {
  it("maps regional tags to base language", () => {
    assert.equal(localeLanguageFamily("de-de"), "de");
    assert.equal(localeLanguageFamily("hi-in"), "hi");
    assert.equal(localeLanguageFamily("es"), "es");
    assert.equal(localeLanguageFamily("fr-fr"), "fr");
  });

  it("treats English variants as en", () => {
    assert.equal(localeLanguageFamily("en-us"), "en");
    assert.equal(localeLanguageFamily("en"), "en");
    assert.equal(localeLanguageFamily(""), "en");
    assert.equal(localeLanguageFamily(null), "en");
  });
});
