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
 * Behavioural tests for the search-criteria dialog slice of
 * WebUI/src/main/webapp/cm/plugins/perc_gadgets_search_criteria_dialog.js
 * (mirrored in cm/app/js/legacy/plugins/ and WebUI/war/plugins/).
 *
 * Issue #7322 extracted the per-concern helpers (markup builders, field
 * reader, criterion entries, option loaders, combo enable/disable, selection
 * handlers, type-ahead init, modified-by prefill) out of the cognitive-99
 * openSearchCriteriaDialog body. These tests drive the live source
 * end-to-end with stubbed Percussion services and pin the preserved
 * behaviour: pages/assets markup, option loading, change-handler flows,
 * the Search return payload (including the per-field @all defaulting),
 * cancel, and the modified-by prefill. Mirror-parity pins assert the
 * extracted helpers exist in all three trees.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEBAPP_CM = resolve(__dirname, "../../../../webapp/cm");
const SRC_PATH = resolve(
  WEBAPP_CM,
  "plugins/perc_gadgets_search_criteria_dialog.js",
);
const LEGACY_PATH = resolve(
  WEBAPP_CM,
  "app/js/legacy/plugins/perc_gadgets_search_criteria_dialog.js",
);
const WAR_PATH = resolve(
  __dirname,
  "../../../../../../war/plugins/perc_gadgets_search_criteria_dialog.js",
);

const EXTRACTED_HELPERS = [
  "buildTypeSpecificCriteriaMarkup",
  "buildCommonCriteriaMarkup",
  "buildSearchCriteriaDialogMarkup",
  "readSearchCriteriaFields",
  "criterionDisplayName",
  "rawSearchCriterion",
  "defaultedSearchCriterion",
  "blankDefaultedSearchCriterion",
  "collectSearchCriteriaReturnData",
  "loadSiteOptions",
  "loadWorkflowOptions",
  "loadAssetTypeOptions",
  "loadSearchCriteriaFields",
  "disableTemplateCombo",
  "enableTemplateCombo",
  "loadSiteTemplatesCombo",
  "handleSiteSelectionChange",
  "disableStateCombo",
  "enableStateCombo",
  "loadWorkflowStatesCombo",
  "handleWorkflowSelectionChange",
  "initModifiedByTypeAhead",
  "prefillModifiedByField",
];

const RETIRED_INNARDS = [
  "loadFields()",
  "loadTemplatesCombo(",
  "loadStatesCombo(",
];

let $;
let serviceCalls;
let autocompleteConfigs;
let capturedButtons;
let fixtures;

function pagesCriteria(overrides = {}) {
  return {
    type: "pages",
    site: { name: "SiteB", value: "SiteB" },
    workflow: { name: "All", value: "@all" },
    template: null,
    state: null,
    modifiedby: null,
    ...overrides,
  };
}

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
  // The dialog module talks to the CMS through percJQuery; the tests reuse
  // the same jQuery instance so .find/.val/.append work on real DOM.
  const percJQuery = $;
  percJQuery.PercServiceUtils = { STATUS_SUCCESS: "success" };
  percJQuery.PercMetadataService = { find: () => {} };
  percJQuery.PercReusableSearchService = {
    getSites: (callback) => {
      serviceCalls.getSites += 1;
      callback("success", fixtures.sites);
    },
    getWorkflows: (callback) => {
      serviceCalls.getWorkflows += 1;
      callback("success", fixtures.workflows);
    },
    getStates: (workflow, callback) => {
      serviceCalls.getStates.push(workflow);
      callback("success", fixtures.states);
    },
    getUsers: (filter, callback) => {
      serviceCalls.getUsers.push(filter);
      callback("success", fixtures.users);
    },
  };
  percJQuery.PercAssetService = {
    getAssetTypes: (flag, callback) => {
      serviceCalls.getAssetTypes.push(flag);
      callback("success", fixtures.assetTypes);
    },
  };
  percJQuery.PercSiteService = {
    getTemplates: (siteName, callback) => {
      serviceCalls.getTemplates.push(siteName);
      callback("success", { TemplateSummary: fixtures.templates });
    },
  };
  percJQuery.perc_utils = {
    convertCXFArray: (summary) => summary,
    getContentId: (id) => `cid-${id}`,
  };
  globalThis.percJQuery = percJQuery;
}

function stubJqueryPlugins() {
  $.fn.perc_dialog = function (opts) {
    capturedButtons = opts.percButtons;
    document.body.appendChild(this[0]);
    return this;
  };
  $.fn.autocomplete = function (options, ...rest) {
    if (typeof options === "string") {
      if (options === "widget") return $([]);
      return this;
    }
    autocompleteConfigs.push(options);
    return this;
  };
}

function evalDialogSource() {
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
}

function loadSource() {
  bootstrapJquery();
  stubJqueryPlugins();
  evalDialogSource();
}

