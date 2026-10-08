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
import type { SlotDetail } from "../../../main/ts/api/developer/types";
import {
  savedSlotDescription,
  slotDescriptionWrite,
  storedSlotDescription,
} from "../../../main/ts/developer/slotDescription";

const baseline: SlotDetail = {
  name: "qa5408slot",
  label: "QA slot",
  description: "Folder list",
  slotType: "INLINE",
  finderName: "sys_RelationshipContentFinder",
  relationshipName: "ActiveAssembly",
  finderArguments: { type: "qa5408" },
};

describe("slotDescriptionWrite", () => {
  it("sends the description only and omits name, label, type, and finder", () => {
    const sent = slotDescriptionWrite(baseline, " note ");
    expect(sent).toEqual({ description: "note" });
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("slotType");
    expect(sent).not.toHaveProperty("finderName");
    expect(sent).not.toHaveProperty("relationshipName");
    expect(sent).not.toHaveProperty("finderArguments");
    expect(sent).not.toHaveProperty("associations");
  });

  it("sends an empty description to clear and skips an unchanged description", () => {
    expect(slotDescriptionWrite(baseline, "   ")).toEqual({ description: "" });
    expect(slotDescriptionWrite(baseline, " Folder list ")).toBe("unchanged");
    expect(slotDescriptionWrite({ description: "  " }, "\n")).toBe("unchanged");
    expect(storedSlotDescription(" a\nb ")).toBe("a b");
  });
});

describe("savedSlotDescription", () => {
  it("accepts a description when name, label, type, and finder stay", () => {
    const sent = slotDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedSlotDescription(sent, baseline, {
        SlotDetail: { ...baseline, description: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank description that clears and keeps the other fields", () => {
    const sent = slotDescriptionWrite(baseline, "");
    expect(sent).toEqual({ description: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedSlotDescription(sent, baseline, { ...baseline, description: "" })).toBe("");
  });

  it("rejects a response that changes the label, type, or finder, or sends another field", () => {
    const sent = slotDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedSlotDescription(sent, baseline, { ...baseline, description: "note", label: "Other" }),
    ).toBeNull();
    expect(
      savedSlotDescription(sent, baseline, { ...baseline, description: "note", name: "other" }),
    ).toBeNull();
    expect(
      savedSlotDescription(sent, baseline, {
        ...baseline,
        description: "note",
        slotType: "REGULAR",
      }),
    ).toBeNull();
    expect(
      savedSlotDescription(sent, baseline, {
        ...baseline,
        description: "note",
        finderName: "otherFinder",
      }),
    ).toBeNull();
    expect(
      savedSlotDescription(sent, baseline, {
        ...baseline,
        description: "note",
        finderArguments: {},
      }),
    ).toBeNull();
    expect(savedSlotDescription({ ...sent, label: "QA slot" }, baseline, { ...baseline, description: "note" })).toBeNull();
  });
});
