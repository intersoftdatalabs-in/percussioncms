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
 * Unit tests for Explorer search-submit helpers (#3617) — no live CMS.
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  EXTENDED_RESULTS_PATH,
  explorerEntryUrl,
  isPilotSearchJsp,
  isExtendedResultsPost,
  isSearchSubmitSuccessStatus,
  isSearchSubmitFailureStatus,
  terminalSuccessSelector,
  classifySubmitOutcome,
  isSuccessfulSubmitOutcome,
} = require("../helpers/explorer-search-submit");

describe("explorer-search-submit helpers (#3617)", () => {
  it("exports stable SearchPanel / shell test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.toggleSearch, "explorer-toggle-search");
    assert.equal(TEST_IDS.searchPanel, "search-panel");
    assert.equal(TEST_IDS.searchInput, "search-panel-input");
    assert.equal(TEST_IDS.searchSubmit, "search-panel-submit");
    assert.equal(TEST_IDS.results, "search-panel-results");
    assert.equal(TEST_IDS.empty, "search-panel-empty");
    assert.equal(TEST_IDS.error, "search-panel-error");
    assert.equal(
      EXTENDED_RESULTS_PATH,
      "/searchmanagement/search/get/extendedresults",
    );
  });

  it("explorerEntryUrl builds spa.jsp explorer entry, not searchModern.jsp", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    const live = explorerEntryUrl("http://127.0.0.1:9993/");
    assert.match(live, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(isPilotSearchJsp(live), false);
    assert.equal(
      isPilotSearchJsp(
        "http://127.0.0.1:9993/Rhythmyx/cm/app/searchModern.jsp",
      ),
      true,
    );
  });

  it("isExtendedResultsPost matches POST extendedresults only", () => {
    const url =
      "http://127.0.0.1:9993/Rhythmyx/services/searchmanagement/search/get/extendedresults";
    assert.equal(isExtendedResultsPost(url, "POST"), true);
    assert.equal(isExtendedResultsPost(url, "GET"), false);
    assert.equal(
      isExtendedResultsPost(
        "http://127.0.0.1:9993/Rhythmyx/services/searches/View_All/execute",
        "POST",
      ),
      false,
    );
  });

  it("treats HTTP 200/204 as success and 4xx/5xx as failure", () => {
    assert.equal(isSearchSubmitSuccessStatus(200), true);
    assert.equal(isSearchSubmitSuccessStatus(204), true);
    assert.equal(isSearchSubmitSuccessStatus(400), false);
    assert.equal(isSearchSubmitSuccessStatus(500), false);
    assert.equal(isSearchSubmitFailureStatus(400), true);
    assert.equal(isSearchSubmitFailureStatus(500), true);
    assert.equal(isSearchSubmitFailureStatus(200), false);
  });

  it("classifySubmitOutcome prefers HTTP failure then results/empty, never skip", () => {
    assert.equal(
      classifySubmitOutcome({ status: 500, hasEmpty: true }),
      "http-error",
    );
    assert.equal(
      classifySubmitOutcome({ status: 200, hasResults: true }),
      "results",
    );
    assert.equal(
      classifySubmitOutcome({ status: 200, hasEmpty: true }),
      "empty-success",
    );
    assert.equal(
      classifySubmitOutcome({ status: 200, hasError: true }),
      "ui-error",
    );
    assert.equal(classifySubmitOutcome({}), "unknown");
    assert.equal(isSuccessfulSubmitOutcome("results"), true);
    assert.equal(isSuccessfulSubmitOutcome("empty-success"), true);
    assert.equal(isSuccessfulSubmitOutcome("http-error"), false);
    assert.equal(isSuccessfulSubmitOutcome("ui-error"), false);
  });

  it("terminalSuccessSelector is results or empty, not error", () => {
    const sel = terminalSuccessSelector();
    assert.match(sel, /search-panel-results/);
    assert.match(sel, /search-panel-empty/);
    assert.equal(/search-panel-error/.test(sel), false);
  });
});
