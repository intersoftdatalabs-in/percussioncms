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

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  PASTE_TEST_IDS,
  explorerProductPasteUrl,
  hasRxFolderMutationsQuery,
  uniquePasteName,
  isFoldersCopyItemUrl,
} = require("../helpers/explorer-paste-clipboard");

describe("explorer-paste-clipboard helpers (#4638)", () => {
  it("exports paste panel test ids", () => {
    assert.equal(PASTE_TEST_IDS.clipboardPaste, "clipboard-paste");
    assert.equal(PASTE_TEST_IDS.clipboardDest, "clipboard-paste-dest");
    assert.equal(PASTE_TEST_IDS.shell, "content-explorer-shell");
  });

  it("builds product explorer URL without rxFolderMutations", () => {
    const url = explorerProductPasteUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.equal(hasRxFolderMutationsQuery(url), false);
  });

  it("unique paste names stay qa4638-prefixed", () => {
    assert.match(uniquePasteName("qa4638src", 1), /^qa4638src_1$/);
  });

  it("classifies copy/item URLs", () => {
    assert.equal(
      isFoldersCopyItemUrl("http://127.0.0.1/Rhythmyx/rest/folders/copy/item"),
      true,
    );
  });
});
