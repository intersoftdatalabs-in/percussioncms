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
 * First-party query-string helpers for the classic CM UI (issue #5663).
 *
 * This file replaces the query-string half of jQuery BBQ v1.1 (2010, "Cowboy"
 * Ben Alman, MIT/GPL-dual — dead upstream, no npm package). The parse/merge
 * algorithm below is derived from that MIT-licensed source; only the surface
 * the classic UI actually calls is carried over:
 *
 *   - `jQuery.deparam( params [, coerce ] )`
 *   - `jQuery.deparam.querystring( [ url ] [, coerce ] )`
 *   - `jQuery.param.querystring( [ url ] )` /
 *     `jQuery.param.querystring( url, params [, merge_mode ] )`
 *
 * Deliberately omitted (verified zero callers repo-wide, November 2026):
 * fragment (hash) variants, the `jQuery.bbq` hash-state API
 * (pushState/getState/removeState), `jQuery.elemUrlAttr`, and the
 * `jQuery.fn.querystring`/`jQuery.fn.fragment` DOM helpers. If a future
 * caller needs them, prefer the native `URLSearchParams` API instead of
 * resurrecting the vendored BBQ copy.
 *
 * Requires jQuery core (`$.extend`, `$.param`) to be loaded first, which
 * both `common_js.jsp` and the minify bundle configs guarantee.
 */
(function ($, window) {
  "use strict";

  // Get location.search (or what you'd expect location.search to be) sans any
  // leading #. (BBQ 1.1 get_querystring, verbatim.)
  function getQuerystring(url) {
    return url.replace(/(?:^[^?#]*\?([^#]*).*$)?.*/, "$1");
  }

  // Guard against prototype pollution via crafted query-string keys
  // (e.g. `?__proto__[x]=1`). Matches the guard BBQ 1.1 added around its
  // dynamic property writes.
  function sanitizeKey(t) {
    return t &&
      ["_proto_", "__proto__", "constructor", "prototype"].indexOf(
        t.toLowerCase(),
      ) !== -1
      ? t.toUpperCase()
      : t;
  }

  // Deserialize a params string into an object, optionally coercing numbers,
  // booleans, null and undefined values. Counterpart to jQuery.param.
  // (BBQ 1.1 jQuery.deparam, verbatim semantics.)
  function deparam(params, coerce) {
    var obj = {},
      coerceTypes = { true: !0, false: !1, null: null };

    var pairs = String(params)
      .replace(/\+/g, " ")
      .split("&");

    for (var j = 0; j < pairs.length; j++) {
      var param = pairs[j].split("="),
        key = decodeURIComponent(param[0]),
        val,
        cur = obj,
        i = 0,
        // If key is more complex than 'foo', like 'a[]' or 'a[b][c]',
        // split it into its component parts.
        keys = key.split("]["),
        keysLast = keys.length - 1;

      // If the first keys part contains [ and the last ends with ], then []
      // are correctly balanced.
      if (/\[/.test(keys[0]) && /\]$/.test(keys[keysLast])) {
        // Remove the trailing ] from the last keys part.
        keys[keysLast] = keys[keysLast].replace(/\]$/, "");

        // Split first keys part into two parts on the [ and add them back
        // onto the beginning of the keys array.
        keys = keys.shift().split("[").concat(keys);

        keysLast = keys.length - 1;
      } else {
        // Basic 'foo' style key.
        keysLast = 0;
      }

      // Are we dealing with a name=value pair, or just a name?
      if (param.length === 2) {
        val = decodeURIComponent(param[1]);

        // Coerce values.
        if (coerce) {
          val =
            val && !isNaN(val)
              ? +val // number
              : val === "undefined"
                ? undefined // undefined
                : coerceTypes[sanitizeKey(val)] !== undefined
                  ? coerceTypes[sanitizeKey(val)] // true, false, null
                  : val; // string
        }

        if (keysLast) {
          // Complex key, build deep object structure.
          for (; i <= keysLast; i++) {
            key = keys[i] === "" ? cur.length : keys[i];
            cur = cur[sanitizeKey(key)] =
              i < keysLast
                ? cur[sanitizeKey(key)] ||
                  (keys[i + 1] && isNaN(keys[i + 1]) ? {} : [])
                : val;
          }
        } else {
          // Simple key, even simpler rules, since only scalars and shallow
          // arrays are allowed.
          if (Array.isArray(obj[sanitizeKey(key)])) {
            // val is already an array, so push on the next value.
            obj[sanitizeKey(key)].push(val);
          } else if (obj[sanitizeKey(key)] !== undefined) {
            // val isn't an array, but since a second value has been
            // specified, convert val into an array.
            obj[sanitizeKey(key)] = [obj[sanitizeKey(key)], val];
          } else {
            // val is a scalar.
            obj[sanitizeKey(key)] = val;
          }
        }
      } else if (key) {
        // No value was defined, so set something meaningful.
        obj[sanitizeKey(key)] = coerce ? undefined : "";
      }
    }

    return obj;
  }

  // Parse the query string from a URL or the current window.location,
  // deserializing it into an object.
  function deparamQuerystring(urlOrParams, coerce) {
    var str;
    if (urlOrParams === undefined || typeof urlOrParams === "boolean") {
      // No URL specified: parse the current location.
      coerce = urlOrParams;
      str = getQuerystring(
        String(window.location.href || window.location),
      );
    } else {
      str = String(urlOrParams).replace(/^.*\?|#.*$/g, "");
    }
    return deparam(str, coerce);
  }

  // Merge a URL, with or without pre-existing query string params, plus any
  // object, params string or URL containing query string params into a new
  // URL. Merge behavior (mergeMode):
  //   0 (default): params in the params argument override query string
  //      params in url.
  //   1: query string params in url override params in the params argument.
  //   2: params argument completely replaces any query string in url.
  function paramQuerystring(url, params, mergeMode) {
    var result, qs, matches, urlParams, hash;

    if (params !== undefined) {
      // Build URL by merging params into url string.
      //
      // matches[1] = url part that precedes params, not including trailing
      //   ?/#; matches[2] = params, not including leading ?/#;
      // matches[3] = hash including leading #, if any.
      matches = String(url).match(/^([^#?]*)\??([^#]*)(#?.*)/);

      // Get the hash, if it exists.
      hash = matches[3] || "";

      if (mergeMode === 2 && typeof params === "string") {
        // If merge_mode is 2 and params is a string, merge the query string
        // into the URL wholesale, without converting it into an object.
        qs = params.replace(/^.*\?|#.*$/g, "");
      } else {
        // Convert relevant params in url to object.
        urlParams = deparam(matches[2]);

        params =
          typeof params === "string"
            ? // Convert passed params string into object.
              deparamQuerystring(params)
            : // Passed params object.
              params;

        qs =
          mergeMode === 2
            ? params // passed params replace url params
            : mergeMode === 1
              ? $.extend({}, params, urlParams) // url params win
              : $.extend({}, urlParams, params); // passed params win

        // Convert params object to a string via jQuery core serialization
        // (identical to BBQ, whose internal serializer was `$.param`).
        qs = $.param(qs);
      }

      // Build URL from the base url, querystring and hash. In querystring
      // mode, ? is only added if a query string exists.
      result = matches[1] + (qs || !matches[1] ? "?" : "") + qs + hash;
    } else {
      // If URL was passed in, parse params from URL string, otherwise parse
      // params from window.location.
      result = getQuerystring(
        url !== undefined
          ? String(url)
          : String(window.location.href || window.location),
      );
    }

    return result;
  }

  $.deparam = deparam;
  $.deparam.querystring = deparamQuerystring;
  $.param.querystring = paramQuerystring;
})(jQuery, window);
