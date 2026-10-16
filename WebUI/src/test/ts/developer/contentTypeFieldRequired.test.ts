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
  contentTypeFieldRequiredWrite,
  contentTypeWithFieldRequired,
  localContentTypeFields,
  savedContentTypeFieldRequired,
} from "../../../main/ts/developer/contentTypeFieldRequired";

const baseline: ContentTypeDetail = {
  name: "percPage",
  label: "Page",
  description: "Page type",
  enabled: true,
  fields: [
    { name: "sys_title", fieldType: "system", label: "Title:", required: true, occurrence: "required" },
    { name: "rx_shared", fieldType: "shared", label: "Shared:", required: false, occurrence: "optional" },
    { name: "rx_note", fieldType: "local", label: "Note:", required: false, occurrence: "optional" },
    { name: "rx_other", fieldType: "local", label: "Other:", required: true, occurrence: "required" },
  ],
};

describe("localContentTypeFields", () => {
  it("lists only local fields", () => {
    expect(localContentTypeFields(baseline).map((field) => field.name)).toEqual([
      "rx_note",
      "rx_other",
    ]);
    expect(localContentTypeFields({ fields: [{ name: "sys_title", fieldType: "system" }] })).toEqual(
      [],
    );
  });
});

describe("contentTypeFieldRequiredWrite", () => {
  it("sends one local field required and omits the label, description, and other fields", () => {
    const sent = contentTypeFieldRequiredWrite(baseline, "rx_note", true);
    expect(sent).toEqual({ fields: [{ name: "rx_note", required: true }] });
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("enabled");
    expect(sent).not.toHaveProperty("name");
  });

  it("clears required and skips an unchanged flag", () => {
    expect(contentTypeFieldRequiredWrite(baseline, "rx_other", false)).toEqual({
      fields: [{ name: "rx_other", required: false }],
    });
    expect(contentTypeFieldRequiredWrite(baseline, "rx_note", false)).toBe("unchanged");
    expect(contentTypeFieldRequiredWrite(baseline, "rx_other", true)).toBe("unchanged");
  });

  it("does not write shared, system, or missing fields", () => {
    expect(contentTypeFieldRequiredWrite(baseline, "sys_title", false)).toBe("not-local");
    expect(contentTypeFieldRequiredWrite(baseline, "rx_shared", true)).toBe("not-local");
    expect(contentTypeFieldRequiredWrite(baseline, "missing", true)).toBe("missing");
    expect(contentTypeFieldRequiredWrite(baseline, "  ", true)).toBe("missing");
  });
});

describe("savedContentTypeFieldRequired", () => {
  it("accepts the sent flag when the label, description, and other fields stay", () => {
    const sent = contentTypeFieldRequiredWrite(baseline, "rx_note", true);
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged" || sent === "not-local" || sent === "missing") {
      return;
    }
    const accepted = savedContentTypeFieldRequired(sent, baseline, {
      ContentTypeDetail: {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { name: "rx_note", fieldType: "local", label: "Note:", required: true, occurrence: "required" },
          baseline.fields?.[3],
        ],
      },
    });
    expect(accepted).toEqual({ name: "rx_note", required: true, occurrence: "required" });
    expect(contentTypeWithFieldRequired(baseline, accepted!).fields?.[2]?.required).toBe(true);
    expect(contentTypeWithFieldRequired(baseline, accepted!).fields?.[3]?.required).toBe(true);
    expect(contentTypeWithFieldRequired(baseline, accepted!).label).toBe("Page");
    expect(contentTypeWithFieldRequired(baseline, accepted!).description).toBe("Page type");
  });

  it("accepts a cleared flag", () => {
    const sent = contentTypeFieldRequiredWrite(baseline, "rx_other", false);
    if (sent === "unchanged" || sent === "not-local" || sent === "missing") {
      return;
    }
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          baseline.fields?.[2],
          {
            name: "rx_other",
            fieldType: "local",
            label: "Other:",
            required: false,
            occurrence: "optional",
          },
        ],
      }),
    ).toEqual({ name: "rx_other", required: false, occurrence: "optional" });
  });

  it("rejects a response that changes the label, description, enabled flag, or another field", () => {
    const sent = { fields: [{ name: "rx_note", required: true }] };
    expect(
      savedContentTypeFieldRequired(sent, baseline, { ...baseline, label: "Pages" }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, { ...baseline, description: "changed" }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, { ...baseline, enabled: false }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [{ name: "rx_note", fieldType: "local", required: true }],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          { ...baseline.fields?.[1], required: true },
          { name: "rx_note", fieldType: "local", label: "Note:", required: true, occurrence: "required" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(
        { fields: [{ name: "rx_note", required: true }], label: "Page" },
        baseline,
        baseline,
      ),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(
        { fields: [{ name: "rx_note", required: true, searchable: false }] },
        baseline,
        baseline,
      ),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { name: "rx_note", fieldType: "local", label: "Note:", required: true, occurrence: "optional" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { name: "rx_note", fieldType: "local", label: "Renamed:", required: true, occurrence: "required" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
    expect(
      savedContentTypeFieldRequired(sent, baseline, {
        ...baseline,
        fields: [
          baseline.fields?.[0],
          baseline.fields?.[1],
          { name: "rx_note", fieldType: "system", label: "Note:", required: true, occurrence: "required" },
          baseline.fields?.[3],
        ],
      }),
    ).toBeNull();
  });
});
