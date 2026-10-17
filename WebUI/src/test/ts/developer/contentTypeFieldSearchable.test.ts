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
  contentTypeFieldSearchableWrite,
  contentTypeSearchableFields,
  contentTypeWithFieldSearchable,
  savedContentTypeFieldSearchable,
} from "../../../main/ts/developer/contentTypeFieldSearchable";

const baseline: ContentTypeDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  fields: [
    {
      name: "sys_title",
      fieldType: "system",
      label: "Title:",
      searchable: true,
      required: true,
      occurrence: "required",
    },
    {
      name: "rx_shared",
      fieldType: "shared",
      label: "Shared:",
      searchable: false,
      required: false,
      occurrence: "optional",
    },
    {
      name: "rx_note",
      fieldType: "local",
      label: "Note:",
      searchable: false,
      required: false,
      occurrence: "optional",
    },
    {
      name: "rx_other",
      fieldType: "local",
      label: "Other:",
      searchable: true,
      required: true,
      occurrence: "required",
    },
  ],
};

describe("contentTypeSearchableFields", () => {
  it("lists system, shared, and local fields", () => {
    expect(contentTypeSearchableFields(baseline).map((field) => field.name)).toEqual([
      "sys_title",
      "rx_shared",
      "rx_note",
      "rx_other",
    ]);
    expect(contentTypeSearchableFields({ fields: [] })).toEqual([]);
  });
});

describe("contentTypeFieldSearchableWrite", () => {
  it("sends one field searchable and omits the label, description, and other fields", () => {
    const sent = contentTypeFieldSearchableWrite(baseline, "rx_note", true);
    expect(sent).toEqual({ fields: [{ name: "rx_note", searchable: true }] });
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("enabled");
    expect(sent).not.toHaveProperty("name");
  });

  it("clears searchable on system and shared fields and skips an unchanged flag", () => {
    expect(contentTypeFieldSearchableWrite(baseline, "sys_title", false)).toEqual({
      fields: [{ name: "sys_title", searchable: false }],
    });
    expect(contentTypeFieldSearchableWrite(baseline, "rx_shared", true)).toEqual({
      fields: [{ name: "rx_shared", searchable: true }],
    });
    expect(contentTypeFieldSearchableWrite(baseline, "rx_other", false)).toEqual({
      fields: [{ name: "rx_other", searchable: false }],
    });
    expect(contentTypeFieldSearchableWrite(baseline, "rx_note", false)).toBe("unchanged");
    expect(contentTypeFieldSearchableWrite(baseline, "sys_title", true)).toBe("unchanged");
  });

  it("does not write a missing field", () => {
    expect(contentTypeFieldSearchableWrite(baseline, "missing", true)).toBe("missing");
    expect(contentTypeFieldSearchableWrite(baseline, "  ", true)).toBe("missing");
  });
});

describe("savedContentTypeFieldSearchable", () => {
  it("accepts the sent flag when the label, description, and other fields stay", () => {
    const sent = contentTypeFieldSearchableWrite(baseline, "rx_note", true);
    if (sent === "unchanged" || sent === "missing") {
      throw new Error("expected a write");
    }
    const accepted = savedContentTypeFieldSearchable(sent, baseline, {
      ContentTypeDetail: {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { ...baseline.fields?.[2], searchable: true },
          baseline.fields?.[3],
        ],
      },
    });
    expect(accepted).toEqual({ name: "rx_note", searchable: true });
    const next = contentTypeWithFieldSearchable(baseline, accepted!);
    expect(next.fields?.[2]?.searchable).toBe(true);
    expect(next.fields?.[3]?.searchable).toBe(true);
    expect(next.fields?.[1]?.searchable).toBe(false);
    expect(next.fields?.[2]?.required).toBe(false);
    expect(next.label).toBe("Page");
    expect(next.description).toBe("Page type");
  });

  it("accepts a cleared flag", () => {
    const sent = contentTypeFieldSearchableWrite(baseline, "rx_other", false);
    if (sent === "unchanged" || sent === "missing") {
      throw new Error("expected a write");
    }
    expect(
      savedContentTypeFieldSearchable(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          baseline.fields?.[2],
          { ...baseline.fields?.[3], searchable: false },
        ],
      }),
    ).toEqual({ name: "rx_other", searchable: false });
  });

  it("rejects a response that changes the label, another field, or required", () => {
    const sent = { fields: [{ name: "rx_note", searchable: true }] };
    expect(savedContentTypeFieldSearchable(sent, baseline, { ...baseline, label: "Pages" })).toBeNull();
    expect(
      savedContentTypeFieldSearchable(sent, baseline, { ...baseline, description: "changed" }),
    ).toBeNull();
    expect(savedContentTypeFieldSearchable(sent, baseline, { ...baseline, enabled: false })).toBeNull();
    expect(
      savedContentTypeFieldSearchable(sent, baseline, {
        ...baseline,
        fields: [{ name: "rx_note", fieldType: "local", searchable: true }],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldSearchable(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          { ...baseline.fields?.[1], searchable: true },
          { ...baseline.fields?.[2], searchable: true },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldSearchable(
        { fields: [{ name: "rx_note", searchable: true }], label: "Page" },
        baseline,
        baseline,
      ),
    ).toBeNull();
    expect(
      savedContentTypeFieldSearchable(
        { fields: [{ name: "rx_note", searchable: true, required: true }] },
        baseline,
        baseline,
      ),
    ).toBeNull();
    expect(
      savedContentTypeFieldSearchable(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { ...baseline.fields?.[2], searchable: true, required: true, occurrence: "required" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldSearchable(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { ...baseline.fields?.[2], searchable: true, label: "Renamed:" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
  });
});
