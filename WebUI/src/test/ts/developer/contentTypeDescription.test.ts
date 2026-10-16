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
import type { ContentTypeDetail } from "../../../main/ts/api/developer/types";
import {
  contentTypeDescriptionWrite,
  savedContentTypeDescription,
  storedContentTypeDescription,
} from "../../../main/ts/developer/contentTypeDescription";

const baseline: ContentTypeDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  fields: [{ name: "sys_title" }, { name: "title" }],
};

describe("contentTypeDescriptionWrite", () => {
  it("sends the description only and omits label and enabled", () => {
    const sent = contentTypeDescriptionWrite(baseline, " note ");
    expect(sent).toEqual({ description: "note" });
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("enabled");
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("fields");
  });

  it("sends an empty description to clear and skips an unchanged description", () => {
    expect(contentTypeDescriptionWrite(baseline, "   ")).toEqual({ description: "" });
    expect(contentTypeDescriptionWrite(baseline, " Page type ")).toBe("unchanged");
    expect(contentTypeDescriptionWrite({ description: "  " }, "\n")).toBe("unchanged");
    expect(storedContentTypeDescription(" a\nb ")).toBe("a b");
  });
});

describe("savedContentTypeDescription", () => {
  it("accepts a description when label, enabled, name, and fields stay", () => {
    const sent = contentTypeDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedContentTypeDescription(sent, baseline, {
        ContentTypeDetail: { ...baseline, description: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank description that clears and keeps label and enabled", () => {
    const sent = contentTypeDescriptionWrite(baseline, "");
    expect(sent).toEqual({ description: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedContentTypeDescription(sent, baseline, { ...baseline, description: "" })).toBe("");
  });

  it("rejects a response that changes the label, enabled flag, or fields", () => {
    const sent = { description: "note" };
    expect(
      savedContentTypeDescription(sent, baseline, { ...baseline, description: "note", label: "Pages" }),
    ).toBeNull();
    expect(
      savedContentTypeDescription(sent, baseline, { ...baseline, description: "note", enabled: false }),
    ).toBeNull();
    expect(
      savedContentTypeDescription(sent, baseline, {
        ...baseline,
        description: "note",
        fields: [{ name: "sys_title" }],
      }),
    ).toBeNull();
    expect(
      savedContentTypeDescription(
        { description: "note", label: "Page" },
        baseline,
        { ...baseline, description: "note" },
      ),
    ).toBeNull();
  });
});