function openDialog(criteria, callbacks = {}) {
  let saved;
  let cancelled = 0;
  loadSource();
  $.perc_gadgets_search_criteria_dialog.openSearchCriteriaDialog(
    criteria,
    (data) => {
      saved = data;
      if (callbacks.onSave) callbacks.onSave(data);
    },
    () => {
      cancelled += 1;
      if (callbacks.onCancel) callbacks.onCancel();
    },
  );
  return {
    get saved() {
      return saved;
    },
    get cancelled() {
      return cancelled;
    },
    clickSearch: () => capturedButtons.Search.click(),
    clickCancel: () => capturedButtons.Cancel.click(),
  };
}

beforeEach(() => {
  document.body.innerHTML = "";
  serviceCalls = {
    getSites: 0,
    getWorkflows: 0,
    getStates: [],
    getUsers: [],
    getAssetTypes: [],
    getTemplates: [],
  };
  autocompleteConfigs = [];
  capturedButtons = null;
  fixtures = {
    sites: [{ value: "SiteA" }, { value: "SiteB" }],
    workflows: [
      { displayValue: "w1", value: "W1" },
      { displayValue: "w2", value: "W2" },
    ],
    states: [{ displayValue: "s1", value: "S1" }],
    users: ["alice", "bob"],
    assetTypes: [{ contentTypeId: "ct1", widgetLabel: "Article" }],
    templates: [{ id: "t1", name: "Template1" }],
  };
  delete globalThis.dialog;
  delete globalThis.buttons;
  delete globalThis.resultItems;
});

