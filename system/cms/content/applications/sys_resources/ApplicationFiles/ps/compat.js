/******************************************************************************
 *
 * [ ps/compat.js ]
 *
 * Compatibility shim for the Dojo-to-jQuery migration (Track A2+).
 *
 * Provides:
 *   - ps.* namespace initialization
 *   - Lightweight replacements for the most common Dojo utility functions
 *     used by ps/* modules (assertions, type checks, collections, events,
 *     class inheritance).
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

// ===========================================================================
// Track A6 shims — collections, class inheritance, event wiring
// ===========================================================================

// ---- Collections namespace --------------------------------------------------
ps.collections = ps.collections || {};

// ---------------------------------------------------------------------------
// ps.collections.ArrayList  (replaces dojo.collections.ArrayList)
//
// Only the API methods actually used in ps/* are implemented:
//   constructor, add, addRange, clear, contains, count, indexOf, item,
//   remove, setByIndex, toArray
// ---------------------------------------------------------------------------

/**
 * A simple ordered list backed by a native Array.
 * Drop-in replacement for dojo.collections.ArrayList.
 *
 * @param {Array} [arr] - Optional initial contents (shallow-copied).
 * @constructor
 */
ps.collections.ArrayList = function (arr) {
  this._data = arr ? arr.slice() : [];
  this.count = this._data.length;
};

/** Adds an item to the end of the list. */
ps.collections.ArrayList.prototype.add = function (obj) {
  this._data.push(obj);
  this.count = this._data.length;
};

/** Appends all items from an array or another ArrayList. */
ps.collections.ArrayList.prototype.addRange = function (a) {
  if (a && typeof a.toArray === "function") {
    this._data = this._data.concat(a.toArray());
  } else if (Array.isArray(a)) {
    this._data = this._data.concat(a);
  }
  this.count = this._data.length;
};

/** Removes all items. */
ps.collections.ArrayList.prototype.clear = function () {
  this._data = [];
  this.count = 0;
};

/** Returns true if the list contains an item (== comparison). */
ps.collections.ArrayList.prototype.contains = function (obj) {
  for (var i = 0; i < this._data.length; i++) {
    if (this._data[i] == obj) return true;
  }
  return false;
};

/** Returns the index of the first match, or -1. */
ps.collections.ArrayList.prototype.indexOf = function (obj) {
  for (var i = 0; i < this._data.length; i++) {
    if (this._data[i] == obj) return i;
  }
  return -1;
};

/** Returns the item at the given index. */
ps.collections.ArrayList.prototype.item = function (i) {
  return this._data[i];
};

/** Removes the first occurrence of obj from the list. */
ps.collections.ArrayList.prototype.remove = function (obj) {
  var idx = this.indexOf(obj);
  if (idx >= 0) {
    this._data.splice(idx, 1);
    this.count = this._data.length;
  }
};

/** Sets the item at the given index. */
ps.collections.ArrayList.prototype.setByIndex = function (i, obj) {
  this._data[i] = obj;
};

/** Returns a shallow copy of the internal array. */
ps.collections.ArrayList.prototype.toArray = function () {
  return this._data.slice();
};

/** Iterates items using a callback. */
ps.collections.ArrayList.prototype.forEach = function (fn, scope) {
  var s = scope || window;
  for (var i = 0; i < this._data.length; i++) {
    fn.call(s, this._data[i], i, this._data);
  }
};

// ---------------------------------------------------------------------------
// ps.collections.Dictionary  (replaces dojo.collections.Dictionary)
//
// Used API: constructor, add, containsKey, item, remove
// ---------------------------------------------------------------------------

/**
 * A simple string-keyed dictionary backed by a plain object.
 * Drop-in replacement for dojo.collections.Dictionary.
 *
 * @constructor
 */
ps.collections.Dictionary = function () {
  this._data = {};
  this.count = 0;
};

/** Adds or overwrites a key/value pair. */
ps.collections.Dictionary.prototype.add = function (key, value) {
  if (!(key in this._data)) {
    this.count++;
  }
  this._data[key] = value;
};

/** Returns true if the given key exists. */
ps.collections.Dictionary.prototype.containsKey = function (key) {
  return key in this._data;
};

/** Alias for containsKey (matching dojo API). */
ps.collections.Dictionary.prototype.contains =
  ps.collections.Dictionary.prototype.containsKey;

/** Returns the value for the given key, or undefined. */
ps.collections.Dictionary.prototype.item = function (key) {
  return this._data[key];
};

/** Removes the entry for the given key. Returns true if the key existed. */
ps.collections.Dictionary.prototype.remove = function (key) {
  if (key in this._data) {
    delete this._data[key];
    this.count--;
    return true;
  }
  return false;
};

// ---------------------------------------------------------------------------
// ps.declare  (replaces dojo.declare)
//
// Minimal Dojo 0.4 class declaration: creates a constructor that calls
// an initFn, mixes in proto, and sets up single inheritance via a
// superclass property.
// ---------------------------------------------------------------------------

