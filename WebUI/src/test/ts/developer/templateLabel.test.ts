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
import type { TemplateDetail } from "../../../main/ts/api/developer/types";
import {
  savedTemplateLabel,
  storedTemplateLabel,
  templateLabelWrite,
} from "../../../main/ts/developer/templateLabel";

const baseline: TemplateDetail = {
  name: "qa5407tpl",
  label: "QA template",
  description: "Folder list",
  assembler: "Java/global/percussion/assembly/htmlAssembler",
  templateSource: "<p>stay</p>",
  bindings: [{ executionOrder: 1, variable: "$qa", expression: "1" }],
  slots: [{ name: "target", label: "Target" }],
  associatedContentTypes: [{ name: "percPage", guid: { stringValue: "0-6-311" } }],
};

describe("templateLabelWrite", () => {
  it("sends the label only and omits fields that must stay", () => {
    const sent = templateLabelWrite(baseline, " note ");
    expect(sent).toEqual({ label: "note" });
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("templateSource");
    expect(sent).not.toHaveProperty("bindings");
    expect(sent).not.toHaveProperty("slots");
    expect(sent).not.toHaveProperty("associatedContentTypes");
    expect(sent).not.toHaveProperty("name");
  });

  it("sends an empty label and skips an unchanged label", () => {
    expect(templateLabelWrite(baseline, "   ")).toEqual({ label: "" });
    expect(templateLabelWrite(baseline, " QA template ")).toBe("unchanged");
    expect(templateLabelWrite({ label: "  " }, "\n")).toBe("unchanged");
    expect(storedTemplateLabel(" a\nb ")).toBe("a b");
  });
});

describe("savedTemplateLabel", () => {
  it("accepts a label when name, description, source, bindings, slots, and content types stay", () => {
    const sent = templateLabelWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedTemplateLabel(sent, baseline, {
        TemplateDetail: { ...baseline, label: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank label that keeps the name and the description", () => {
    const sent = templateLabelWrite(baseline, "");
    expect(sent).toEqual({ label: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedTemplateLabel(sent, baseline, { ...baseline, label: "" })).toBe("");
    expect(savedTemplateLabel(sent, baseline, { ...baseline, label: baseline.name })).toBe(
      "qa5407tpl",
    );
  });

  it("rejects a blank label that clears the name or changes the description", () => {
    const sent = { label: "" };
    expect(savedTemplateLabel(sent, baseline, { ...baseline, label: "", name: "" })).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, { ...baseline, label: "", name: "other" }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, {
        ...baseline,
        label: baseline.name,
        description: "changed",
      }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, { ...baseline, label: "Some other label" }),
    ).toBeNull();
  });

  it("rejects a response that changes the name, drops bindings, or sends another field", () => {
    const sent = { label: "note" };
    expect(savedTemplateLabel(sent, baseline, { ...baseline, label: "note", name: "other" })).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, { ...baseline, label: "note", description: "changed" }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, {
        ...baseline,
        label: "note",
        bindings: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, {
        ...baseline,
        label: "note",
        templateSource: "<p>changed</p>",
      }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, {
        ...baseline,
        label: "note",
        slots: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateLabel(sent, baseline, {
        ...baseline,
        label: "note",
        associatedContentTypes: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateLabel(
        { label: "note", description: "Folder list" },
        baseline,
        { ...baseline, label: "note" },
      ),
    ).toBeNull();
  });

  it("treats one Jackson binding object as the same binding", () => {
    const sent = { label: "note" };
    const singleton = {
      ...baseline,
      label: "note",
      bindings: { executionOrder: 0, variable: "$qa", expression: 1 },
    };
    expect(savedTemplateLabel(sent, singleton, singleton)).toBe("note");
    expect(savedTemplateLabel(sent, singleton, { ...singleton, bindings: [] })).toBeNull();
  });
});
