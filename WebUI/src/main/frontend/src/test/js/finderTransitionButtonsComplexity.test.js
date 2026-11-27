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
 * distributed under the License is distributed on a platform basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Behavioural tests for the finder-view slice of
 * WebUI/src/main/webapp/cm/views/PercFinderView.js
 * (mirrored in cm/app/js/legacy/views/ and WebUI/war/views/).
 *
 * Issue #7322 extracted per-concern helpers (save/edit/close button
 * builders, save-click and close flows, editor-open dispatch, transition
 * dropdown data/render/response handling) out of the cognitive-73
 * addTransitionButtons body. These tests drive the live source end-to-end
 * through P.PercFinderView construction with stubbed services and pin the
 * preserved behaviour: which buttons render per mode/view, the new-asset
 * save click, the edit-button open dispatch (page / Sites-node asset /
 * unknown view), and the comment-icon success/failure paths. Mirror-parity
 * pins assert the extracted helpers exist in all three trees and the
 * legacy twin is byte-identical.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEBAPP_CM = resolve(__dirname, "../../../../webapp/cm");
const SRC_PATH = resolve(WEBAPP_CM, "views/PercFinderView.js");
const LEGACY_PATH = resolve(WEBAPP_CM, "app/js/legacy/views/PercFinderView.js");
const WAR_PATH = resolve(
  __dirname,
  "../../../../../../war/views/PercFinderView.js",
);

const EXTRACTED_HELPERS = [
  "isAssetEditView",
  "alertSaveBlockedDialog",
  "saveExistingAssetContent",
  "saveCheckedOutAssetContent",
  "handleSaveContentClick",
  "addSaveButton",
  "openEditorForCurrentItem",
  "openAssetForEdit",
  "openAssetFromSitesNode",
  "alertUnknownEditorView",
  "bindEditButtonForPathItem",
  "addEditButton",
  "checkInAndReturnToEditor",
  "finishCloseAfterDirtyCheck",
  "confirmAndCheckInBeforeClose",
  "closeCheckedOutItem",
  "closeEditorAndReturnToDashboard",
  "addCloseButton",
  "buildTransitionDropdownData",
  "renderWorkflowDropdown",
  "handleTransitionsResponse",
];

let $;
let P;
let nav;
let calls;
let workflowStubs;

function bootstrapJquery() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;
  jq.Percussion = {};
}

function stubServices() {
  nav = {
    MODE_EDIT: "edit",
    MODE_READONLY: "readonly",
    VIEW_EDITOR: "editor",
    VIEW_EDIT_ASSET: "editAsset",
    PATH_TYPE_PAGE: "percPage",
    PATH_TYPE_ASSET: "percAsset",
    mode: "edit",
    view: "editor",
    getMode: () => nav.mode,
    getView: () => nav.view,
    getId: () => "c1",
    getPathType: () => nav.PATH_TYPE_PAGE,
    getName: () => "Page1",
    getPath: () => "/Sites/SiteA/Page1",
    registerFinder: () => {},
    addLocationChangeListener: () => {},
    handleOpenPage: (item) => calls.opened.push(["page", item]),
    handleOpenAsset: (item) => calls.opened.push(["asset", item]),
    goTo: (...args) => calls.goTo.push(args),
  };
  $.PercNavigationManager = nav;
  $.PercDirtyController = { confirmIfDirty: (cb) => cb() };
  $.PercServiceUtils = { STATUS_SUCCESS: "success" };
  $.PercWorkflowController = () => workflowStubs;
  $.PercPageService = { checkForEmptyMigrationWidgets: () => {} };
  $.PercPathService = {
    getPathItemForPath: (path, callback) =>
      callback("ok", { PathItem: calls.pathItem }),
    getPathItemById: (id, callback) =>
      callback("success", { PathItem: { name: "Asset1" } }),
  };
  $.PercRevisionService = {
    getLastComment: (contentId, callback) => callback(...calls.commentResult),
  };
  $.PercIFrameView = {
    saveContent: (isNew) => calls.saved.push(isNew),
  };
  $.PercBlockUI = (mode) => calls.blocked.push(mode);
  $.PercBlockUIMode = { CURSORONLY: "cursor" };
  $.unblockUI = () => calls.unblocked.push(true);
  $.PercViewReadyManager = { getWrapper: () => null };
  $.perc_utils = {
    alert_dialog: (opts) => calls.alerts.push(opts),
    info: (msg) => calls.infos.push(msg),
    getContentId: (id) => id,
  };
  $.perc_textFilters = { IDNAMECDATA: (s) => s };
  $.perc_finder = () => ({
    addPathChangedListener: () => {},
    update_finder_height: () => {},
    on: () => {},
    open: () => {},
    getCurrentPath: () => "/",
    scrollIntoView: () => {},
    setStateButtonsDesignNode: () => {},
  });
  $.Percussion.pageView = () => ({
    openPage: (...args) => calls.openedPages.push(args),
  });
  $.percFinderButtons = () => ({
    createButtons: () => ({}),
    disableAllButtonsButSite: () => {},
  });
  $.fn.PercDropdown = function (opts) {
    calls.dropdowns.push(opts);
    return this;
  };
  globalThis.I18N = { message: (key) => `[${key}]` };
  globalThis.bootstrap = {
    Tooltip: function (el, opts) {
      calls.tooltips.push(opts);
    },
  };
}

