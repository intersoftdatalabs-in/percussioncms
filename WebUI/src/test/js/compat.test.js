/**
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Tests for ps/compat.js
 *
 * Verifies that each shim faithfully reproduces the subset of the Dojo 0.4
 * API that our ps/* consumer modules depend on.
 *
 * The file under test is plain browser-global JavaScript, so we load it
 * via readFileSync + eval (same pattern as the perc-common-ui-bundle shim
 * tests).  A minimal `dojo` stub is installed to satisfy the Track A7
 * facades that delegate to the real Dojo runtime.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const COMPAT_PATH = resolve(
  __dirname,
  "../../../../system/cms/content/applications/sys_resources/ApplicationFiles/ps/compat.js",
);

// ---------------------------------------------------------------------------
// Bootstrap: eval compat.js into the global scope before each test suite
// ---------------------------------------------------------------------------
let ps;

/**
 * Installs a minimal dojo stub so the Track A7 facades (widget, dnd, uri)
 * can resolve without the real Dojo runtime.
 */
function installDojoStub() {
  const stub = {
    widget: {
      defineWidget: vi.fn(function () {
        return "defineWidget-result";
      }),
      createWidget: vi.fn(function () {
        return "createWidget-result";
      }),
      byId: vi.fn(function (id) {
        return { widgetId: id };
      }),
      manager: {
        getWidgetById: vi.fn(function (id) {
          return { widgetId: id };
        }),
      },
      // Base-class constructors referenced via lazy accessors
      Button: function Button() {},
      ContentPane: function ContentPane() {},
      FloatingPane: function FloatingPane() {},
      MenuItem2: function MenuItem2() {},
      ModalFloatingPane: function ModalFloatingPane() {},
      PopupContainerBase: { close: vi.fn() },
      PopupMenu2: function PopupMenu2() {},
      SplitContainer: function SplitContainer() {},
      TreeDndControllerV3: function TreeDndControllerV3() {},
      TreeDocIconExtension: function TreeDocIconExtension() {},
      TreeNodeV3: function TreeNodeV3() {},
      TreeSelectorV3: function TreeSelectorV3() {},
      TreeV3: function TreeV3() {},
    },
    dnd: {
      HtmlDropTarget: function HtmlDropTarget() {},
      HtmlDragSource: function HtmlDragSource() {},
      dragManager: { nestedTargets: false, onScroll: vi.fn() },
      DragEvent: function DragEvent() {},
    },
    uri: {
      moduleUri: vi.fn(function () {
        return "/resolved/module/path";
      }),
      dojoUri: vi.fn(function () {
        return "/resolved/dojo/path";
      }),
    },
  };
  globalThis.dojo = stub;
  return stub;
}

beforeEach(() => {
  // Clean slate
  delete globalThis.ps;
  installDojoStub();

  const code = readFileSync(COMPAT_PATH, "utf8");
  // Indirect eval — runs in the global scope so that var declarations in
  // compat.js (e.g. "var ps = ps || {}") become global properties, just
  // like they would in a browser <script> tag.
  (0, eval)(code);

  ps = globalThis.ps;
});

afterEach(() => {
  delete globalThis.ps;
  delete globalThis.dojo;
  vi.restoreAllMocks();
});

// ===========================================================================
// Namespace initialization
// ===========================================================================
describe("Namespace initialization", () => {
  it("creates the ps root namespace", () => {
    expect(ps).toBeDefined();
    expect(typeof ps).toBe("object");
  });

  it("creates all expected sub-namespaces", () => {
    for (const ns of ["aa", "content", "io", "util", "widget", "workflow"]) {
      expect(ps[ns]).toBeDefined();
    }
  });
});

