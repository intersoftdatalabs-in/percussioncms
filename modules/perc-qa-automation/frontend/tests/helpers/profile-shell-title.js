/**
 * Pure helpers for profile hub title assertions under modern-locale TMX
 * ({@code perc.ui.profile.modern@My profile} in CmsUi.tmx).
 *
 * <p>No Playwright dependency — unit-tested via node:test. Used by
 * {@code tests/profile-shell.spec.js} locale residual (#2499 / parent #2374).</p>
 *
 * @see modules/perc-i18n/.../CmsUi.tmx tu tuid="perc.ui.profile.modern@My profile"
 */

"use strict";

const { localeLanguageFamily } = require("./pick-locale-tag");

/**
 * Preferred login tags for profile-title locale residual (#2499).
 * Prefer German (best modern-locale coverage) then Spanish — matches issue
 * acceptance ("es or de after #2426").
 */
const PROFILE_TITLE_PREFERRED_LOCALES = Object.freeze(["de-de", "de", "es"]);

/**
 * Expected title text for language family, matching ship TMX segs after #2426.
 * Values must stay in lockstep with CmsUi.tmx (not en-us fallback after @).
 */
const PROFILE_TITLE_BY_FAMILY = Object.freeze({
  en: "My profile",
  de: "Mein Profil",
  es: "mi perfil",
});

/**
 * Resolve expected profile title for a login tag or language family.
 *
 * @param {string|null|undefined} localeTagOrFamily e.g. de-de, de, es, en-us
 * @returns {string|null} expected title, or null when family has no sample map
 */
function expectedProfileTitle(localeTagOrFamily) {
  const family = localeLanguageFamily(localeTagOrFamily);
  if (!Object.prototype.hasOwnProperty.call(PROFILE_TITLE_BY_FAMILY, family)) {
    return null;
  }
  return PROFILE_TITLE_BY_FAMILY[family];
}

/**
 * Case-insensitive exact-title matcher for Playwright {@code toContainText}
 * / {@code toHaveText}. Escapes metacharacters in the TMX string.
 *
 * @param {string|null|undefined} localeTagOrFamily
 * @returns {RegExp|null}
 */
function profileTitleMatcher(localeTagOrFamily) {
  const title = expectedProfileTitle(localeTagOrFamily);
  if (!title) {
    return null;
  }
  const escaped = String(title).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`^\\s*${escaped}\\s*$`, "i");
}

/**
 * English title matcher used by default (en-us) smoke cases.
 * Kept as a shared constant so English and locale tests stay aligned.
 */
const ENGLISH_PROFILE_TITLE_MATCHER = /my profile/i;

module.exports = {
  PROFILE_TITLE_BY_FAMILY,
  PROFILE_TITLE_PREFERRED_LOCALES,
  expectedProfileTitle,
  profileTitleMatcher,
  ENGLISH_PROFILE_TITLE_MATCHER,
};