/**
 * Declares a named class with single inheritance, matching the subset of
 * dojo.declare() used in ps/content tab panels.
 *
 * @param {string}   className  - Dot-separated class name (e.g. "ps.content.Foo").
 * @param {Function} superclass - Parent constructor.
 * @param {Function} [initFn]   - Initializer (called in constructor).
 * @param {Object}   [proto]    - Methods/properties mixed into prototype.
 */
ps.declare = function (className, superclass, initFn, proto) {
  // dojo.declare overloads: (name, super, init, proto) or (name, super, proto)
  if (typeof initFn !== "function") {
    proto = initFn;
    initFn = null;
  }

  // Build the constructor
  var ctor = function () {
    if (initFn) {
      initFn.apply(this, arguments);
    }
  };

  // Wire inheritance
  if (superclass) {
    ctor.prototype = Object.create(superclass.prototype);
    ctor.prototype.constructor = ctor;
    ctor.superclass = superclass.prototype;
  }

  // Mix in instance members
  if (proto) {
    for (var key in proto) {
      if (proto.hasOwnProperty(key)) {
        ctor.prototype[key] = proto[key];
      }
    }
  }

  // Register into the global namespace (e.g. "ps.content.Foo")
  var parts = className.split(".");
  var cur = window;
  for (var i = 0; i < parts.length - 1; i++) {
    cur[parts[i]] = cur[parts[i]] || {};
    cur = cur[parts[i]];
  }
  cur[parts[parts.length - 1]] = ctor;

  return ctor;
};

// ---------------------------------------------------------------------------
// ps.event  (replaces dojo.event.connect / connectAround / connectBefore /
//            topic.subscribe)
//
// Lightweight AOP-style wiring used by the AA widget system.
// ---------------------------------------------------------------------------
ps.event = ps.event || {};

/**
 * Connects a listener to a method on an object, matching the two call
 * signatures used in the codebase:
 *
 *   ps.event.connect(src, "method", listener)
 *   ps.event.connect(src, "method", scope, "handler")
 *
 * After wiring, calling src.method() also calls listener / scope[handler].
 *
 * @param {Object}          src      - Source object.
 * @param {string}          method   - Method name on src.
 * @param {Object|Function} listener - Either a callback or the scope object.
 * @param {string}          [handler]- Method name on scope (when 4 args).
 */
ps.event.connect = function (src, method, listener, handler) {
  var orig = src[method];
  if (typeof handler === "string") {
    src[method] = function () {
      var r = orig ? orig.apply(src, arguments) : undefined;
      listener[handler].apply(listener, arguments);
      return r;
    };
  } else {
    src[method] = function () {
      var r = orig ? orig.apply(src, arguments) : undefined;
      listener.apply(null, arguments);
      return r;
    };
  }
};

/**
 * Wraps src[method] with an around-advice function.  The advice receives
 * an invocation object with { args, proceed(), object }.
 *
 *   ps.event.connectAround(src, "method", scope, "advice")
 *
 * @param {Object} src    - Source object.
 * @param {string} method - Method name on src.
 * @param {Object} scope  - Scope for the advice.
 * @param {string} advice - Method name on scope.
 */
ps.event.connectAround = function (src, method, scope, advice) {
  var orig = src[method];
  src[method] = function () {
    var args = arguments;
    var invocation = {
      args: args,
      object: src,
      proceed: function () {
        return orig ? orig.apply(src, args) : undefined;
      },
    };
    return scope[advice].call(scope, invocation);
  };
};

/**
 * Connects a before-advice.  The advice runs before the original method.
 *
 *   ps.event.connectBefore(src, "method", listener)
 *   ps.event.connectBefore(src, "method", scope, "handler")
 */
ps.event.connectBefore = function (src, method, listener, handler) {
  var orig = src[method];
  if (typeof handler === "string") {
    src[method] = function () {
      listener[handler].apply(listener, arguments);
      return orig ? orig.apply(src, arguments) : undefined;
    };
  } else {
    src[method] = function () {
      listener.apply(null, arguments);
      return orig ? orig.apply(src, arguments) : undefined;
    };
  }
};

// ---- Event topics (pub/sub) -------------------------------------------------
ps.event.topic = ps.event.topic || {};

/** Simple topic registry for pub/sub (replaces dojo.event.topic). */
ps.event._topics = {};

/**
 * Subscribes a listener to a named topic.
 *
 *   ps.event.topic.subscribe("topicName", scope, "handler")
 *   ps.event.topic.subscribe("topicName", callback)
 */
ps.event.topic.subscribe = function (topic, scopeOrFn, handler) {
  if (!ps.event._topics[topic]) {
    ps.event._topics[topic] = [];
  }
  if (typeof handler === "string") {
    ps.event._topics[topic].push(function () {
      scopeOrFn[handler].apply(scopeOrFn, arguments);
    });
  } else {
    ps.event._topics[topic].push(scopeOrFn);
  }
};

/**
 * Publishes a message to all subscribers of a named topic.
 *
 *   ps.event.topic.publish("topicName", data)
 */
