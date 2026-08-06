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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * BBQ Compatibility Shim
 *
 * Replaces the deprecated jquery-bbq library with native URLSearchParams
 * equivalents. Only the API surface actually used in the Perc delivery
 * widgets is implemented here.
 *
 * Replaced APIs:
 *   $.deparam.querystring()          - parse current URL query string into an object
 *   $.param.querystring()            - get the current URL query string (includes leading "?")
 *   $.param.querystring("", obj)     - serialize obj to a query string (includes leading "?")
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams
 */
(function ($) {
  "use strict";

  if (!$.deparam) {
    $.deparam = {};
  }

  /**
   * Parse the current URL's query string into a plain key-value object.
   * Replaces $.deparam.querystring() from jquery-bbq.
   *
   * @returns {Object} Key-value pairs decoded from window.location.search.
   */
  $.deparam.querystring = function () {
    return Object.fromEntries(new URLSearchParams(window.location.search));
  };

  /**
   * Serialize an object to a URL query string, or retrieve the current one.
   * Extends $.param with a .querystring method to replace jquery-bbq.
   *
   * Called as $.param.querystring()        → returns window.location.search (e.g. "?foo=bar")
   * Called as $.param.querystring("", obj) → returns "?key=val&..." or "" if obj is empty.
   *
   * @param {string} [url]  - ignored; pass "" for backward compatibility
   * @param {Object} [obj]  - object to serialize into a query string
   * @returns {string} Query string with a leading "?", or "" if empty/no args.
   */
  $.param.querystring = function (url, obj) {
    if (arguments.length === 0) {
      return window.location.search;
    }
    var qs = new URLSearchParams(obj || {}).toString();
    return qs.length > 0 ? "?" + qs : "";
  };
})(jQuery);
