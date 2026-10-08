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
  displayFormatDescriptionWrite,
  savedDisplayFormatDescription,
  storedDisplayFormatDescription,
} from "../../../main/ts/developer/displayFormatDescription";

const baseline: DisplayFormat = {
  name: "qa5381fmt",
  label: "QA format",
  displayName: "QA format",
  description: "Folder list",
  columns: [
    { source: "sys_title", displayName: "Title", position: 0 },
    { source: "sys_contentcreatedby", displayName: "Created by", position: 1 },
  ],
  allowedCommunities: [{ guid: "0-13-10", name: "Default" }],
};

describe("displayFormatDescriptionWrite", () => {
  it("sends the description only and omits lists that must stay", () => {
    const sent = displayFormatDescriptionWrite(baseline, " note ");
    expect(sent).toEqual({ description: "note" });
    expect(sent).not.toHaveProperty("columns");
    expect(sent).not.toHaveProperty("allowedCommunities");
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("label");
  });

  it("sends an empty description to clear and skips an unchanged description", () => {
    expect(displayFormatDescriptionWrite(baseline, "   ")).toEqual({ description: "" });
    expect(displayFormatDescriptionWrite(baseline, " Folder list ")).toBe("unchanged");
    expect(displayFormatDescriptionWrite({ description: "  " }, "\n")).toBe("unchanged");
    expect(storedDisplayFormatDescription(" a\nb ")).toBe("a b");
  });
});

describe("savedDisplayFormatDescription", () => {
  it("accepts a description when name, label, columns, and communities stay", () => {
    const sent = displayFormatDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedDisplayFormatDescription(sent, baseline, {
        DisplayFormat: { ...baseline, description: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank description that clears and keeps the other fields", () => {
    const sent = displayFormatDescriptionWrite(baseline, "");
    expect(sent).toEqual({ description: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedDisplayFormatDescription(sent, baseline, { ...baseline, description: "" })).toBe(
      "",
    );
  });

  it("rejects a response that changes the name, drops columns, or changes communities", () => {
    const sent = { description: "note" };
    expect(
      savedDisplayFormatDescription(sent, baseline, { ...baseline, description: "note", name: "Other" }),
    ).toBeNull();
    expect(
      savedDisplayFormatDescription(sent, baseline, {
        ...baseline,
        description: "note",
        columns: [{ source: "sys_title" }],
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatDescription(sent, baseline, {
        ...baseline,
        description: "note",
        allowedCommunities: [],
      }),
    ).toBeNull();
    expect(
      savedDisplayFormatDescription(
        { description: "note", columns: baseline.columns },
        baseline,
        { ...baseline, description: "note" },
      ),
    ).toBeNull();
  });
});
