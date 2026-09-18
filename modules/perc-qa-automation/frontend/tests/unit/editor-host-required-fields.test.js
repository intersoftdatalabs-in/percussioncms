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
 * Unit tests for editor required-field helper URLs (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  editorSpaUrl,
  isItemFieldsPutUrl,
  isWorkflowCheckinUrl,
} = require("../helpers/editor-host-required-fields");

describe("editor-host-required-fields helpers (#4541)", () => {
  it("exports stable test ids", () => {
    assert.equal(TEST_IDS.host, "editor-host");
    assert.equal(TEST_IDS.fieldErrorDisplayTitle, "editor-field-error-displaytitle");
    assert.equal(TEST_IDS.saveError, "editor-save-error");
  });

  it("builds spa.jsp editor URLs with slash path segments", () => {
    const url = editorSpaUrl("http://127.0.0.1:9990", "contentId=42&mode=edit");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?/);
    assert.match(url, /entry=editor/);
    assert.match(url, /contentId=42/);
  });

  it("detects fields PUT and check-in URLs", () => {
    assert.equal(
      isItemFieldsPutUrl("http://cms/Rhythmyx/services/itemmanagement/item/fields/42"),
      true,
    );
    assert.equal(
      isWorkflowCheckinUrl("http://cms/Rhythmyx/services/itemmanagement/workflow/checkIn/42"),
      true,
    );
    assert.equal(isItemFieldsPutUrl("http://cms/Rhythmyx/services/contenttypes/percPage"), false);
  });
});
