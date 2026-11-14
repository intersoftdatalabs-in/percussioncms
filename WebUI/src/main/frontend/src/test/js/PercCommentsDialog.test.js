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
 * Regression tests for the comments-dialog service-error path
 * (issue #6338; residual of #6287's defect class).
 *
 * Both copies — `WebUI/src/main/webapp/cm/plugins/PercCommentsDialog.js`
 * and `WebUI/src/main/webapp/cm/app/js/legacy/plugins/PercCommentsDialog.js`
 * — called the undefined bare identifier `callback(false)` in the
 * `STATUS_ERROR` branch of `createDialog`. When the comments service call
 * failed, the error branch threw
 * `ReferenceError: callback is not defined` after rendering the alert.
 *
 * Test strategy: load the live canonical source end-to-end via
 * `$.PercCommentsDialog.open()` with a stubbed `$.PercRevisionService`
 * that reports STATUS_ERROR, then assert the alert fires and no
 * ReferenceError escapes. A source-pattern test pins both copies so any
 * reintroduction of the bare `callback(` call fails immediately.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const CANONICAL_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/plugins/PercCommentsDialog.js",
);
const TWIN_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/app/js/legacy/plugins/PercCommentsDialog.js",
);

let $;
let alertCalls;
let serviceCallback;

function loadSource() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;

  alertCalls = [];
  serviceCallback = null;

  $.PercServiceUtils = {
    STATUS_ERROR: "error",
    extractDefaultErrorMessage: () => "service failure",
  };
  $.perc_utils = {
    alert_dialog: (opts) => {
      alertCalls.push(opts);
    },
  };
  // Capture the service callback so tests can drive either branch.
  $.PercRevisionService = {
    getRevisionDetails: (itemId, cb) => {
      serviceCallback = cb;
    },
  };

  globalThis.I18N = { message: (key) => `[${key}]` };
  // Run the source as a script in the global scope so its IIFE sees
  // globalThis.jQuery / globalThis.I18N.
  (0, eval)(readFileSync(CANONICAL_PATH, "utf8"));
}

beforeEach(() => {
  document.body.innerHTML = "";
  loadSource();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
  delete globalThis.I18N;
});

// ---------------------------------------------------------------------------
// Source-pattern tests — pin the fix in both shipped copies (canonical +
// legacy twin). Comments are stripped before matching so we don't
// false-positive on explanatory comments.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for #6338)", () => {
  for (const [label, path] of [
    ["canonical", CANONICAL_PATH],
    ["legacy twin", TWIN_PATH],
  ]) {
    const src = readFileSync(path, "utf8");
    const code = src
      .replace(/\/\*[\s\S]*?\*\//g, "")
      .replace(/(^|[^:])\/\/.*$/gm, "$1");

    it(`${label} copy has no bare callback( invocation`, () => {
      // `createDialog` takes (status, result) — there is no callback
      // in scope, so any bare callback( call throws ReferenceError.
      expect(code).not.toMatch(/(^|[^\w$.])callback\s*\(/);
    });
  }

  it("canonical and legacy twin copies stay in sync on the error branch", () => {
    const canon = readFileSync(CANONICAL_PATH, "utf8");
    const twin = readFileSync(TWIN_PATH, "utf8");
    expect(twin).toBe(canon);
  });
});

// ---------------------------------------------------------------------------
// Behavioural tests — drive the live error path end-to-end.
// ---------------------------------------------------------------------------
describe("service-error path", () => {
  it("alerts and returns without throwing when the service call fails", () => {
    expect(() =>
      $.PercCommentsDialog.open("item-id", "item-name", "page"),
    ).not.toThrow();
    expect(serviceCallback, "service must have been invoked").toBeTruthy();
    expect(() => serviceCallback("error", { request: {} })).not.toThrow();
    expect(alertCalls.length).toBe(1);
    expect(alertCalls[0].title).toBe("Error");
    expect(alertCalls[0].content).toBe("service failure");
  });

  it("exposes the documented dialog API", () => {
    expect(typeof $.PercCommentsDialog.open).toBe("function");
    expect($.PercCommentsDialog.ITEM_TYPE_ASSET).toBe("asset");
    expect($.PercCommentsDialog.ITEM_TYPE_PAGE).toBe("page");
    expect($.PercCommentsDialog.ITEM_MODE_EDIT).toBe("edit");
    expect($.PercCommentsDialog.ITEM_MODE_VIEW).toBe("view");
  });
});
