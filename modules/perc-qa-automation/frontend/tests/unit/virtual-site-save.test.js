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
 * Unit tests for Virtual Site live Save PUT matcher (no live CMS).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const { isVirtualSitePropertiesPut } = require("../helpers/virtual-site-save");

describe("virtual-site-save PUT matcher (#4174)", () => {
  it("matches properties PUT and rejects build/preview/publish", () => {
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual",
        "PUT",
      ),
      true,
    );
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual?_=",
        "PUT",
      ),
      true,
    );
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual/",
        "PUT",
      ),
      true,
    );
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual/build",
        "PUT",
      ),
      false,
    );
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual/publish",
        "PUT",
      ),
      false,
    );
    assert.equal(
      isVirtualSitePropertiesPut(
        "http://127.0.0.1:9993/Rhythmyx/services/sites/Help/virtual",
        "GET",
      ),
      false,
    );
  });
});
