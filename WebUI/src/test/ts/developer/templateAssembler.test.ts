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
  TEMPLATE_ASSEMBLER_LABELS,
  templateAssemblerLabel,
  templateAssemblerShortId,
} from "../../../main/ts/developer/templateAssembler";

describe("templateAssemblerShortId", () => {
  it("resolves full extension paths to the short id", () => {
    expect(
      templateAssemblerShortId("Java/global/percussion/assembly/htmlAssembler"),
    ).toBe("htmlAssembler");
    expect(
      templateAssemblerShortId("Java/global/percussion/assembly/velocityAssembler"),
    ).toBe("velocityAssembler");
  });

  it("passes short ids through and trims surrounding space", () => {
    expect(templateAssemblerShortId("  markdownAssembler  ")).toBe("markdownAssembler");
  });

  it("returns null for blank, missing, or non-string values", () => {
    expect(templateAssemblerShortId("")).toBeNull();
    expect(templateAssemblerShortId("   ")).toBeNull();
    expect(templateAssemblerShortId(null)).toBeNull();
    expect(templateAssemblerShortId(undefined)).toBeNull();
    expect(templateAssemblerShortId(42)).toBeNull();
  });
});

describe("templateAssemblerLabel", () => {
  it("labels every registered catalog assembler", () => {
    expect(templateAssemblerLabel("velocityAssembler")).toBe("Velocity (power path)");
    expect(templateAssemblerLabel("htmlAssembler")).toBe("HTML-first");
    expect(templateAssemblerLabel("markdownAssembler")).toBe("Markdown");
    expect(templateAssemblerLabel("legacyAssembler")).toBe("Legacy XSL");
    expect(templateAssemblerLabel("pageAssembler")).toBe("Page");
    expect(templateAssemblerLabel("pageVariantAssembler")).toBe("Page variant");
    expect(templateAssemblerLabel("pageDatabaseAssembler")).toBe("Page database");
    expect(templateAssemblerLabel("binaryAssembler")).toBe("Binary");
    expect(templateAssemblerLabel("dispatchAssembler")).toBe("Dispatch");
    expect(templateAssemblerLabel("databaseAssembler")).toBe("Database");
    expect(templateAssemblerLabel("resourceAssembler")).toBe("Resource");
    expect(templateAssemblerLabel("debugAssembler")).toBe("Debug");
  });

  it("labels full extension paths via the short id", () => {
    expect(
      templateAssemblerLabel("Java/global/percussion/assembly/markdownAssembler"),
    ).toBe("Markdown");
    expect(
      templateAssemblerLabel("Java/global/percussion/assembly/legacyAssembler"),
    ).toBe("Legacy XSL");
  });

  it("falls back to the raw value for unknown assemblers", () => {
    expect(templateAssemblerLabel("Java/global/percussion/assembly/customAssembler")).toBe(
      "Java/global/percussion/assembly/customAssembler",
    );
    expect(templateAssemblerLabel("  customAssembler  ")).toBe("customAssembler");
  });

  it("returns null for blank, missing, or non-string values", () => {
    expect(templateAssemblerLabel("")).toBeNull();
    expect(templateAssemblerLabel("   ")).toBeNull();
    expect(templateAssemblerLabel(null)).toBeNull();
    expect(templateAssemblerLabel(undefined)).toBeNull();
    expect(templateAssemblerLabel(42)).toBeNull();
  });

  it("covers every catalog key with a non-blank label", () => {
    for (const [id, label] of Object.entries(TEMPLATE_ASSEMBLER_LABELS)) {
      expect(id.trim().length).toBeGreaterThan(0);
      expect(label.trim().length).toBeGreaterThan(0);
      expect(templateAssemblerLabel(id)).toBe(label);
    }
  });
});
