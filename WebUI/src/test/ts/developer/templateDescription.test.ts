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
  savedTemplateDescription,
  storedTemplateDescription,
  templateDescriptionWrite,
} from "../../../main/ts/developer/templateDescription";

const baseline: TemplateDetail = {
  name: "qa5409tpl",
  label: "QA template",
  description: "Folder list",
  assembler: "Java/global/percussion/assembly/htmlAssembler",
  templateSource: "<p>stay</p>",
  bindings: [{ executionOrder: 1, variable: "$qa", expression: "1" }],
  slots: [{ name: "target", label: "Target" }],
  associatedContentTypes: [{ name: "percPage", guid: { stringValue: "0-6-311" } }],
};

describe("templateDescriptionWrite", () => {
  it("sends the description only and omits fields that must stay", () => {
    const sent = templateDescriptionWrite(baseline, " note ");
    expect(sent).toEqual({ description: "note" });
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("templateSource");
    expect(sent).not.toHaveProperty("bindings");
    expect(sent).not.toHaveProperty("slots");
    expect(sent).not.toHaveProperty("associatedContentTypes");
    expect(sent).not.toHaveProperty("name");
  });

  it("sends an empty description to clear and skips an unchanged description", () => {
    expect(templateDescriptionWrite(baseline, "   ")).toEqual({ description: "" });
    expect(templateDescriptionWrite(baseline, " Folder list ")).toBe("unchanged");
    expect(templateDescriptionWrite({ description: "  " }, "\n")).toBe("unchanged");
    expect(storedTemplateDescription(" a\nb ")).toBe("a b");
  });
});

describe("savedTemplateDescription", () => {
  it("accepts a description when name, label, source, bindings, slots, and content types stay", () => {
    const sent = templateDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedTemplateDescription(sent, baseline, {
        TemplateDetail: { ...baseline, description: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank description that clears and keeps the other fields", () => {
    const sent = templateDescriptionWrite(baseline, "");
    expect(sent).toEqual({ description: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedTemplateDescription(sent, baseline, { ...baseline, description: "" })).toBe("");
  });

  it("rejects a response that changes the label, drops bindings, or sends another field", () => {
    const sent = { description: "note" };
    expect(
      savedTemplateDescription(sent, baseline, { ...baseline, description: "note", label: "Other" }),
    ).toBeNull();
    expect(
      savedTemplateDescription(sent, baseline, {
        ...baseline,
        description: "note",
        bindings: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateDescription(sent, baseline, {
        ...baseline,
        description: "note",
        templateSource: "<p>changed</p>",
      }),
    ).toBeNull();
    expect(
      savedTemplateDescription(sent, baseline, {
        ...baseline,
        description: "note",
        slots: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateDescription(sent, baseline, {
        ...baseline,
        description: "note",
        associatedContentTypes: [],
      }),
    ).toBeNull();
    expect(
      savedTemplateDescription(
        { description: "note", label: "QA template" },
        baseline,
        { ...baseline, description: "note" },
      ),
    ).toBeNull();
  });

  it("treats one Jackson binding object as the same binding", () => {
    const sent = { description: "note" };
    const singleton = {
      ...baseline,
      description: "note",
      bindings: { executionOrder: 0, variable: "$qa", expression: 1 },
    };
    expect(savedTemplateDescription(sent, singleton, singleton)).toBe("note");
    expect(
      savedTemplateDescription(sent, singleton, { ...singleton, bindings: [] }),
    ).toBeNull();
  });
});
