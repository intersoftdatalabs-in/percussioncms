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
  isStagedStatusUrl,
  isWritePublishUrl,
} = require("../helpers/publish-sites-staged-status");

describe("publish-sites-staged-status helpers (#5838)", () => {
  it("builds the Publish sites SPA entry URL", () => {
    assert.equal(
      publishingSitesUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish",
    );
  });

  it("matches staged-status readback GET paths only", () => {
    assert.equal(
      isStagedStatusUrl("/Rhythmyx/services/itemmanagement/item/pubhistory/42"),
      true,
    );
    assert.equal(
      isStagedStatusUrl("/Rhythmyx/sitemanage/publish/42/prod"),
      false,
    );
  });

  it("flags write publish URLs", () => {
    assert.equal(
      isWritePublishUrl("/Rhythmyx/services/sitemanage/publish/42/prod"),
      true,
    );
    assert.equal(
      isWritePublishUrl(
        "/Rhythmyx/services/itemmanagement/item/pubhistory/42",
      ),
      false,
    );
  });
});
