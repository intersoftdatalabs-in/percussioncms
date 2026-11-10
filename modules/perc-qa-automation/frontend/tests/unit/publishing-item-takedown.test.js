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
  publishingSitesUrl,
  isFindLinkedItemsUrl,
  isTakedownUrl,
  isWritePublishUrl,
} = require("../helpers/publishing-item-takedown");

describe("publishing-item-takedown helpers (#5930)", () => {
  it("builds the Publish sites SPA entry URL", () => {
    assert.equal(
      publishingSitesUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish",
    );
  });

  it("matches the linked-page lookup GET", () => {
    assert.equal(
      isFindLinkedItemsUrl(
        "/Rhythmyx/services/itemmanagement/item/findLinkedItems/42",
      ),
      true,
    );
    assert.equal(
      isFindLinkedItemsUrl("/Rhythmyx/services/sitemanage/publish/page/42"),
      false,
    );
  });

  it("matches the existing page/resource takedown route", () => {
    assert.equal(
      isTakedownUrl("/Rhythmyx/services/sitemanage/publish/takedown/page/42"),
      true,
    );
    assert.equal(
      isTakedownUrl(
        "/Rhythmyx/services/sitemanage/publish/takedown/resource/99",
      ),
      true,
    );
    assert.equal(
      isTakedownUrl("/Rhythmyx/services/sitemanage/publish/page/42"),
      false,
    );
  });

  it("flags takedown calls as publish writes", () => {
    assert.equal(
      isWritePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/takedown/page/42",
      ),
      true,
    );
    assert.equal(
      isWritePublishUrl(
        "/Rhythmyx/services/itemmanagement/item/findLinkedItems/42",
      ),
      false,
    );
  });
});
