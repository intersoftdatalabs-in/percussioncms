/******************************************************************************
 *
 * [ ps/compat.js ]
 *
 * Compatibility shim for the Dojo-to-jQuery migration (Track A2+).
 *
 * Provides:
 *   - ps.* namespace initialization
 *   - Lightweight replacements for the most common Dojo utility functions
 *     used by ps/* modules (assertions, type checks, collections).
 *
 * Load order: jQuery -> compat.js -> dojo.js (bundle) -> other scripts
 *
 * COPYRIGHT (c) 1999 - 2025 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/

// ---- Namespace initialization ------------------------------------------------
// Ensure every ps.* sub-namespace exists so that individual modules can attach
// constructors / objects without needing dojo.provide().
var ps = ps || {};
ps.aa = ps.aa || {};
ps.content = ps.content || {};
ps.io = ps.io || {};
ps.util = ps.util || {};
ps.widget = ps.widget || {};
ps.workflow = ps.workflow || {};

// ---- Assertion utilities ----------------------------------------------------
// Replace dojo.lang.assert / dojo.lang.assertType

/**
 * Asserts that a condition is truthy.  Throws an Error when the condition
 * is falsy, mirroring the behaviour of the old dojo.lang.assert().
 *
 * @param {*}      condition - The condition to test.
 * @param {string} [message] - Optional message included in the Error.
 * @throws {Error} if condition is falsy.
 */
ps.assert = function (condition, message) {
  if (!condition) {
    throw new Error("Assertion failed" + (message ? ": " + message : ""));
  }
};

/**
 * Asserts that value is of the expected type, mirroring dojo.lang.assertType().
 *
 * For the five built-in wrapper types (String, Number, Boolean, Function,
 * Array) a typeof / Array.isArray check is used so that primitive values
 * pass.  For every other constructor, instanceof is used.
 *
 * @param {*}        value - The value to check.
 * @param {Function} type  - The expected type constructor.
 * @throws {Error} if the type check fails.
 */
ps.assertType = function (value, type) {
  var valid;
  if (type === String) {
    valid = typeof value === "string" || value instanceof String;
  } else if (type === Number) {
    valid = typeof value === "number" || value instanceof Number;
  } else if (type === Boolean) {
    valid = typeof value === "boolean" || value instanceof Boolean;
  } else if (type === Function) {
    valid = typeof value === "function";
  } else if (type === Array) {
    valid = Array.isArray(value);
  } else {
    valid = value instanceof type;
  }
  if (!valid) {
    var typeName = type.name || type.toString();
    throw new Error(
      "assertType failed: expected " + typeName + ", got " + typeof value,
    );
  }
};

// ---- Type-check utilities ---------------------------------------------------
// Replace dojo.lang.isNumeric, dojo.lang.isBoolean, dojo.lang.isString

/**
 * Returns true if value can be interpreted as a finite number.
 * Mirrors dojo.lang.isNumeric().
 *
 * @param {*} value
 * @returns {boolean}
 */
ps.isNumeric = function (value) {
  return !isNaN(parseFloat(value)) && isFinite(value);
};

/**
 * Returns true if value is a boolean primitive.
 * Mirrors dojo.lang.isBoolean().
 *
 * @param {*} value
 * @returns {boolean}
 */
ps.isBoolean = function (value) {
  return typeof value === "boolean";
};

/**
 * Returns true if value is a string primitive.
 * Mirrors dojo.lang.isString().
 *
 * @param {*} value
 * @returns {boolean}
 */
ps.isString = function (value) {
  return typeof value === "string";
};

// ---- Collections ------------------------------------------------------------
// Replace dojo.collections.Stack

/**
 * Simple stack implementation replacing dojo.collections.Stack.
 * Exposes the same API surface used by the ps/* codebase:
 * push(), pop(), peek(), clear(), toArray(), and a count property.
 *
 * @param {Array} [initialArray] - Optional initial contents (bottom-to-top).
 * @constructor
 */
ps.Stack = function (initialArray) {
  this._data = initialArray ? initialArray.slice() : [];
};

Object.defineProperty(ps.Stack.prototype, "count", {
  get: function () {
    return this._data.length;
  },
  enumerable: true,
});

ps.Stack.prototype.push = function (item) {
  this._data.push(item);
};

ps.Stack.prototype.pop = function () {
  return this._data.pop();
};

ps.Stack.prototype.peek = function () {
  return this._data.length > 0 ? this._data[this._data.length - 1] : undefined;
};

ps.Stack.prototype.clear = function () {
  this._data = [];
};

ps.Stack.prototype.toArray = function () {
  return this._data.slice();
};

// ---------------------------------------------------------------------------
// Cookie helpers (replaces dojo.io.cookie.getCookie / setCookie)
// ---------------------------------------------------------------------------

/**
 * Reads a cookie value by name.
 * @param {string} name the cookie name.
 * @return {string|null} the cookie value, or null if not found.
 */
ps.util._getCookie = function (name) {
  var match = document.cookie.match(
    new RegExp(
      "(?:^|;\\s*)" + name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "=([^;]*)",
    ),
  );
  return match ? decodeURIComponent(match[1]) : null;
};

/**
 * Sets a cookie with the given name, value, and expiry in days.
 * @param {string} name the cookie name.
 * @param {string} value the cookie value.
 * @param {number} days number of days until the cookie expires.
 */
ps.util._setCookie = function (name, value, days) {
  var expires = "";
  if (days) {
    var d = new Date();
    d.setTime(d.getTime() + days * 86400000);
    expires = "; expires=" + d.toUTCString();
  }
  document.cookie =
    name + "=" + encodeURIComponent(value) + expires + "; path=/";
};

// ---------------------------------------------------------------------------
// DOM helpers (replaces dojo.html.createNodesFromText)
// ---------------------------------------------------------------------------

/**
 * Parses an HTML string and returns its child nodes as an array.
 * @param {string} html the HTML markup.
 * @return {Node[]} array of parsed child nodes.
 */
ps.util._createNodesFromText = function (html) {
  var div = document.createElement("div");
  div.innerHTML = html;
  return Array.from(div.childNodes);
};