function evalFinderSource() {
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
  P = $.Percussion;
}

function construct({ mode = "edit", view = "editor" } = {}) {
  document.body.innerHTML =
    '<div id="perc-content-menu"></div>' +
    '<div id="perc-dropdown-page-workflow"></div>';
  bootstrapJquery();
  stubServices();
  nav.mode = mode;
  nav.view = view;
  evalFinderSource();
  return new P.PercFinderView();
}

beforeEach(() => {
  document.body.innerHTML = "";
  calls = {
    opened: [],
    openedPages: [],
    goTo: [],
    saved: [],
    alerts: [],
    infos: [],
    blocked: [],
    unblocked: [],
    tooltips: [],
    dropdowns: [],
    commentResult: [false, null],
    transitionsResult: [true, [{ name: "Approve", cssClass: "x", alt: "y" }]],
    pathItem: { name: "PageItem1" },
  };
  workflowStubs = {
    checkOut: (type, id, callback) => callback(true),
    getTransitions: (contentId, callback) =>
      callback(...calls.transitionsResult),
    doesItemExist: (id, callback) => callback(true),
    isCheckedOutToCurrentUser: (id, callback) => callback(true),
    checkIn: (id, callback) => callback(true),
  };
  delete globalThis.view;
  delete globalThis.defaultButtonImage;
  delete globalThis.defaultButtonImageOver;
});

