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
  checkoutOwnerErrorReason,
  unwrapCheckoutOwner,
} from "../../../main/ts/contentExplorer/checkoutOwner";

describe("checkout owner lookup (#4910)", () => {
  it("reads another user's lock", () => {
    const info = unwrapCheckoutOwner({
      EditorItemLockInfo: { checkOutUser: "editor", currentUser: "Admin", itemName: "Home" },
    });
    expect(info.checkOutUser).toBe("editor");
  });

  it("treats a missing owner as not checked out", () => {
    expect(unwrapCheckoutOwner({ checkOutUser: "" }).checkOutUser).toBe("");
  });

  it("maps HTTP 403 onto the panel reason", () => {
    expect(checkoutOwnerErrorReason({ status: 403, statusText: "Forbidden" })).toBe(
      "forbidden",
    );
    expect(checkoutOwnerErrorReason({ status: 500, statusText: "err" })).toBe("failed");
  });
});