ps.event.topic.publish = function (topic) {
  var subs = ps.event._topics[topic];
  if (!subs) return;
  var args = Array.prototype.slice.call(arguments, 1);
  for (var i = 0; i < subs.length; i++) {
    subs[i].apply(null, args);
  }
};

// ---- Widget-system facades (Track A7) ---------------------------------------
// These thin wrappers delegate to the real Dojo 0.4 widget runtime so that
// consumer code references only ps.* APIs.  When the Dojo widget system is
// eventually replaced, only this file needs to change.

/**
 * Defines a new widget class (maps to dojo.widget.defineWidget).
 *
 * Signature mirrors Dojo 0.4:
 *   ps.widget.defineWidget(name, superclass, props)
 *   ps.widget.defineWidget(name, superclass, initFn, props)
 */
ps.widget.defineWidget = function () {
  return dojo.widget.defineWidget.apply(dojo.widget, arguments);
};

/**
 * Creates and returns a new widget instance (maps to dojo.widget.createWidget).
 *
 *   ps.widget.createWidget(type, props, node, position)
 */
ps.widget.createWidget = function () {
  return dojo.widget.createWidget.apply(dojo.widget, arguments);
};

/**
 * Looks up a widget by its widgetId (maps to dojo.widget.byId).
 *
 *   ps.widget.byId(id)
 */
ps.widget.byId = function (id) {
  return dojo.widget.byId(id);
};

/**
 * Widget manager namespace — provides getWidgetById.
 */
ps.widget.manager = ps.widget.manager || {};

/**
 * Looks up a widget by its widgetId through the manager
 * (maps to dojo.widget.manager.getWidgetById).
 *
 *   ps.widget.manager.getWidgetById(id)
 */
ps.widget.manager.getWidgetById = function (id) {
  return dojo.widget.manager.getWidgetById(id);
};

// ---- DnD facades (Track A7) ------------------------------------------------
// Thin proxies so consumer code can reference ps.dnd.* instead of dojo.dnd.*.
// Actual objects are resolved lazily because dojo.dnd is populated after
// dojo.js loads.

ps.dnd = ps.dnd || {};

/**
 * Lazy accessor for dojo.dnd.HtmlDropTarget.
 * Usage:  new ps.dnd.HtmlDropTarget(node, types)
 */
Object.defineProperty(ps.dnd, "HtmlDropTarget", {
  get: function () {
    return dojo.dnd.HtmlDropTarget;
  },
  configurable: true,
});

/**
 * Lazy accessor for dojo.dnd.HtmlDragSource.
 * Usage:  new ps.dnd.HtmlDragSource(node, type)
 */
Object.defineProperty(ps.dnd, "HtmlDragSource", {
  get: function () {
    return dojo.dnd.HtmlDragSource;
  },
  configurable: true,
});

/**
 * Lazy accessor for dojo.dnd.dragManager.
 * Usage:  ps.dnd.dragManager.nestedTargets = true
 */
Object.defineProperty(ps.dnd, "dragManager", {
  get: function () {
    return dojo.dnd.dragManager;
  },
  configurable: true,
});

/**
 * Lazy accessor for dojo.dnd.DragEvent (used as a type reference).
 */
Object.defineProperty(ps.dnd, "DragEvent", {
  get: function () {
    return dojo.dnd.DragEvent;
  },
  configurable: true,
});

// ---- URI facades (Track A7) ------------------------------------------------

ps.uri = ps.uri || {};

/**
 * Resolves a module-relative URI (maps to dojo.uri.moduleUri).
 *
 *   ps.uri.moduleUri("ps", "widget/PSButton.css")
 */
ps.uri.moduleUri = function () {
  return dojo.uri.moduleUri.apply(dojo.uri, arguments);
};

/**
 * Resolves a Dojo-relative URI (maps to dojo.uri.dojoUri).
 *
 *   ps.uri.dojoUri("../ps/widget/images/icon.gif")
 */
ps.uri.dojoUri = function () {
  return dojo.uri.dojoUri.apply(dojo.uri, arguments);
};

// ---- Dojo base-widget type accessors (Track A7) -----------------------------
// Lazy accessors for Dojo built-in widget constructors referenced in ps/* code.
// These are resolved after dojo.js loads because compat.js is sourced first.
//
// NOTE: dojo.widget.MenuBar2 and dojo.widget.MenuBarItem2 are intentionally
// NOT proxied here — our custom ps.widget.MenuBar2 / ps.widget.MenuBarItem2
// classes (defined via ps.widget.defineWidget) share the same property name
// and would conflict with a lazy getter.

(function () {
  var types = [
    "Button",
    "ContentPane",
    "FloatingPane",
    "MenuItem2",
    "ModalFloatingPane",
    "PopupContainerBase",
    "PopupMenu2",
    "SplitContainer",
    "TreeDndControllerV3",
    "TreeDocIconExtension",
    "TreeNodeV3",
    "TreeSelectorV3",
    "TreeV3",
  ];
  for (var i = 0; i < types.length; i++) {
    (function (name) {
      Object.defineProperty(ps.widget, name, {
        get: function () {
          return dojo.widget[name];
        },
        configurable: true,
      });
    })(types[i]);
  }
})();