describe("finder transition buttons behaviour (issue #7322)", () => {
  it("renders the transition dropdown then save/close in asset edit view", () => {
    construct({ mode: "edit", view: "editAsset" });
    expect(calls.dropdowns.length, "workflow dropdown rendered").toBe(1);
    expect(
      calls.dropdowns[0].percDropdownOptionLabels,
      "transition duplicated for the split-button head",
    ).toEqual(["Approve", "Approve"]);
    expect(
      calls.dropdowns[0].percDropdownCallbackData,
      "params carry content id and item type",
    ).toEqual([
      { name: "Approve", contentId: "c1", itemType: "percPage" },
      { name: "Approve", contentId: "c1", itemType: "percPage" },
    ]);
    expect(
      calls.dropdowns[0].percDropdownCallbacks.length,
      "one action per transition",
    ).toBe(2);
    expect($("#perc-save-content").length, "save button added").toBe(1);
    expect($("#perc-page-edit").length, "no edit button in edit mode").toBe(0);
    expect($("#perc-page-close").length, "close button added").toBe(1);
  });

  it("renders edit (not save) button and no dropdown in readonly mode", () => {
    construct({ mode: "readonly", view: "editor" });
    expect(calls.dropdowns.length, "no dropdown outside edit mode").toBe(0);
    expect($("#perc-save-content").length, "no save button readonly").toBe(0);
    expect($("#perc-page-edit").length, "edit button added").toBe(1);
    expect($("#perc-page-close").length, "close button added").toBe(1);
  });

  it("save click saves the checked-out existing asset", () => {
    construct({ mode: "edit", view: "editAsset" });
    $("#perc-save-content").trigger("click");
    expect(calls.saved, "existing asset saved").toEqual([false]);
  });

  it("save click alerts when the asset was deleted elsewhere", () => {
    workflowStubs.doesItemExist = (id, callback) => callback(false);
    construct({ mode: "edit", view: "editAsset" });
    $("#perc-save-content").trigger("click");
    expect(calls.saved, "nothing saved for a deleted asset").toEqual([]);
    expect(calls.alerts.length, "deleted alert shown").toBe(1);
    expect(calls.alerts[0].content).toContain("Action Not Performed Deleted");
  });

  it("save click alerts when an admin overrode the checkout", () => {
    workflowStubs.isCheckedOutToCurrentUser = (id, callback) =>
      callback(false);
    construct({ mode: "edit", view: "editAsset" });
    $("#perc-save-content").trigger("click");
    expect(calls.saved, "nothing saved when overridden").toEqual([]);
    expect(calls.alerts.length, "overridden alert shown").toBe(1);
    expect(calls.alerts[0].content).toContain(
      "Action Not Performed Overridden",
    );
  });

  it("close click checks in and returns to the editor in edit mode", () => {
    construct({ mode: "edit", view: "editAsset" });
    $("#perc-page-close").trigger("click");
    expect(calls.blocked, "cursor-only block during close").toEqual([
      "cursor",
    ]);
    expect(calls.goTo, "returned to the editor view").toEqual([
      ["editor", true],
    ]);
    expect(calls.unblocked, "ui unblocked after close").toEqual([true]);
  });

  it("close click just returns to the editor in readonly mode", () => {
    construct({ mode: "readonly", view: "editor" });
    $("#perc-page-close").trigger("click");
    expect(calls.blocked, "cursor-only block during close").toEqual([
      "cursor",
    ]);
    expect(calls.goTo, "returned to the editor view").toEqual([
      ["editor", true],
    ]);
    expect(calls.unblocked, "ui unblocked after close").toEqual([true]);
  });

  it("edit click opens the page in editor view", () => {
    construct({ mode: "readonly", view: "editor" });
    $("#perc-page-edit").trigger("click");
    expect(calls.opened, "page opened for edit").toEqual([
      ["page", { name: "PageItem1" }],
    ]);
  });

  it("edit click resolves through the Sites node in asset edit view", () => {
    calls.pathItem = { name: "Sites" };
    construct({ mode: "readonly", view: "editAsset" });
    $("#perc-page-edit").trigger("click");
    expect(calls.opened, "asset opened via Sites node").toEqual([
      ["asset", { name: "Asset1" }],
    ]);
  });

  it("edit click opens the asset directly when a leaf is selected", () => {
    calls.pathItem = { name: "Asset1" };
    construct({ mode: "readonly", view: "editAsset" });
    $("#perc-page-edit").trigger("click");
    expect(calls.opened, "asset opened directly").toEqual([
      ["asset", { name: "Asset1" }],
    ]);
  });

  it("edit click alerts on an unknown view", () => {
    construct({ mode: "readonly", view: "mystery" });
    $("#perc-page-edit").trigger("click");
    expect(calls.opened, "nothing opened").toEqual([]);
    expect(calls.alerts.length, "unknown-view alert shown").toBe(1);
    expect(calls.alerts[0].content).toContain("Cannot Open Unknown View");
  });

  it("shows the comment tooltip when a last comment exists", () => {
    // The comments icon renders on the readonly branch (pre-existing).
    calls.commentResult = ["success", { data: "hello" }];
    construct({ mode: "readonly", view: "editor" });
    expect(calls.tooltips.length, "tooltip constructed").toBe(1);
    expect(calls.tooltips[0]).toMatchObject({
      html: true,
      placement: "left",
      customClass: "perc-comment-tooltip",
    });
    expect(calls.tooltips[0].delay).toMatchObject({ show: 500 });
    expect($(".perc-last-comment-menubar").length, "icon added").toBe(1);
  });

  it("infos when comment fetch fails", () => {
    calls.commentResult = [false, null];
    construct({ mode: "readonly", view: "editor" });
    expect(calls.infos.length, "failure info shown").toBe(1);
    expect(calls.tooltips.length, "no tooltip on failure").toBe(0);
  });
});

describe("finder view mirrors stay in lockstep (issue #7322)", () => {
  it("webapp and legacy mirrors are byte-identical", () => {
    expect(readFileSync(LEGACY_PATH, "utf8"), "legacy mirror drifted").toBe(
      readFileSync(SRC_PATH, "utf8"),
    );
  });

  it("extracted helpers exist in all three trees", () => {
    const sources = {
      webapp: readFileSync(SRC_PATH, "utf8"),
      legacy: readFileSync(LEGACY_PATH, "utf8"),
      war: readFileSync(WAR_PATH, "utf8"),
    };
    for (const [tree, src] of Object.entries(sources)) {
      for (const helper of EXTRACTED_HELPERS) {
        expect(
          src.includes(`function ${helper}(`),
          `${tree} is missing extracted helper ${helper}`,
        ).toBe(true);
      }
    }
  });

  it("preserves the transition-dropdown quirks", () => {
    for (const path of [SRC_PATH, LEGACY_PATH, WAR_PATH]) {
      const src = readFileSync(path, "utf8");
      expect(
        src.includes("results.unshift(results[0])"),
        `${path} lost the split-button duplication quirk`,
      ).toBe(true);
      expect(
        src.includes("handleTransitionsResponse(status, results, itemType)"),
        `${path} lost the transitions response wiring`,
      ).toBe(true);
    }
  });
});

