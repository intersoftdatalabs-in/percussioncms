/**
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
 * Behavioral tests for Empty Recycling finder action (#2206 / parent #944 slice 2).
 *
 * Covers:
 *   - PercRecycleService.emptyRecycling → DELETE RECYCLE_EMPTY
 *   - Enablement rule (Admin + Recycling path only)
 *   - Result summarization (alreadyEmpty / partial / clean)
 *   - Dual-tree lockstep (src ↔ war ↔ legacy service)
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const CM_ROOT = resolve(__dirname, "../../main/webapp/cm");
const SERVICE_SRC = resolve(CM_ROOT, "services/PercRecycleService.js");
const BUTTON_SRC = resolve(CM_ROOT, "widgets/perc_empty_recycling_button.js");
const WAR_SERVICE = resolve(
  __dirname,
  "../../../war/services/PercRecycleService.js",
);
const WAR_BUTTON = resolve(
  __dirname,
  "../../../war/widgets/perc_empty_recycling_button.js",
);
const LEGACY_SERVICE = resolve(
  CM_ROOT,
  "app/js/legacy/services/PercRecycleService.js",
);

let $;
let makeJsonRequestSpy;

function installServiceEnvironment() {
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

  makeJsonRequestSpy = vi.fn(function (url, type, sync, callback) {
    makeJsonRequestSpy.last = { url, type, sync, callback };
  });

  $.PercServiceUtils = {
    STATUS_SUCCESS: "success",
    STATUS_ERROR: "error",
    TYPE_DELETE: "DELETE",
    TYPE_PUT: "PUT",
    makeJsonRequest: makeJsonRequestSpy,
    extractDefaultErrorMessage: vi.fn(() => "default error"),
  };
  $.perc_paths = {
    RECYCLE_EMPTY: "/Rhythmyx/services/pathmanagement/recycle/empty",
    RECYCLING_ROOT_NO_SLASH: "Recycling",
  };

  const factory = new Function(
    "jQuery",
    "$",
    readFileSync(SERVICE_SRC, "utf8") + "\nreturn $.PercRecycleService;",
  );
  return factory($, $);
}

function installButtonHelpers() {
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

  $.perc_paths = {
    RECYCLING_ROOT_NO_SLASH: "Recycling",
  };
  globalThis.I18N = {
    message: (key) => key,
  };
  $.perc_utils = {
    confirm_dialog: vi.fn(),
    alert_dialog: vi.fn(),
  };
  $.PercBlockUI = vi.fn();
  $.PercBlockUIMode = { CURSORONLY: "cursor" };
  $.unblockUI = vi.fn();
  $.PercNavigationManager = { isAdmin: vi.fn(() => true) };
  $.PercServiceUtils = {
    STATUS_SUCCESS: "success",
    STATUS_ERROR: "error",
  };
  $.PercRecycleService = { emptyRecycling: vi.fn() };

  const factory = new Function(
    "jQuery",
    "$",
    "I18N",
    readFileSync(BUTTON_SRC, "utf8") +
      "\nreturn { enabled: $.perc_empty_recycling_enabled, summarize: $.perc_empty_recycling_summarize, build: $.perc_build_empty_recycling_button };",
  );
  return factory($, $, globalThis.I18N);
}

describe("PercRecycleService.emptyRecycling", () => {
  let service;

  beforeEach(() => {
    service = installServiceEnvironment();
  });

  it("issues DELETE against RECYCLE_EMPTY path constant", () => {
    const cb = vi.fn();
    service.emptyRecycling(cb);

    expect(makeJsonRequestSpy).toHaveBeenCalledTimes(1);
    const args = makeJsonRequestSpy.mock.calls[0];
    expect(args[0]).toBe($.perc_paths.RECYCLE_EMPTY);
    expect(args[1]).toBe($.PercServiceUtils.TYPE_DELETE);
    expect(args[2]).toBe(false);
    expect(typeof args[3]).toBe("function");
  });

  it("forwards success payload to callback", () => {
    const cb = vi.fn();
    service.emptyRecycling(cb);
    const result = {
      alreadyEmpty: false,
      purgedFolderCount: 2,
      purgedItemCount: 1,
      undeletedCount: 0,
    };
    makeJsonRequestSpy.last.callback("success", { data: result });
    expect(cb).toHaveBeenCalledWith("success", result);
  });

  it("forwards extracted error message on failure", () => {
    const cb = vi.fn();
    service.emptyRecycling(cb);
    makeJsonRequestSpy.last.callback("error", {
      request: { status: 403, responseText: "Only Admin" },
    });
    expect($.PercServiceUtils.extractDefaultErrorMessage).toHaveBeenCalled();
    expect(cb).toHaveBeenCalledWith("error", "default error");
  });
});

describe("Empty Recycling enablement + summary helpers", () => {
  let helpers;

  beforeEach(() => {
    helpers = installButtonHelpers();
  });

  it("enables only for Admin under Recycling path", () => {
    expect(helpers.enabled(["", "Recycling"], true)).toBe(true);
    expect(helpers.enabled(["", "Recycling", "Sites"], true)).toBe(true);
    expect(helpers.enabled(["", "Sites"], true)).toBe(false);
    expect(helpers.enabled(["", "Recycling"], false)).toBe(false);
    expect(helpers.enabled(["", "Assets"], true)).toBe(false);
    expect(helpers.enabled([], true)).toBe(false);
    expect(helpers.enabled(null, true)).toBe(false);
  });

  it("summarizes alreadyEmpty / partial / clean results", () => {
    expect(helpers.summarize({ alreadyEmpty: true })).toEqual({
      alreadyEmpty: true,
      partial: false,
      messageKey: "perc.ui.empty.recycling@Already Empty",
      messageArgs: [],
    });
    expect(
      helpers.summarize({
        alreadyEmpty: false,
        undeletedCount: 3,
      }),
    ).toEqual({
      alreadyEmpty: false,
      partial: true,
      messageKey: "perc.ui.empty.recycling@Partial",
      messageArgs: ["3"],
    });
    expect(
      helpers.summarize({
        alreadyEmpty: false,
        undeletedCount: 0,
        purgedFolderCount: 1,
      }),
    ).toEqual({
      alreadyEmpty: false,
      partial: false,
      messageKey: null,
      messageArgs: [],
    });
    expect(helpers.summarize(null).messageKey).toBe(null);
  });

  it("builds menu entry with stable id/testid and confirms before purge", () => {
    const pathListener = vi.fn();
    const finderRef = {
      addPathChangedListener: pathListener,
      refresh: vi.fn(),
    };
    const btn = helpers.build(finderRef, null);
    expect(btn.attr("id")).toBe("perc-finder-empty-recycling");
    expect(btn.attr("data-testid")).toBe("perc-finder-empty-recycling");
    expect(pathListener).toHaveBeenCalledTimes(1);

    // Simulate Admin + Recycling path so button is enabled, then click.
    pathListener.mock.calls[0][0](["", "Recycling"]);
    expect(btn.hasClass("ui-enabled")).toBe(true);

    btn.trigger("click");
    expect($.perc_utils.confirm_dialog).toHaveBeenCalledTimes(1);
    const opts = $.perc_utils.confirm_dialog.mock.calls[0][0];
    expect(opts.id).toBe("perc-finder-empty-recycling-confirm");
    expect(opts.question).toContain("perc.ui.empty.recycling@Confirm");

    // Confirm success path calls service then refreshes finder.
    $.PercRecycleService.emptyRecycling.mockImplementation((cb) => {
      cb("success", {
        alreadyEmpty: false,
        undeletedCount: 0,
        purgedFolderCount: 1,
      });
    });
    opts.success();
    expect($.PercRecycleService.emptyRecycling).toHaveBeenCalledTimes(1);
    expect(finderRef.refresh).toHaveBeenCalledTimes(1);
  });

  it("does not re-enable delete of SYSTEM roots — enablement never uses Sites/Assets roots", () => {
    // Guardrail: empty is Recycling-only; not a generalized delete of system roots.
    expect(helpers.enabled(["", "Sites"], true)).toBe(false);
    expect(helpers.enabled(["", "Assets"], true)).toBe(false);
    expect(helpers.enabled(["", "Design"], true)).toBe(false);
  });
});

describe("dual-tree lockstep Empty Recycling surface", () => {
  it("keeps war service + button in sync with src for emptyRecycling markers", () => {
    const srcService = readFileSync(SERVICE_SRC, "utf8");
    const warService = readFileSync(WAR_SERVICE, "utf8");
    const legacyService = readFileSync(LEGACY_SERVICE, "utf8");
    const srcButton = readFileSync(BUTTON_SRC, "utf8");
    const warButton = readFileSync(WAR_BUTTON, "utf8");

    for (const body of [srcService, warService, legacyService]) {
      expect(body).toMatch(/emptyRecycling/);
      expect(body).toMatch(/RECYCLE_EMPTY/);
      expect(body).toMatch(/TYPE_DELETE/);
    }
    for (const body of [srcButton, warButton]) {
      expect(body).toMatch(/perc-finder-empty-recycling/);
      expect(body).toMatch(/perc_build_empty_recycling_button/);
      expect(body).toMatch(/perc\.ui\.empty\.recycling@Confirm/);
      // No multi-call client purge walk
      expect(body).not.toMatch(/purgeItem\s*\(/);
    }
  });
});