// ===========================================================================
// ps.assert
// ===========================================================================
describe("ps.assert", () => {
  it("does not throw for truthy values", () => {
    expect(() => ps.assert(true)).not.toThrow();
    expect(() => ps.assert(1)).not.toThrow();
    expect(() => ps.assert("non-empty")).not.toThrow();
    expect(() => ps.assert({})).not.toThrow();
  });

  it("throws for falsy values", () => {
    expect(() => ps.assert(false)).toThrow("Assertion failed");
    expect(() => ps.assert(null)).toThrow("Assertion failed");
    expect(() => ps.assert(undefined)).toThrow("Assertion failed");
    expect(() => ps.assert(0)).toThrow("Assertion failed");
    expect(() => ps.assert("")).toThrow("Assertion failed");
  });

  it("includes the custom message in the error", () => {
    expect(() => ps.assert(false, "must be true")).toThrow(
      "Assertion failed: must be true",
    );
  });
});

// ===========================================================================
// ps.assertType
// ===========================================================================
describe("ps.assertType", () => {
  it("passes for string primitives and String type", () => {
    expect(() => ps.assertType("hello", String)).not.toThrow();
  });

  it("passes for number primitives and Number type", () => {
    expect(() => ps.assertType(42, Number)).not.toThrow();
  });

  it("passes for boolean primitives and Boolean type", () => {
    expect(() => ps.assertType(true, Boolean)).not.toThrow();
  });

  it("passes for functions and Function type", () => {
    expect(() => ps.assertType(function () {}, Function)).not.toThrow();
  });

  it("passes for arrays and Array type", () => {
    expect(() => ps.assertType([1, 2], Array)).not.toThrow();
  });

  it("passes for instanceof checks with custom constructors", () => {
    function MyClass() {}
    const obj = new MyClass();
    expect(() => ps.assertType(obj, MyClass)).not.toThrow();
  });

  it("throws when type does not match", () => {
    expect(() => ps.assertType(42, String)).toThrow("assertType failed");
    expect(() => ps.assertType("str", Number)).toThrow("assertType failed");
    expect(() => ps.assertType(true, String)).toThrow("assertType failed");
  });
});

// ===========================================================================
// ps.isNumeric / ps.isBoolean / ps.isString
// ===========================================================================
describe("Type-check utilities", () => {
  describe("ps.isNumeric", () => {
    it("returns true for numbers and numeric strings", () => {
      expect(ps.isNumeric(42)).toBe(true);
      expect(ps.isNumeric(3.14)).toBe(true);
      expect(ps.isNumeric("42")).toBe(true);
      expect(ps.isNumeric("3.14")).toBe(true);
      expect(ps.isNumeric(0)).toBe(true);
    });

    it("returns false for non-numeric values", () => {
      expect(ps.isNumeric("abc")).toBe(false);
      expect(ps.isNumeric(NaN)).toBe(false);
      expect(ps.isNumeric(Infinity)).toBe(false);
      expect(ps.isNumeric(null)).toBe(false);
      expect(ps.isNumeric(undefined)).toBe(false);
    });
  });

  describe("ps.isBoolean", () => {
    it("returns true for boolean primitives", () => {
      expect(ps.isBoolean(true)).toBe(true);
      expect(ps.isBoolean(false)).toBe(true);
    });

    it("returns false for non-booleans", () => {
      expect(ps.isBoolean(0)).toBe(false);
      expect(ps.isBoolean("true")).toBe(false);
      expect(ps.isBoolean(null)).toBe(false);
    });
  });

  describe("ps.isString", () => {
    it("returns true for string primitives", () => {
      expect(ps.isString("")).toBe(true);
      expect(ps.isString("hello")).toBe(true);
    });

    it("returns false for non-strings", () => {
      expect(ps.isString(42)).toBe(false);
      expect(ps.isString(null)).toBe(false);
      expect(ps.isString(undefined)).toBe(false);
    });
  });
});

