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
import {
  BOOTSTRAP_ROLE_ADMIN,
  BOOTSTRAP_ROLE_DESIGNER,
  resolveCurrentUserIdentities,
} from "../../../main/ts/contentExplorer/currentUserIdentities";

describe("resolveCurrentUserIdentities", () => {
  it("returns empty when no session identity is available", () => {
    expect(resolveCurrentUserIdentities({})).toEqual([]);
    expect(
      resolveCurrentUserIdentities({ userName: "   ", roles: ["", "  "] }),
    ).toEqual([]);
  });

  it("includes the signed-in user name", () => {
    expect(resolveCurrentUserIdentities({ userName: "Editor1" })).toEqual([
      "Editor1",
    ]);
  });

  it("adds Admin role when isAdmin is true (does not invent when false)", () => {
    // Dedup when userName already equals the role name.
    expect(
      resolveCurrentUserIdentities({ userName: "Admin", isAdmin: true }),
    ).toEqual(["Admin"]);
    expect(
      resolveCurrentUserIdentities({ userName: "Alice", isAdmin: true }),
    ).toEqual(["Alice", BOOTSTRAP_ROLE_ADMIN]);
    expect(
      resolveCurrentUserIdentities({ userName: "Alice", isAdmin: false }),
    ).toEqual(["Alice"]);
  });

  it("adds Designer role when isDesigner is true", () => {
    expect(
      resolveCurrentUserIdentities({
        userName: "Des",
        isDesigner: true,
      }),
    ).toEqual(["Des", BOOTSTRAP_ROLE_DESIGNER]);
  });

  it("merges explicit roles without duplicates or blanks", () => {
    expect(
      resolveCurrentUserIdentities({
        userName: "Admin",
        isAdmin: true,
        roles: ["Admin", "Editor", "  ", "Contributor", "Editor"],
      }),
    ).toEqual(["Admin", "Editor", "Contributor"]);
  });

  it("works with roles-only source (no userName)", () => {
    expect(
      resolveCurrentUserIdentities({
        isAdmin: true,
        roles: ["Publisher"],
      }),
    ).toEqual([BOOTSTRAP_ROLE_ADMIN, "Publisher"]);
  });
});
