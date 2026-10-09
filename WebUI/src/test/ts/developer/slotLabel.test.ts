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
import { savedSlotLabel, slotLabelWrite, storedSlotLabel } from "../../../main/ts/developer/slotLabel";

const baseline: SlotDetail = {
  name: "qa5431slot",
  label: "QA slot",
  description: "Folder list",
  slotType: "INLINE",
  finderName: "sys_RelationshipContentFinder",
  relationshipName: "ActiveAssembly",
  finderArguments: { type: "qa5431" },
};

describe("slotLabelWrite", () => {
  it("sends the label only and omits name, description, type, and finder", () => {
    const sent = slotLabelWrite(baseline, " note ");
    expect(sent).toEqual({ label: "note" });
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("slotType");
    expect(sent).not.toHaveProperty("finderName");
    expect(sent).not.toHaveProperty("relationshipName");
    expect(sent).not.toHaveProperty("finderArguments");
    expect(sent).not.toHaveProperty("associations");
  });

  it("sends an empty label and skips an unchanged label", () => {
    expect(slotLabelWrite(baseline, "   ")).toEqual({ label: "" });
    expect(slotLabelWrite(baseline, " QA slot ")).toBe("unchanged");
    expect(slotLabelWrite({ label: "  " }, "\n")).toBe("unchanged");
    expect(storedSlotLabel(" a\nb ")).toBe("a b");
  });
});

describe("savedSlotLabel", () => {
  it("accepts a label when name, description, type, and finder stay", () => {
    const sent = slotLabelWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedSlotLabel(sent, baseline, {
        SlotDetail: { ...baseline, label: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank label that echoes the name and does not wipe it", () => {
    const sent = slotLabelWrite(baseline, "");
    expect(sent).toEqual({ label: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedSlotLabel(sent, baseline, { ...baseline, label: "" })).toBe("");
    expect(savedSlotLabel(sent, baseline, { ...baseline, label: baseline.name })).toBe(
      baseline.name,
    );
    expect(
      savedSlotLabel(sent, baseline, { ...baseline, label: baseline.name, name: "" }),
    ).toBeNull();
    expect(
      savedSlotLabel(sent, baseline, { ...baseline, label: "other", name: "other" }),
    ).toBeNull();
    expect(savedSlotLabel(sent, baseline, { ...baseline, label: "other" })).toBeNull();
  });

  it("rejects a response that changes the description, type, or finder, or sends another field", () => {
    const sent = slotLabelWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedSlotLabel(sent, baseline, {
        ...baseline,
        label: "note",
        description: "changed",
      }),
    ).toBeNull();
    expect(savedSlotLabel(sent, baseline, { ...baseline, label: "note", name: "other" })).toBeNull();
    expect(
      savedSlotLabel(sent, baseline, {
        ...baseline,
        label: "note",
        slotType: "REGULAR",
      }),
    ).toBeNull();
    expect(
      savedSlotLabel(sent, baseline, {
        ...baseline,
        label: "note",
        finderName: "otherFinder",
      }),
    ).toBeNull();
    expect(
      savedSlotLabel(sent, baseline, {
        ...baseline,
        label: "note",
        finderArguments: {},
      }),
    ).toBeNull();
    expect(
      savedSlotLabel({ ...sent, description: "Folder list" }, baseline, {
        ...baseline,
        label: "note",
      }),
    ).toBeNull();
    expect(
      savedSlotLabel({ ...sent, name: baseline.name }, baseline, { ...baseline, label: "note" }),
    ).toBeNull();
  });
});