// ===========================================================================
// ps.Stack
// ===========================================================================
describe("ps.Stack", () => {
  it("starts empty when constructed without arguments", () => {
    const s = new ps.Stack();
    expect(s.count).toBe(0);
  });

  it("initializes from an array (shallow-copied)", () => {
    const source = [1, 2, 3];
    const s = new ps.Stack(source);
    expect(s.count).toBe(3);
    // Modifying source should not affect stack
    source.push(4);
    expect(s.count).toBe(3);
  });

  it("push / pop / peek follow LIFO order", () => {
    const s = new ps.Stack();
    s.push("a");
    s.push("b");
    s.push("c");
    expect(s.count).toBe(3);
    expect(s.peek()).toBe("c");
    expect(s.pop()).toBe("c");
    expect(s.pop()).toBe("b");
    expect(s.count).toBe(1);
    expect(s.peek()).toBe("a");
  });

  it("peek returns undefined on empty stack", () => {
    const s = new ps.Stack();
    expect(s.peek()).toBeUndefined();
  });

  it("pop returns undefined on empty stack", () => {
    const s = new ps.Stack();
    expect(s.pop()).toBeUndefined();
  });

  it("clear empties the stack", () => {
    const s = new ps.Stack([1, 2, 3]);
    s.clear();
    expect(s.count).toBe(0);
    expect(s.toArray()).toEqual([]);
  });

  it("toArray returns a shallow copy", () => {
    const s = new ps.Stack([1, 2, 3]);
    const arr = s.toArray();
    expect(arr).toEqual([1, 2, 3]);
    arr.push(4);
    expect(s.count).toBe(3); // original unaffected
  });
});

// ===========================================================================
// ps.util._getCookie / _setCookie
// ===========================================================================
describe("Cookie helpers", () => {
  it("_setCookie writes to document.cookie and _getCookie reads it back", () => {
    ps.util._setCookie("testkey", "testval", 1);
    expect(ps.util._getCookie("testkey")).toBe("testval");
  });

  it("_getCookie returns null for a missing cookie", () => {
    expect(ps.util._getCookie("nonexistent_cookie_key_xyz")).toBeNull();
  });

  it("_setCookie handles special characters in the value", () => {
    ps.util._setCookie("special", "a=b&c d", 1);
    expect(ps.util._getCookie("special")).toBe("a=b&c d");
  });
});

// ===========================================================================
// ps.util._createNodesFromText
// ===========================================================================
describe("ps.util._createNodesFromText", () => {
  it("parses a simple HTML string", () => {
    const nodes = ps.util._createNodesFromText("<p>Hello</p>");
    expect(nodes.length).toBe(1);
    expect(nodes[0].nodeName.toLowerCase()).toBe("p");
    expect(nodes[0].textContent).toBe("Hello");
  });

  it("parses multiple sibling nodes", () => {
    const nodes = ps.util._createNodesFromText("<span>A</span><span>B</span>");
    expect(nodes.length).toBe(2);
    expect(nodes[0].textContent).toBe("A");
    expect(nodes[1].textContent).toBe("B");
  });

  it("returns an empty array for empty string", () => {
    const nodes = ps.util._createNodesFromText("");
    expect(nodes.length).toBe(0);
  });
});

