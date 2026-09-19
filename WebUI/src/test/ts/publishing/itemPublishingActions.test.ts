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
  actionTargetForName,
  parsePublishingActions,
  publishingActionsErrorMessage,
  safeActionsItemId,
} from "@/publishing/itemPublishingActions";

describe("itemPublishingActions", () => {
  it("parses a bare server array", () => {
    expect(
      parsePublishingActions([
        { name: "Publish", enabled: true },
        { name: "Schedule...", enabled: false },
        { name: "Remove from Site", enabled: true },
        { name: "Stage", enabled: false },
        { name: "Remove from Staging", enabled: true },
      ]),
    ).toEqual([
      { name: "Publish", enabled: true },
      { name: "Schedule...", enabled: false },
      { name: "Remove from Site", enabled: true },
      { name: "Stage", enabled: false },
      { name: "Remove from Staging", enabled: true },
    ]);
  });

  it("parses envelope keys and drops nameless rows", () => {
    expect(
      parsePublishingActions({
        PSPublishingActionList: [
          { name: "Publish", enabled: true },
          { name: "", enabled: true },
          { enabled: true },
        ],
      }),
    ).toEqual([{ name: "Publish", enabled: true }]);
    expect(parsePublishingActions(null)).toEqual([]);
    expect(parsePublishingActions({ unexpected: 1 })).toEqual([]);
  });

  it("treats missing enabled as unavailable", () => {
    expect(parsePublishingActions([{ name: "Stage" }])).toEqual([
      { name: "Stage", enabled: false },
    ]);
  });

  it("maps server names to panel targets", () => {
    expect(actionTargetForName("Publish")).toBe("publish-now");
    expect(actionTargetForName("Schedule...")).toBe("schedule");
    expect(actionTargetForName("Remove from Site")).toBe("takedown");
    expect(actionTargetForName("Stage")).toBe("stage");
    expect(actionTargetForName("Remove from Staging")).toBe("unstage");
    expect(actionTargetForName("Bogus")).toBeNull();
  });

  it("normalizes the menu item id like peer panels", () => {
    expect(safeActionsItemId("42")).toBe("42");
    expect(safeActionsItemId("  ")).toBe("");
    expect(safeActionsItemId(undefined)).toBe("");
  });

  it("maps 403 to forbidden and 404 to not found", () => {
    expect(
      publishingActionsErrorMessage({ status: 403, message: "x" }),
    ).toContain("Forbidden");
    expect(
      publishingActionsErrorMessage({ status: 404, message: "x" }),
    ).toContain("not found");
  });
});
