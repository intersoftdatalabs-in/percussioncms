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

import { describe, expect, it } from "vitest";
import { availableUsersMinusAssigned } from "../../../main/ts/workflowAdmin/role/roleUsers";

describe("availableUsersMinusAssigned", () => {
  it("returns all users when none are assigned", () => {
    expect(
      availableUsersMinusAssigned(["Admin", "Editor", "Contributor"], []),
    ).toEqual(["Admin", "Editor", "Contributor"]);
  });

  it("drops assigned members and keeps remaining users (#3504)", () => {
    expect(
      availableUsersMinusAssigned(
        ["Admin", "Editor", "Contributor"],
        ["Editor"],
      ),
    ).toEqual(["Admin", "Contributor"]);
  });

  it("returns empty when every user is assigned", () => {
    expect(
      availableUsersMinusAssigned(["Admin", "Editor"], ["Editor", "Admin"]),
    ).toEqual([]);
  });

  it("skips blank names and does not mutate inputs", () => {
    const all = ["Admin", "", "Editor"];
    const assigned = ["Admin"];
    expect(availableUsersMinusAssigned(all, assigned)).toEqual(["Editor"]);
    expect(all).toEqual(["Admin", "", "Editor"]);
    expect(assigned).toEqual(["Admin"]);
  });
});
