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
 * Behavioral parity for the npm-served blockUI + cookie pair
 * (issue #7177, parent audit #5627).
 *
 * The deleted malsup blockUI 2.70 copy (locally reformatted) is
 * functionally identical to the upstream `block-ui` 2.70.1 dist, and the
 * deleted bannerless 1KB `$.cookie` rewrite is a modern-syntax
 * reimplementation of the Klaus Hartl `jquery.cookie` 1.4.1 write path —
 * so both stable jslib URLs are now served from those Dependabot-tracked
 * npm dists. Both plugins have live in-repo callers (unlike the #7176
 * dropdown): `$.blockUI()` / `$.unblockUI()` across the classic dialogs
 * and views, and `$.cookie(name[, value])` reads/writes in navigation,
 * template-design, and confirm-dialog flows.
 *
 * The harness mirrors the production script order in common_js.jsp
 * (jquery core, then jquery-migrate, then the plugins): jquery 4 dropped
 * `$.isFunction`, which both dists touch, and the migrate script that
 * common_js.jsp loads before the plugins restores it.
 *
 * Recorded deltas pinned here: upstream blockUI keeps the CRLF/tabs
 * formatting and the `$.isFunction(setExpression)` check; upstream cookie
 * returns `undefined` (not `null`) for a missing key and lacks the
 * rewrite's unused `sameSite` option. Any npm upgrade that changes this
 * surface fails here instead of silently changing classic UI behavior.
 */

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import jquery from "jquery";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = dirname(dirname(dirname(__dirname)));
const NPM_ROOT = join(FRONTEND_ROOT, "node_modules");

function loadPlugins() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  globalThis.jQuery = jq;
  globalThis.$ = jq;

  // Production order: core -> migrate (restores $.isFunction on jquery 4)
  // -> blockUI -> cookie.
  for (const file of [
    join(NPM_ROOT, "jquery-migrate/dist/jquery-migrate.js"),
    join(NPM_ROOT, "block-ui/jquery.blockUI.js"),
    join(NPM_ROOT, "jquery.cookie/jquery.cookie.js"),
  ]) {
    const src = readFileSync(file, "utf8");
    const run = new Function("window", "jQuery", "$", src);
    run(globalThis.window, jq, jq);
  }
  // Synchronous animation completion: block/unblock assert on the DOM.
  jq.fx.off = true;
  return jq;
}

const $ = loadPlugins();

describe("malsup blockUI served from npm (issue #7177)", () => {
  it("exposes the blocking entry points used by the classic call sites", () => {
    expect(typeof $.blockUI).toBe("function");
    expect(typeof $.unblockUI).toBe("function");
    expect(typeof $.growlUI).toBe("function");
    expect(typeof $.fn.block).toBe("function");
    expect(typeof $.fn.unblock).toBe("function");
    expect($.blockUI.version).toBe(2.7);
  });

  it("blocks and unblocks the page with the no-arg call shape", () => {
    document.body.innerHTML = `<div id="work">content</div>`;
    expect(() => $.blockUI()).not.toThrow();
    expect(document.querySelectorAll(".blockUI.blockPage").length).toBe(1);
    expect(() => $.unblockUI()).not.toThrow();
    expect(document.querySelectorAll(".blockUI.blockPage").length).toBe(0);
  });

  it("blocks and unblocks a single element with a message", () => {
    document.body.innerHTML = `<div id="panel">panel</div>`;
    const panel = $("#panel");
    expect(() => panel.block({ message: "Please wait..." })).not.toThrow();
    // Overlay + message layers land inside the blocked element...
    expect(panel.find("div.blockOverlay").length).toBe(1);
    expect(panel.text()).toContain("Please wait...");
    expect(() => panel.unblock()).not.toThrow();
    // ...and every blocking layer is removed on unblock.
    expect(panel.find(".blockUI").length).toBe(0);
  });

  it("ships the malsup dual-license banner and version stamp in the npm dist", () => {
    const dist = readFileSync(join(NPM_ROOT, "block-ui/jquery.blockUI.js"), "utf8");
    expect(dist).toMatch(/Version 2\.70/);
    expect(dist).toMatch(/Dual licensed under the MIT and GPL licenses/);
    expect(dist).toMatch(/M\. Alsup/);
  });
});

describe("Klaus Hartl cookie served from npm (issue #7177)", () => {
  it("round-trips the getter/setter shapes used by the classic call sites", () => {
    $.cookie("perc_isAdmin", "true");
    expect($.cookie("perc_isAdmin")).toBe("true");
    // Template-design panel settings: JSON payload through the cookie.
    const settings = { showPanel: true, pageNumber: 2 };
    $.cookie("perc-unassigned-panel-site-settings", JSON.stringify(settings));
    expect(
      JSON.parse($.cookie("perc-unassigned-panel-site-settings")),
    ).toEqual(settings);
    // Confirm-dialog "do not show again" flag.
    $.cookie("dontShowAgain-admin-id", "true");
    expect($.cookie("dontShowAgain-admin-id") === "true").toBe(true);
  });

  it("documents the missing-key delta (undefined, not the rewrite null)", () => {
    expect($.cookie("perc-no-such-cookie-7177")).toBeUndefined();
    // The two JSON.parse($.cookie(...)) readers in PercTemplateDesignView
    // default to "null" after the swap: same-as-before result, and it also
    // hardens the pre-existing JSON.parse("") throw on an empty value.
    expect(JSON.parse($.cookie("perc-no-such-cookie-7177") || "null")).toBeNull();
  });

  it("round-trips values needing encoding (spaces, separators, plus)", () => {
    $.cookie("perc-enc-7177", "a b;c=d+e");
    expect($.cookie("perc-enc-7177")).toBe("a b;c=d+e");
  });

  it("persists the expires/path shapes and removes on demand", () => {
    $.cookie("perc-exp-7177", "stays", { expires: 7, path: "/" });
    expect($.cookie("perc-exp-7177")).toBe("stays");
    expect($.removeCookie("perc-exp-7177", { path: "/" })).toBe(true);
    expect($.cookie("perc-exp-7177")).toBeUndefined();
  });

  it("carries the expires/path/domain/secure builders in the npm dist", () => {
    const dist = readFileSync(join(NPM_ROOT, "jquery.cookie/jquery.cookie.js"), "utf8");
    for (const needle of [
      "options.expires",
      "'; expires='",
      "'; path='",
      "'; domain='",
      "'; secure'",
    ]) {
      expect(dist, `npm cookie dist lost ${needle}`).toContain(needle);
    }
    // The rewrite's sameSite option has zero in-repo callers and no
    // jquery.cookie 1.4.1 counterpart: pin the absence so a sameSite need
    // forces a conscious follow-up instead of a silent no-op.
    expect(dist).not.toMatch(/sameSite/i);
  });

  it("ships the Klaus Hartl MIT banner in the npm dist", () => {
    const dist = readFileSync(join(NPM_ROOT, "jquery.cookie/jquery.cookie.js"), "utf8");
    expect(dist).toMatch(/jQuery Cookie Plugin v1\.4\.1/);
    expect(dist).toMatch(/Copyright 2013 Klaus Hartl/);
    expect(dist).toMatch(/Released under the MIT license/);
  });
});
