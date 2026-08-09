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
  isValidEmailAddress,
  normalizeCurrentUserBasic,
} from "../../../main/ts/api/user/userCurrentApi";

describe("userCurrentApi", () => {
  it("normalizes flat and wrapped CurrentUser bodies", () => {
    expect(
      normalizeCurrentUserBasic({
        name: "Admin",
        email: " admin@example.com ",
      }),
    ).toEqual({ name: "Admin", email: "admin@example.com" });

    expect(
      normalizeCurrentUserBasic({
        CurrentUser: { name: "ed", email: "ed@x.com" },
      }),
    ).toEqual({ name: "ed", email: "ed@x.com" });
  });

  it("validates email shape; blank allowed", () => {
    expect(isValidEmailAddress("")).toBe(true);
    expect(isValidEmailAddress("a@b.co")).toBe(true);
    expect(isValidEmailAddress("not-email")).toBe(false);
    expect(isValidEmailAddress("a@-b.com")).toBe(false);
  });
});
