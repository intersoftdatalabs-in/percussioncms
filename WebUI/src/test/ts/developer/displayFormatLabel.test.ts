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
import type { DisplayFormat } from "../../../main/ts/api/developer/types";
import {
  displayFormatLabelWrite,
  savedDisplayFormatLabel,
  storedDisplayFormatLabel,
} from "../../../main/ts/developer/displayFormatLabel";

const baseline: DisplayFormat = {
  name: "qa5432fmt",
  label: "QA format",
  displayName: "QA format",
  description: "Folder list",
  columns: [
    { source: "sys_title", displayName: "Title", position: 0 },
    { source: "sys_contentcreatedby", displayName: "Created by", position: 1 },
  ],
  allowedCommunities: [{ guid: "0-13-10", name: "Default" }],
};

describe("displayFormatLabelWrite", () => {
  it("sends the label only and omits name, description, and lists that must stay", () => {
    const sent = displayFormatLabelWrite(baseline, " note ");
    expect(sent).toEqual({ label: "note" });
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("columns");
    expect(sent).not.toHaveProperty("allowedCommunities");
    expect(sent).not.toHaveProperty("displayName");
  });

  it("sends an empty label and skips an unchanged label", () => {
    expect(displayFormatLabelWrite(baseline, "   ")).toEqual({ label: "" });
    expect(displayFormatLabelWrite(baseline, " QA format ")).toBe("unchanged");
    expect(displayFormatLabelWrite({ label: "  " }, "\n")).toBe("unchanged");
    expect(storedDisplayFormatLabel(" a\nb ")).toBe("a b");
  });
});

describe("savedDisplayFormatLabel", () => {
  it("accepts a label when name, description, columns, and communities stay", () => {
    const sent = displayFormatLabelWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedDisplayFormatLabel(sent, baseline, {
        DisplayFormat: { ...baseline, label: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank label that echoes the name and does not wipe it", () => {
    const sent = displayFormatLabelWrite(baseline, "");
    expect(sent).toEqual({ label: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedDisplayFormatLabel(sent, baseline, { ...baseline, label: "" })).toBe("");
    expect(savedDisplayFormatLabel(sent, baseline, { ...baseline, label: baseline.name })).toBe(
      baseline.name,
    );
    expect(
      savedDisplayFormatLabel(sent, baseline, { ...baseline, label: baseline.name, name: "" }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel(sent, baseline, { ...baseline, label: "other", name: "other" }),
    ).toBeNull();
    expect(savedDisplayFormatLabel(sent, baseline, { ...baseline, label: "other" })).toBeNull();
  });

  it("rejects a response that changes the name, description, columns, or communities", () => {
    const sent = displayFormatLabelWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedDisplayFormatLabel(sent, baseline, {
        ...baseline,
        label: "note",
        name: "Other",
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel(sent, baseline, {
        ...baseline,
        label: "note",
        description: "changed",
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel(sent, baseline, {
        ...baseline,
        label: "note",
        columns: [{ source: "sys_title" }],
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel(sent, baseline, {
        ...baseline,
        label: "note",
        allowedCommunities: [],
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel({ ...sent, description: "Folder list" }, baseline, {
        ...baseline,
        label: "note",
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatLabel({ ...sent, name: baseline.name }, baseline, {
        ...baseline,
        label: "note",
      }),
    ).toBeNull();
  });
});
