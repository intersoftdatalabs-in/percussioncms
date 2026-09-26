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
 * Unit tests for Explorer Open/Edit helpers (no live CMS).
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
  isH2QaEnv,
  shouldSkipListedPageEditor,
  noListedItemSkipMessage,
  isProductEditorUrl,
  isLeftoverContentEditorUrl,
  isKeywordTrimCrash,
  isEditorStayVisible,
} = require("../helpers/explorer-content-editor");

describe("explorer-content-editor helpers (#3638)", () => {
  it("exports stable test ids used by Open/Edit chrome", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.open, "action-open");
    assert.equal(TEST_IDS.edit, "action-toolbar-item-Edit");
    assert.equal(TEST_IDS.contextEdit, "context-menu-item-Edit");
    assert.equal(TEST_IDS.editorHost, "editor-host");
    assert.equal(TEST_IDS.editorError, "editor-error");
  });

  it("isH2QaEnv reads TEST_DB_TYPE", () => {
    assert.equal(isH2QaEnv({ TEST_DB_TYPE: "h2" }), true);
    assert.equal(isH2QaEnv({ TEST_DB_TYPE: "mysql" }), false);
    assert.equal(isH2QaEnv({ TEST_DATABASE: "H2" }), true);
  });

  it("shouldSkipListedPageEditor never skips when H2 or an item row exists (#3638)", () => {
    assert.equal(
      shouldSkipListedPageEditor({ listedPage: { id: "1" } }, { TEST_DB_TYPE: "mysql" }),
      false,
    );
    assert.equal(
      shouldSkipListedPageEditor({ itemRowCount: 1 }, { TEST_DB_TYPE: "mysql" }),
      false,
    );
    assert.equal(
      shouldSkipListedPageEditor({ uiHasItemRow: true }, { TEST_DB_TYPE: "mysql" }),
      false,
    );
    assert.equal(
      shouldSkipListedPageEditor({}, { TEST_DB_TYPE: "h2" }),
      false,
    );
    assert.equal(
      shouldSkipListedPageEditor({ h2: true }, { TEST_DB_TYPE: "mysql" }),
      false,
    );
    assert.equal(
      shouldSkipListedPageEditor({}, { TEST_DB_TYPE: "mysql" }),
      true,
    );
  });

  it("noListedItemSkipMessage names the Open/Edit slice", () => {
    const msg = noListedItemSkipMessage();
    assert.match(msg, /#3638/);
    assert.match(msg, /H2/);
  });

  it("isProductEditorUrl accepts spa.jsp?entry=editor and rejects leftover CE", () => {
    assert.equal(
      isProductEditorUrl(
        "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=editor&contentId=55&mode=edit",
      ),
      true,
    );
    assert.equal(
      isProductEditorUrl("/Rhythmyx/cm/app/editor?contentId=55"),
      true,
    );
    assert.equal(
      isProductEditorUrl("/Rhythmyx/cm/app/?view=editor"),
      false,
    );
    assert.equal(isProductEditorUrl(""), false);
  });

  it("isLeftoverContentEditorUrl matches Data Flow CE HTML", () => {
    assert.equal(isLeftoverContentEditorUrl("/cm/app/editAsset.jsp"), true);
    assert.equal(
      isLeftoverContentEditorUrl("../sys_action/checkoutedit.xml"),
      true,
    );
    assert.equal(
      isLeftoverContentEditorUrl("/Rhythmyx/rx_ce/percPage"),
      true,
    );
    assert.equal(
      isLeftoverContentEditorUrl("/Rhythmyx/cm/app/spa.jsp?entry=editor&contentId=1"),
      false,
    );
    assert.equal(
      isLeftoverContentEditorUrl("/Rhythmyx/services/contenttypes/percPage"),
      false,
    );
  });

  it("open/edit spec does not skip when REST listed a page on H2", () => {
    const specPath = path.join(__dirname, "..", "explorer-content-editor.spec.js");
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /shouldSkipListedPageEditor/);
    assert.match(src, /do not skip/);
    assert.match(src, /TEST_IDS\.open/);
    assert.match(src, /Open\/Edit selected page lands React editor/);
    assert.match(src, /#3968/);
    assert.match(src, /right-click Edit stays on the React editor/);
  });
});

