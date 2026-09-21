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
import {
  canUseEditorCheckoutActions,
  editorLockErrorReason,
  isCheckedOutToSelf,
} from "../../../main/ts/editor/editorCheckout";

describe("editorCheckout", () => {
  it("treats matching names as checked out to self", () => {
    expect(isCheckedOutToSelf("admin", "admin")).toBe(true);
    expect(isCheckedOutToSelf("Admin", "admin")).toBe(true);
  });

  it("is view-only when another user holds the lock", () => {
    expect(isCheckedOutToSelf("editor", "admin")).toBe(false);
  });

  it("treats 200 checkout with empty session as self when fields list a user", () => {
    expect(isCheckedOutToSelf("", "", "admin", true)).toBe(true);
    expect(isCheckedOutToSelf("", "", "admin", false)).toBe(false);
  });

  it("does not grant canEdit when currentUser is empty and another holder is set", () => {
    expect(isCheckedOutToSelf("editor", "", "admin", true)).toBe(false);
    expect(isCheckedOutToSelf("editor", undefined, undefined, true)).toBe(false);
  });

  it("is not checked out to self when nobody holds the lock", () => {
    expect(isCheckedOutToSelf("", "admin")).toBe(false);
  });

  it("allows checkout actions only in edit mode", () => {
    expect(canUseEditorCheckoutActions("edit")).toBe(true);
    expect(canUseEditorCheckoutActions("view")).toBe(false);
    expect(canUseEditorCheckoutActions("promote")).toBe(false);
  });

  it("maps 403 and 409 without treating them as success", () => {
    expect(editorLockErrorReason({ status: 403, statusText: "Forbidden", body: {} })).toBe(
      "forbidden",
    );
    expect(editorLockErrorReason({ status: 409, statusText: "Conflict", body: {} })).toBe(
      "conflict",
    );
    expect(editorLockErrorReason({ status: 500, statusText: "Error", body: {} })).toBe(
      "failed",
    );
  });
});
