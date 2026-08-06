/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Display-only labels for classic Finder repository roots.
 *
 * Path identity (REST/folder segments, perc_path_constants, bookmarks,
 * path bar values) stays English. Only visible label / title / alt text
 * should use {@link displayLabelForFinderRoot}.
 *
 * TMX keys (shipped by #2092 / PR #2092):
 *   perc.ui.finder.root@Sites|Assets|Design|Search|Recycling
 *
 * Exposes {@code globalThis.percFinderRootDisplay}.
 */
(function (global) {
  "use strict";

  var FINDER_ROOT_I18N_KEYS = {
    Sites: "perc.ui.finder.root@Sites",
    Assets: "perc.ui.finder.root@Assets",
    Design: "perc.ui.finder.root@Design",
    Search: "perc.ui.finder.root@Search",
    Recycling: "perc.ui.finder.root@Recycling",
  };

  /**
   * @param {*} englishName repository root English name (path segment)
   * @return {string|null} TMX key, or null when not a known root
   */
  function i18nKeyForFinderRoot(englishName) {
    if (englishName == null || englishName === "") {
      return null;
    }
    return FINDER_ROOT_I18N_KEYS[String(englishName)] || null;
  }

  /**
   * Map known English Finder root names to locale display labels.
   * Unknown names pass through unchanged.
   *
   * @param {*} englishName English root name (e.g. "Sites")
   * @param {function(string):*} [messageFn] optional I18N.message-compatible
   *        lookup; defaults to global I18N.message when present
   * @return {string} localized label, or the original English name
   */
  function displayLabelForFinderRoot(englishName, messageFn) {
    if (englishName == null) {
      return "";
    }
    var name = String(englishName);
    var key = i18nKeyForFinderRoot(name);
    if (!key) {
      return name;
    }
    var lookup = messageFn;
    if (typeof lookup !== "function") {
      if (
        typeof global.I18N !== "undefined" &&
        global.I18N &&
        typeof global.I18N.message === "function"
      ) {
        lookup = function (k) {
          return global.I18N.message(k);
        };
      } else {
        return name;
      }
    }
    try {
      var translated = lookup(key);
      if (translated != null && String(translated).length > 0) {
        return String(translated);
      }
    } catch (e) {
      // Fall through to English identity when TMX lookup fails.
    }
    return name;
  }

  global.percFinderRootDisplay = {
    FINDER_ROOT_I18N_KEYS: FINDER_ROOT_I18N_KEYS,
    i18nKeyForFinderRoot: i18nKeyForFinderRoot,
    displayLabelForFinderRoot: displayLabelForFinderRoot,
  };
})(
  typeof globalThis !== "undefined"
    ? globalThis
    : typeof window !== "undefined"
      ? window
      : this,
);
