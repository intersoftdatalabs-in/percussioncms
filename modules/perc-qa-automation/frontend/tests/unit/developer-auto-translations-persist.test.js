/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
const fs = require("fs");
const path = require("path");

const specPath = path.join(
  __dirname,
  "..",
  "developer-auto-translations-persist.spec.js",
);

describe("developer-auto-translations-persist spec (#4039)", () => {
  it("exists and does not intercept PUT auto-translations", () => {
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(src, /locales\/auto-translations/);
    assert.match(src, /page\.request\.put/);
    assert.doesNotMatch(src, /page\.route\s*\(/);
    assert.doesNotMatch(src, /route\.fulfill/);
    assert.match(src, /empty PUT/);
  });
});
