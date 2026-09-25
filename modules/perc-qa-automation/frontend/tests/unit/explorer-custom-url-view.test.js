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
  unwrapViewDefs,
  pickNonInboxCustomView,
  isNamedViewExecute,
} = require("../helpers/explorer-custom-url-view");

describe("explorer custom URL view helpers (#4834)", () => {
  it("unwraps a ViewDefList wrapper and a bare array", () => {
    const wrapped = unwrapViewDefs({
      ViewDefList: { ViewDef: [{ name: "Outbox", customView: true }] },
    });
    assert.equal(wrapped.length, 1);
    assert.equal(wrapped[0].name, "Outbox");
    assert.equal(unwrapViewDefs([{ name: "Recent" }]).length, 1);
  });

  it("prefers Outbox and skips Inbox", () => {
    const picked = pickNonInboxCustomView([
      { name: "Inbox", customView: true },
      { name: "Recent", customView: true },
      { name: "Outbox", customView: true },
      { name: "All", standardView: true },
    ]);
    assert.equal(picked.name, "Outbox");
  });

  it("matches only the exact view execute URL", () => {
    assert.equal(
      isNamedViewExecute(
        "http://cms/Rhythmyx/services/views/Outbox/execute",
        "Outbox",
      ),
      true,
    );
    assert.equal(
      isNamedViewExecute(
        "http://cms/Rhythmyx/services/views/notoutbox/execute",
        "Outbox",
      ),
      false,
    );
    assert.equal(
      isNamedViewExecute(
        "http://cms/Rhythmyx/services/views/Outbox/execute?x=1",
        "Outbox",
      ),
      true,
    );
    assert.equal(isNamedViewExecute("http://cms/%", "Outbox"), false);
  });
});