// ===========================================================================
// ps.collections.ArrayList
// ===========================================================================
describe("ps.collections.ArrayList", () => {
  it("starts empty when no array is provided", () => {
    const list = new ps.collections.ArrayList();
    expect(list.count).toBe(0);
    expect(list.toArray()).toEqual([]);
  });

  it("initializes from an array (shallow-copied)", () => {
    const src = [1, 2, 3];
    const list = new ps.collections.ArrayList(src);
    expect(list.count).toBe(3);
    src.push(4);
    expect(list.count).toBe(3); // not affected
  });

  it("add appends items and updates count", () => {
    const list = new ps.collections.ArrayList();
    list.add("a");
    list.add("b");
    expect(list.count).toBe(2);
    expect(list.toArray()).toEqual(["a", "b"]);
  });

  it("addRange from an array", () => {
    const list = new ps.collections.ArrayList([1]);
    list.addRange([2, 3]);
    expect(list.count).toBe(3);
    expect(list.toArray()).toEqual([1, 2, 3]);
  });

  it("addRange from another ArrayList", () => {
    const a = new ps.collections.ArrayList([1, 2]);
    const b = new ps.collections.ArrayList([3, 4]);
    a.addRange(b);
    expect(a.count).toBe(4);
    expect(a.toArray()).toEqual([1, 2, 3, 4]);
  });

  it("clear empties the list", () => {
    const list = new ps.collections.ArrayList([1, 2, 3]);
    list.clear();
    expect(list.count).toBe(0);
    expect(list.toArray()).toEqual([]);
  });

  it("contains uses == comparison", () => {
    const list = new ps.collections.ArrayList([1, 2, 3]);
    expect(list.contains(2)).toBe(true);
    expect(list.contains(99)).toBe(false);
    // == coercion: "2" should match 2
    expect(list.contains("2")).toBe(true);
  });

  it("indexOf returns correct indices", () => {
    const list = new ps.collections.ArrayList(["a", "b", "c"]);
    expect(list.indexOf("b")).toBe(1);
    expect(list.indexOf("z")).toBe(-1);
  });

  it("item returns the element at an index", () => {
    const list = new ps.collections.ArrayList([10, 20, 30]);
    expect(list.item(0)).toBe(10);
    expect(list.item(2)).toBe(30);
    expect(list.item(5)).toBeUndefined();
  });

  it("remove removes the first occurrence", () => {
    const list = new ps.collections.ArrayList(["a", "b", "c", "b"]);
    list.remove("b");
    expect(list.count).toBe(3);
    expect(list.toArray()).toEqual(["a", "c", "b"]);
  });

  it("remove does nothing when item is not found", () => {
    const list = new ps.collections.ArrayList([1, 2]);
    list.remove(99);
    expect(list.count).toBe(2);
  });

  it("setByIndex replaces an item", () => {
    const list = new ps.collections.ArrayList(["x", "y", "z"]);
    list.setByIndex(1, "Y");
    expect(list.item(1)).toBe("Y");
  });

  it("forEach iterates all items", () => {
    const list = new ps.collections.ArrayList([10, 20, 30]);
    const collected = [];
    list.forEach(function (item) {
      collected.push(item);
    });
    expect(collected).toEqual([10, 20, 30]);
  });

  it("forEach respects scope parameter", () => {
    const list = new ps.collections.ArrayList([1]);
    const scope = { result: null };
    list.forEach(function (item) {
      this.result = item;
    }, scope);
    expect(scope.result).toBe(1);
  });

  it("toArray returns a shallow copy", () => {
    const list = new ps.collections.ArrayList([1, 2]);
    const arr = list.toArray();
    arr.push(3);
    expect(list.count).toBe(2); // unaffected
  });
});

// ===========================================================================
// ps.collections.Dictionary
// ===========================================================================
describe("ps.collections.Dictionary", () => {
  it("starts empty", () => {
    const dict = new ps.collections.Dictionary();
    expect(dict.count).toBe(0);
  });

  it("add stores key-value pairs and updates count", () => {
    const dict = new ps.collections.Dictionary();
    dict.add("a", 1);
    dict.add("b", 2);
    expect(dict.count).toBe(2);
    expect(dict.item("a")).toBe(1);
    expect(dict.item("b")).toBe(2);
  });

  it("add overwrites without incrementing count", () => {
    const dict = new ps.collections.Dictionary();
    dict.add("k", "v1");
    dict.add("k", "v2");
    expect(dict.count).toBe(1);
    expect(dict.item("k")).toBe("v2");
  });

  it("containsKey and contains are aliases", () => {
    const dict = new ps.collections.Dictionary();
    dict.add("x", 10);
    expect(dict.containsKey("x")).toBe(true);
    expect(dict.contains("x")).toBe(true);
    expect(dict.containsKey("nope")).toBe(false);
  });

  it("item returns undefined for missing keys", () => {
    const dict = new ps.collections.Dictionary();
    expect(dict.item("missing")).toBeUndefined();
  });

  it("remove deletes entries and decrements count", () => {
    const dict = new ps.collections.Dictionary();
    dict.add("a", 1);
    dict.add("b", 2);
    const result = dict.remove("a");
    expect(result).toBe(true);
    expect(dict.count).toBe(1);
    expect(dict.containsKey("a")).toBe(false);
  });

  it("remove returns false for non-existent keys", () => {
    const dict = new ps.collections.Dictionary();
    expect(dict.remove("nope")).toBe(false);
  });
});

