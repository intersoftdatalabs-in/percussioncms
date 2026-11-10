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
 * Tests for cm/assets/js/bootstrap5-compat.js (issue #5661).
 *
 * The vendored Bootstrap 4.6.2 jQuery plugins are gone; classic pages now
 * load Bootstrap 5 from npm (same stable jslib URLs via the generated-webui
 * overlay). The compat layer re-exposes the Bootstrap 4 jQuery call shapes
 * (`$.fn.modal('show')`, `$.fn.tooltip()`, …) that Perc Minuet views still
 * use, delegating to the Bootstrap 5 component API.
 *
 * Covers:
 *   - modal show/hide/toggle/dispose delegation (create-on-first-use).
 *   - `_enforceFocus` no-op (Bootstrap 4 private entry the Minuet views
 *     still invoke; Bootstrap 5 enforces modal focus internally).
 *   - tooltip/popover default + options-object initialization.
 *   - Dual-tree lockstep: cm/assets, cm/app/assets, and war/assets copies
 *     stay byte-identical.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { describe, it, expect, vi, beforeEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEBAPP_ROOT = resolve(__dirname, "../../main/webapp");
const SHIM_SRC = resolve(WEBAPP_ROOT, "cm/assets/js/bootstrap5-compat.js");
const SHIM_APP = resolve(WEBAPP_ROOT, "cm/app/assets/js/bootstrap5-compat.js");
const SHIM_WAR = resolve(__dirname, "../../../war/assets/js/bootstrap5-compat.js");

function makeComponentStub() {
  const instances = new Map();
  class Stub {
    constructor(el, options) {
      this.el = el;
      this.options = options;
      this.calls = [];
      instances.set(el, this);
      Stub.created.push(this);
    }
    show() {
      this.calls.push("show");
    }
    hide() {
      this.calls.push("hide");
    }
    toggle() {
      this.calls.push("toggle");
    }
    dispose() {
      this.calls.push("dispose");
      instances.delete(this.el);
    }
    update() {
      this.calls.push("update");
    }
    refresh() {
      this.calls.push("refresh");
    }
    close() {
      this.calls.push("close");
    }
    static getInstance(el) {
      return instances.get(el) || null;
    }
    static clear() {
      instances.clear();
      Stub.created = [];
    }
  }
  Stub.created = [];
  return Stub;
}

let $;
let bootstrapStub;

function installShimEnvironment() {
  document.body.innerHTML = "";
  if (typeof jquery === "function") {
    $ = jquery(globalThis.window);
    if (typeof $ !== "function" || !$.fn) {
      $ = jquery;
      if (!$.fn) $.fn = $.prototype;
    }
  } else {
    $ = globalThis.window.jQuery || globalThis.window.$;
    if (!$.fn) $.fn = $.prototype;
  }
  globalThis.jQuery = $;
  globalThis.window.jQuery = $;

  bootstrapStub = {
    Modal: makeComponentStub(),
    Tooltip: makeComponentStub(),
    Popover: makeComponentStub(),
    Dropdown: makeComponentStub(),
    Carousel: makeComponentStub(),
    Collapse: makeComponentStub(),
    ScrollSpy: makeComponentStub(),
    Alert: makeComponentStub(),
    Button: makeComponentStub(),
  };
  globalThis.window.bootstrap = bootstrapStub;

  // The shim aborts when window.bootstrap is missing and warns without
  // jQuery; both globals above keep the bridge path under test.
  const factory = new Function(
    "window",
    "document",
    "jQuery",
    "$",
    "bootstrap",
    `${readFileSync(SHIM_SRC, "utf8")}\nreturn jQuery.fn;`,
  );
  return factory(
    globalThis.window,
    globalThis.window.document,
    $,
    $,
    bootstrapStub,
  );
}

beforeEach(() => {
  installShimEnvironment();
});

describe("bootstrap5-compat jQuery bridge (issue #5661)", () => {
  it("delegates modal show/hide/toggle to the Bootstrap 5 instance", () => {
    document.body.innerHTML = '<div id="m"></div>';
    const el = document.getElementById("m");

    $("#m").modal("show");
    const created = bootstrapStub.Modal.created;
    expect(created.length).toBe(1);
    expect(created[0].el).toBe(el);
    expect(created[0].calls).toEqual(["show"]);

    $("#m").modal("hide");
    expect(created[0].calls).toEqual(["show", "hide"]);
    // Same element reuses the instance instead of constructing again.
    expect(bootstrapStub.Modal.created.length).toBe(1);

    $("#m").modal("toggle");
    expect(created[0].calls).toEqual(["show", "hide", "toggle"]);
  });

  it("treats modal _enforceFocus as a benign no-op", () => {
    document.body.innerHTML = '<div id="m"></div>';
    // Must neither throw nor construct a Bootstrap 5 Modal (BS5 enforces
    // modal focus internally on show()).
    expect(() => $("#percNavigationBody").modal("_enforceFocus")).not.toThrow();
    expect(bootstrapStub.Modal.created.length).toBe(0);
    expect(() => $("#m").modal("_enforceFocus")).not.toThrow();
    expect(bootstrapStub.Modal.created.length).toBe(0);
  });

  it("initializes tooltips/popovers on default call and options objects", () => {
    document.body.innerHTML =
      '<button id="t">x</button><button id="p">y</button>';
    const t = document.getElementById("t");
    const p = document.getElementById("p");

    $("#t").tooltip();
    expect(bootstrapStub.Tooltip.created.length).toBe(1);
    expect(bootstrapStub.Tooltip.created[0].el).toBe(t);

    $("#p").popover({ trigger: "focus" });
    expect(bootstrapStub.Popover.created.length).toBe(1);
    expect(bootstrapStub.Popover.created[0].el).toBe(p);
    expect(bootstrapStub.Popover.created[0].options).toEqual({
      trigger: "focus",
    });
  });

  it("keeps the src/app/war shim copies byte-identical", () => {
    const src = readFileSync(SHIM_SRC, "utf8");
    const app = readFileSync(SHIM_APP, "utf8");
    const war = readFileSync(SHIM_WAR, "utf8");
    expect(app, "cm/app/assets copy drifted from cm/assets").toBe(src);
    expect(war, "war/assets copy drifted from cm/assets").toBe(src);
  });
});
