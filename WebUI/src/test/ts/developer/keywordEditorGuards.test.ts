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

import { describe, expect, it } from "vitest";
import type {
  KeywordEditStartContention,
  KeywordWriteContention,
} from "../../../main/ts/developer/keywordEditorGuards";
import {
  anyFlagSet,
  isKeywordEditStartContended,
  isKeywordWriteContended,
} from "../../../main/ts/developer/keywordEditorGuards";

const idleSave: KeywordWriteContention = {
  inflight: false,
  peerBusy: false,
  busy: false,
  confirmKind: null,
};

const idleStart: KeywordEditStartContention = {
  writeBusy: false,
  confirmOpen: false,
  editOpen: false,
  inflight: false,
};

describe("isKeywordWriteContended", () => {
  it("stays open when nothing is in flight", () => {
    expect(isKeywordWriteContended(idleSave)).toBe(false);
  });

  it("blocks on any in-flight write, peer busy flag, keyword busy, or open confirm", () => {
    expect(isKeywordWriteContended({ ...idleSave, inflight: true })).toBe(true);
    expect(isKeywordWriteContended({ ...idleSave, peerBusy: true })).toBe(true);
    expect(isKeywordWriteContended({ ...idleSave, busy: true })).toBe(true);
    expect(isKeywordWriteContended({ ...idleSave, confirmKind: "choice" })).toBe(true);
    expect(isKeywordWriteContended({ ...idleSave, confirmKind: "keyword" })).toBe(true);
    expect(isKeywordWriteContended({ ...idleSave, confirmKind: "clear-description" })).toBe(true);
  });
});

describe("isKeywordEditStartContended", () => {
  it("stays open when nothing is in flight", () => {
    expect(isKeywordEditStartContended(idleStart)).toBe(false);
  });

  it("blocks on write busy, open confirm, open editor, or in-flight write", () => {
    expect(isKeywordEditStartContended({ ...idleStart, writeBusy: true })).toBe(true);
    expect(isKeywordEditStartContended({ ...idleStart, confirmOpen: true })).toBe(true);
    expect(isKeywordEditStartContended({ ...idleStart, editOpen: true })).toBe(true);
    expect(isKeywordEditStartContended({ ...idleStart, inflight: true })).toBe(true);
  });
});

describe("anyFlagSet", () => {
  it("aggregates busy and inflight flags without branching", () => {
    expect(anyFlagSet([])).toBe(false);
    expect(anyFlagSet([false, false, false])).toBe(false);
    expect(anyFlagSet([false, true, false])).toBe(true);
    expect(anyFlagSet([true, true, true])).toBe(true);
  });
});