// ===========================================================================
// ps.declare
// ===========================================================================
describe("ps.declare", () => {
  afterEach(() => {
    // Clean up any declared classes from globalThis
    delete globalThis.TestNs;
  });

  it("creates a constructor registered in the global namespace", () => {
    globalThis.TestNs = {};
    ps.declare("TestNs.MyClass", null, function () {
      this.initialized = true;
    });
    expect(typeof TestNs.MyClass).toBe("function");
    const obj = new TestNs.MyClass();
    expect(obj.initialized).toBe(true);
  });

  it("sets up single inheritance via prototype chain", () => {
    globalThis.TestNs = {};
    ps.declare(
      "TestNs.Base",
      null,
      function () {
        this.base = true;
      },
      {
        greet: function () {
          return "hello";
        },
      },
    );
    ps.declare("TestNs.Child", TestNs.Base, function () {
      this.child = true;
    });
    const obj = new TestNs.Child();
    expect(obj.child).toBe(true);
    expect(obj.greet()).toBe("hello");
    expect(obj instanceof TestNs.Base).toBe(true);
  });

  it("supports the 3-arg overload (name, super, proto) without initFn", () => {
    globalThis.TestNs = {};
    ps.declare("TestNs.Simple", null, {
      value: 42,
      getValue: function () {
        return this.value;
      },
    });
    const obj = new TestNs.Simple();
    expect(obj.getValue()).toBe(42);
  });

  it("mixes proto methods into the prototype", () => {
    globalThis.TestNs = {};
    ps.declare("TestNs.WithProto", null, function () {}, {
      foo: function () {
        return "bar";
      },
    });
    const obj = new TestNs.WithProto();
    expect(obj.foo()).toBe("bar");
  });
});

// ===========================================================================
// ps.event.connect
// ===========================================================================
describe("ps.event.connect", () => {
  it("calls the listener after the original method (3-arg form)", () => {
    const calls = [];
    const src = {
      doStuff: function () {
        calls.push("original");
      },
    };
    ps.event.connect(src, "doStuff", function () {
      calls.push("listener");
    });
    src.doStuff();
    expect(calls).toEqual(["original", "listener"]);
  });

  it("calls scope[handler] after the original method (4-arg form)", () => {
    const calls = [];
    const src = {
      doStuff: function () {
        calls.push("original");
      },
    };
    const scope = {
      myHandler: function () {
        calls.push("scoped");
      },
    };
    ps.event.connect(src, "doStuff", scope, "myHandler");
    src.doStuff();
    expect(calls).toEqual(["original", "scoped"]);
  });

  it("returns the original method's return value", () => {
    const src = {
      getValue: function () {
        return 42;
      },
    };
    ps.event.connect(src, "getValue", function () {});
    expect(src.getValue()).toBe(42);
  });

  it("forwards arguments to both original and listener", () => {
    const receivedArgs = [];
    const src = {
      act: function (a, b) {
        return a + b;
      },
    };
    ps.event.connect(src, "act", function (a, b) {
      receivedArgs.push(a, b);
    });
    src.act(3, 7);
    expect(receivedArgs).toEqual([3, 7]);
  });

  it("works when the original method does not exist yet", () => {
    const src = {};
    const calls = [];
    ps.event.connect(src, "newMethod", function () {
      calls.push("called");
    });
    src.newMethod();
    expect(calls).toEqual(["called"]);
  });
});

