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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for Explorer workflow-transition helpers (#3668 / #3639) — no live CMS.
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  TEST_IDS,
  JSON_ACCEPT_HEADERS,
  explorerEntryUrl,
  workflowTransitionsUrl,
  workflowTransitionInvokeUrl,
  coerceTransitionTriggers,
  unwrapItemStateTransition,
  listedItemContentId,
  isFolderishRow,
  isWorkflowEligibleRow,
  isH2Qa,
  shouldSkipWorkflowTransitionProof,
  isSuccessfulTransitionStatus,
  isHonestTransitionStatus,
  isWorkflowTransitionInvokeUrl,
} = require("../helpers/explorer-workflow-transitions");

describe("explorer-workflow-transitions helpers (#3668 / #3639)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.workflowGroup, "action-toolbar-group-workflow");
    assert.equal(
      TEST_IDS.workflowTransitionPrefix,
      "action-toolbar-item-workflow-transition:",
    );
    assert.equal(JSON_ACCEPT_HEADERS.Accept, "application/json");
  });

  it("builds explorer SPA and getTransitions URLs with encoded ids", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9992/", { cacheBuster: "42" }),
      "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    assert.equal(
      workflowTransitionsUrl("http://127.0.0.1:9992/", "33554432-101-1"),
      "http://127.0.0.1:9992/Rhythmyx/services/itemmanagement/workflow/getTransitions/33554432-101-1",
    );
    assert.equal(
      workflowTransitionInvokeUrl("http://localhost:9992", "1 2", "Submit"),
      "http://localhost:9992/Rhythmyx/services/itemmanagement/workflow/transitionWithComments/1%202/Submit",
    );
    assert.equal(workflowTransitionsUrl("http://x", ""), "");
  });

  it("unwraps Jackson ItemStateTransition envelopes", () => {
    const wrapped = unwrapItemStateTransition({
      ItemStateTransition: {
        itemId: "101-1",
        stateName: "Draft",
        transitionTriggers: ["Submit", "Approve"],
      },
    });
    assert.equal(wrapped.itemId, "101-1");
    assert.equal(wrapped.stateName, "Draft");
    assert.deepEqual(wrapped.transitionTriggers, ["Submit", "Approve"]);
    assert.deepEqual(
      unwrapItemStateTransition({
        PSItemStateTransition: {
          transitionTriggers: { string: ["Reject"] },
        },
      }).transitionTriggers,
      ["Reject"],
    );
    assert.deepEqual(
      unwrapItemStateTransition({
        itemId: "9",
        transitionTriggers: "Submit",
      }).transitionTriggers,
      ["Submit"],
    );
    assert.deepEqual(unwrapItemStateTransition(null).transitionTriggers, []);
  });

  it("coerceTransitionTriggers ignores blank and unknown shapes", () => {
    assert.deepEqual(coerceTransitionTriggers(undefined), []);
    assert.deepEqual(coerceTransitionTriggers("  "), []);
    assert.deepEqual(coerceTransitionTriggers({ empty: false }), []);
    assert.deepEqual(coerceTransitionTriggers(["Submit", "  "]), ["Submit"]);
  });

  it("listedItemContentId reads string, number, and GUID object ids", () => {
    assert.equal(listedItemContentId({ id: "42" }), "42");
    assert.equal(listedItemContentId({ id: 7 }), "7");
    assert.equal(
      listedItemContentId({ id: { stringValue: "33554432-101-1" } }),
      "33554432-101-1",
    );
    assert.equal(
      listedItemContentId({
        displayProperties: { sys_contentid: "99" },
      }),
      "99",
    );
    assert.equal(listedItemContentId(null), "");
  });

  it("folders are not workflow-eligible; pages with ids are", () => {
    assert.equal(
      isFolderishRow({ type: "Folder", path: "/Sites/Demo/", id: "1" }),
      true,
    );
    assert.equal(
      isWorkflowEligibleRow({
        type: "Folder",
        path: "/Sites/Demo/",
        id: "1",
      }),
      false,
    );
    assert.equal(
      isWorkflowEligibleRow({
        type: "percPage",
        path: "/Sites/Demo/Home",
        id: "33554432-101-1",
      }),
      true,
    );
    assert.equal(
      isWorkflowEligibleRow({
        type: "page",
        path: "/Sites/Demo/Home",
      }),
      false,
    );
  });

  it("does not skip when REST listed an eligible item or H2 QA is under test", () => {
    assert.equal(
      shouldSkipWorkflowTransitionProof({ restEligibleCount: 1, dbType: "h2" }),
      false,
    );
    assert.equal(
      shouldSkipWorkflowTransitionProof({ restEligibleCount: 0, dbType: "h2" }),
      false,
    );
    assert.equal(
      shouldSkipWorkflowTransitionProof({
        restEligibleCount: 0,
        dbType: "postgresql",
      }),
      true,
    );
    assert.equal(isH2Qa("h2"), true);
    assert.equal(isH2Qa("postgresql"), false);
  });

  it("requires HTTP 200 for a listed workflow transition (#3668)", () => {
    assert.equal(isSuccessfulTransitionStatus(200), true);
    assert.equal(isHonestTransitionStatus(200), true);
    assert.equal(isSuccessfulTransitionStatus(400), false);
    assert.equal(isSuccessfulTransitionStatus(403), false);
    assert.equal(isSuccessfulTransitionStatus(409), false);
    assert.equal(isSuccessfulTransitionStatus(500), false);
    assert.equal(isHonestTransitionStatus(500), false);
    assert.equal(isSuccessfulTransitionStatus(502), false);
    assert.equal(isSuccessfulTransitionStatus(0), false);
    assert.equal(
      isWorkflowTransitionInvokeUrl(
        "http://127.0.0.1:9992/Rhythmyx/services/itemmanagement/workflow/transitionWithComments/1/Expire",
      ),
      true,
    );
    assert.equal(
      isWorkflowTransitionInvokeUrl(
        "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/folder/Sites",
      ),
      false,
    );
  });

  it("workflow spec opens REST-listed site via tree-node name or testid (#3684)", () => {
    const specPath = path.join(
      __dirname,
      "..",
      "explorer-workflow-transitions.spec.js",
    );
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /treeNodeMatchesFoldedSite/);
    assert.match(src, /data-node-name/);
    assert.match(
      src,
      /\[data-testid\^="tree-node-\/Sites\/"\]:not\(\[data-testid="tree-node-\/Sites\/"\]\)/,
    );
    assert.match(src, /tree-toggle-\/Sites/);
    assert.match(src, /errors\.TimeoutError/);
    assert.match(src, /getAttribute\("data-item-name"\)/);
    assert.match(src, /tree=\$\{seen\.join/);
  });
});
