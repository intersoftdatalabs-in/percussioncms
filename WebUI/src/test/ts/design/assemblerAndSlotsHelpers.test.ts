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
  ASSEMBLER_OPTIONS,
  assemblerSelectOptions,
  isValidAssemblerValue,
} from "../../../main/ts/design/assemblerOptions";
import {
  layoutDraftFromMap,
  layoutMapFromDraft,
  stylesDraftFromMap,
  stylesMapFromDraft,
  SLOT_SCHEMA_VERSION,
  templateSlotKey,
} from "../../../main/ts/design/slotLayoutStyles";

describe("assemblerOptions (#2810)", () => {
  it("includes html, markdown, and velocity assemblers", () => {
    const values = ASSEMBLER_OPTIONS.map((o) => o.value);
    expect(values.some((v) => v.endsWith("htmlAssembler"))).toBe(true);
    expect(values.some((v) => v.endsWith("markdownAssembler"))).toBe(true);
    expect(values.some((v) => v.endsWith("velocityAssembler"))).toBe(true);
  });

  it("prepends custom current value when outside catalog", () => {
    const opts = assemblerSelectOptions("Java/custom/myAssembler");
    expect(opts[0]?.value).toBe("Java/custom/myAssembler");
    expect(opts.length).toBe(ASSEMBLER_OPTIONS.length + 1);
  });

  it("does not duplicate when current is known", () => {
    const known = ASSEMBLER_OPTIONS[0]!.value;
    const opts = assemblerSelectOptions(known);
    expect(opts.filter((o) => o.value === known)).toHaveLength(1);
    expect(opts.length).toBe(ASSEMBLER_OPTIONS.length);
  });

  it("validates non-empty assembler", () => {
    expect(isValidAssemblerValue("  ")).toBe(false);
    expect(isValidAssemblerValue("Java/global/percussion/assembly/htmlAssembler")).toBe(
      true,
    );
  });
});

describe("slotLayoutStyles (#2810)", () => {
  it("round-trips layout draft and stamps schemaVersion", () => {
    const draft = layoutDraftFromMap({
      schemaVersion: 1,
      orientation: "horizontal",
      columns: "3",
      maxItems: "10",
    });
    expect(draft.orientation).toBe("horizontal");
    expect(draft.columns).toBe("3");
    const map = layoutMapFromDraft(draft);
    expect(map.schemaVersion).toBe(SLOT_SCHEMA_VERSION);
    expect(map.orientation).toBe("horizontal");
    expect(map.columns).toBe("3");
    expect(map.maxItems).toBe("10");
  });

  it("round-trips styles draft", () => {
    const draft = stylesDraftFromMap({ rootclass: "my-root", itemclass: "my-item" });
    expect(draft.rootclass).toBe("my-root");
    const map = stylesMapFromDraft(draft);
    expect(map.schemaVersion).toBe(SLOT_SCHEMA_VERSION);
    expect(map.rootclass).toBe("my-root");
    expect(map.itemclass).toBe("my-item");
  });

  it("omits blank structural keys", () => {
    const map = layoutMapFromDraft({
      orientation: "",
      columns: "  ",
      maxItems: "",
      emptyState: "",
      wrapperClassPolicy: "",
    });
    expect(Object.keys(map)).toEqual(["schemaVersion"]);
  });

  it("templateSlotKey prefers name", () => {
    expect(templateSlotKey({ name: "main", guid: { stringValue: "0-10-1" } })).toBe(
      "main",
    );
    expect(templateSlotKey({ guid: { stringValue: "0-10-1" } })).toBe("0-10-1");
    expect(templateSlotKey({})).toBeNull();
  });
});
