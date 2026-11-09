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
  canViewScheduledTransitionsFromEditor,
  hasScheduledTransitions,
  scheduledTransitionEntries,
  summarizeScheduledTransition,
} from "../../../main/ts/editor/editorScheduledTransitions";

describe("editorScheduledTransitions (#5830)", () => {
  it("is viewable in edit and view, never in promote", () => {
    expect(canViewScheduledTransitionsFromEditor("edit")).toBe(true);
    expect(canViewScheduledTransitionsFromEditor("view")).toBe(true);
    expect(canViewScheduledTransitionsFromEditor("promote")).toBe(false);
  });

  it("lists the embargo row before the expiry row", () => {
    expect(
      scheduledTransitionEntries({
        startDate: "09/18/2026 09:00 am",
        endDate: "09/19/2026 10:00 am",
      }),
    ).toEqual([
      { kind: "embargo", value: "09/18/2026 09:00 am" },
      { kind: "expiry", value: "09/19/2026 10:00 am" },
    ]);
  });

  it("drops blank or whitespace-only sides", () => {
    expect(
      scheduledTransitionEntries({ startDate: "  ", endDate: "" }),
    ).toEqual([]);
    expect(
      scheduledTransitionEntries({
        startDate: " 09/18/2026 09:00 am ",
        endDate: "",
      }),
    ).toEqual([{ kind: "embargo", value: "09/18/2026 09:00 am" }]);
    expect(
      scheduledTransitionEntries({ startDate: "", endDate: "09/19/2026 10:00 am" }),
    ).toEqual([{ kind: "expiry", value: "09/19/2026 10:00 am" }]);
  });

  it("treats a missing payload as no scheduled triggers", () => {
    expect(scheduledTransitionEntries(null)).toEqual([]);
    expect(scheduledTransitionEntries(undefined)).toEqual([]);
    expect(hasScheduledTransitions(null)).toBe(false);
    expect(
      hasScheduledTransitions({ startDate: "", endDate: "" }),
    ).toBe(false);
    expect(
      hasScheduledTransitions({ startDate: "09/18/2026 09:00 am", endDate: "" }),
    ).toBe(true);
  });

  it("summarizes one row with its trigger label", () => {
    expect(
      summarizeScheduledTransition({
        kind: "embargo",
        value: "09/18/2026 09:00 am",
      }),
    ).toBe("Embargo: 09/18/2026 09:00 am");
    expect(
      summarizeScheduledTransition({
        kind: "expiry",
        value: "09/19/2026 10:00 am",
      }),
    ).toBe("Expiry: 09/19/2026 10:00 am");
  });
});