describe("explorer-content-editor helpers (#3968)", () => {
  it("isKeywordTrimCrash matches the KeywordFieldWidget stack", () => {
    assert.equal(
      isKeywordTrimCrash(
        "Uncaught TypeError: (e.value ?? \"\").trim is not a function",
      ),
      true,
    );
    assert.equal(isKeywordTrimCrash("network failed"), false);
    assert.equal(isKeywordTrimCrash(""), false);
  });

  it("isEditorStayVisible requires host chrome plus form or error", () => {
    assert.equal(isEditorStayVisible({}), false);
    assert.equal(isEditorStayVisible({ host: true, overlay: true }), false);
    assert.equal(
      isEditorStayVisible({ host: true, overlay: true, form: true }),
      true,
    );
    assert.equal(
      isEditorStayVisible({ host: true, overlay: true, error: true }),
      true,
    );
    assert.equal(
      isEditorStayVisible({ host: true, overlay: true, loading: true }),
      true,
    );
  });
});

const editorWorkflow = require("../helpers/editor-host-workflow");

describe("editor-host-workflow helpers (#4539)", () => {
  it("builds editor SPA URL and parses transitionWithComments", () => {
    assert.equal(
      editorWorkflow.editorSpaUrl("http://127.0.0.1:9992/", "contentId=42&mode=edit"),
      "http://127.0.0.1:9992/Rhythmyx/cm/app/spa.jsp?contentId=42&mode=edit&entry=editor",
    );
    assert.equal(
      editorWorkflow.triggerTestId("Reject"),
      "editor-workflow-trigger-Reject",
    );
    const parsed = editorWorkflow.parseTransitionWithCommentsUrl(
      "http://cms/Rhythmyx/services/itemmanagement/workflow/transitionWithComments/42/Reject?comment=needs%20work",
    );
    assert.equal(parsed.itemId, "42");
    assert.equal(parsed.trigger, "Reject");
    assert.equal(parsed.comment, "needs work");
    assert.equal(
      editorWorkflow.isGetTransitionsUrl(
        "/Rhythmyx/services/itemmanagement/workflow/getTransitions/42",
      ),
      true,
    );
    assert.equal(
      editorWorkflow.isTransitionWithCommentsUrl(
        "/Rhythmyx/services/itemmanagement/workflow/transitionWithComments/42/Submit",
      ),
      true,
    );
  });

  it("spec covers comment-required block and view-mode read-only", () => {
    const specPath = path.join(
      __dirname,
      "..",
      "editor-host-workflow-transitions.spec.js",
    );
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /Enter a comment/);
    assert.match(src, /mode=view/);
    assert.match(src, /triggerTestId\("Reject"\)/);
    assert.match(src, /#4539/);
  });
});

const editorPublish = require("../helpers/editor-host-publish");

describe("editor-host-publish helpers (#4540)", () => {
  it("classifies page vs resource demand-publish URLs", () => {
    assert.equal(
      editorPublish.isPagePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/page/42",
      ),
      true,
    );
    assert.equal(
      editorPublish.isPagePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/page/staging/42",
      ),
      false,
    );
    assert.equal(
      editorPublish.isResourcePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/resource/99",
      ),
      true,
    );
    const parsed = editorPublish.parsePublishUrl(
      "http://cms/Rhythmyx/services/sitemanage/publish/page/42",
    );
    assert.equal(parsed.kind, "page");
    assert.equal(parsed.itemId, "42");
  });

  it("spec covers confirm, FORBIDDEN failure, and view-mode read-only", () => {
    const specPath = path.join(
      __dirname,
      "..",
      "editor-host-publish-now.spec.js",
    );
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /FORBIDDEN/);
    assert.match(src, /mode=view/);
    assert.match(src, /#4540/);
    assert.match(src, /publish\/resource/);
  });
});

const editorStage = require("../helpers/editor-host-stage");

describe("editor-host-stage helpers (#4915)", () => {
  it("classifies page vs resource staging URLs", () => {
    assert.equal(
      editorStage.isPageStageUrl(
        "/Rhythmyx/services/sitemanage/publish/page/staging/42",
      ),
      true,
    );
    assert.equal(
      editorStage.isPageStageUrl("/Rhythmyx/services/sitemanage/publish/page/42"),
      false,
    );
    assert.equal(
      editorStage.isResourceStageUrl(
        "/Rhythmyx/services/sitemanage/publish/resource/staging/99",
      ),
      true,
    );
    const parsed = editorStage.parseStageUrl(
      "http://cms/Rhythmyx/services/sitemanage/publish/page/staging/42",
    );
    assert.equal(parsed.kind, "page");
    assert.equal(parsed.itemId, "42");
  });

  it("spec covers confirm, cancel, HTTP 400/403, and templates", () => {
    const specPath = path.join(__dirname, "..", "editor-host-stage.spec.js");
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /Stage this item/);
    assert.match(src, /dialog.dismiss/);
    assert.match(src, /stageStatus: 403/);
    assert.match(src, /stageStatus: 400/);
    assert.match(src, /Could not stage this item/);
    assert.match(src, /percTemplate/);
    assert.match(src, /#4915/);
  });
});