// ===========================================================================
// ps.event.connectAround
// ===========================================================================
describe("ps.event.connectAround", () => {
  it("wraps the method with around-advice", () => {
    const calls = [];
    const src = {
      compute: function () {
        calls.push("original");
        return 10;
      },
    };
    const advisor = {
      myAdvice: function (invocation) {
        calls.push("before");
        var result = invocation.proceed();
        calls.push("after");
        return result + 5;
      },
    };
    ps.event.connectAround(src, "compute", advisor, "myAdvice");
    const result = src.compute();
    expect(calls).toEqual(["before", "original", "after"]);
    expect(result).toBe(15);
  });

  it("provides invocation.args and invocation.object", () => {
    const src = {
      add: function (a, b) {
        return a + b;
      },
    };
    let capturedArgs, capturedObj;
    const advisor = {
      spy: function (invocation) {
        capturedArgs = Array.from(invocation.args);
        capturedObj = invocation.object;
        return invocation.proceed();
      },
    };
    ps.event.connectAround(src, "add", advisor, "spy");
    src.add(3, 4);
    expect(capturedArgs).toEqual([3, 4]);
    expect(capturedObj).toBe(src);
  });

  it("allows skipping the original method", () => {
    const src = {
      risky: function () {
        throw new Error("should not run");
      },
    };
    const advisor = {
      guard: function (_invocation) {
        return "safe";
      },
    };
    ps.event.connectAround(src, "risky", advisor, "guard");
    expect(src.risky()).toBe("safe");
  });
});

// ===========================================================================
// ps.event.connectBefore
// ===========================================================================
describe("ps.event.connectBefore", () => {
  it("calls the listener before the original method", () => {
    const calls = [];
    const src = {
      action: function () {
        calls.push("original");
      },
    };
    ps.event.connectBefore(src, "action", function () {
      calls.push("before");
    });
    src.action();
    expect(calls).toEqual(["before", "original"]);
  });

  it("supports the 4-arg scope/handler form", () => {
    const calls = [];
    const src = {
      action: function () {
        calls.push("original");
      },
    };
    const scope = {
      prep: function () {
        calls.push("prep");
      },
    };
    ps.event.connectBefore(src, "action", scope, "prep");
    src.action();
    expect(calls).toEqual(["prep", "original"]);
  });
});

// ===========================================================================
// ps.event.topic (pub/sub)
// ===========================================================================
describe("ps.event.topic", () => {
  beforeEach(() => {
    // Reset topics between tests
    ps.event._topics = {};
  });

  it("subscribe + publish delivers messages to listeners", () => {
    const received = [];
    ps.event.topic.subscribe("myTopic", function (data) {
      received.push(data);
    });
    ps.event.topic.publish("myTopic", "hello");
    ps.event.topic.publish("myTopic", "world");
    expect(received).toEqual(["hello", "world"]);
  });

  it("supports the scope/handler subscribe form", () => {
    const scope = {
      messages: [],
      handler: function (msg) {
        this.messages.push(msg);
      },
    };
    ps.event.topic.subscribe("scoped", scope, "handler");
    ps.event.topic.publish("scoped", "test");
    expect(scope.messages).toEqual(["test"]);
  });

  it("publishes to multiple subscribers", () => {
    const a = [],
      b = [];
    ps.event.topic.subscribe("multi", function (d) {
      a.push(d);
    });
    ps.event.topic.subscribe("multi", function (d) {
      b.push(d);
    });
    ps.event.topic.publish("multi", 42);
    expect(a).toEqual([42]);
    expect(b).toEqual([42]);
  });

  it("publish with no subscribers does not throw", () => {
    expect(() => ps.event.topic.publish("nobody", "data")).not.toThrow();
  });

  it("publish passes multiple arguments", () => {
    let captured;
    ps.event.topic.subscribe("multiArg", function (a, b, c) {
      captured = [a, b, c];
    });
    ps.event.topic.publish("multiArg", 1, 2, 3);
    expect(captured).toEqual([1, 2, 3]);
  });
});

