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
  contentTypeLabelWrite,
  savedContentTypeLabel,
  shownContentTypeLabel,
  storedContentTypeLabel,
} from "../../../main/ts/developer/contentTypeLabel";

const baseline: ContentTypeDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  fields: [{ name: "sys_title" }, { name: "title" }],
};

describe("contentTypeLabelWrite", () => {
  it("sends the label only and omits description and enabled", () => {
    const sent = contentTypeLabelWrite(baseline, " note ");
    expect(sent).toEqual({ label: "note" });
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("enabled");
    expect(sent).not.toHaveProperty("name");
    expect(sent).not.toHaveProperty("fields");
  });

  it("sends an empty label and skips an unchanged label", () => {
    expect(contentTypeLabelWrite(baseline, "   ")).toEqual({ label: "" });
    expect(contentTypeLabelWrite(baseline, " Page ")).toBe("unchanged");
    expect(contentTypeLabelWrite({ label: "  " }, "\n")).toBe("unchanged");
    expect(storedContentTypeLabel(" a\nb ")).toBe("a b");
    expect(shownContentTypeLabel("", "percPage")).toBe("percPage");
    expect(shownContentTypeLabel("Page", "percPage")).toBe("Page");
  });
});

describe("savedContentTypeLabel", () => {
  it("accepts a label when description, enabled, name, and fields stay", () => {
    const sent = contentTypeLabelWrite(baseline, "Pages");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedContentTypeLabel(sent, baseline, {
        ContentTypeDetail: { ...baseline, label: " Pages " },
      }),
    ).toBe("Pages");
  });

  it("accepts a blank label that echoes the name and does not wipe it", () => {
    const sent = contentTypeLabelWrite(baseline, "");
    expect(sent).toEqual({ label: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedContentTypeLabel(sent, baseline, { ...baseline, label: "" })).toBe("percPage");
    expect(savedContentTypeLabel(sent, baseline, { ...baseline, label: "percPage" })).toBe(
      "percPage",
    );
  });

  it("rejects a blank label that clears the name or changes description or enabled", () => {
    const sent = { label: "" };
    expect(savedContentTypeLabel(sent, baseline, { ...baseline, label: "", name: "" })).toBeNull();
    expect(
      savedContentTypeLabel(sent, baseline, { ...baseline, label: "", name: "other" }),
    ).toBeNull();
    expect(
      savedContentTypeLabel(sent, baseline, { ...baseline, label: "Hacked" }),
    ).toBeNull();
    expect(
      savedContentTypeLabel(sent, baseline, {
        ...baseline,
        label: "",
        description: "changed",
      }),
    ).toBeNull();
    expect(
      savedContentTypeLabel(sent, baseline, { ...baseline, label: "", enabled: false }),
    ).toBeNull();
    expect(
      savedContentTypeLabel(
        { label: "", description: "Page type" },
        baseline,
        { ...baseline, label: "" },
      ),
    ).toBeNull();
  });
});
