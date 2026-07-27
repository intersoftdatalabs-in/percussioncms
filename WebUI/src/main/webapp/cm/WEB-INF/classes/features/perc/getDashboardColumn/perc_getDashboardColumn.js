/**
 * Feature to retrieve the column index of the current gadget
 */
gadgets.window = gadgets.window || {};

/**
 * Helper function to get a query string parameter.
 *
 * Returns the URL-decoded, character-allow-listed value of the named query
 * parameter from the current `window.location.href`, or the empty string
 * when the parameter is absent, when percent-decoding fails, or when the
 * captured value contains no characters outside the safe set
 * `[A-Za-z0-9._-]`.
 *
 * Sanitisation rationale: the raw regex capture from `RegExp.exec` is
 * never safe to flow into a downstream string context (selector, URL,
 * data-context attribute). This helper was flagged by GitHub CodeQL as
 * `js/incomplete-sanitization`; the fix (1) allow-lists the parameter
 * *name* before building the RegExp (no incomplete `[` / `]` first-only
 * escape), (2) URL-decodes the captured value, and (3) reduces it to a
 * safe character set so callers can concatenate the result without a
 * second pass of escaping.
 */
function __gup( name )
{
  // Parameter names are fixed gadget identifiers (e.g. "mid"). Reject
  // anything outside a safe identifier set so we never build a RegExp from
  // untrusted text. (Closes js/incomplete-sanitization on the previous
  // non-global [ / ] escape.)
  if (typeof name !== "string" || !/^[A-Za-z0-9._-]+$/.test(name)) {
    return "";
  }
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  var raw = results[1] || "";
  try {
    raw = decodeURIComponent(raw);
  } catch (e) {
    // Malformed percent-escape; fall back to the raw capture.
  }
  // Allow only characters that are safe in every downstream string
  // context (selector, URL, data-context attribute): ASCII letters,
  // digits, dot, underscore, dash. Everything else is stripped.
  return raw.replace(/[^A-Za-z0-9._-]/g, "");
}

/**
 * Return the index of the column that the current gadget exists in.
 * @return the gadget index
 * @type {int}
 */
gadgets.window.getDashboardColumn = function() {
   var __mid = __gup("mid");
   var __gad = percJQuery("#gid_" + __mid);
   var __columnRawId = __gad.parent().attr("id");
   return parseInt(__columnRawId.substr(4));
}