// ===========================================================================
// Track A7 — Widget facades
// ===========================================================================
describe("Widget facades (Track A7)", () => {
  it("ps.widget.defineWidget delegates to dojo.widget.defineWidget", () => {
    const result = ps.widget.defineWidget("name", null, {});
    expect(result).toBe("defineWidget-result");
    expect(dojo.widget.defineWidget).toHaveBeenCalledWith("name", null, {});
  });

  it("ps.widget.createWidget delegates to dojo.widget.createWidget", () => {
    const result = ps.widget.createWidget("Button", { label: "OK" });
    expect(result).toBe("createWidget-result");
    expect(dojo.widget.createWidget).toHaveBeenCalledWith("Button", {
      label: "OK",
    });
  });

  it("ps.widget.byId delegates to dojo.widget.byId", () => {
    const result = ps.widget.byId("myWidget");
    expect(result).toEqual({ widgetId: "myWidget" });
    expect(dojo.widget.byId).toHaveBeenCalledWith("myWidget");
  });

  it("ps.widget.manager.getWidgetById delegates to dojo", () => {
    const result = ps.widget.manager.getWidgetById("w1");
    expect(result).toEqual({ widgetId: "w1" });
    expect(dojo.widget.manager.getWidgetById).toHaveBeenCalledWith("w1");
  });
});

// ===========================================================================
// Track A7 — Base-widget type lazy accessors
// ===========================================================================
describe("Widget type lazy accessors (Track A7)", () => {
  const expectedTypes = [
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

  for (const typeName of expectedTypes) {
    it(`ps.widget.${typeName} resolves to dojo.widget.${typeName}`, () => {
      expect(ps.widget[typeName]).toBe(dojo.widget[typeName]);
    });
  }
});

// ===========================================================================
// Track A7 — DnD facades
// ===========================================================================
describe("DnD facades (Track A7)", () => {
  it("ps.dnd.HtmlDropTarget resolves to dojo.dnd.HtmlDropTarget", () => {
    expect(ps.dnd.HtmlDropTarget).toBe(dojo.dnd.HtmlDropTarget);
  });

  it("ps.dnd.HtmlDragSource resolves to dojo.dnd.HtmlDragSource", () => {
    expect(ps.dnd.HtmlDragSource).toBe(dojo.dnd.HtmlDragSource);
  });

  it("ps.dnd.dragManager resolves to dojo.dnd.dragManager", () => {
    expect(ps.dnd.dragManager).toBe(dojo.dnd.dragManager);
  });

  it("ps.dnd.DragEvent resolves to dojo.dnd.DragEvent", () => {
    expect(ps.dnd.DragEvent).toBe(dojo.dnd.DragEvent);
  });
});

// ===========================================================================
// Track A7 — URI facades
// ===========================================================================
describe("URI facades (Track A7)", () => {
  it("ps.uri.moduleUri delegates to dojo.uri.moduleUri", () => {
    const result = ps.uri.moduleUri("ps", "widget/Tree.css");
    expect(result).toBe("/resolved/module/path");
    expect(dojo.uri.moduleUri).toHaveBeenCalledWith("ps", "widget/Tree.css");
  });

  it("ps.uri.dojoUri delegates to dojo.uri.dojoUri", () => {
    const result = ps.uri.dojoUri("../ps/widget/images/icon.gif");
    expect(result).toBe("/resolved/dojo/path");
    expect(dojo.uri.dojoUri).toHaveBeenCalledWith(
      "../ps/widget/images/icon.gif",
    );
  });
});
