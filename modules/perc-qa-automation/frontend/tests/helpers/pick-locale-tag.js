/**
 * Pure helpers for choosing a non-English login locale and mapping it to a
 * TMX language family used by Home / dashboard.modern chrome assertions.
 *
 * <p>No Playwright dependency — unit-tested via node:test.</p>
 *
 * @see tests/bugs/bug-1876-home-gadget-locale.spec.js
 */

"use strict";

/**
 * Preferred non-English login tags for residual Home gadget body/modal
 * locale regression (GH-1876). Order: German (best TMX coverage in residual
 * keys), Hindi (login locale repro locale), Spanish (optional).
 *
 * Do not expand Spanish/#961 scope beyond a sample key check.
 */
const DEFAULT_PREFERRED_NON_ENGLISH = Object.freeze([
  "de-de",
  "de",
  "hi-in",
  "hi",
  "es",
]);

/**
 * Pick the first preferred locale that is present in the install's login list.
 *
 * @param {string[]|null|undefined} available option values from login UI
 * @param {string[]} [preferred]
 * @returns {string|null} selected tag or null when none match
 */
function pickPreferredLocaleTag(
  available,
  preferred = DEFAULT_PREFERRED_NON_ENGLISH,
) {
  if (!Array.isArray(available) || available.length === 0) {
    return null;
  }
  const set = new Set(available.map((v) => String(v).trim()).filter(Boolean));
  const list = Array.isArray(preferred)
    ? preferred
    : DEFAULT_PREFERRED_NON_ENGLISH;
  for (const p of list) {
    const tag = String(p || "").trim();
    if (tag && set.has(tag)) {
      return tag;
    }
  }
  return null;
}

/**
 * Map a login locale tag to the TMX language family used in CmsUi.tmx
 * (usually base language: de, hi, es).
 *
 * @param {string|null|undefined} tag login j_locale value (e.g. de-de, hi-in)
 * @returns {string} family key such as "de", "hi", "es", or "en"
 */
function localeLanguageFamily(tag) {
  const t = String(tag || "")
    .trim()
    .toLowerCase();
  if (!t) {
    return "en";
  }
  if (t === "en" || t.startsWith("en-")) {
    return "en";
  }
  const base = t.split("-")[0];
  return base || "en";
}

module.exports = {
  DEFAULT_PREFERRED_NON_ENGLISH,
  pickPreferredLocaleTag,
  localeLanguageFamily,
};