describe("search-criteria dialog behaviour (issue #7322)", () => {
  it("builds pages markup and loads site/workflow options", () => {
    openDialog(pagesCriteria());
    const dialog = globalThis.dialog;
    expect(serviceCalls.getSites, "getSites called").toBe(1);
    expect(serviceCalls.getWorkflows, "getWorkflows called").toBe(1);
    const siteOptions = dialog.find("#perc-search-criteria-dialog-site option");
    expect(siteOptions.length, "two site options").toBe(2);
    expect(
      dialog.find("#perc-search-criteria-dialog-site").val(),
      "saved site preselected",
    ).toBe("SiteB");
    expect(
      dialog.find("#perc-search-criteria-dialog-template").prop("disabled"),
      "template enabled once the saved site loads its templates",
    ).toBe(false);
    const templateOptions = dialog.find(
      "#perc-search-criteria-dialog-template option",
    );
    expect(templateOptions.length, "template options loaded").toBe(2);
    const workflowOptions = dialog.find(
      "#perc-search-criteria-dialog-workflow option",
    );
    // "@all" from markup plus the two stubbed workflows.
    expect(workflowOptions.length, "workflow options loaded").toBe(3);
    expect(
      dialog.find("#perc-search-criteria-dialog-state").prop("disabled"),
      "state starts disabled",
    ).toBe(true);
    expect(
      dialog.find("#perc-search-criteria-dialog-created-by-field").length,
      "modified-by field present",
    ).toBe(1);
  });

  it("collects the pages Search payload with @all defaults", () => {
    const handle = openDialog(pagesCriteria());
    handle.clickSearch();
    expect(handle.saved).toEqual({
      site: { name: "SiteB", value: "SiteB" },
      template: { name: "All", value: "@all" },
      workflow: { name: "All", value: "@all" },
      state: { name: "All", value: "@all" },
      modifiedby: { name: "All", value: "@all" },
    });
  });

  it("collects chosen template/state/modified-by values", () => {
    const handle = openDialog(pagesCriteria());
    const dialog = globalThis.dialog;
    // Pick a site so templates load, then choose the stubbed template.
    dialog.find("#perc-search-criteria-dialog-site").val("SiteA");
    dialog.find("#perc-search-criteria-dialog-site").trigger("change");
    expect(serviceCalls.getTemplates, "templates loaded per site").toEqual([
      "SiteB",
      "SiteA",
    ]);
    expect(
      dialog.find("#perc-search-criteria-dialog-template").prop("disabled"),
      "template enabled after site pick",
    ).toBe(false);
    dialog.find("#perc-search-criteria-dialog-template").val("t1");
    // Pick a workflow so states load, then choose the stubbed state.
    dialog.find("#perc-search-criteria-dialog-workflow").val("w1");
    dialog.find("#perc-search-criteria-dialog-workflow").trigger("change");
    expect(serviceCalls.getStates, "states loaded for W1").toEqual(["W1"]);
    dialog.find("#perc-search-criteria-dialog-state").val("s1");
    dialog.find("#perc-search-criteria-dialog-created-by-field").val("alice");
    handle.clickSearch();
    expect(handle.saved.template).toEqual({
      name: "Template1",
      value: "t1",
    });
    expect(handle.saved.state).toEqual({ name: "S1", value: "s1" });
    expect(handle.saved.modifiedby).toEqual({
      name: "alice",
      value: "alice",
    });
    expect(handle.saved.workflow).toEqual({ name: "W1", value: "w1" });
  });

  it("clears and disables the template combo on @all site", () => {
    openDialog(pagesCriteria());
    const dialog = globalThis.dialog;
    dialog.find("#perc-search-criteria-dialog-site").val("SiteA");
    dialog.find("#perc-search-criteria-dialog-site").trigger("change");
    expect(
      dialog.find("#perc-search-criteria-dialog-template option").length,
      "templates loaded",
    ).toBeGreaterThan(1);
    // The markup ships no @all site option, so drive the defensive @all
    // branch by staging one (the handler keys off the selected value).
    dialog
      .find("#perc-search-criteria-dialog-site")
      .append($("<OPTION>").html("All").attr("value", "@all"));
    dialog.find("#perc-search-criteria-dialog-site").val("@all");
    dialog.find("#perc-search-criteria-dialog-site").trigger("change");
    expect(
      dialog.find("#perc-search-criteria-dialog-template").prop("disabled"),
      "template disabled on @all",
    ).toBe(true);
    expect(
      dialog.find("#perc-search-criteria-dialog-template").html(),
      "template options cleared on @all",
    ).toBe("");
  });

  it("clears and disables the state combo on @all workflow", () => {
    openDialog(pagesCriteria());
    const dialog = globalThis.dialog;
    dialog.find("#perc-search-criteria-dialog-workflow").val("w1");
    dialog.find("#perc-search-criteria-dialog-workflow").trigger("change");
    expect(
      dialog.find("#perc-search-criteria-dialog-state option").length,
      "states loaded",
    ).toBeGreaterThan(1);
    dialog.find("#perc-search-criteria-dialog-workflow").val("@all");
    dialog.find("#perc-search-criteria-dialog-workflow").trigger("change");
    expect(
      dialog.find("#perc-search-criteria-dialog-state").prop("disabled"),
      "state disabled on @all",
    ).toBe(true);
  });

  it("builds assets markup and collects the asset Search payload", () => {
    const handle = openDialog({
      type: "assets",
      assetType: { name: "All", value: "@all" },
      workflow: { name: "All", value: "@all" },
      state: null,
      modifiedby: null,
    });
    const dialog = globalThis.dialog;
    expect(serviceCalls.getSites, "no site load for assets").toBe(0);
    expect(serviceCalls.getAssetTypes, "asset types loaded").toEqual(["no"]);
    expect(
      dialog.find("#perc-search-criteria-dialog-site").length,
      "no site select for assets",
    ).toBe(0);
    handle.clickSearch();
    expect(handle.saved.assetType).toEqual({
      name: "All",
      value: "@all",
    });
    expect(handle.saved.site, "no site entry for assets").toBeUndefined();
    expect(handle.saved.template, "no template entry for assets").toBeUndefined();
  });

  it("wires the modified-by autocomplete from the user list", () => {
    openDialog(pagesCriteria());
    expect(serviceCalls.getUsers, "users fetched with wildcard").toEqual(["*"]);
    expect(autocompleteConfigs.length, "autocomplete configured").toBe(1);
    expect(autocompleteConfigs[0].source, "user list as source").toEqual([
      "alice",
      "bob",
    ]);
    expect(autocompleteConfigs[0].minLength, "minLength 0 preserved").toBe(0);
    expect(globalThis.resultItems, "resultItems global assigned").toEqual([
      "alice",
      "bob",
    ]);
  });

  it("prefills modified-by from saved criteria", () => {
    openDialog(pagesCriteria({ modifiedby: { name: "bob", value: "bob" } }));
    expect(
      globalThis.dialog
        .find("#perc-search-criteria-dialog-created-by-field")
        .val(),
      "saved user prefilled",
    ).toBe("bob");
    openDialog(pagesCriteria({ modifiedby: { name: "All", value: "@all" } }));
    expect(
      globalThis.dialog
        .find("#perc-search-criteria-dialog-created-by-field")
        .val(),
      "@all prefilled as empty",
    ).toBe("");
  });

  it("cancel invokes the callback and removes the dialog", () => {
    const onCancel = vi.fn();
    const handle = openDialog(pagesCriteria(), { onCancel });
    const root = globalThis.dialog[0];
    expect(document.body.contains(root), "dialog attached").toBe(true);
    handle.clickCancel();
    expect(onCancel, "cancel callback invoked").toHaveBeenCalledTimes(1);
    expect(handle.saved, "no payload on cancel").toBeUndefined();
    expect(document.body.contains(root), "dialog removed").toBe(false);
  });
});

describe("search-criteria dialog mirrors stay in lockstep (issue #7322)", () => {
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

  it("retired monolith innards are gone from webapp and legacy", () => {
    for (const path of [SRC_PATH, LEGACY_PATH]) {
      const src = readFileSync(path, "utf8");
      for (const needle of RETIRED_INNARDS) {
        expect(src.includes(needle), `${path} still has ${needle}`).toBe(
          false,
        );
      }
    }
  });
});
