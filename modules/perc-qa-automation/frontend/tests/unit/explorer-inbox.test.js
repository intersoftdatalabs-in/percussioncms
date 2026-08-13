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
 * Unit tests for Explorer Inbox pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  PATH_MY_CONTENT_INBOX,
  INBOX_VIEW_NAME,
  PATH_VIEWS,
  explorerEntryUrl,
  viewsCatalogUrl,
  viewsExecuteUrl,
  unwrapViewDefs,
  isInboxView,
  findInboxView,
  inboxLeafSelector,
  inboxResultsSelector,
  isViewExecuteJaxbError,
  missingInboxSkipMessage,
  noAssignmentsSkipMessage,
} = require("../helpers/explorer-inbox");

describe("explorer-inbox helpers (#3241)", () => {
  it("exports stable test ids and DCE Inbox path", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.viewsTree, "explorer-views-tree");
    assert.equal(TEST_IDS.inboxLeaf, "explorer-views-leaf-Inbox");
    assert.equal(TEST_IDS.inboxIcon, "explorer-views-inbox-icon");
    assert.equal(TEST_IDS.resultsEmpty, "explorer-view-results-empty");
    assert.equal(PATH_MY_CONTENT_INBOX, "//Views//MyContent/Inbox");
    assert.equal(INBOX_VIEW_NAME, "Inbox");
    assert.equal(PATH_VIEWS, "/Rhythmyx/services/views");
  });

  it("explorerEntryUrl builds spa.jsp explorer entry with cache-buster", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    assert.equal(
      explorerEntryUrl("http://localhost:9992/", { cacheBuster: "a b" }),
      "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=a%20b",
    );
  });

  it("views catalog and execute URLs encode idOrName", () => {
    assert.equal(
      viewsCatalogUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/Rhythmyx/services/views",
    );
    assert.equal(
      viewsExecuteUrl("http://cms.example", "Inbox"),
      "http://cms.example/Rhythmyx/services/views/Inbox/execute",
    );
    assert.equal(
      viewsExecuteUrl("http://cms.example/", "My Content"),
      "http://cms.example/Rhythmyx/services/views/My%20Content/execute",
    );
  });

  it("unwrapViewDefs handles arrays and Jackson wrappers", () => {
    assert.deepEqual(unwrapViewDefs(null), []);
    assert.deepEqual(unwrapViewDefs([{ name: "Inbox" }]), [{ name: "Inbox" }]);
    assert.deepEqual(unwrapViewDefs({ ViewDef: { name: "B" } }), [
      { name: "B" },
    ]);
    assert.deepEqual(
      unwrapViewDefs({ viewDef: [{ name: "C" }, { name: "D" }] }),
      [{ name: "C" }, { name: "D" }],
    );
  });

  it("isInboxView matches name, label, classic URL, and DCE path", () => {
    assert.equal(isInboxView(null), false);
    assert.equal(isInboxView({ name: "Outbox" }), false);
    assert.equal(isInboxView({ name: "Inbox" }), true);
    assert.equal(isInboxView({ name: "inbox" }), true);
    assert.equal(isInboxView({ label: "Inbox", name: "sys_inbox" }), true);
    assert.equal(
      isInboxView({ name: "x", url: "../sys_cxViews/inbox.xml" }),
      true,
    );
    assert.equal(
      isInboxView({ name: "//Views//MyContent/Inbox" }),
      true,
    );
    assert.equal(
      isInboxView({ name: "x", url: "../SYS_CXVIEWS/INBOX.xml" }),
      false,
    );
    assert.equal(
      isInboxView({ name: "//views//mycontent/inbox" }),
      true,
    );
  });

  it("findInboxView returns the Inbox def or null", () => {
    assert.equal(findInboxView(null), null);
    assert.equal(findInboxView([{ name: "Recent" }]), null);
    const hit = findInboxView([
      { name: "Recent" },
      { name: "Inbox", customView: true },
    ]);
    assert.equal(hit && hit.name, "Inbox");
  });

  it("inboxLeafSelector and inboxResultsSelector target clickable/content nodes only", () => {
    const leaf = inboxLeafSelector();
    assert.match(leaf, /explorer-views-leaf-Inbox/);
    assert.doesNotMatch(leaf, /explorer-views-inbox[^-]/);
    assert.doesNotMatch(leaf, /data-cx-path/);
    const results = inboxResultsSelector();
    assert.match(results, /explorer-view-results-empty/);
    assert.match(results, /explorer-view-results-list/);
    assert.match(results, /explorer-view-results-loading/);
    assert.match(results, /explorer-view-results-error/);
    assert.doesNotMatch(results, /data-testid="explorer-view-results"/);
  });

  it("isViewExecuteJaxbError matches Inbox 400 envelope failures (#3323)", () => {
    assert.equal(isViewExecuteJaxbError(""), false);
    assert.equal(isViewExecuteJaxbError("timeout"), false);
    assert.equal(
      isViewExecuteJaxbError(
        'JAXBException occurred: unexpected element (uri:"", local:"startIndex"). Expected elements are <{}ViewExecuteRequest>.',
      ),
      true,
    );
    assert.equal(
      isViewExecuteJaxbError("Failed to run view: unexpected element startIndex / ViewExecuteRequest"),
      true,
    );
  });

  it("skip messages mention leaf vs empty assignments", () => {
    const missing = missingInboxSkipMessage({
      catalogEmpty: true,
      restStatus: 404,
    });
    assert.match(missing, /Inbox leaf not on Explorer/);
    assert.match(missing, /REST status=404/);
    assert.match(missing, /no Inbox view/);
    const empty = noAssignmentsSkipMessage({ restStatus: 200 });
    assert.match(empty, /no assignment rows/);
    assert.match(empty, /REST status=200/);
  });
});
