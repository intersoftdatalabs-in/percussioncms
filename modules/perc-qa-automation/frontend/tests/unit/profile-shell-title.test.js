/**
 * Unit tests for profile hub title i18n helpers (no live CMS).
 * Residual #2499 / parent #2374 — CmsUi.tmx perc.ui.profile.modern@My profile.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  expectedProfileTitle,
  profileTitleMatcher,
  PROFILE_TITLE_BY_FAMILY,
  PROFILE_TITLE_PREFERRED_LOCALES,
  ENGLISH_PROFILE_TITLE_MATCHER,
} = require("../helpers/profile-shell-title");

describe("expectedProfileTitle", () => {
  it("returns English source for en / en-us", () => {
    assert.equal(expectedProfileTitle("en"), "My profile");
    assert.equal(expectedProfileTitle("en-us"), "My profile");
  });

  it("returns German TMX seg for de / de-de", () => {
    assert.equal(expectedProfileTitle("de"), "Mein Profil");
    assert.equal(expectedProfileTitle("de-de"), "Mein Profil");
  });

  it("returns Spanish TMX seg for es (lowercase as shipped)", () => {
    assert.equal(expectedProfileTitle("es"), "mi perfil");
  });

  it("returns null for families without a sample map", () => {
    assert.equal(expectedProfileTitle("fr"), null);
    assert.equal(expectedProfileTitle("hi-in"), null);
  });
});

describe("profileTitleMatcher", () => {
  it("matches German title case-insensitively and rejects English", () => {
    const re = profileTitleMatcher("de-de");
    assert.ok(re);
    assert.ok(re.test("Mein Profil"));
    assert.ok(re.test("mein profil"));
    assert.equal(re.test("My profile"), false);
  });

  it("matches Spanish TMX seg and rejects English", () => {
    const re = profileTitleMatcher("es");
    assert.ok(re);
    assert.ok(re.test("mi perfil"));
    assert.ok(re.test("Mi Perfil"));
    assert.equal(re.test("My profile"), false);
  });

  it("matches English source", () => {
    const re = profileTitleMatcher("en-us");
    assert.ok(re);
    assert.ok(re.test("My profile"));
  });

  it("returns null for unmapped family", () => {
    assert.equal(profileTitleMatcher("fr-fr"), null);
  });
});

describe("constants", () => {
  it("exports preferred de then es order", () => {
    assert.deepEqual([...PROFILE_TITLE_PREFERRED_LOCALES], ["de-de", "de", "es"]);
  });

  it("ships de and es sample titles", () => {
    assert.equal(PROFILE_TITLE_BY_FAMILY.de, "Mein Profil");
    assert.equal(PROFILE_TITLE_BY_FAMILY.es, "mi perfil");
  });

  it("English smoke matcher matches My profile", () => {
    assert.ok(ENGLISH_PROFILE_TITLE_MATCHER.test("My profile"));
    assert.ok(ENGLISH_PROFILE_TITLE_MATCHER.test("my profile"));
  });
});
