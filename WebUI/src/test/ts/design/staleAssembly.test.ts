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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { beforeEach, describe, expect, it } from "vitest";
import {
  STALE_ASSEMBLY_MAX_ENTRIES,
  STALE_ASSEMBLY_STORAGE_KEY,
  clearAllTemplateAssemblyStale,
  clearTemplateAssemblyStale,
  isTemplateAssemblyStale,
  listStaleAssemblyTemplates,
  markTemplateAssemblySaved,
  markTemplateAssemblyUnsaved,
  normalizeStaleTemplateName,
  staleAssemblyStateFor,
} from "../../../main/ts/design/staleAssembly";

describe("staleAssembly session registry (#6478)", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("normalizes names and rejects blanks", () => {
    expect(normalizeStaleTemplateName("  MyTemplate ")).toBe("MyTemplate");
    expect(normalizeStaleTemplateName(null)).toBe("");
    expect(normalizeStaleTemplateName("   ")).toBe("");
    expect(markTemplateAssemblyUnsaved("   ")).toBe(false);
    expect(markTemplateAssemblySaved(undefined)).toBe(false);
    expect(clearTemplateAssemblyStale("")).toBe(false);
    expect(listStaleAssemblyTemplates()).toEqual([]);
  });

  it("marks unsaved edits and reads them back", () => {
    expect(markTemplateAssemblyUnsaved("HeroTemplate")).toBe(true);
    expect(isTemplateAssemblyStale("HeroTemplate")).toBe(true);
    expect(isTemplateAssemblyStale("OtherTemplate")).toBe(false);
    expect(staleAssemblyStateFor("HeroTemplate")).toBe("unsaved");
    expect(staleAssemblyStateFor("Missing")).toBeNull();
    expect(listStaleAssemblyTemplates()).toEqual([
      { name: "HeroTemplate", state: "unsaved" },
    ]);
  });

  it("a save upgrades unsaved but never downgrades saved", () => {
    markTemplateAssemblyUnsaved("A");
    markTemplateAssemblyUnsaved("B");
    expect(markTemplateAssemblySaved("A")).toBe(true);
    expect(listStaleAssemblyTemplates()).toEqual([
      { name: "A", state: "saved" },
      { name: "B", state: "unsaved" },
    ]);
    // Further dirty edits keep the saved (pending-republish) mark.
    expect(markTemplateAssemblyUnsaved("A")).toBe(true);
    expect(staleAssemblyStateFor("A")).toBe("saved");
  });

  it("clearing forgets one template and keeps the rest", () => {
    markTemplateAssemblySaved("A");
    markTemplateAssemblyUnsaved("B");
    expect(clearTemplateAssemblyStale("Missing")).toBe(true);
    expect(clearTemplateAssemblyStale("A")).toBe(true);
    expect(listStaleAssemblyTemplates()).toEqual([
      { name: "B", state: "unsaved" },
    ]);
    clearAllTemplateAssemblyStale();
    expect(listStaleAssemblyTemplates()).toEqual([]);
  });

  it("tolerates corrupt or foreign storage payloads", () => {
    window.localStorage.setItem(STALE_ASSEMBLY_STORAGE_KEY, "not-json{{{");
    expect(listStaleAssemblyTemplates()).toEqual([]);
    window.localStorage.setItem(
      STALE_ASSEMBLY_STORAGE_KEY,
      JSON.stringify({
        Good: "saved",
        BadState: "published",
        "  ": "unsaved",
        Nested: { state: "saved" },
      }),
    );
    expect(listStaleAssemblyTemplates()).toEqual([
      { name: "Good", state: "saved" },
    ]);
    window.localStorage.setItem(STALE_ASSEMBLY_STORAGE_KEY, JSON.stringify([]));
    expect(listStaleAssemblyTemplates()).toEqual([]);
  });

  it("caps entries so long sessions stay bounded", () => {
    for (let i = 0; i < STALE_ASSEMBLY_MAX_ENTRIES + 5; i++) {
      markTemplateAssemblySaved(`Template${i}`);
    }
    const listed = listStaleAssemblyTemplates();
    expect(listed).toHaveLength(STALE_ASSEMBLY_MAX_ENTRIES);
    expect(listed[0]).toEqual({ name: "Template5", state: "saved" });
    expect(isTemplateAssemblyStale("Template0")).toBe(false);
  });
});
